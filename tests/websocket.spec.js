import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
// 注意：setup 只能静态导入一次。若在 vi.resetModules() 之后再动态 import，会重新执行 setup.js，
// 把 globalThis.uni 换成全新的 mock 对象，导致测试持有的 uniMock 与被测代码调用的不是同一个
import { clearStorage } from './setup'

const uniMock = globalThis.uni

/** 模拟 SocketTask：记录回调，测试里手动触发 open / message / close */
function makeTask() {
  const handlers = {}
  return {
    handlers,
    onOpen: vi.fn((cb) => (handlers.open = cb)),
    onMessage: vi.fn((cb) => (handlers.message = cb)),
    onClose: vi.fn((cb) => (handlers.close = cb)),
    onError: vi.fn((cb) => (handlers.error = cb)),
    send: vi.fn(),
    close: vi.fn(),
  }
}

/** websocket.ts 持有大量模块级状态（连接、重连计数、定时器），每个用例重新加载以保证互不干扰 */
async function loadModule() {
  vi.resetModules()
  const ws = await import('@/utils/websocket')
  const { useUserStore } = await import('@/stores/user')
  return { ws, useUserStore }
}

let ws
let task

beforeEach(async () => {
  vi.useFakeTimers()
  vi.clearAllMocks()
  clearStorage()
  const mod = await loadModule()
  setActivePinia(createPinia())
  ws = mod.ws
  mod.useUserStore().updateUser({ id: 7, token: 'tk-7', role: 'STUDENT' })
  task = makeTask()
  uniMock.connectSocket.mockReturnValue(task)
})

afterEach(() => {
  ws.closeWs()
  vi.useRealTimers()
})

describe('connectWs 建连', () => {
  it('把 token 拼进路径、http 换成 ws，并绑定任务级回调', () => {
    ws.connectWs()
    const { url } = uniMock.connectSocket.mock.calls[0][0]
    expect(url).toMatch(/^ws:\/\/.+\/ws\/notice\/tk-7$/)
    expect(task.onOpen).toHaveBeenCalled()
    expect(task.onMessage).toHaveBeenCalled()
  })

  it('幂等：已在连接中时重复调用不会重复建连', () => {
    ws.connectWs()
    ws.connectWs()
    ws.connectWs()
    expect(uniMock.connectSocket).toHaveBeenCalledTimes(1)
  })

  it('无 token（未登录）时不建连', async () => {
    const mod = await loadModule()
    setActivePinia(createPinia())
    mod.useUserStore().clearUser()
    mod.ws.connectWs()
    expect(uniMock.connectSocket).not.toHaveBeenCalled()
  })

  it('真机返回 Promise 而非 SocketTask 时：回退到 uni 全局回调模式，不报错', () => {
    uniMock.connectSocket.mockReturnValue(Promise.resolve())
    expect(() => ws.connectWs()).not.toThrow()
    expect(uniMock.onSocketOpen).toHaveBeenCalled()
    expect(uniMock.onSocketMessage).toHaveBeenCalled()
  })
})

describe('推送处理', () => {
  beforeEach(() => {
    ws.connectWs()
    task.handlers.open()
  })

  it('合法推送：未读数 +1、首页 tab 角标、广播 ws:push 事件', () => {
    task.handlers.message({ data: JSON.stringify({ title: '成绩发布', content: '高数 90' }) })
    expect(uniMock.setTabBarBadge).toHaveBeenCalledWith(expect.objectContaining({ index: 0, text: '1' }))
    expect(uniMock.$emit).toHaveBeenCalledWith('ws:push', { title: '成绩发布', content: '高数 90' })
  })

  it('未读超过 99 时角标显示 99+', () => {
    for (let i = 0; i < 100; i++) task.handlers.message({ data: JSON.stringify({ title: 't' + i }) })
    const last = uniMock.setTabBarBadge.mock.calls.at(-1)[0]
    expect(last.text).toBe('99+')
  })

  it('非 JSON 或缺少 title 的消息直接忽略', () => {
    task.handlers.message({ data: 'not json' })
    task.handlers.message({ data: JSON.stringify({ content: 'no title' }) })
    expect(uniMock.$emit).not.toHaveBeenCalled()
    expect(uniMock.setTabBarBadge).not.toHaveBeenCalled()
  })

  it('resetWsUnread 清零并移除角标', () => {
    task.handlers.message({ data: JSON.stringify({ title: 'x' }) })
    ws.resetWsUnread()
    expect(uniMock.removeTabBarBadge).toHaveBeenCalled()
  })
})

describe('心跳与半开连接检测', () => {
  it('连上后每 25 秒发送一次 ping', () => {
    ws.connectWs()
    task.handlers.open()
    vi.advanceTimersByTime(25000)
    vi.advanceTimersByTime(25000)
    expect(task.send).toHaveBeenCalledTimes(2)
    expect(task.send.mock.calls[0][0].data).toBe('ping')
  })

  it('超过 5 分钟收不到任何入站消息：判定为僵尸连接，主动断开交给重连', () => {
    ws.connectWs()
    task.handlers.open()
    vi.advanceTimersByTime(300000 + 25000)
    expect(task.close).toHaveBeenCalled()
  })

  it('期间持续有推送进来则不会被误判断开', () => {
    ws.connectWs()
    task.handlers.open()
    for (let i = 0; i < 20; i++) {
      vi.advanceTimersByTime(25000)
      task.handlers.message({ data: JSON.stringify({ title: 'keepalive' + i }) })
    }
    expect(task.close).not.toHaveBeenCalled()
  })
})

describe('断线重连（指数退避）', () => {
  it('断线后按 3s → 6s → 12s 退避重连', () => {
    ws.connectWs()
    const delays = []
    for (let i = 0; i < 3; i++) {
      const before = uniMock.connectSocket.mock.calls.length
      const current = uniMock.connectSocket.mock.results.at(-1).value
      current.handlers.close()
      // 下一个任务
      uniMock.connectSocket.mockReturnValue(makeTask())
      let waited = 0
      while (uniMock.connectSocket.mock.calls.length === before && waited < 60000) {
        vi.advanceTimersByTime(1000)
        waited += 1000
      }
      delays.push(waited)
    }
    expect(delays).toEqual([3000, 6000, 12000])
  })

  it('退避上限 30 秒', () => {
    ws.connectWs()
    let waited = 0
    for (let i = 0; i < 6; i++) {
      const before = uniMock.connectSocket.mock.calls.length
      uniMock.connectSocket.mock.results.at(-1).value.handlers.close()
      uniMock.connectSocket.mockReturnValue(makeTask())
      waited = 0
      while (uniMock.connectSocket.mock.calls.length === before && waited < 120000) {
        vi.advanceTimersByTime(1000)
        waited += 1000
      }
    }
    expect(waited).toBe(30000)
  })

  it('连上后重连计数归零：下次断线重新从 3 秒开始', () => {
    ws.connectWs()
    task.handlers.close()
    const next = makeTask()
    uniMock.connectSocket.mockReturnValue(next)
    vi.advanceTimersByTime(3000)
    next.handlers.open()
    next.handlers.close()
    const before = uniMock.connectSocket.mock.calls.length
    uniMock.connectSocket.mockReturnValue(makeTask())
    vi.advanceTimersByTime(3000)
    expect(uniMock.connectSocket.mock.calls.length).toBe(before + 1)
  })
})

describe('closeWs 主动断开（退出登录 / 改密码）', () => {
  it('关闭连接、停止心跳、清零角标', () => {
    ws.connectWs()
    task.handlers.open()
    ws.closeWs()
    expect(task.close).toHaveBeenCalled()
    expect(uniMock.removeTabBarBadge).toHaveBeenCalled()
    vi.advanceTimersByTime(60000)
    expect(task.send).not.toHaveBeenCalled()
  })

  it('已排队的重连定时器被取消：退出后不会偷偷重新连上', () => {
    ws.connectWs()
    task.handlers.close() // 排队一次 3 秒后的重连
    ws.closeWs()
    vi.advanceTimersByTime(60000)
    expect(uniMock.connectSocket).toHaveBeenCalledTimes(1)
  })
})

describe('initWs 网络恢复补连', () => {
  it('只注册一次网络监听；网络恢复且非主动断开时补连', () => {
    ws.initWs()
    ws.initWs()
    expect(uniMock.onNetworkStatusChange).toHaveBeenCalledTimes(1)
    const onChange = uniMock.onNetworkStatusChange.mock.calls[0][0]
    onChange({ isConnected: true })
    expect(uniMock.connectSocket).toHaveBeenCalledTimes(1)
  })
})

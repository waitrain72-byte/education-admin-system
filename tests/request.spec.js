import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('@/utils/websocket', () => ({ closeWs: vi.fn() }))

const { request, get, getData, resolveFileUrl } = await import('@/utils/request')
const { onLoadingChange, resetLoading } = await import('@/utils/loading')
const { useUserStore } = await import('@/stores/user')
const { closeWs } = await import('@/utils/websocket')
const { clearStorage } = await import('./setup')

const uniMock = globalThis.uni

beforeEach(() => {
  setActivePinia(createPinia())
  clearStorage()
  vi.clearAllMocks()
  // 复位上一用例遗留的加载状态（"最短展示时长"的收起定时器可能仍在排队，visible 还挂在 true）
  resetLoading()
})

const resolveWith = (data) =>
  uniMock.request.mockImplementationOnce((opts) => {
    opts.success({ data })
    opts.complete()
  })

/** 订阅加载状态并收集事件（订阅时会立即收到一次当前状态） */
function trackLoading() {
  const events = []
  const off = onLoadingChange((v) => events.push(v))
  return { events, off }
}

describe('request 统一请求层', () => {
  it('成功请求：携带 token header，拼 baseUrl，直接 resolve 业务数据', async () => {
    const store = useUserStore()
    store.updateUser({ id: 1, token: 'tk-1', role: 'ADMIN' })
    resolveWith({ code: '200', data: { x: 1 } })

    const data = await request({ url: '/thing/selectPage' })

    expect(uniMock.request.mock.calls[0][0].url).toContain('/thing/selectPage')
    expect(uniMock.request.mock.calls[0][0].header.token).toBe('tk-1')
    // 已解包：拿到的就是 data 本身，不再是 { data: { code, data } }
    expect(data).toEqual({ x: 1 })
  })

  it('业务失败（非 200）：统一提示后端文案并 reject ApiError，携带错误码', async () => {
    resolveWith({ code: '5006', msg: '该门课选课人数已满' })

    const err = await request({ url: '/choice/add', method: 'POST' }).catch((e) => e)

    expect(err.name).toBe('ApiError')
    expect(err.code).toBe('5006')
    expect(uniMock.showToast).toHaveBeenCalledTimes(1)
  })

  it('并发请求同时失败：相同提示只弹一次，不相互覆盖闪烁', async () => {
    resolveWith({ code: '500', msg: '系统异常' })
    resolveWith({ code: '500', msg: '系统异常' })
    resolveWith({ code: '500', msg: '系统异常' })

    await Promise.allSettled([request({ url: '/a' }), request({ url: '/b' }), request({ url: '/c' })])

    expect(uniMock.showToast).toHaveBeenCalledTimes(1)
  })

  it('未包装的响应原样返回（防御性分支）', async () => {
    resolveWith('plain-text')
    await expect(request({ url: '/raw' })).resolves.toBe('plain-text')
  })

  it('401：提示、断开 WS、清空登录态并跳转登录页', async () => {
    const store = useUserStore()
    store.updateUser({ id: 1, token: 'tk-1' })
    resolveWith({ code: '401' })

    await expect(request({ url: '/x' })).rejects.toThrow('401')

    expect(closeWs).toHaveBeenCalled()
    expect(store.token).toBe('')
    expect(uniMock.reLaunch).toHaveBeenCalledWith({ url: '/pages/login/login' })
  })

  it('失败时按断网提示', async () => {
    uniMock.getNetworkType.mockImplementationOnce((o) => o.success({ networkType: 'none' }))
    uniMock.request.mockImplementationOnce((opts) => {
      opts.fail({ errMsg: 'request:fail' })
      opts.complete()
    })

    await expect(request({ url: '/x' })).rejects.toBeTruthy()
    expect(uniMock.showToast).toHaveBeenCalledWith(expect.objectContaining({ title: '网络不可用，请检查网络连接' }))
  })

  it('失败时按超时提示', async () => {
    uniMock.getNetworkType.mockImplementationOnce((o) => o.success({ networkType: 'wifi' }))
    uniMock.request.mockImplementationOnce((opts) => {
      opts.fail({ errMsg: 'request:fail timeout' })
      opts.complete()
    })

    await expect(request({ url: '/x' })).rejects.toBeTruthy()
    expect(uniMock.showToast).toHaveBeenCalledWith(expect.objectContaining({ title: '连接超时，请稍后重试' }))
  })

  it('loading 计数：出现即通知 show，收起保证最短展示时长', async () => {
    vi.useFakeTimers()
    const { events, off } = trackLoading()
    try {
      resolveWith({ code: '200' })
      await request({ url: '/x' })

      // 请求立刻结束也只先通知 show；收起被推迟到最短展示时长之后
      expect(events).toEqual([false, true])

      await vi.advanceTimersByTimeAsync(499)
      expect(events).toEqual([false, true])

      await vi.advanceTimersByTimeAsync(1)
      expect(events).toEqual([false, true, false])
    } finally {
      off()
      vi.useRealTimers()
    }
  })

  it('并发请求：动画只开一次、全部结束后才收起', async () => {
    vi.useFakeTimers()
    const { events, off } = trackLoading()
    try {
      // 模拟真实并发：两个请求都在 10ms 后才返回，收起前两者均在途
      uniMock.request.mockImplementation((opts) => {
        setTimeout(() => {
          opts.success({ data: { code: '200' } })
          opts.complete()
        }, 10)
      })
      const all = Promise.all([request({ url: '/a' }), request({ url: '/b' })])
      await vi.advanceTimersByTimeAsync(10)
      await all

      expect(events).toEqual([false, true])

      await vi.advanceTimersByTimeAsync(500)
      expect(events).toEqual([false, true, false])
    } finally {
      off()
      vi.useRealTimers()
    }
  })

  it('收起排队期间来了新请求：复用蒙层不闪烁，重新起算最短时长', async () => {
    vi.useFakeTimers()
    const { events, off } = trackLoading()
    try {
      uniMock.request.mockImplementation((opts) => {
        opts.success({ data: { code: '200' } })
        opts.complete()
      })
      await request({ url: '/a' })
      await vi.advanceTimersByTimeAsync(300) // hide 已排队但未触发

      await request({ url: '/b' })

      // 第二次 show 是复用通知（状态仍为 true），期间没有 false 闪断
      expect(events).toEqual([false, true, true])

      await vi.advanceTimersByTimeAsync(500)
      expect(events).toEqual([false, true, true, false])
    } finally {
      off()
      vi.useRealTimers()
    }
  })

  it('订阅制：蒙层晚于请求挂载时立即同步进行中状态（页面跳转不丢动画）', async () => {
    vi.useFakeTimers()
    try {
      let finish
      uniMock.request.mockImplementationOnce((opts) => {
        finish = () => {
          opts.success({ data: { code: '200' } })
          opts.complete()
        }
      })
      const p = request({ url: '/x' })

      // 模拟新页面蒙层组件在请求已开始后才挂载订阅
      const { events, off } = trackLoading()
      expect(events).toEqual([true])

      finish()
      await p
      await vi.advanceTimersByTimeAsync(500)
      expect(events).toEqual([true, false])
      off()
    } finally {
      vi.useRealTimers()
    }
  })

  it('loading:false 时静默请求不触发动画状态', async () => {
    const { events, off } = trackLoading()
    try {
      resolveWith({ code: '200' })
      await get('/x', {}, { loading: false })

      expect(events).toEqual([false])
    } finally {
      off()
    }
  })

  it('getData：200 时直接返回 data.data', async () => {
    resolveWith({ code: '200', data: [{ id: 1 }] })
    const data = await getData('/thing/selectAll')
    expect(data).toEqual([{ id: 1 }])
  })

  it('getData：非 200 时提示并返回 null（不抛出）', async () => {
    resolveWith({ code: '5001', msg: '用户名已存在' })
    const data = await getData('/x')
    expect(data).toBe(null)
    expect(uniMock.showToast).toHaveBeenCalled()
  })
})

describe('resolveFileUrl 文件地址归一化', () => {
  it.each([
    ['', ''],
    [undefined, ''],
    ['/files/a.png', 'http://localhost:9091/files/a.png'],
    ['/api/files/a.png', 'http://localhost:9091/files/a.png'],
    ['http://localhost:9091/files/a.png', 'http://localhost:9091/files/a.png'],
    ['http://cdn.example.com/other.png', 'http://cdn.example.com/other.png'],
  ])('%s → %s', (input, expected) => {
    expect(resolveFileUrl(input)).toBe(expected)
  })
})

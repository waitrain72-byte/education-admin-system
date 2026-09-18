import { baseUrl } from './config'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'

/**
 * 实时通知 WebSocket（与 Web 端共用后端端点 ws://主机:9091/ws/notice/{token}）：
 * - token 拼在 URL 路径里（后端从路径鉴权），规避小程序 connectSocket 无法可靠携带自定义 header 的限制。
 *   已知取舍：token 会出现在服务器访问日志中，生产化改造应改为「连接后首条消息鉴权」，需后端同步调整
 * - 收到推送后：静默入列（「首页」tab 角标未读数 + uni.$emit('ws:push', 消息) 供页面实时刷新），
 *   不再逐条弹 toast 打断用户（对齐高星项目的消息中心式体验）
 * - 断线按指数退避自动重连；网络恢复 / 切回前台时立即补连；退出登录由 closeWs() 主动断开
 * - 心跳附带「入站静默检测」：长时间收不到任何消息判定为半开连接，主动断开触发重连
 *
 * 返回值兼容（重要）：uni.connectSocket 不传回调时各端返回不一致——
 * 模拟器/部分平台返回 SocketTask（可绑 onOpen 等任务级回调），真机可能返回 Promise（任务方法不存在，
 * 直接 .onOpen 会报 "undefined is not a function"）。因此按返回形态分派：
 * - SocketTask → 任务级回调（task.onOpen 等）
 * - Promise / undefined → uni 全局回调模式（uni.onSocketOpen 等，本项目同一时刻只有一条连接，安全）
 *
 * 使用方式：
 * - App.vue onLaunch 调 initWs()（注册网络监听，仅一次），onShow 调 connectWs()（幂等，有 token 才连）
 * - 登录成功后 login.vue 调 connectWs()；退出登录 / 修改密码 / 请求 401 处调 closeWs()
 * - 首页 onShow 调 resetWsUnread() 清角标，并监听 'ws:push' 刷新通知列表
 */

/** 首页在 tabBar 中的位置（pages.json tabBar.list 顺序） */
const HOME_TAB_INDEX = 0
/** 心跳间隔：后端 @OnMessage 收到 "ping" 后静默忽略，仅用于防止长连接空闲过久被中间层断开 */
const HEARTBEAT_INTERVAL = 25000
/** 入站静默阈值：超过该时长未收到任何消息判定为「僵尸连接」，主动断开交给重连逻辑。
 * 后端对 ping 静默（不回 pong），无法用 pong 判活；正常空闲断开（如 nginx 超时）会先触发 onClose，
 * 这里只兜底收不到 close 事件的半开连接，阈值取 5 分钟避免健康空闲连接被频繁重建 */
const INBOUND_STALE_MS = 300000
/** 重连退避：3s 起步翻倍，上限 30s */
const MAX_RETRY_DELAY = 30000

let task: any = null
let globalEventsBound = false
let connected = false
let manuallyClosed = false
let retryCount = 0
let heartbeatTimer: ReturnType<typeof setInterval> | null = null
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let unread = 0
let networkListenerBound = false
let lastInboundAt = 0

function updateBadge() {
  try {
    if (unread > 0) {
      uni.setTabBarBadge({
        index: HOME_TAB_INDEX,
        text: unread > 99 ? '99+' : String(unread),
        fail: () => {},
      })
    } else {
      uni.removeTabBarBadge({ index: HOME_TAB_INDEX, fail: () => {} })
    }
  } catch {
    // 非 tabBar 场景忽略角标错误
  }
}

/** 心跳保活：SocketTask 与全局模式分别用各自的发送通道；顺带做入站静默检测 */
function startHeartbeat() {
  stopHeartbeat()
  heartbeatTimer = setInterval(() => {
    if (!connected) return
    // 半开连接兜底：长时间收不到任何入站消息时主动断开，onClose → onDrop → 重连
    if (lastInboundAt && Date.now() - lastInboundAt > INBOUND_STALE_MS) {
      lastInboundAt = 0
      try {
        if (task) {
          task.close({ code: 1000 })
        } else if (globalEventsBound) {
          uni.closeSocket({ code: 1000, fail: () => {} })
        }
      } catch {
        // 连接已断时忽略，交给重连逻辑
      }
      return
    }
    try {
      if (task) {
        task.send({ data: 'ping', fail: () => {} })
      } else if (globalEventsBound) {
        uni.sendSocketMessage({ data: 'ping', fail: () => {} })
      }
    } catch {
      // 连接已断时忽略心跳失败，交给重连逻辑
    }
  }, HEARTBEAT_INTERVAL)
}

function stopHeartbeat() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

function scheduleReconnect() {
  if (manuallyClosed || reconnectTimer) return
  const delay = Math.min(3000 * 2 ** retryCount, MAX_RETRY_DELAY)
  retryCount += 1
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    connectWs()
  }, delay)
}

function handlePush(raw: any) {
  let msg: any = null
  try {
    msg = typeof raw === 'string' ? JSON.parse(raw) : raw
  } catch {
    return
  }
  if (!msg || !msg.title) return
  lastInboundAt = Date.now()
  unread += 1
  updateBadge()
  // 落库消息中心（本地按用户持久化，供用户回看历史通知）；已退出登录（无 token）时丢弃
  try {
    if (useUserStore().token) useMessageStore().push(msg)
  } catch {
    // Pinia 未就绪时跳过落库
  }
  // 静默入列：角标计数 + 广播事件（页面可监听 uni.$on('ws:push') 做实时刷新，如首页通知列表）
  uni.$emit('ws:push', msg)
}

function onOpenOnce() {
  connected = true
  retryCount = 0
  lastInboundAt = Date.now()
  startHeartbeat()
}

/** 断开（onClose/onError 共用）：清状态并安排重连 */
function onDrop() {
  connected = false
  stopHeartbeat()
  task = null
  scheduleReconnect()
}

/** SocketTask 形态：绑定任务级回调 */
function bindTask(t: any) {
  t.onOpen(onOpenOnce)
  t.onMessage((res: any) => handlePush(res.data))
  t.onClose(onDrop)
  t.onError(onDrop)
}

/** 全局回调形态（uni.onSocketOpen 等）：只注册一次，重复 connectSocket 时事件继续作用于当前连接 */
function bindGlobalEvents() {
  if (globalEventsBound) return
  globalEventsBound = true
  uni.onSocketOpen(onOpenOnce)
  uni.onSocketMessage((res: any) => handlePush(res.data))
  uni.onSocketClose(onDrop)
  uni.onSocketError(onDrop)
}

/**
 * 建立 WebSocket 连接（幂等：无 token / 已在连接中时不重复建连）。
 * 注意：不要给 uni.connectSocket 传 success/fail 回调，否则部分平台不返回 SocketTask。
 */
export function connectWs() {
  const userStore = useUserStore()
  // connected 兜底全局回调形态（部分真机 task 为空）：防止 onShow/网络恢复多次触发重复建连
  if (!userStore.token || task || connected) return
  manuallyClosed = false
  // http(s) → ws(s)：与 request.ts 共用同一 baseUrl
  const url = baseUrl.replace(/^http/, 'ws') + '/ws/notice/' + userStore.token
  const t = uni.connectSocket({ url })
  if (t && typeof (t as any).onOpen === 'function') {
    task = t
    bindTask(t)
    return
  }
  // Promise / undefined 形态（部分真机基础库）：回退全局回调模式
  bindGlobalEvents()
  if (t && typeof (t as any).then === 'function') {
    // 连接结果由全局回调接管，这里仅接住 rejection 避免 unhandled promise rejection
    ;(t as Promise<any>).catch(() => {})
  }
}

/** 主动断开（退出登录 / 修改密码后旧 token 失效时调用），并清零未读角标 */
export function closeWs() {
  manuallyClosed = true
  stopHeartbeat()
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  if (task) {
    const closing = task
    task = null
    connected = false
    try {
      closing.close({ code: 1000 })
    } catch {
      // 连接已断开时 close 会报错，忽略
    }
  } else if (globalEventsBound) {
    try {
      uni.closeSocket({ code: 1000, fail: () => {} })
    } catch {
      // 未建立连接时忽略
    }
  }
  unread = 0
  updateBadge()
}

/** 清零未读数并去掉「首页」tab 角标（回到首页时调用） */
export function resetWsUnread() {
  unread = 0
  updateBadge()
}

/** 应用启动时调用一次：网络恢复时自动补连 */
export function initWs() {
  if (networkListenerBound) return
  networkListenerBound = true
  uni.onNetworkStatusChange((res) => {
    if (res.isConnected && !manuallyClosed) connectWs()
  })
}

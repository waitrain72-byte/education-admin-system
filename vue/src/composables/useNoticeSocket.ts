import { watch } from 'vue'
import { ElNotification } from '@/utils/element-plus'
import { useUserStore } from '@/stores/user'

/**
 * 实时通知 WebSocket（Web 端）：
 * - 登录后自动连接 ws://后端/ws/notice/{token}，接收实时推送（请假审核结果、成绩发布、新教务通知等）
 * - 带 title 的消息弹 Element Plus 右上角通知；不带 title 的是「静默事件」（如课堂签到人数变化），只分发给订阅者
 * - 所有消息都会分发给 onSocketMessage 的订阅者（顶栏未读数、签到面板等据此实时刷新）
 * - 连接断开自动重连（10 秒间隔）；退出登录自动断开
 */
const RECONNECT_DELAY = 10000

export type SocketMessage = Record<string, any>
type Listener = (message: SocketMessage) => void

let socket: WebSocket | null = null
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let intentionallyClosed = false
const listeners = new Set<Listener>()
let notificationClick: ((message: SocketMessage) => void) | null = null

/** 订阅推送；返回取消订阅函数（组件卸载时调用） */
export function onSocketMessage(listener: Listener): () => void {
    listeners.add(listener)
    return () => {
        listeners.delete(listener)
    }
}

/** 设置点击右上角通知时的处理（布局组件里注册，用来跳转到消息对应的页面） */
export function setNotificationClickHandler(handler: ((message: SocketMessage) => void) | null): void {
    notificationClick = handler
}

/** 解析并分发一条推送（导出供单测直接调用） */
export function dispatchSocketMessage(raw: string): void {
    let data: SocketMessage
    try {
        data = JSON.parse(raw)
    } catch {
        return
    }
    if (!data || typeof data !== 'object') return
    for (const listener of listeners) {
        try {
            listener(data)
        } catch {
            // 单个订阅者出错不影响其他订阅者
        }
    }
    if (data.title) {
        ElNotification({
            title: data.title,
            message: data.content || '',
            type: 'success',
            duration: 6000,
            onClick: () => notificationClick?.(data),
        })
    }
}

function wsUrl(): string {
    const base = (import.meta.env.VITE_BASE_URL as string) || ''
    let origin = window.location.host
    let secure = window.location.protocol === 'https:'
    let prefix = ''
    if (base.startsWith('http')) {
        // 完整地址（开发模式）：http://localhost:9091 -> ws://localhost:9091
        origin = base.replace(/^https?:\/\//, '').replace(/\/$/, '')
        secure = base.startsWith('https')
    } else if (base) {
        // 相对前缀（生产模式 /api）：保留前缀走 nginx 反代 -> ws://host/api/ws/notice/xxx
        prefix = base.replace(/\/$/, '')
    }
    return (secure ? 'wss://' : 'ws://') + origin + prefix + '/ws/notice/' + useUserStore().token
}

function connect() {
    const store = useUserStore()
    if (!store.isLoggedIn || (socket && socket.readyState === WebSocket.OPEN)) return
    intentionallyClosed = false
    try {
        socket = new WebSocket(wsUrl())
    } catch {
        scheduleReconnect()
        return
    }
    socket.onmessage = (event) => dispatchSocketMessage(event.data as string)
    socket.onclose = () => {
        socket = null
        scheduleReconnect()
    }
    socket.onerror = () => {
        socket?.close()
    }
}

function scheduleReconnect() {
    if (reconnectTimer || intentionallyClosed) return
    reconnectTimer = setTimeout(() => {
        reconnectTimer = null
        connect()
    }, RECONNECT_DELAY)
}

/**
 * 在应用根组件安装一次：登录后自动建连，退出登录自动断开
 */
export function installNoticeSocket() {
    const store = useUserStore()
    watch(
        () => store.isLoggedIn,
        (loggedIn) => {
            if (loggedIn) {
                intentionallyClosed = false
                connect()
            } else {
                intentionallyClosed = true
                // 必须同时清掉待触发的重连定时器：只置 intentionallyClosed 拦不住已在排队的那一次，
                // 它的回调会在退出登录后仍调用 connect() 重新建连
                if (reconnectTimer) {
                    clearTimeout(reconnectTimer)
                    reconnectTimer = null
                }
                socket?.close()
                socket = null
            }
        },
        { immediate: true }
    )
}

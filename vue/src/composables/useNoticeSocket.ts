import { watch } from 'vue'
import { ElNotification } from 'element-plus'
import { useUserStore } from '@/stores/user'

/**
 * 实时通知 WebSocket（Web 端）：
 * - 登录后自动连接 ws://后端/ws/notice/{token}，接收实时推送（请假审核结果、成绩发布、新教务通知等）
 * - 收到消息弹 Element Plus 右上角通知
 * - 连接断开自动重连（10 秒间隔）；退出登录自动断开
 */
const RECONNECT_DELAY = 10000

let socket: WebSocket | null = null
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let intentionallyClosed = false

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
    socket.onmessage = (event) => {
        try {
            const data = JSON.parse(event.data as string)
            if (data && data.title) {
                ElNotification({
                    title: data.title,
                    message: data.content || '',
                    type: 'success',
                    duration: 6000,
                })
            }
        } catch {
            // 非 JSON 消息忽略
        }
    }
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
                socket?.close()
                socket = null
            }
        },
        { immediate: true }
    )
}

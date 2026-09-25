import { defineStore } from 'pinia'

export interface PushMessage {
  id: string
  title: string
  content: string
  /** 展示用时间（M-DD HH:mm） */
  time: string
  read: boolean
  /** 接收时间戳：去重与截断用 */
  ts: number
}

const MAX_MESSAGES = 50

const storageKey = (userId: unknown) => 'xm-messages-' + (userId || 'guest')

const pad = (n: number) => (n < 10 ? '0' + n : String(n))

function formatNow(): string {
  const d = new Date()
  return `${d.getMonth() + 1}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function loadMessages(userId: unknown): PushMessage[] {
  try {
    const raw = uni.getStorageSync(storageKey(userId))
    return Array.isArray(raw) ? raw : []
  } catch {
    return []
  }
}

/**
 * 消息中心仓库：WebSocket 推送在本地按用户持久化（后端无消息历史接口，不丢已收到的通知）。
 * - push：落库 + 一分钟内同内容去重 + 超量截断
 * - unreadCount：「我的」页红点与首页铃铛角标
 * - open：按推送标题关键词映射到对应功能页
 */
export const useMessageStore = defineStore('message', {
  state: () => ({
    messages: [] as PushMessage[],
    ownerId: '' as unknown,
  }),
  getters: {
    unreadCount: (state) => state.messages.filter((m) => !m.read).length,
  },
  actions: {
    /**
     * 确认当前用户后载入其历史消息（同一用户只载入一次，切号自动重载）。
     * 传 userStore.accountKey（「角色-id」）：三类账号 id 会重复，只用 id 会让 1 号管理员看到 1 号学生的推送。
     */
    loadForUser(userId: unknown) {
      if (this.ownerId === userId) return
      this.ownerId = userId
      this.messages = loadMessages(userId)
    },
    push(payload: { title?: string; content?: string } | null) {
      if (!payload || !payload.title) return
      const now = Date.now()
      const content = payload.content || ''
      // 一分钟内同标题同内容的推送去重（后端重试/重连补发场景）
      const duplicated = this.messages.some(
        (m) => m.title === payload.title && m.content === content && now - m.ts < 60000,
      )
      if (duplicated) return
      this.messages.unshift({
        id: `${now}-${Math.floor(Math.random() * 1000)}`,
        title: payload.title,
        content,
        time: formatNow(),
        read: false,
        ts: now,
      })
      if (this.messages.length > MAX_MESSAGES) this.messages.length = MAX_MESSAGES
      this.persist()
    },
    markAllRead() {
      this.messages.forEach((m) => {
        m.read = true
      })
      this.persist()
    },
    /** 退出登录时重置归属（下一位用户登录后 loadForUser 重新载入其历史） */
    resetOwnership() {
      this.ownerId = ''
      this.messages = []
    },
    clear() {
      this.messages = []
      this.persist()
    },
    /** 点击单条：标记已读，并按标题关键词返回应跳转的功能页（无匹配返回 null 停留原页） */
    open(msg: PushMessage): string | null {
      const hit = this.messages.find((m) => m.id === msg.id)
      if (hit && !hit.read) {
        hit.read = true
        this.persist()
      }
      const text = `${msg.title}${msg.content}`
      // 预警推送的建议文字里可能含「成绩」，必须先于成绩匹配
      if (/预警/.test(text)) return '/pages/warning/warning'
      if (/成绩/.test(text)) return '/pages/score/score'
      if (/作业/.test(text)) return '/pages/homework/homework'
      if (/请假/.test(text)) return '/pages/apply/apply'
      if (/考试/.test(text)) return '/pages/examplan/examplan'
      if (/通知|公告/.test(text)) return '/pages/notice/notice'
      return null
    },
    persist() {
      uni.setStorageSync(storageKey(this.ownerId), this.messages)
    },
  },
})

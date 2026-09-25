import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const { useMessageStore } = await import('@/stores/message')
const { clearStorage } = await import('./setup')

beforeEach(() => {
  setActivePinia(createPinia())
  clearStorage()
  vi.clearAllMocks()
})

describe('message store 消息中心', () => {
  it('push：落库、一分钟内同内容去重、未读计数', () => {
    const store = useMessageStore()
    store.loadForUser(1)
    store.push({ title: '成绩发布', content: '高数 92 分' })
    store.push({ title: '成绩发布', content: '高数 92 分' })
    expect(store.messages.length).toBe(1)
    expect(store.unreadCount).toBe(1)
    expect(store.messages[0].read).toBe(false)
  })

  it('按用户隔离：不同账号载入各自历史', () => {
    const store = useMessageStore()
    store.loadForUser(1)
    store.push({ title: '通知A', content: '' })
    store.loadForUser(2)
    expect(store.messages.length).toBe(0)
    store.loadForUser(1)
    expect(store.messages.length).toBe(1)
  })

  it('markAllRead 与 clear', () => {
    const store = useMessageStore()
    store.loadForUser(1)
    store.push({ title: '作业批改', content: '已批改' })
    store.markAllRead()
    expect(store.unreadCount).toBe(0)
    store.clear()
    expect(store.messages.length).toBe(0)
  })

  it('open：按关键词映射跳转页并标记已读', () => {
    const store = useMessageStore()
    store.loadForUser(1)
    store.push({ title: '成绩发布通知', content: '' })
    const target = store.open(store.messages[0])
    expect(target).toBe('/pages/score/score')
    expect(store.messages[0].read).toBe(true)
  })

  it('open：学业预警推送跳预警页（建议文字里含「成绩」也不会误跳成绩页）', () => {
    const store = useMessageStore()
    store.loadForUser('STUDENT-1')
    store.push({ title: '学业预警提醒', content: '你的学业风险指数 42（中风险）：成绩或出勤存在明显波动' })
    expect(store.open(store.messages[0])).toBe('/pages/warning/warning')
  })

  it('按账号（角色-id）隔离：1 号管理员看不到 1 号学生的推送', () => {
    const store = useMessageStore()
    store.loadForUser('STUDENT-1')
    store.push({ title: '学业预警提醒', content: '' })
    store.loadForUser('ADMIN-1')
    expect(store.messages.length).toBe(0)
    store.loadForUser('STUDENT-1')
    expect(store.messages.length).toBe(1)
  })

  it('open：无匹配关键词返回 null', () => {
    const store = useMessageStore()
    store.loadForUser(1)
    store.push({ title: 'hello', content: '' })
    expect(store.open(store.messages[0])).toBe(null)
  })

  it('push：超过上限截断到 50 条且最新在前', () => {
    const store = useMessageStore()
    store.loadForUser(1)
    for (let i = 0; i < 55; i += 1) {
      store.push({ title: 't' + i, content: '' })
    }
    expect(store.messages.length).toBe(50)
    expect(store.messages[0].title).toBe('t54')
  })
})

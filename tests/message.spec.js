import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const { useMessageStore, applyMessageBadge } = await import('@/stores/message')
const { useUserStore } = await import('@/stores/user')
const { MESSAGES_TAB_INDEX } = await import('@/utils/tabbar')
const { clearStorage } = await import('./setup')

const uniMock = globalThis.uni

/** 让下一次 uni.request 成功返回 { code: '200', data } */
const respondWith = (data) => {
  uniMock.request.mockImplementationOnce((opts) => {
    opts.success({ statusCode: 200, data: { code: '200', data } })
    if (opts.complete) opts.complete()
  })
}

beforeEach(() => {
  setActivePinia(createPinia())
  clearStorage()
  vi.clearAllMocks()
})

describe('applyMessageBadge 消息 tab 角标', () => {
  it('有未读时挂在「消息」tab 上，超过 99 显示 99+', () => {
    applyMessageBadge(3)
    expect(uniMock.setTabBarBadge).toHaveBeenCalledWith(
      expect.objectContaining({ index: MESSAGES_TAB_INDEX, text: '3' }),
    )
    applyMessageBadge(120)
    expect(uniMock.setTabBarBadge.mock.calls.at(-1)[0].text).toBe('99+')
  })

  it('没有未读时移除角标', () => {
    applyMessageBadge(0)
    expect(uniMock.removeTabBarBadge).toHaveBeenCalledWith(expect.objectContaining({ index: MESSAGES_TAB_INDEX }))
  })
})

describe('message store 未读数', () => {
  it('refresh：已登录时从后端拉未读数并更新角标', async () => {
    useUserStore().updateUser({ id: 1, role: 'STUDENT', token: 'tk' })
    respondWith(5)
    const store = useMessageStore()
    await store.refresh()
    expect(uniMock.request.mock.calls[0][0].url).toMatch(/\/message\/unreadCount$/)
    expect(store.unread).toBe(5)
    expect(uniMock.setTabBarBadge).toHaveBeenCalledWith(expect.objectContaining({ text: '5' }))
  })

  it('refresh：未登录时不请求，直接清零', async () => {
    const store = useMessageStore()
    store.unread = 4
    await store.refresh()
    expect(uniMock.request).not.toHaveBeenCalled()
    expect(store.unread).toBe(0)
  })

  it('markOneRead 不会减成负数；markAllRead、reset 清零', () => {
    const store = useMessageStore()
    store.setUnread(1)
    store.markOneRead()
    store.markOneRead()
    expect(store.unread).toBe(0)
    store.setUnread(9)
    store.markAllRead()
    expect(store.unread).toBe(0)
    store.setUnread(2)
    store.reset()
    expect(store.unread).toBe(0)
  })

  it('退出登录（clearUser）时未读数随之清零', () => {
    const store = useMessageStore()
    useUserStore().updateUser({ id: 1, role: 'STUDENT', token: 'tk' })
    store.setUnread(7)
    useUserStore().clearUser()
    expect(store.unread).toBe(0)
  })
})

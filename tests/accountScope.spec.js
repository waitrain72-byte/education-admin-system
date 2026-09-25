import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { readUserCache, writeUserCache } from '@/utils/userCache'
import { useUserStore } from '@/stores/user'
import { clearStorage } from './setup'

const uniMock = globalThis.uni

beforeEach(() => {
  clearStorage()
  vi.clearAllMocks()
  setActivePinia(createPinia())
})

describe('accountKey 账号标识', () => {
  it('「角色-id」：三类账号 id 重复时仍能区分；未登录为空串', () => {
    const store = useUserStore()
    expect(store.accountKey).toBe('')
    store.updateUser({ id: 1, role: 'ADMIN', token: 't' })
    expect(store.accountKey).toBe('ADMIN-1')
    store.updateUser({ id: 1, role: 'STUDENT', token: 't' })
    expect(store.accountKey).toBe('STUDENT-1')
  })
})

describe('userCache 按账号隔离的本地缓存', () => {
  it('同 id 不同角色互不可见', () => {
    writeUserCache('home', 'ADMIN-1', { notices: [1] })
    expect(readUserCache('home', 'ADMIN-1')).toEqual({ notices: [1] })
    expect(readUserCache('home', 'STUDENT-1')).toBe(null)
    expect(uniMock.setStorageSync).toHaveBeenCalledWith('xm-home-cache-ADMIN-1', { notices: [1] })
  })

  it('没有账号标识时不读不写；存储异常时静默返回 null', () => {
    writeUserCache('home', '', { a: 1 })
    expect(uniMock.setStorageSync).not.toHaveBeenCalled()
    expect(readUserCache('home', '')).toBe(null)
    uniMock.getStorageSync.mockImplementationOnce(() => {
      throw new Error('storage broken')
    })
    expect(readUserCache('curriculum', 'STUDENT-1')).toBe(null)
  })
})

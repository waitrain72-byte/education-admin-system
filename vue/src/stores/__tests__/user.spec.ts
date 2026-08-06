import { describe, it, expect, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { loadUserFromStorage, useUserStore } from '@/stores/user'

const STORAGE_KEY = 'xm-user'

describe('user store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('本地存储被污染时安全降级为空对象', () => {
    localStorage.setItem(STORAGE_KEY, 'not-json{{{')
    expect(loadUserFromStorage()).toEqual({})
  })

  it('解析结果为非对象时降级为空对象', () => {
    localStorage.setItem(STORAGE_KEY, '123')
    expect(loadUserFromStorage()).toEqual({})
  })

  it('初始状态从 localStorage 读取', () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ id: 1, role: 'ADMIN', token: 't' }))
    const store = useUserStore()
    expect(store.user.id).toBe(1)
    expect(store.isLoggedIn).toBe(true)
    expect(store.role).toBe('ADMIN')
    expect(store.token).toBe('t')
  })

  it('updateUser 写入内存并持久化', () => {
    const store = useUserStore()
    store.updateUser({ id: 2, name: '张三', token: 'abc' })
    expect(store.user.name).toBe('张三')
    expect(JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')).toMatchObject({
      id: 2,
      token: 'abc',
    })
  })

  it('patchUser 局部合并并持久化', () => {
    const store = useUserStore()
    store.updateUser({ id: 1, name: 'a', avatar: 'x.png' })
    store.patchUser({ avatar: 'y.png' })
    expect(store.user.name).toBe('a')
    expect(store.user.avatar).toBe('y.png')
    expect(JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}').avatar).toBe('y.png')
  })

  it('clearUser 清空内存与本地存储', () => {
    const store = useUserStore()
    store.updateUser({ id: 1 })
    store.clearUser()
    expect(store.isLoggedIn).toBe(false)
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull()
  })

  it('refreshUser 从本地存储重新同步', () => {
    const store = useUserStore()
    store.updateUser({ id: 1, name: 'old' })
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ id: 9, name: 'new' }))
    store.refreshUser()
    expect(store.user.id).toBe(9)
    expect(store.user.name).toBe('new')
  })
})

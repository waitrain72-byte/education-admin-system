import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const { authInterceptor, ensureLoggedIn, installAuthInterceptor, isPublicPage } = await import('@/utils/authGuard')
const { useUserStore } = await import('@/stores/user')
const { clearStorage } = await import('./setup')

const uniMock = globalThis.uni

beforeEach(() => {
  setActivePinia(createPinia())
  clearStorage()
  vi.clearAllMocks()
})

describe('isPublicPage 白名单判定', () => {
  it('登录页 / 注册页无需登录，带不带前导斜杠、带不带参数都能识别', () => {
    expect(isPublicPage('/pages/login/login')).toBe(true)
    expect(isPublicPage('pages/register/register')).toBe(true)
    expect(isPublicPage('/pages/login/login?from=share')).toBe(true)
  })

  it('其余页面需要登录', () => {
    expect(isPublicPage('/pages/home/home')).toBe(false)
    expect(isPublicPage('/pages-admin/student/student')).toBe(false)
  })
})

describe('authInterceptor 路由拦截', () => {
  it('未登录跳转受保护页面：拦下原跳转并改道登录页', () => {
    const pass = authInterceptor.invoke({ url: '/pages/score/score' })
    expect(pass).toBe(false)
    expect(uniMock.reLaunch).toHaveBeenCalledWith({ url: '/pages/login/login' })
  })

  it('未登录跳转登录/注册页：放行（改道登录页时的重入也靠这条不会死循环）', () => {
    expect(authInterceptor.invoke({ url: '/pages/register/register' })).toBe(true)
    expect(authInterceptor.invoke({ url: '/pages/login/login' })).toBe(true)
    expect(uniMock.reLaunch).not.toHaveBeenCalled()
  })

  it('已登录：任意页面放行', () => {
    useUserStore().updateUser({ id: 1, token: 'tk', role: 'STUDENT' })
    expect(authInterceptor.invoke({ url: '/pages/score/score' })).toBe(true)
    expect(uniMock.reLaunch).not.toHaveBeenCalled()
  })
})

describe('ensureLoggedIn 页面级兜底', () => {
  it('未登录：跳登录页并返回 false，调用方据此提前 return', () => {
    expect(ensureLoggedIn()).toBe(false)
    expect(uniMock.reLaunch).toHaveBeenCalledWith({ url: '/pages/login/login' })
  })

  it('已登录：返回 true，不跳转', () => {
    useUserStore().updateUser({ id: 1, token: 'tk' })
    expect(ensureLoggedIn()).toBe(true)
    expect(uniMock.reLaunch).not.toHaveBeenCalled()
  })
})

describe('installAuthInterceptor 安装', () => {
  it('对四个跳转 API 注册拦截器，重复调用只注册一次', () => {
    installAuthInterceptor()
    installAuthInterceptor()
    const apis = uniMock.addInterceptor.mock.calls.map((c) => c[0]).sort()
    expect(apis).toEqual(['navigateTo', 'reLaunch', 'redirectTo', 'switchTab'])
  })
})

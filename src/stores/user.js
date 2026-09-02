import { defineStore } from 'pinia'

const STORAGE_KEY = 'xm-user'

/**
 * 全局用户状态：与 Web 端 Pinia store 语义一致，
 * uni-app 环境下使用 uni.setStorageSync / getStorageSync 持久化。
 */
function loadUser() {
  try {
    const raw = uni.getStorageSync(STORAGE_KEY)
    return raw && typeof raw === 'object' ? raw : {}
  } catch {
    return {}
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    user: loadUser(),
  }),
  getters: {
    isLoggedIn: (state) => !!state.user.id,
    role: (state) => state.user.role || '',
    token: (state) => state.user.token || '',
    /** RBAC 权限码集合（登录后从 /permission/my 拉取，随 user 持久化） */
    permissions: (state) => state.user.permissions || [],
  },
  actions: {
    updateUser(newUser) {
      if (newUser == null) {
        this.refreshUser()
        return
      }
      this.user = { ...newUser }
      uni.setStorageSync(STORAGE_KEY, this.user)
    },
    /** 登录/进入首页后写入权限码集合，随 user 一并持久化 */
    setPermissions(permissions) {
      this.user = { ...this.user, permissions: permissions || [] }
      uni.setStorageSync(STORAGE_KEY, this.user)
    },
    patchUser(data) {
      this.user = { ...this.user, ...data }
      uni.setStorageSync(STORAGE_KEY, this.user)
    },
    clearUser() {
      this.user = {}
      uni.removeStorageSync(STORAGE_KEY)
    },
    refreshUser() {
      this.user = loadUser()
    },
  },
})

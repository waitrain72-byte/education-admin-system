import { defineStore } from 'pinia'
import { useMessageStore } from '@/stores/message'

const STORAGE_KEY = 'xm-user'
/** 持久化前剔除的字段：后端登录响应若回传密码类字段，绝不写入本地存储（防御性白名单外的黑名单剔除） */
const SENSITIVE_KEYS = ['password', 'passwd', 'pwd', 'passwordHash']

type UserInfo = Record<string, any>

/** 浅拷贝并剔除敏感字段 */
function sanitize(user: UserInfo | null | undefined): UserInfo {
  if (!user || typeof user !== 'object') return {}
  const copy = { ...user }
  for (const key of SENSITIVE_KEYS) delete copy[key]
  return copy
}

function loadUser(): UserInfo {
  try {
    const raw = uni.getStorageSync(STORAGE_KEY)
    return raw && typeof raw === 'object' ? raw : {}
  } catch {
    return {}
  }
}

/**
 * 全局用户状态：与 Web 端 Pinia store 语义一致，
 * uni-app 环境下使用 uni.setStorageSync / getStorageSync 持久化。
 * 所有写入本地存储的动作都先经过 sanitize()，密码类字段不落盘。
 */
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
    /**
     * 账号标识「角色-id」，用作本地缓存 / 消息历史的键。
     * 管理员、教师、学生分表存储，id 只在同一角色内唯一：只用 id 会让 1 号管理员和 1 号学生共用本地数据。
     */
    accountKey: (state) => (state.user.id ? `${state.user.role || ''}-${state.user.id}` : ''),
  },
  actions: {
    updateUser(newUser: UserInfo | null) {
      if (newUser == null) {
        this.refreshUser()
        return
      }
      this.user = sanitize(newUser)
      uni.setStorageSync(STORAGE_KEY, this.user)
    },
    /** 登录/进入首页后写入权限码集合，随 user 一并持久化 */
    setPermissions(permissions: string[]) {
      this.user = sanitize({ ...this.user, permissions: permissions || [] })
      uni.setStorageSync(STORAGE_KEY, this.user)
    },
    patchUser(data: UserInfo) {
      this.user = sanitize({ ...this.user, ...data })
      uni.setStorageSync(STORAGE_KEY, this.user)
    },
    clearUser() {
      this.user = {}
      uni.removeStorageSync(STORAGE_KEY)
      // 重置消息仓库归属：防止极端时序下推送落库到上一用户名下
      try {
        useMessageStore().resetOwnership()
      } catch {
        // Pinia 未就绪时忽略
      }
    },
    refreshUser() {
      this.user = loadUser()
    },
  },
})

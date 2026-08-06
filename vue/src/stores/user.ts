import { defineStore } from 'pinia'

const STORAGE_KEY = 'xm-user'

/**
 * 从 localStorage 安全读取用户信息。
 * 使用 try/catch 避免本地存储被污染（如非 JSON 内容）时导致整个应用白屏。
 */
export function loadUserFromStorage(): Record<string, any> {
    try {
        const raw = localStorage.getItem(STORAGE_KEY)
        if (!raw) return {}
        const parsed = JSON.parse(raw)
        return parsed && typeof parsed === 'object' ? parsed : {}
    } catch {
        return {}
    }
}

/**
 * 全局用户状态：登录态统一由 Pinia 管理，localStorage 仅作为持久化载体。
 */
export const useUserStore = defineStore('user', {
    state: () => ({
        user: loadUserFromStorage() as Record<string, any>,
    }),

    getters: {
        isLoggedIn: (state) => !!state.user?.id,
        role: (state) => state.user?.role || '',
        token: (state) => state.user?.token || '',
    },

    actions: {
        /** 登录成功或保存个人信息后，整体替换用户信息并持久化 */
        updateUser(newUser: Record<string, any> | null | undefined) {
            if (newUser == null) {
                // 保持旧行为兼容：传入空值时从存储刷新
                this.refreshUser()
                return
            }
            this.user = { ...newUser }
            localStorage.setItem(STORAGE_KEY, JSON.stringify(this.user))
        },

        /** 局部更新用户字段（如头像、姓名），并持久化 */
        patchUser(data: Partial<Record<string, any>>) {
            this.user = { ...this.user, ...data }
            localStorage.setItem(STORAGE_KEY, JSON.stringify(this.user))
        },

        /** 退出登录：清空内存与本地存储 */
        clearUser() {
            this.user = {}
            localStorage.removeItem(STORAGE_KEY)
        },

        /** 从 localStorage 重新同步（如 401 后或跨标签页场景） */
        refreshUser() {
            this.user = loadUserFromStorage()
        },
    },
})

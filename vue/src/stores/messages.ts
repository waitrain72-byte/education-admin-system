import { defineStore } from 'pinia'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

/**
 * 站内消息未读数：顶栏铃铛的角标。
 * 进入系统时拉一次、收到实时推送时再拉一次；标记已读后由消息页直接改本地值，不必再请求。
 */
export const useMessageStore = defineStore('messages', {
    state: () => ({
        unread: 0,
    }),

    actions: {
        async refresh() {
            if (!useUserStore().isLoggedIn) {
                this.unread = 0
                return
            }
            try {
                const count = await request.get<number>('/message/unreadCount')
                this.unread = typeof count === 'number' && count > 0 ? count : 0
            } catch {
                // 拉取失败保留原值，不打断使用
            }
        },

        markOneRead() {
            this.unread = Math.max(0, this.unread - 1)
        },

        markAllRead() {
            this.unread = 0
        },
    },
})

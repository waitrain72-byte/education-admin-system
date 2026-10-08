import { defineStore } from 'pinia'
import { messageApi } from '@/api'
import { SILENT } from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { MESSAGES_TAB_INDEX } from '@/utils/tabbar'

/**
 * 「消息」tab 的未读角标。tabBar 接口只能在 tabBar 页面调用，其他页面会失败——
 * 失败直接忽略，进入 tabBar 页面时全局混入（main.js）会按当前未读数再设一次。
 */
export function applyMessageBadge(count: number): void {
  try {
    if (count > 0) {
      uni.setTabBarBadge({ index: MESSAGES_TAB_INDEX, text: count > 99 ? '99+' : String(count), fail: () => {} })
    } else {
      uni.removeTabBarBadge({ index: MESSAGES_TAB_INDEX, fail: () => {} })
    }
  } catch {
    // 平台不支持时忽略
  }
}

/**
 * 站内消息未读数（与 Web 端 stores/messages.ts 一致）：消息本身存在后端（/message），这里只记未读数。
 * 进入应用时拉一次、收到实时推送时再拉一次；标记已读后由消息页直接改本地值，不必再请求。
 */
export const useMessageStore = defineStore('messages', {
  state: () => ({
    unread: 0,
  }),
  actions: {
    async refresh() {
      if (!useUserStore().isLoggedIn) {
        this.setUnread(0)
        return
      }
      try {
        const count = await messageApi.unreadCount(SILENT)
        this.setUnread(typeof count === 'number' ? count : 0)
      } catch {
        // 拉取失败保留原值，不打断使用
      }
    },
    setUnread(count: number) {
      this.unread = Math.max(0, Math.floor(count) || 0)
      applyMessageBadge(this.unread)
    },
    markOneRead() {
      this.setUnread(this.unread - 1)
    },
    markAllRead() {
      this.setUnread(0)
    },
    /** 退出登录：清零（下一位用户登录后重新拉取） */
    reset() {
      this.setUnread(0)
    },
  },
})

import type { Directive } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 按钮级权限指令：
 *   v-permission="'ADMIN'"                 —— 仅 ADMIN 可见
 *   v-permission="['ADMIN', 'TEACHER']"    —— ADMIN / TEACHER 可见
 * 当前角色不在允许列表中时，直接移除该 DOM 元素。
 */
export const permission: Directive<HTMLElement, string | string[]> = {
    mounted(el, binding) {
        const allowed = Array.isArray(binding.value) ? binding.value : [binding.value]
        if (!allowed.length) return
        const userStore = useUserStore()
        if (!userStore.role || !allowed.includes(userStore.role)) {
            el.remove()
        }
    },
}

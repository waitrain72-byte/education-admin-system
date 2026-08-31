import type { Directive } from 'vue'
import { useUserStore } from '@/stores/user'

const ROLE_VALUES = ['ADMIN', 'TEACHER', 'STUDENT']

/**
 * 权限指令：
 *   v-permission="'ADMIN'"                  —— 仅 ADMIN 角色可见（旧用法，按角色名判断）
 *   v-permission="['ADMIN', 'TEACHER']"     —— ADMIN / TEACHER 视角判断（旧用法）
 *   v-permission="'score:manage'"           —— 拥有 score:manage 权限码才可见（RBAC 新用法）
 *   v-permission="['score:manage','apply']" —— 任一权限码命中即可见
 *
 * 判断规则：
 *   - 传入值全是标准角色名 -> 沿用旧的按角色判断逻辑（兼容现有页面）；
 *   - 否则视为 RBAC 权限码，按当前用户权限码集合判断（ADMIN 直接放行）。
 * 不满足时直接移除该 DOM 元素。
 */
export const permission: Directive<HTMLElement, string | string[]> = {
    mounted(el, binding) {
        const values = Array.isArray(binding.value) ? binding.value : [binding.value]
        if (!values.length) return
        const userStore = useUserStore()

        const isRoleMode = values.every((v) => ROLE_VALUES.includes(v))
        if (isRoleMode) {
            // 旧用法：按角色名判断
            if (!userStore.role || !values.includes(userStore.role)) {
                el.remove()
            }
            return
        }

        // RBAC 新用法：按权限码判断，ADMIN 放行
        if (userStore.role === 'ADMIN') return
        const perms = userStore.permissions
        if (!perms.length || !values.some((v) => perms.includes(v))) {
            el.remove()
        }
    },
}

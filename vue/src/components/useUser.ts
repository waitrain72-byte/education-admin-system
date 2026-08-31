
import { readonly } from 'vue'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/user'

const ROLE_VALUES = ['ADMIN', 'TEACHER', 'STUDENT']

/**
 * 用户状态组合式封装：内部委托给 Pinia store。
 * 保持原有 API（user ref + 四个方法）不变，避免业务组件大规模改动。
 */
export const useUser = () => {
    const store = useUserStore()
    const { user } = storeToRefs(store)
    return {
        user: readonly(user), // 只读，防止外部直接修改
        /** 判断当前角色是否命中给定角色列表之一（替代散落的 user.role === 'xxx' 判断） */
        hasRole: (...roles: string[]) => roles.includes(store.role),
        /** RBAC：判断当前用户是否拥有指定权限码（ADMIN 直接放行；兼容只传一个权限码的情况） */
        hasPermission: (code: string | string[]): boolean => {
            const codes = Array.isArray(code) ? code : [code]
            if (store.role === 'ADMIN') return true
            const perms = store.permissions
            return codes.some((c) => perms.includes(c))
        },
        updateUser: store.updateUser,
        patchUser: store.patchUser,
        clearUser: store.clearUser,
        refreshUser: store.refreshUser,
        setPermissions: store.setPermissions,
        /** 供 v-permission 指令复用：一组值是否按角色判断（全是角色名） */
        isRoleValue: (v: string | string[]) => {
            const values = Array.isArray(v) ? v : [v]
            return values.length > 0 && values.every((x) => ROLE_VALUES.includes(x))
        },
    }
}

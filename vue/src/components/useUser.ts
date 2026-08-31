
import { readonly } from 'vue'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/user'

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
        updateUser: store.updateUser,
        patchUser: store.patchUser,
        clearUser: store.clearUser,
        refreshUser: store.refreshUser,
    }
}

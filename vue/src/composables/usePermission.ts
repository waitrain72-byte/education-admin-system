import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

/**
 * 权限组合式封装（RBAC）：
 *   pullPermissions —— 从后端拉取当前用户权限码并写入 user store（登录后 / 进入主布局时调用）。
 *
 * 说明：权限码随 user 一并持久化到 localStorage，因此刷新后无需再次拉取即可用于按钮控制；
 * 管理员在权限设置页调整授权后，重新登录或调用本方法即可让前端与后端保持一致。
 */
export function usePermission() {
    const store = useUserStore()

    async function pullPermissions(): Promise<string[]> {
        if (!store.token) return []
        try {
            const res: any = await request.get('/permission/my')
            const codes: string[] = res.data?.data || []
            store.setPermissions(codes)
            return codes
        } catch {
            // 拉取失败不阻断主流程；此时按钮级权限按最保守策略隐藏（payload 空）
            store.setPermissions([])
            return []
        }
    }

    return { pullPermissions }
}

import { useUserStore } from '@/stores/user'
import { get } from '@/utils/request'

/**
 * 权限组合式封装（RBAC，与 Web 端 usePermission 语义一致）：
 * pullPermissions —— 从后端拉取当前用户权限码并写入 user store。
 * 登录成功后与进入首页时调用；管理员在 Web 端调整授权后，重新登录即同步。
 */
export function usePermission() {
  const store = useUserStore()

  async function pullPermissions() {
    if (!store.token) return []
    try {
      const res = await get('/permission/my')
      const codes = (res.data && res.data.data) || []
      store.setPermissions(codes)
      return codes
    } catch {
      // 拉取失败不阻断主流程；此时 home 菜单退化为仅按角色过滤，避免误隐藏
      store.setPermissions([])
      return []
    }
  }

  return { pullPermissions }
}

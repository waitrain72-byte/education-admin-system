import { useUserStore } from '@/stores/user'
import { SILENT } from '@/utils/request'
import { accountApi } from '@/api'

/**
 * 权限组合式封装（RBAC，与 Web 端 usePermission / useUser 语义一致）：
 * - pullPermissions：从后端拉取当前用户权限码并写入 user store。登录成功后与进入首页时调用；
 *   管理员在 Web 端调整授权后，重新登录即同步
 * - hasPermission：当前账号是否有某个权限码（管理员全部放行，与后端 RBAC 切面一致）
 */
export function usePermission() {
  const store = useUserStore()

  async function pullPermissions() {
    if (!store.token) return []
    try {
      // 后台静默同步：不弹加载蒙层
      const codes = (await accountApi.getPermissions(SILENT)) || []
      store.setPermissions(codes)
      return codes
    } catch {
      // 拉取失败不阻断主流程；此时按权限显示的入口退化为仅按角色判断，避免误隐藏
      store.setPermissions([])
      return []
    }
  }

  function hasPermission(code) {
    return store.role === 'ADMIN' || store.permissions.includes(code)
  }

  return { pullPermissions, hasPermission }
}

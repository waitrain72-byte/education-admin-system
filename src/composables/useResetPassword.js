import { confirm } from '@/utils/confirm'
import { t } from '@/i18n'

/**
 * 重置密码（管理员 / 教师 / 学生管理页共用，重置为后端默认密码）：
 *   const resetPassword = useResetPassword(teacherApi, 'teacher')
 *   <button @click="resetPassword(item)">
 * - api：账号接口（adminApi / teacherApi / studentApi），调用其 resetPassword(id)
 * - ns：词条命名空间，确认 / 成功文案取 pages.{ns}.resetConfirm / resetSuccess
 */
export function useResetPassword(api, ns) {
  return async (row) => {
    const ok = await confirm(t(`pages.${ns}.resetConfirm`, { username: row.username }), {
      title: t('common.resetPassword'),
    })
    if (!ok) return
    try {
      await api.resetPassword(row.id)
      uni.showToast({ title: t(`pages.${ns}.resetSuccess`), icon: 'none' })
    } catch {
      // 请求层已统一提示
    }
  }
}

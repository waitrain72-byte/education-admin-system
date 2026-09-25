import { useUserStore } from '@/stores/user'
import { chooseImage, uploadFile } from '@/utils/upload'

/**
 * 用户管理页（管理员 / 教师 / 学生）共用的表单逻辑。
 */

/** 头像上传：选图（压缩图、超限本地拦截）后上传，写回 form.avatar；失败提示由上传层统一弹出 */
export function useAvatarUpload(form) {
  return async () => {
    const picked = await chooseImage()
    if (!picked) return
    try {
      form.value.avatar = await uploadFile(picked.path)
    } catch {
      // 提示已由上传层统一弹出
    }
  }
}

/**
 * 生成 afterSave：保存的正是「当前登录账号自己」时，把指定字段同步到全局用户状态（头像 / 姓名立即生效）。
 * 管理员、教师、学生分表存储，id 只在同一角色内唯一，所以必须同时比较角色——
 * 只比 id 的话，管理员（id=1）编辑 1 号教师或学生，会把自己的姓名头像改成对方的。
 */
export function syncCurrentUser(role, fields) {
  return (formData) => {
    const userStore = useUserStore()
    if (userStore.role !== role || formData.id !== userStore.user.id) return
    userStore.patchUser(Object.fromEntries(fields.map((k) => [k, formData[k]])))
  }
}

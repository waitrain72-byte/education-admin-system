import { t } from '@/i18n'

/**
 * Promise 版确认框，替代各页面 uni.showModal({ success(res) { if (res.confirm) … } }) 的回调写法：
 *
 *   if (!(await confirm(t('pages.apply.deleteConfirm')))) return
 *
 * - title 默认「确认删除」（多数确认场景是删除 / 撤销），其它场景自行传入
 * - 用户点取消、点遮罩关闭或接口调用失败时 resolve(false)
 */
export function confirm(content, { title } = {}) {
  return new Promise((resolve) => {
    uni.showModal({
      title: title || t('common.confirmDeleteTitle'),
      content,
      success: (res) => resolve(!!(res && res.confirm)),
      fail: () => resolve(false),
    })
  })
}

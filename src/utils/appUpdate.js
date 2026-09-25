import { t } from '@/i18n'
import { confirm } from '@/utils/confirm'

/**
 * 小程序新版本提示（微信官方推荐的更新机制做法）：
 * 冷启动时微信会在后台检查新版本，但本次仍运行本地旧版本，要等下次冷启动才生效；
 * 这里在新版本下载完成后提示用户立即重启，发布修复后用户马上就能用上。
 * 只在提供 getUpdateManager 的平台（微信小程序等）生效，H5 上直接跳过。
 */
export function checkAppUpdate() {
  if (typeof uni.getUpdateManager !== 'function') return
  const manager = uni.getUpdateManager()
  manager.onUpdateReady(async () => {
    if (await confirm(t('update.readyContent'), { title: t('update.title') })) manager.applyUpdate()
  })
  // 新版本下载失败（多为网络问题）：提示用户删除小程序后重新打开，以拉取最新版本
  manager.onUpdateFailed(() => {
    uni.showModal({ title: t('update.title'), content: t('update.failedContent'), showCancel: false })
  })
}

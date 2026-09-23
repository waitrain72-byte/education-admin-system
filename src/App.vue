<script>
import { initWs, connectWs } from '@/utils/websocket'
import { useUserStore } from '@/stores/user'
import { syncNativeChrome } from '@/composables/useTheme'
import { installAuthInterceptor } from '@/utils/authGuard'
import { reportError } from '@/utils/errorReport'

export default {
  onLaunch() {
    // 路由拦截：未登录时拦下代码发起的跳转（tabBar 点击 / 冷启动直达由各页 onShow 的 ensureLoggedIn 兜底）
    installAuthInterceptor()
    // 应用启动：主题与语言偏好由各模块自行从本地存储恢复
    // 注册网络状态监听（仅一次），网络恢复时自动补连实时通知
    initWs()
  },
  onShow() {
    // 冷启动带登录态 / 切回前台时恢复实时通知连接（connectWs 幂等，无 token 不连）
    if (useUserStore().token) connectWs()
    // 兜底同步原生导航栏/tabBar 配色（模块加载时页面可能尚未就绪）
    syncNativeChrome()
  },

  // ===== 全局错误兜底：未被页面捕获的异常统一上报，不再静默丢失 =====
  onError(err) {
    reportError('runtime', err)
  },
  onUnhandledRejection(res) {
    reportError('unhandledRejection', res && res.reason)
  },
  // 分享卡片 / 旧版本链接指向已不存在的页面时，回到首页而不是白屏
  onPageNotFound(res) {
    reportError('pageNotFound', res && res.path)
    uni.reLaunch({ url: '/pages/home/home' })
  },
}
</script>

<style lang="scss">
@import '@/styles/theme.scss';
</style>

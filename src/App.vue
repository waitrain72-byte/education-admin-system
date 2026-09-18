<script>
import { initWs, connectWs } from '@/utils/websocket'
import { useUserStore } from '@/stores/user'
import { syncNativeChrome } from '@/composables/useTheme'

export default {
  onLaunch() {
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
}
</script>

<style lang="scss">
@import '@/styles/theme.scss';
</style>

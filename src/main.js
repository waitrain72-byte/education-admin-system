import { createSSRApp } from 'vue'
import * as Pinia from 'pinia'
import App from './App.vue'
import { installI18n } from './i18n'
import { themeClass, syncNativeChrome } from './composables/useTheme'
import { themeStyle } from './composables/useThemeColor'
import { shareMessage, currentRoute } from './utils/share'

export function createApp() {
  const app = createSSRApp(App)
  app.use(Pinia.createPinia())
  installI18n(app)
  // 全局混入：每个页面根节点绑定 :class="themeClass" :style="themeStyle" 即可跟随主题
  // - themeClass：深浅模式（切换 theme.scss 中的整套变量）
  // - themeStyle：自定义主题色（覆盖品牌色变量；默认色时为空串，完全回落到样式表）
  app.mixin({
    computed: {
      themeClass() {
        return themeClass.value
      },
      themeStyle() {
        return themeStyle.value
      },
    },
    // 原生导航栏配色（setNavigationBarColor）只作用于当前页：每个页面显示时按当前主题补同步一次，
    // 否则「应用内选了深色、系统是浅色」时，新打开的页面导航栏仍是 app.json 默认的亮蓝色
    onShow() {
      if (this.$mpType === 'page') syncNativeChrome()
    },
    // 右上角「转发」：公开信息页转发当前页，其余转发首页（规则见 utils/share.js）
    onShareAppMessage() {
      return shareMessage(currentRoute())
    },
  })
  return {
    app,
    Pinia,
  }
}

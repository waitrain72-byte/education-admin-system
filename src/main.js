import { createSSRApp } from 'vue'
import * as Pinia from 'pinia'
import App from './App.vue'
import { installI18n } from './i18n'
import { themeClass } from './composables/useTheme'
import { themeStyle } from './composables/useThemeColor'

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
  })
  return {
    app,
    Pinia,
  }
}

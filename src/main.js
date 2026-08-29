import { createSSRApp } from 'vue'
import * as Pinia from 'pinia'
import App from './App.vue'
import { installI18n } from './i18n'
import { themeClass } from './composables/useTheme'

export function createApp() {
  const app = createSSRApp(App)
  app.use(Pinia.createPinia())
  installI18n(app)
  // 全局混入：每个页面根节点绑定 :class="themeClass" 即可跟随主题
  app.mixin({
    computed: {
      themeClass() {
        return themeClass.value
      },
    },
  })
  return {
    app,
    Pinia,
  }
}

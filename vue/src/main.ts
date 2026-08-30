import { createApp } from 'vue'
import { createPinia } from 'pinia'
import {
    ChatRound, Check, CircleClose, Clock, HomeFilled, Medal, Message, Setting,
    Opportunity, Plus, Stamp, Star, User, Warning,
} from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
// Element Plus 暗色变量：配合 <html> 上的 dark 类生效
import 'element-plus/theme-chalk/dark/css-vars.css'
// 全局样式：基础 reset + 主题变量 + 后台布局样式（manager.css 曾放在 Manager.vue 的
// style @import 中，Vite 热更新场景下会偶发丢失导致布局错乱，现统一在入口引入）
import '@/assets/css/global.css'
import '@/assets/css/theme.css'
import '@/assets/css/manager.css'

import App from './App.vue'
import router from './router/router-index.ts'
import request from './utils/request'
import i18n from '@/i18n'
import { permission } from '@/directives/permission'

const app = createApp(App)

declare global {
    interface Window {
        $baseUrl: string
    }
}
// 仅注册项目中实际使用到的图标，避免全量注册 294 个图标带来的包体积浪费
const usedIcons = {
    ChatRound, Check, CircleClose, Clock, HomeFilled, Medal, Message, Setting,
    Opportunity, Plus, Stamp, Star, User, Warning,
}
for (const [key, component] of Object.entries(usedIcons)) {
    app.component(key, component)
}

// ===== 添加这一行：全局配置 baseUrl =====
const baseUrl = import.meta.env.VITE_BASE_URL
app.config.globalProperties.$baseUrl = baseUrl

// 也可以挂载到 window 上，方便其他地方使用
window.$baseUrl = baseUrl

app.use(createPinia())
app.use(i18n)
app.use(router)
app.directive('permission', permission)

// 挂载 request 到全局（可选，推荐直接在各组件中 import 使用）
app.config.globalProperties.$request = request

app.mount('#app')

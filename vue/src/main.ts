import { createApp } from 'vue'
import { createPinia } from 'pinia'
import {
    ChatRound, Check, CircleClose, Clock, HomeFilled, Medal, Message,
    Opportunity, Plus, Stamp, Star, User, Warning,
} from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router/router-index.ts'
import request from './utils/request'
import { permission } from '@/directives/permission'

const app = createApp(App)

declare global {
    interface Window {
        $baseUrl: string
    }
}
// 仅注册项目中实际使用到的图标，避免全量注册 294 个图标带来的包体积浪费
const usedIcons = {
    ChatRound, Check, CircleClose, Clock, HomeFilled, Medal, Message,
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
app.use(router)
app.directive('permission', permission)

// 挂载 request 到全局（可选，推荐直接在各组件中 import 使用）
app.config.globalProperties.$request = request

app.mount('#app')

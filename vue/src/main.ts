import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router/router-index.ts'
import request from './utils/request'

const app = createApp(App)

declare global {
    interface Window {
        $baseUrl: string
    }
}
// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

// ===== 添加这一行：全局配置 baseUrl =====
const baseUrl = import.meta.env.VITE_BASE_URL
app.config.globalProperties.$baseUrl = baseUrl

// 也可以挂载到 window 上，方便其他地方使用
window.$baseUrl = baseUrl

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 挂载 request 到全局（可选，推荐直接在各组件中 import 使用）
app.config.globalProperties.$request = request

app.mount('#app')

import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router/router-index.ts'
import { useUserStore } from '@/stores/user'
import { t } from '@/i18n'

const request: AxiosInstance = axios.create({
    baseURL: import.meta.env.VITE_BASE_URL,
    timeout: 10000,
    withCredentials: true,
})

// 请求拦截器：自动带上 token
request.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        // 从统一状态管理读取 token，避免重复 JSON.parse 且缺少异常保护
        const userStore = useUserStore()
        if (userStore.token) {
            config.headers['token'] = userStore.token
        }
        return config
    },
    (error) => Promise.reject(error)
)

// 响应拦截器：统一处理错误码
request.interceptors.response.use(
    (response: AxiosResponse) => {
        const res = response.data as { code?: string; msg?: string }
        if (res.code === '401') {
            ElMessage.error(t('errors.401'))
            // 同步清除内存与本地存储，避免 401 后页面仍显示已登录
            useUserStore().clearUser()
            router.push('/login')
            return Promise.reject(new Error(res.msg || t('errors.401')))
        }
        return response
    },
    (error) => {
        ElMessage.error(error.message || t('request.failed'))
        return Promise.reject(error)
    }
)

export default request

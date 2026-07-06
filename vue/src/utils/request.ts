import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router/router-index.ts'

const request: AxiosInstance = axios.create({
    baseURL: import.meta.env.VITE_BASE_URL,
    timeout: 10000,
    withCredentials: true,
})

// 请求拦截器：自动带上 token
request.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        const userStr = localStorage.getItem('xm-user')
        if (userStr) {
            const user = JSON.parse(userStr) as { token?: string }
            if (user.token) {
                config.headers['token'] = user.token
            }
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
            ElMessage.error('请先登录')
            localStorage.removeItem('xm-user')
            router.push('/login')
            return Promise.reject(new Error(res.msg || '未授权'))
        }
        return response
    },
    (error) => {
        ElMessage.error(error.message || '请求失败')
        return Promise.reject(error)
    }
)

export default request

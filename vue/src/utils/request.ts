import axios, { type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from '@/utils/element-plus'
import router from '@/router/router-index.ts'
import { useUserStore } from '@/stores/user'
import { apiMessage, t } from '@/i18n'

/** 后端统一返回结构 */
export interface ApiResult<T = unknown> {
    code: string
    msg?: string
    data: T
}

/** 分页接口的 data 结构（PageHelper 的 PageInfo） */
export interface PageResult<T> {
    list: T[]
    total: number
}

/** 业务错误码（非 200）对应的异常，提示已由拦截器统一弹出，调用方一般只需忽略 */
export class ApiError extends Error {
    readonly code: string

    constructor(code: string, message: string) {
        super(message)
        this.name = 'ApiError'
        this.code = code
    }
}

const instance = axios.create({
    baseURL: import.meta.env.VITE_BASE_URL,
    timeout: 10000,
    withCredentials: true,
})

// 请求拦截器：自动带上 token
instance.interceptors.request.use(
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

/** 错误提示去重窗口：并行请求同时失败时只弹一次，避免 toast 叠一屏 */
const TOAST_DEDUPE_MS = 2000
const recentToasts = new Map<string, number>()

function toastOnce(message: string): void {
    const now = Date.now()
    // 顺带清理过期条目，避免这张表随运行时间无限增长
    for (const [key, at] of recentToasts) {
        if (now - at > TOAST_DEDUPE_MS) {
            recentToasts.delete(key)
        }
    }
    if (recentToasts.has(message)) {
        return
    }
    recentToasts.set(message, now)
    ElMessage.error(message)
}

/** 是否为后端的 { code, msg, data } 包装（二进制响应与裸数据不是） */
function isApiResult(payload: unknown): payload is ApiResult {
    return typeof payload === 'object' && payload !== null && 'code' in payload
}

/**
 * 响应拦截器：统一解包与错误处理。
 *
 * <p>成功（code 200）直接返回业务数据本身，调用方拿到的就是 `data`，
 * 不必再层层写 `res.data.data` 与 `if (res.data.code === '200')`；
 * 失败统一弹一次提示并 reject，调用方只需 `try/await` 后忽略异常即可。</p>
 *
 * <p>验证码图片、Excel 导出等二进制响应没有 code 包装，原样返回 Blob。</p>
 */
instance.interceptors.response.use(
    (response: AxiosResponse) => {
        const payload = response.data
        if (!isApiResult(payload)) {
            // responseType: 'blob' 等场景：直接把内容给调用方
            return payload
        }
        if (payload.code === '200') {
            return payload.data
        }
        if (payload.code === '401') {
            toastOnce(t('errors.401'))
            // 同步清除内存与本地存储，避免 401 后页面仍显示已登录
            useUserStore().clearUser()
            router.push('/login')
            return Promise.reject(new ApiError(payload.code, payload.msg || t('errors.401')))
        }
        const message = apiMessage(payload)
        toastOnce(message)
        return Promise.reject(new ApiError(payload.code, message))
    },
    (error) => {
        toastOnce(error.message || t('request.failed'))
        return Promise.reject(error)
    }
)

/**
 * 请求门面：返回值类型即业务数据类型（拦截器已解包）。
 * axios 自身的类型签名返回 AxiosResponse，与解包后的运行时行为不符，故在此重新声明。
 */
export interface Request {
    <T = unknown>(config: AxiosRequestConfig): Promise<T>
    get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
    post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
    put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
    delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
}

const request = instance as unknown as Request

export default request

import { baseUrl } from './config'
import { useUserStore } from '@/stores/user'
import { closeWs } from './websocket'
import { apiMessage, t } from '@/i18n'
import { hideLoading, resetLoading, showLoading } from './loading'

/**
 * 小程序没有浏览器自动管理 Cookie 的机制，验证码依赖 Session，
 * 这里手动保存验证码响应返回的 Cookie，并在后续请求中回传。
 */
let cookie = ''

/** 401 处理防抖：并发请求同时 401 时只提示/跳转一次（3 秒窗口） */
let handling401 = false

export function saveCookie(res: any): void {
  const c =
    res.cookies && res.cookies[0]
      ? res.cookies[0]
      : res.header && (res.header['Set-Cookie'] || res.header['set-cookie'])
  if (c) cookie = Array.isArray(c) ? c.join('; ') : c
}

export function clearCookie(): void {
  cookie = ''
}

function toast(msg: string): void {
  uni.showToast({ title: msg, icon: 'none' })
}

/** 错误提示去重窗口：并发请求同时失败时只弹一次，避免 toast 相互覆盖、闪烁 */
const TOAST_DEDUPE_MS = 2000
const recentToasts = new Map<string, number>()

function toastOnce(msg: string): void {
  const now = Date.now()
  for (const [key, at] of recentToasts) {
    if (now - at > TOAST_DEDUPE_MS) recentToasts.delete(key)
  }
  if (recentToasts.has(msg)) return
  recentToasts.set(msg, now)
  toast(msg)
}

/** 业务错误码（非 200）对应的异常；提示已由请求层统一弹出，调用方一般只需忽略 */
export class ApiError extends Error {
  readonly code: string

  constructor(code: string, message: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

/** 是否为后端的 { code, msg, data } 包装 */
function isApiResult(payload: any): boolean {
  return payload !== null && typeof payload === 'object' && 'code' in payload
}

export interface RequestOptions {
  url: string
  data?: Record<string, any>
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  header?: Record<string, string>
  /** 是否显示统一加载动画（xm-loader），默认开启；静默的后台同步请求可显式关闭 */
  loading?: boolean
}

/** 按失败原因归类提示文案：超时 / 其他 */
function describeFail(err: any): string {
  const msg: string = (err && err.errMsg) || ''
  if (/timeout/i.test(msg)) return t('request.timeout')
  return t('request.failed')
}

/** 网络层失败（未拿到响应）的统一提示：区分断网 / 超时 / 其他（请求与上传共用） */
export function toastRequestFail(err: any): void {
  uni.getNetworkType({
    success: (net: any) => {
      toastOnce(net.networkType === 'none' ? t('request.offline') : describeFail(err))
    },
    fail: () => toastOnce(t('request.failed')),
  })
}

/**
 * 登录态失效（401）的统一处理（请求与上传共用）：提示、复位加载动画、断开实时通知并回登录页。
 * 并发请求同时 401 时只处理一次，避免重复 toast 与多次 reLaunch。
 */
export function handleUnauthorized(data: any): void {
  if (handling401) return
  handling401 = true
  toast(apiMessage(data))
  resetLoading()
  closeWs()
  useUserStore().clearUser()
  uni.reLaunch({ url: '/pages/login/login' })
  setTimeout(() => {
    handling401 = false
  }, 3000)
}

/** 业务失败（非 200）：提示一次并返回对应的 ApiError（请求与上传共用） */
export function toApiError(data: any): ApiError {
  const message = apiMessage(data)
  toastOnce(message)
  return new ApiError(data.code, message)
}

/**
 * 统一请求封装：与 Web 端 axios 拦截器语义一致
 * - 自动携带 token（自定义 header 'token'）与验证码会话 Cookie
 * - 计数式统一加载动画：并发请求在蒙层上叠加计数，全部结束才收起
 * - **成功（code 200）直接 resolve 业务数据本身**，调用方拿到的就是 data，
 *   不必再层层写 res.data.data 与 if (res.data.code === '200')
 * - **业务失败（非 200）统一弹一次提示并 reject ApiError**，调用方 try/await 后忽略即可
 * - 失败时区分「断网 / 超时 / 其他」给出可读提示
 * - 401 统一提示、复位动画并跳转登录页
 */
export function request<T = any>(options: RequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    const userStore = useUserStore()
    const header = Object.assign({}, options.header || {})
    if (userStore.token) header['token'] = userStore.token
    if (cookie) header['Cookie'] = cookie
    const withLoader = options.loading !== false
    if (withLoader) showLoading()
    uni.request({
      url: baseUrl + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      timeout: 10000,
      header,
      success: (res: any) => {
        const data = res.data
        if (data && data.code === '401') {
          handleUnauthorized(data)
          reject(new Error('401'))
          return
        }
        // 没有 { code, msg, data } 包装的响应（理论上不会出现）原样返回
        if (!isApiResult(data)) {
          resolve(data)
          return
        }
        if (data.code === '200') {
          resolve(data.data)
          return
        }
        reject(toApiError(data))
      },
      fail: (err: any) => {
        toastRequestFail(err)
        reject(err)
      },
      complete: () => {
        if (withLoader) hideLoading()
      },
    })
  })
}

/** 静默请求：不唤起全屏加载蒙层（后台刷新、下拉选项等不阻塞用户操作的请求） */
export const SILENT: Partial<RequestOptions> = Object.freeze({ loading: false })

export const get = <T = any>(url: string, data?: Record<string, any>, opts?: Partial<RequestOptions>) =>
  request<T>({ ...opts, url, data })
export const post = <T = any>(url: string, data?: Record<string, any>, opts?: Partial<RequestOptions>) =>
  request<T>({ ...opts, url, data, method: 'POST' })
export const put = <T = any>(url: string, data?: Record<string, any>, opts?: Partial<RequestOptions>) =>
  request<T>({ ...opts, url, data, method: 'PUT' })
export const del = <T = any>(url: string, data?: Record<string, any>, opts?: Partial<RequestOptions>) =>
  request<T>({ ...opts, url, data, method: 'DELETE' })

/**
 * 文件/头像地址归一化：统一转成「当前 baseUrl + /files/xxx」的完整地址，仅用于展示层。
 * 存库仍存后端返回的原始值，不能把拼好的地址存回去（否则换 Wi-Fi / 换电脑后又会失效）。
 * - 老数据是绝对地址（如 http://localhost:9091/files/xxx）：真机上 localhost 指向手机本身必然加载失败，
 *   剥掉主机部分换成当前 baseUrl，换网络环境也不受影响
 * - 新上传返回相对路径 /api/files/xxx：/api 仅为 Web 端代理与 nginx 的前缀，直连后端没有，去掉后再拼 baseUrl
 */
export function resolveFileUrl(url?: string): string {
  if (!url) return ''
  const fileIndex = url.indexOf('/files/')
  if (url.startsWith('http')) {
    return fileIndex >= 0 ? baseUrl + url.slice(fileIndex) : url
  }
  if (url.startsWith('/api/files/')) {
    return baseUrl + url.slice(4)
  }
  if (url.startsWith('/files/')) {
    return baseUrl + url
  }
  return url
}

/**
 * 「取数或返回 null」：成功返回业务数据，失败返回 null（提示已由请求层统一弹出）。
 * 适用于拉取下拉选项/列表这类失败时只需兜底为空、不需要分支处理的场景：
 *   courseData.value = (await orNull(courseApi.selectAll(undefined, SILENT))) || []
 */
export const orNull = <T>(promise: Promise<T>): Promise<T | null> => promise.catch(() => null)

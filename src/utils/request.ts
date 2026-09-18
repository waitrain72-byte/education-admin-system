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

/**
 * 统一请求封装：与 Web 端 axios 封装语义一致
 * - 自动携带 token（自定义 header 'token'）与验证码会话 Cookie
 * - 计数式统一加载动画：并发请求在蒙层上叠加计数，全部结束才收起
 * - 失败时区分「断网 / 超时 / 其他」给出可读提示
 * - 401 统一提示、复位动画并跳转登录页
 */
export function request(options: RequestOptions): Promise<any> {
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
          // 并发请求同时 401 时只处理一次，避免重复 toast 与多次 reLaunch
          if (!handling401) {
            handling401 = true
            toast(apiMessage(data))
            // 登录态已失效：复位加载动画，断开实时通知连接后回登录页
            resetLoading()
            closeWs()
            userStore.clearUser()
            uni.reLaunch({ url: '/pages/login/login' })
            setTimeout(() => {
              handling401 = false
            }, 3000)
          }
          reject(new Error('401'))
          return
        }
        resolve(res)
      },
      fail: (err: any) => {
        uni.getNetworkType({
          success: (net: any) => {
            toast(net.networkType === 'none' ? t('request.offline') : describeFail(err))
          },
          fail: () => toast(t('request.failed')),
        })
        reject(err)
      },
      complete: () => {
        if (withLoader) hideLoading()
      },
    })
  })
}

export const get = (url: string, data?: Record<string, any>, opts?: Partial<RequestOptions>) =>
  request({ ...opts, url, data })
export const post = (url: string, data?: Record<string, any>, opts?: Partial<RequestOptions>) =>
  request({ ...opts, url, data, method: 'POST' })
export const put = (url: string, data?: Record<string, any>, opts?: Partial<RequestOptions>) =>
  request({ ...opts, url, data, method: 'PUT' })
export const del = (url: string, data?: Record<string, any>, opts?: Partial<RequestOptions>) =>
  request({ ...opts, url, data, method: 'DELETE' })

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
 * 简化取数：code === '200' 时直接返回 data.data，
 * 否则统一 toast 错误信息并返回 null。
 * 适用于"拉取下拉选项/列表数据"这类不需要分支处理的场景，
 * 替代页面里成对的 code 判断 + toast 样板代码。
 */
export const getData = async (url: string, data?: Record<string, any>) => {
  try {
    const res = await get(url, data)
    if (res.data && res.data.code === '200') {
      return res.data.data
    }
    uni.showToast({ title: apiMessage(res.data), icon: 'none' })
  } catch {
    // 请求层已统一提示
  }
  return null
}

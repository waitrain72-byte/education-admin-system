import { baseUrl } from './config'
import { useUserStore } from '@/stores/user'
import { apiMessage, t } from '@/i18n'

/**
 * 小程序没有浏览器自动管理 Cookie 的机制，验证码依赖 Session，
 * 这里手动保存验证码响应返回的 Cookie，并在后续请求中回传。
 */
let cookie = ''

export function saveCookie(res) {
  const c =
    res.cookies && res.cookies[0]
      ? res.cookies[0]
      : res.header && (res.header['Set-Cookie'] || res.header['set-cookie'])
  if (c) cookie = Array.isArray(c) ? c.join('; ') : c
}

export function clearCookie() {
  cookie = ''
}

function toast(msg) {
  uni.showToast({ title: msg, icon: 'none' })
}

/**
 * 统一请求封装：与 Web 端 axios 封装语义一致
 * - 自动携带 token（自定义 header 'token'）与验证码会话 Cookie
 * - 401 统一提示并跳转登录页
 */
export function request(options) {
  return new Promise((resolve, reject) => {
    const userStore = useUserStore()
    const header = Object.assign({}, options.header || {})
    if (userStore.token) header['token'] = userStore.token
    if (cookie) header['Cookie'] = cookie
    uni.request({
      url: baseUrl + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      timeout: 10000,
      header,
      success: (res) => {
        const data = res.data
        if (data && data.code === '401') {
          toast(apiMessage(data))
          userStore.clearUser()
          uni.reLaunch({ url: '/pages/login/login' })
          reject(new Error('401'))
          return
        }
        resolve(res)
      },
      fail: (err) => {
        toast(t('request.failed'))
        reject(err)
      },
    })
  })
}

export const get = (url, data) => request({ url, data })
export const post = (url, data) => request({ url, method: 'POST', data })
export const put = (url, data) => request({ url, method: 'PUT', data })
export const del = (url, data) => request({ url, method: 'DELETE', data })

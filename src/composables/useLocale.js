import { locale, setLocale, t } from '@/i18n'
import { useUserStore } from '@/stores/user'
import { get, put } from '@/utils/request'

/**
 * 语言偏好（与 Web 端 useLocale 语义一致）：
 * - localStorage 持久化，登录后从后端拉取覆盖本地，切换后防抖推送后端
 */
let serverLocale = ''
let pushTimer = null

export { locale, setLocale, t }

export function currentLocale() {
  return locale.value
}

export function isZhLocale() {
  return locale.value === 'zh-CN'
}

function pushLocale(next) {
  try {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn || next === serverLocale) return
    if (pushTimer) clearTimeout(pushTimer)
    pushTimer = setTimeout(() => {
      put('/locale', { locale: next })
        .then(() => {
          serverLocale = next
        })
        .catch(() => {})
    }, 500)
  } catch {
    // Pinia 未就绪时忽略
  }
}

export function changeLocale(next) {
  if (next !== 'zh-CN' && next !== 'en-US') return
  setLocale(next)
  pushLocale(next)
}

export function toggleLocale() {
  changeLocale(locale.value === 'zh-CN' ? 'en-US' : 'zh-CN')
}

/** 登录成功后调用：用后端保存的语言偏好覆盖本地 */
export async function pullLocaleFromServer() {
  try {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) return
    const res = await get('/locale')
    const value = res && res.data && res.data.data
    if (value !== 'zh-CN' && value !== 'en-US') return
    serverLocale = value
    if (locale.value !== value) {
      setLocale(value)
    }
  } catch {
    // 拉取失败时保留本地语言
  }
}

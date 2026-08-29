import { i18n, type AppLocale } from '@/i18n'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

/**
 * 语言偏好组合式 API（与 useTheme 同一套模式）：
 * - localStorage 持久化，刷新不丢失
 * - 登录后可从后端拉取用户保存的语言偏好，实现多端同步
 * - 切换后防抖推送到后端
 */
const STORAGE_KEY = 'xm-locale'

/** 最近一次从后端拉取的值，防止"拉取回显 → 触发推送"的回环 */
let serverLocale = ''
let pushTimer: ReturnType<typeof setTimeout> | null = null

export function currentLocale(): AppLocale {
    return (i18n.global.locale.value as AppLocale) || 'zh-CN'
}

export function setLocale(locale: AppLocale) {
    if (locale === currentLocale()) return
    i18n.global.locale.value = locale
    localStorage.setItem(STORAGE_KEY, locale)
    if (pushTimer) clearTimeout(pushTimer)
    pushTimer = setTimeout(() => {
        if (!isLoggedIn() || locale === serverLocale) return
        request.put('/locale', { locale }).then(() => {
            serverLocale = locale
        }).catch(() => {
            // 同步失败不打断使用，下次切换会重新尝试
        })
    }, 500)
}

function isLoggedIn(): boolean {
    try {
        return useUserStore().isLoggedIn
    } catch {
        return false
    }
}

/** 登录成功后调用：用后端保存的语言偏好覆盖本地 */
export async function pullLocaleFromServer() {
    if (!isLoggedIn()) return
    try {
        const res = await request.get('/locale')
        const value = res.data?.data
        if (value !== 'zh-CN' && value !== 'en-US') return
        serverLocale = value
        if (currentLocale() !== value) {
            i18n.global.locale.value = value
            localStorage.setItem(STORAGE_KEY, value)
        }
    } catch {
        // 拉取失败时保留本地语言
    }
}

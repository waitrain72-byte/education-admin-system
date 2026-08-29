import { createI18n } from 'vue-i18n'
import zhCN from '@/locales/zh-CN'
import enUS from '@/locales/en-US'
import groupAZh from '@/locales/pages/zh-CN/groupA'
import groupAEn from '@/locales/pages/en-US/groupA'
import groupBZh from '@/locales/pages/zh-CN/groupB'
import groupBEn from '@/locales/pages/en-US/groupB'
import groupCZh from '@/locales/pages/zh-CN/groupC'
import groupCEn from '@/locales/pages/en-US/groupC'

/**
 * 深合并语言包：各页面分组模块都向 pages 命名空间贡献键，
 * 需要 deep merge 而不是浅展开，否则后引入的分组会覆盖前面的 pages。
 */
function deepMerge(target: Record<string, any>, source: Record<string, any>): Record<string, any> {
    const result: Record<string, any> = { ...target }
    for (const key of Object.keys(source)) {
        const a = result[key]
        const b = source[key]
        if (a && b && typeof a === 'object' && typeof b === 'object' && !Array.isArray(a) && !Array.isArray(b)) {
            result[key] = deepMerge(a, b)
        } else {
            result[key] = b
        }
    }
    return result
}

export const SUPPORTED_LOCALES = ['zh-CN', 'en-US'] as const
export type AppLocale = (typeof SUPPORTED_LOCALES)[number]

// 消息结构标注为宽类型，避免 vue-i18n 对深层嵌套消息的递归类型展开（TS2589）
const messages = {
    'zh-CN': deepMerge(zhCN, deepMerge(groupAZh, deepMerge(groupBZh, groupCZh))),
    'en-US': deepMerge(enUS, deepMerge(groupAEn, deepMerge(groupBEn, groupCEn))),
}

// 放宽 vue-i18n 的消息 Schema 为索引签名（官方推荐做法），避免深层嵌套消息的递归类型展开
declare module 'vue-i18n' {
    export interface DefineLocaleMessage {
        [key: string]: string | DefineLocaleMessage
    }
}

const i18n = createI18n<{
    'zh-CN': Record<string, any>
    'en-US': Record<string, any>
}, 'zh-CN' | 'en-US', false>({
    legacy: false,
    globalInjection: true,
    locale: localStorage.getItem('xm-locale') || 'zh-CN',
    fallbackLocale: 'zh-CN',
    missingWarn: false,
    fallbackWarn: false,
    messages,
})

/** 脚本（非模板）中使用的全局翻译函数，响应语言切换 */
export const t = i18n.global.t

/**
 * 按后端错误码取本地化提示：已知码返回翻译，未知码回退显示后端原始 msg。
 * 用于统一展示后端 Result 中的中文提示语。
 */
export function apiMessage(data: { code?: string; msg?: string } | null | undefined): string {
    const code = data?.code
    if (code) {
        const key = `errors.${code}`
        if (i18n.global.te(key)) {
            return i18n.global.t(key)
        }
    }
    return data?.msg || i18n.global.t('errors.fallback')
}

export default i18n
export { i18n }

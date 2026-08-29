import { ref } from 'vue'
import zhCN from '@/locales/zh-CN'
import enUS from '@/locales/en-US'
import groupAZh from '@/locales/pages/zh-CN/groupA'
import groupAEn from '@/locales/pages/en-US/groupA'
import groupBZh from '@/locales/pages/zh-CN/groupB'
import groupBEn from '@/locales/pages/en-US/groupB'
import groupCZh from '@/locales/pages/zh-CN/groupC'
import groupCEn from '@/locales/pages/en-US/groupC'

/**
 * 轻量国际化（与 Web 端 vue-i18n 词条键完全一致）：
 * - 模板中 $t('xxx.yyy')，脚本中 import { t } 使用
 * - t 内部读取 locale ref，语言切换后页面自动重渲染
 * - 缺键回退中文，中文也缺失时显示键名，永不报错
 */
function deepMerge(target, source) {
  const result = { ...target }
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

const STORAGE_KEY = 'xm-locale'

export const locale = ref(uni.getStorageSync(STORAGE_KEY) || 'zh-CN')

const messages = {
  'zh-CN': deepMerge(zhCN, deepMerge(groupAZh, deepMerge(groupBZh, groupCZh))),
  'en-US': deepMerge(enUS, deepMerge(groupAEn, deepMerge(groupBEn, groupCEn))),
}

export function isZh() {
  return locale.value === 'zh-CN'
}

export function setLocale(l) {
  if (l !== 'zh-CN' && l !== 'en-US') return
  locale.value = l
  uni.setStorageSync(STORAGE_KEY, l)
}

function lookup(dict, key) {
  return key.split('.').reduce((o, k) => (o == null ? undefined : o[k]), dict)
}

export function t(key, params) {
  let text = lookup(messages[locale.value], key)
  if (text == null) text = lookup(messages['zh-CN'], key)
  if (typeof text !== 'string') return key
  if (params) {
    for (const k of Object.keys(params)) {
      text = text.split('{' + k + '}').join(params[k])
    }
  }
  return text
}

/** 按后端错误码取本地化提示，未知码回退后端原始 msg */
export function apiMessage(data) {
  const code = data && data.code
  if (code) {
    const known = lookup(messages[locale.value], 'errors.' + code)
    if (typeof known === 'string') return known
  }
  return (data && data.msg) || t('errors.fallback')
}

export function installI18n(app) {
  app.config.globalProperties.$t = t
}

import { beforeEach, describe, expect, it } from 'vitest'
import { apiMessage, isZh, locale, setLocale, t } from '@/i18n'

beforeEach(() => {
  setLocale('zh-CN')
})

describe('i18n 轻量国际化', () => {
  it('默认语言为 zh-CN', () => {
    expect(locale.value).toBe('zh-CN')
    expect(isZh()).toBe(true)
  })

  it('t() 读取词条', () => {
    expect(t('common.search')).toBe('查询')
    expect(t('menu.home')).toBe('系统首页')
  })

  it('t() 支持参数插值', () => {
    expect(t('home.welcome', { name: '张三' })).toBe('您好，张三！欢迎使用本系统')
  })

  it('切换语言后词条随之切换并持久化', () => {
    setLocale('en-US')
    expect(t('common.search')).toBe('Search')
    expect(isZh()).toBe(false)
    expect(uni.setStorageSync).toHaveBeenCalledWith('xm-locale', 'en-US')
  })

  it('非法语言值被忽略', () => {
    setLocale('fr-FR')
    expect(locale.value).toBe('zh-CN')
  })

  it('缺失键回退显示键名且不抛错', () => {
    expect(t('no.such.key')).toBe('no.such.key')
  })

  it('apiMessage 已知错误码本地化', () => {
    expect(apiMessage({ code: '5003' })).toBe('账号或密码错误')
  })

  it('apiMessage 未知码回退后端原始 msg', () => {
    expect(apiMessage({ code: '9999', msg: 'boom' })).toBe('boom')
  })

  it('apiMessage 空数据回退兜底文案', () => {
    expect(apiMessage(null)).toBe('操作失败')
  })
})

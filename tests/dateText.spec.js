import { afterEach, describe, expect, it } from 'vitest'
import { dayText, greetingKey } from '@/utils/dateText'
import { setLocale } from '@/i18n'

const at = (hh, mm = 0) => new Date(2026, 8, 24, hh, mm) // 2026-09-24 周四

afterEach(() => setLocale('zh-CN'))

describe('dayText 日期展示', () => {
  it('中文：月日 + 周几', () => {
    expect(dayText(at(9))).toBe('9月24日 周四')
  })

  it('英文：星期缩写 + 月份缩写 + 日', () => {
    setLocale('en-US')
    expect(dayText(at(9))).toBe('Thu, Sep 24')
  })
})

describe('greetingKey 按时段问候', () => {
  it('早上 / 中午 / 下午 / 晚上的边界', () => {
    expect(greetingKey(at(4, 59))).toBe('home.greetEvening')
    expect(greetingKey(at(5))).toBe('home.greetMorning')
    expect(greetingKey(at(10, 59))).toBe('home.greetMorning')
    expect(greetingKey(at(11))).toBe('home.greetNoon')
    expect(greetingKey(at(13))).toBe('home.greetAfternoon')
    expect(greetingKey(at(17, 59))).toBe('home.greetAfternoon')
    expect(greetingKey(at(18))).toBe('home.greetEvening')
  })
})

import { afterEach, describe, expect, it } from 'vitest'
import {
  parseExamTime,
  examCountdown,
  countdownTag,
  countdownTagClass,
  sortByExamTime,
  splitExamTime,
  joinExamTime,
  isCompleteExamTime,
} from '@/utils/examCountdown'
import { setLocale } from '@/i18n'

// 固定「现在」：2026-09-25 15:30（本地时间），与 Web 端用例一致
const NOW = new Date(2026, 8, 25, 15, 30)

afterEach(() => setLocale('zh-CN'))

describe('parseExamTime', () => {
  it('按本地时间解析 yyyy-MM-dd HH:mm（不依赖 iOS 不支持的 Date 字符串解析）', () => {
    expect(parseExamTime('2026-12-20 09:05')).toEqual(new Date(2026, 11, 20, 9, 5))
    expect(parseExamTime('2026-12-20')).toEqual(new Date(2026, 11, 20, 0, 0))
    expect(parseExamTime('2028-02-29 23:59')).toEqual(new Date(2028, 1, 29, 23, 59))
  })

  it('空值、格式不对、不存在的日期或时刻返回 null', () => {
    for (const bad of [
      undefined,
      null,
      '',
      '明天',
      '2026/12/20 09:00',
      '2026-02-30 09:00',
      '2027-02-29 09:00',
      '2026-12-20 24:00',
      '2026-12-20 09:60',
      '2026-13-01',
    ]) {
      expect(parseExamTime(bad), String(bad)).toBeNull()
    }
  })
})

describe('examCountdown 按自然日计算', () => {
  it('今天（不管开考时刻是否已过）/ 明天凌晨算 1 天 / 跨月跨年 / 已结束', () => {
    expect(examCountdown('2026-09-25 08:00', NOW)).toEqual({ status: 'today', days: 0 })
    expect(examCountdown('2026-09-25 23:59', NOW)).toEqual({ status: 'today', days: 0 })
    expect(examCountdown('2026-09-26 00:10', NOW)).toEqual({ status: 'upcoming', days: 1 })
    expect(examCountdown('2026-10-01 09:00', NOW)).toEqual({ status: 'upcoming', days: 6 })
    expect(examCountdown('2027-01-05 09:00', NOW)).toEqual({ status: 'upcoming', days: 102 })
    expect(examCountdown('2026-09-24 23:59', NOW)).toEqual({ status: 'ended', days: -1 })
    expect(examCountdown(null, NOW)).toBeNull()
  })
})

describe('countdownTag 文案与配色', () => {
  it('中文：今天 / 明天 / 还有 N 天 / 已结束', () => {
    expect(countdownTag('2026-09-25 18:00', NOW)).toEqual({ text: '今天', cls: 'xm-tag-danger' })
    expect(countdownTag('2026-09-26 08:30', NOW)).toEqual({ text: '明天', cls: 'xm-tag-danger' })
    expect(countdownTag('2026-10-01 09:00', NOW)).toEqual({ text: '还有 6 天', cls: 'xm-tag-warning' })
    expect(countdownTag('2026-11-02 09:00', NOW)).toEqual({ text: '还有 38 天', cls: 'xm-tag-brand' })
    expect(countdownTag('2024-11-29 19:00', NOW)).toEqual({ text: '已结束', cls: '' })
    expect(countdownTag('', NOW)).toBeNull()
  })

  it('英文文案', () => {
    setLocale('en-US')
    expect(countdownTag('2026-09-26 08:30', NOW).text).toBe('Tomorrow')
    expect(countdownTag('2026-10-01 09:00', NOW).text).toBe('In 6 days')
  })

  it('配色阈值：3 天内红、7 天内橙、更远主色、已结束灰', () => {
    expect(countdownTagClass({ status: 'upcoming', days: 3 })).toBe('xm-tag-danger')
    expect(countdownTagClass({ status: 'upcoming', days: 4 })).toBe('xm-tag-warning')
    expect(countdownTagClass({ status: 'upcoming', days: 7 })).toBe('xm-tag-warning')
    expect(countdownTagClass({ status: 'upcoming', days: 8 })).toBe('xm-tag-brand')
  })
})

describe('sortByExamTime 首页排序', () => {
  it('未结束的由近到远 → 已结束的（刚结束的在前）→ 没有考试时间的保持原顺序；不修改原数组', () => {
    const rows = [
      { id: 1, examTime: null },
      { id: 2, examTime: '2026-09-20 09:00' },
      { id: 3, examTime: '2026-11-02 09:00' },
      { id: 4, examTime: '2026-09-25 08:00' },
      { id: 5, examTime: '' },
      { id: 6, examTime: '2024-11-29 19:00' },
      { id: 7, examTime: '2026-09-26 08:30' },
    ]
    expect(sortByExamTime(rows, NOW).map((r) => r.id)).toEqual([4, 7, 3, 2, 6, 1, 5])
    expect(rows.map((r) => r.id)).toEqual([1, 2, 3, 4, 5, 6, 7])
    expect(sortByExamTime(undefined, NOW)).toEqual([])
  })
})

describe('表单：日期 + 开考时间两个选择器', () => {
  it('拆分：完整值 / 只有日期 / 只有时间 / 空值', () => {
    expect(splitExamTime('2026-12-20 09:00')).toEqual({ date: '2026-12-20', time: '09:00' })
    expect(splitExamTime('2026-12-20')).toEqual({ date: '2026-12-20', time: '' })
    expect(splitExamTime(' 09:00')).toEqual({ date: '', time: '09:00' })
    expect(splitExamTime(null)).toEqual({ date: '', time: '' })
    expect(splitExamTime('2026-12-20 09:00:00')).toEqual({ date: '2026-12-20', time: '' })
  })

  it('合并后再拆分能还原（先选时间再选日期也不丢）', () => {
    expect(joinExamTime('2026-12-20', '09:00')).toBe('2026-12-20 09:00')
    for (const [d, tm] of [
      ['2026-12-20', '09:00'],
      ['2026-12-20', ''],
      ['', '09:00'],
      ['', ''],
    ]) {
      expect(splitExamTime(joinExamTime(d, tm))).toEqual({ date: d, time: tm })
    }
  })

  it('保存前要求日期和开考时间都选了且真实存在', () => {
    expect(isCompleteExamTime('2026-12-20 09:00')).toBe(true)
    expect(isCompleteExamTime('2026-12-20')).toBe(false)
    expect(isCompleteExamTime(' 09:00')).toBe(false)
    expect(isCompleteExamTime('2026-02-30 09:00')).toBe(false)
    expect(isCompleteExamTime(null)).toBe(false)
  })
})

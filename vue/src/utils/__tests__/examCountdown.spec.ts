import { describe, it, expect } from 'vitest'
import { parseExamTime, examCountdown, countdownText, countdownTagType, sortByExamTime } from '@/utils/examCountdown'

// 固定「现在」：2026-09-25 15:30（本地时间）
const NOW = new Date(2026, 8, 25, 15, 30)
const t = (key: string, params?: Record<string, unknown>) => (params ? `${key}:${params.n}` : key)

describe('parseExamTime', () => {
  it('按本地时间解析 yyyy-MM-dd HH:mm，只有日期时按 0 点', () => {
    expect(parseExamTime('2026-12-20 09:05')).toEqual(new Date(2026, 11, 20, 9, 5))
    expect(parseExamTime('2026-12-20')).toEqual(new Date(2026, 11, 20, 0, 0))
    expect(parseExamTime('2028-02-29 23:59')).toEqual(new Date(2028, 1, 29, 23, 59))
  })

  it('空值、格式不对、不存在的日期或时刻返回 null', () => {
    for (const bad of [undefined, null, '', '明天', '2026/12/20 09:00', '2026-02-30 09:00', '2027-02-29 09:00', '2026-12-20 24:00', '2026-12-20 09:60', '2026-13-01']) {
      expect(parseExamTime(bad as any), String(bad)).toBeNull()
    }
  })
})

describe('examCountdown 按自然日计算', () => {
  it('今天：不管当天几点开考、开考时刻是否已过', () => {
    expect(examCountdown('2026-09-25 08:00', NOW)).toEqual({ status: 'today', days: 0 })
    expect(examCountdown('2026-09-25 23:59', NOW)).toEqual({ status: 'today', days: 0 })
  })

  it('明天凌晨也算 1 天（不是按 24 小时折算）', () => {
    expect(examCountdown('2026-09-26 00:10', NOW)).toEqual({ status: 'upcoming', days: 1 })
  })

  it('跨月、跨年', () => {
    expect(examCountdown('2026-10-01 09:00', NOW)).toEqual({ status: 'upcoming', days: 6 })
    expect(examCountdown('2027-01-05 09:00', NOW)).toEqual({ status: 'upcoming', days: 102 })
  })

  it('昨天及更早为已结束', () => {
    expect(examCountdown('2026-09-24 23:59', NOW)).toEqual({ status: 'ended', days: -1 })
    expect(examCountdown('2024-11-29 19:00', NOW)?.status).toBe('ended')
  })

  it('没有考试时间或格式不对时不显示倒计时', () => {
    expect(examCountdown(null, NOW)).toBeNull()
    expect(examCountdown('2026-02-30 09:00', NOW)).toBeNull()
  })
})

describe('文案与配色', () => {
  it('今天 / 明天 / 还有 N 天 / 已结束', () => {
    expect(countdownText({ status: 'today', days: 0 }, t)).toBe('pages.examplan.countdownToday')
    expect(countdownText({ status: 'upcoming', days: 1 }, t)).toBe('pages.examplan.countdownTomorrow')
    expect(countdownText({ status: 'upcoming', days: 12 }, t)).toBe('pages.examplan.countdownDays:12')
    expect(countdownText({ status: 'ended', days: -3 }, t)).toBe('pages.examplan.countdownEnded')
  })

  it('3 天内红、7 天内橙、更远主色、已结束灰', () => {
    expect(countdownTagType({ status: 'today', days: 0 })).toBe('danger')
    expect(countdownTagType({ status: 'upcoming', days: 3 })).toBe('danger')
    expect(countdownTagType({ status: 'upcoming', days: 4 })).toBe('warning')
    expect(countdownTagType({ status: 'upcoming', days: 7 })).toBe('warning')
    expect(countdownTagType({ status: 'upcoming', days: 8 })).toBe('primary')
    expect(countdownTagType({ status: 'ended', days: -1 })).toBe('info')
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
    const sorted = sortByExamTime(rows, NOW).map((r) => r.id)
    expect(sorted).toEqual([4, 7, 3, 2, 6, 1, 5])
    expect(rows.map((r) => r.id)).toEqual([1, 2, 3, 4, 5, 6, 7])
  })
})

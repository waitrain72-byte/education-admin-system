import { describe, it, expect } from 'vitest'
import { deadlineState, heatLevel, latestWeek, maskName, nameList } from '@/utils/dashboard'

describe('最近一周出勤', () => {
  it('和紧挨着的上一周比，保留一位小数', () => {
    expect(latestWeek([{ week: 4, rate: 88.9 }, { week: 5, rate: 94.4 }])).toEqual({ week: 5, rate: 94.4, delta: 5.5 })
    expect(latestWeek([{ week: 4, rate: 96 }, { week: 5, rate: 92.5 }])?.delta).toBe(-3.5)
  })

  it('上一周没有记录（放假）或只有一周时不比；没有数据为 null', () => {
    expect(latestWeek([{ week: 3, rate: 90 }, { week: 5, rate: 95 }])?.delta).toBeNull()
    expect(latestWeek([{ week: 1, rate: 100 }])?.delta).toBeNull()
    expect(latestWeek([])).toBeNull()
    expect(latestWeek(undefined)).toBeNull()
  })
})

describe('作业截止状态', () => {
  const now = '2026-10-13 10:38'
  it('截止时间过了就是已截止', () => {
    expect(deadlineState('2026-10-12 23:59', now)).toEqual({ kind: 'closed', days: 0 })
    expect(deadlineState('', now).kind).toBe('closed')
  })

  it('今天截止 / 还剩几天（按日历日）', () => {
    expect(deadlineState('2026-10-13 23:59', now)).toEqual({ kind: 'today', days: 0 })
    expect(deadlineState('2026-10-15 23:59', now)).toEqual({ kind: 'open', days: 2 })
  })
})

describe('投屏打码', () => {
  it('只留姓，其余换成 *', () => {
    expect(maskName('吴十')).toBe('吴*')
    expect(maskName('欧阳娜娜')).toBe('欧***')
    expect(maskName('李')).toBe('*')
    expect(maskName('')).toBe('')
    expect(maskName(null)).toBe('')
  })
})

describe('排课热力档位', () => {
  it('0 / 1 / 2–4 / 5 门以上', () => {
    expect([0, 1, 2, 4, 5, 30].map(heatLevel)).toEqual([0, 1, 2, 2, 3, 3])
  })
})

describe('名单拼接', () => {
  const more = (n: number) => `等 ${n} 门`
  it('数量超过列出的名字时补一句', () => {
    expect(nameList(['线性代数', '体育'], 5, '、', more)).toBe('线性代数、体育 等 5 门')
    expect(nameList(['线性代数'], 1, '、', more)).toBe('线性代数')
    expect(nameList(undefined, 0, '、', more)).toBe('')
  })
})

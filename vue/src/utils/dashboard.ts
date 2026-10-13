/**
 * 数据大屏的纯函数：最近一周出勤的涨跌、作业截止状态、投屏时的姓名打码、排课热力档位、名单拼接。
 * 不依赖请求层与组件，便于单测（utils/__tests__/dashboard.spec.ts）。
 */
import { daysBetween } from './schedule'

export interface WeekPoint {
    week: number
    rate: number
    present?: number
    counted?: number
}

/**
 * 最近一周的出勤率，以及与紧挨着的上一周相比的涨跌（百分点，一位小数）。
 * 上一周没有考勤记录（如放假）时不比，delta 为 null。
 */
export function latestWeek(weekly: WeekPoint[] | null | undefined): { week: number; rate: number; delta: number | null } | null {
    const list = weekly || []
    if (!list.length) return null
    const last = list[list.length - 1]
    const prev = list.length > 1 ? list[list.length - 2] : null
    const delta = prev && prev.week === last.week - 1 ? Math.round((last.rate - prev.rate) * 10) / 10 : null
    return { week: last.week, rate: last.rate, delta }
}

export type DeadlineKind = 'closed' | 'today' | 'open'

/**
 * 作业截止状态（时间都是「yyyy-MM-dd HH:mm」）：已截止 / 今天截止 / 还剩几天（按日历日算）。
 */
export function deadlineState(deadline: string | null | undefined, now: string): { kind: DeadlineKind; days: number } {
    if (!deadline || deadline < now) return { kind: 'closed', days: 0 }
    const days = daysBetween(now.slice(0, 10), deadline.slice(0, 10))
    return days <= 0 ? { kind: 'today', days: 0 } : { kind: 'open', days }
}

/** 投屏时隐藏学生姓名：只留第一个字（姓），其余换成 * */
export function maskName(name: string | null | undefined): string {
    const text = String(name || '')
    if (!text) return ''
    return text.length === 1 ? '*' : text[0] + '*'.repeat(text.length - 1)
}

/** 排课热力格的深浅：0 门 / 1 门 / 2–4 门 / 5 门以上 */
export function heatLevel(count: number): 0 | 1 | 2 | 3 {
    if (count <= 0) return 0
    if (count === 1) return 1
    return count <= 4 ? 2 : 3
}

/** 名单拼接：列出给到的名字，实际数量更多时在后面补一句（如「等 12 门」） */
export function nameList(names: string[] | null | undefined, total: number, separator: string, more: (n: number) => string): string {
    const list = names || []
    const shown = list.join(separator)
    return total > list.length ? `${shown} ${more(total)}` : shown
}

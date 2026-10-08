/**
 * 课表相关的纯函数：节次时间解析、上课状态判定、按星期分组。
 * 不依赖请求层与组件，便于单测（utils/__tests__/schedule.spec.ts）。
 */

/** 课程表 week 字段的中文值（数据库按中文存储），下标 0 = 星期一 */
export const WEEKDAYS = ['星期一', '星期二', '星期三', '星期四', '星期五', '星期六', '星期日'] as const

/** 全部大节（与后端 SegmentEnum、课程表 segment 字段的中文值一致） */
export const SEGMENTS = [
    '第一大节（08:30 ~ 10:10）',
    '第二大节（10:30 ~ 12:10）',
    '第三大节（14:00 ~ 15:40）',
    '第四大节（16:00 ~ 17:40）',
    '第五大节（19:00 ~ 20:40）',
] as const

/** 从「第一大节（08:30 ~ 10:10）」解析起止时间，解析不到返回 ['', ''] */
export function parseSegmentTime(segment: string | null | undefined): [string, string] {
    const m = /(\d{1,2}:\d{2})\s*~\s*(\d{1,2}:\d{2})/.exec(segment || '')
    if (!m) return ['', '']
    const pad = (t: string) => (t.length === 4 ? '0' + t : t)
    return [pad(m[1]), pad(m[2])]
}

/** 「第一大节（08:30 ~ 10:10）」→「第一大节」 */
export function segmentShortName(segment: string | null | undefined): string {
    return String(segment || '').split(/[（(]/)[0].trim()
}

/** 节次先后（未知节次排最后） */
export function segmentIndex(segment: string | null | undefined): number {
    const i = SEGMENTS.indexOf(segment as (typeof SEGMENTS)[number])
    return i === -1 ? SEGMENTS.length : i
}

/** 星期下标（0 = 星期一；未知为 -1） */
export function weekdayIndex(week: string | null | undefined): number {
    return WEEKDAYS.indexOf(week as (typeof WEEKDAYS)[number])
}

export function toMinutes(hhmm: string): number {
    const [h, m] = String(hhmm).split(':').map(Number)
    return (h || 0) * 60 + (m || 0)
}

export type ClassState = 'upcoming' | 'ongoing' | 'done' | ''

/**
 * 按「当前时刻（HH:mm）」判定一节课的状态。
 * 当前时刻由调用方传入：页面统一用服务器时间（useServerClock），不用浏览器本地时间——
 * 学校按北京时间排课，浏览器时区设成别的地方时本地时间会把正在上的课判成已结束。
 */
export function classState(segment: string | null | undefined, nowHHmm: string): ClassState {
    const [start, end] = parseSegmentTime(segment)
    if (!start || !end || !nowHHmm) return ''
    const now = toMinutes(nowHHmm)
    if (now < toMinutes(start)) return 'upcoming'
    if (now > toMinutes(end)) return 'done'
    return 'ongoing'
}

/** 距离开始还有多少分钟（已开始返回 0；解析不到返回 null） */
export function minutesUntil(segment: string | null | undefined, nowHHmm: string): number | null {
    const [start] = parseSegmentTime(segment)
    if (!start || !nowHHmm) return null
    return Math.max(0, toMinutes(start) - toMinutes(nowHHmm))
}

/** 今天剩下的课里最近的一节（正在上的优先），没有返回 null */
export function nextClass<T extends { segment?: string | null }>(courses: T[], nowHHmm: string): T | null {
    const sorted = [...courses].sort((a, b) => segmentIndex(a.segment) - segmentIndex(b.segment))
    return (
        sorted.find((c) => classState(c.segment, nowHHmm) === 'ongoing') ||
        sorted.find((c) => classState(c.segment, nowHHmm) === 'upcoming') ||
        null
    )
}

/** 按小时给出问候语的 i18n 键：5–11 点上午，11–13 中午，13–18 下午，其余晚上 */
export function greetingKey(nowHHmm: string): string {
    const hour = Math.floor(toMinutes(nowHHmm) / 60)
    if (hour >= 5 && hour < 11) return 'workbench.greeting.morning'
    if (hour >= 11 && hour < 13) return 'workbench.greeting.noon'
    if (hour >= 13 && hour < 18) return 'workbench.greeting.afternoon'
    return 'workbench.greeting.evening'
}

// ---------- 日期（只处理 yyyy-MM-dd 墙上日期，一律按 UTC 承载，不受浏览器时区影响） ----------

/** 「2026-10-08」→ 承载这一天的 Date（UTC 零点）；格式不对返回 null */
export function isoToDate(iso: string | null | undefined): Date | null {
    const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso || '')
    if (!m) return null
    return new Date(Date.UTC(+m[1], +m[2] - 1, +m[3]))
}

/** Date（UTC 承载）→「2026-10-08」 */
export function dateToIso(d: Date): string {
    return `${d.getUTCFullYear()}-${String(d.getUTCMonth() + 1).padStart(2, '0')}-${String(d.getUTCDate()).padStart(2, '0')}`
}

/** 日期加减天数 */
export function addDays(iso: string, n: number): string {
    const d = isoToDate(iso)
    if (!d) return iso
    d.setUTCDate(d.getUTCDate() + n)
    return dateToIso(d)
}

/** 两个日期相差几天（b - a） */
export function daysBetween(a: string, b: string): number {
    const da = isoToDate(a)
    const db = isoToDate(b)
    if (!da || !db) return 0
    return Math.round((db.getTime() - da.getTime()) / 86400000)
}

/** 「yyyy-MM」加减月份 */
export function addMonths(month: string, n: number): string {
    const m = /^(\d{4})-(\d{2})/.exec(month)
    if (!m) return month
    const total = +m[1] * 12 + (+m[2] - 1) + n
    return `${Math.floor(total / 12)}-${String((total % 12) + 1).padStart(2, '0')}`
}

/** 按界面语言格式化日期，如中文「10月8日」、英文「Oct 8」 */
export function formatDay(iso: string, locale: string, options: Intl.DateTimeFormatOptions = { month: 'short', day: 'numeric' }): string {
    const d = isoToDate(iso)
    if (!d) return iso
    return new Intl.DateTimeFormat(locale, { ...options, timeZone: 'UTC' }).format(d)
}

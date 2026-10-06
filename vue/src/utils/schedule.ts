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

/**
 * 考试倒计时（考试安排列表与首页共用；小程序端 utils/examCountdown.js 是同一套规则）：
 * - 按「自然日」算相差天数：今天 / 明天 / 还有 N 天 / 已结束
 * - 手动解析 yyyy-MM-dd HH:mm，不用 new Date(字符串)：iOS / Safari 不认「2026-12-20 09:00」这种写法
 */

export type CountdownStatus = 'upcoming' | 'today' | 'ended'

export interface ExamCountdown {
  status: CountdownStatus
  /** 距考试的自然日天数：今天为 0，已结束为负数 */
  days: number
}

const PATTERN = /^(\d{4})-(\d{2})-(\d{2})(?:[ T](\d{2}):(\d{2}))?/
const DAY_MS = 24 * 60 * 60 * 1000

/** 解析考试时间；为空、格式不对或日期不存在（如 2 月 30 日、25 点）时返回 null */
export function parseExamTime(value?: string | null): Date | null {
  if (!value) return null
  const m = PATTERN.exec(String(value).trim())
  if (!m) return null
  const [year, month, day] = [Number(m[1]), Number(m[2]), Number(m[3])]
  const hour = m[4] === undefined ? 0 : Number(m[4])
  const minute = m[5] === undefined ? 0 : Number(m[5])
  if (hour > 23 || minute > 59) return null
  const date = new Date(year, month - 1, day, hour, minute)
  // Date 会把 2 月 30 日自动进位成 3 月 2 日，这里要求年月日原样对得上
  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) return null
  return date
}

const startOfDay = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()

/** 距考试的自然日天数与状态；没有考试时间或格式不对时返回 null */
export function examCountdown(value?: string | null, now: Date = new Date()): ExamCountdown | null {
  const exam = parseExamTime(value)
  if (!exam) return null
  // round 而不是 floor：跨夏令时切换的那天只有 23 / 25 小时
  const days = Math.round((startOfDay(exam) - startOfDay(now)) / DAY_MS)
  if (days < 0) return { status: 'ended', days }
  if (days === 0) return { status: 'today', days }
  return { status: 'upcoming', days }
}

/** 倒计时文案（t 为当前语言的翻译函数） */
export function countdownText(cd: ExamCountdown, t: (key: string, params?: Record<string, unknown>) => string): string {
  if (cd.status === 'ended') return t('pages.examplan.countdownEnded')
  if (cd.status === 'today') return t('pages.examplan.countdownToday')
  if (cd.days === 1) return t('pages.examplan.countdownTomorrow')
  return t('pages.examplan.countdownDays', { n: cd.days })
}

/** 标签配色：当天与 3 天内红色，一周内橙色，更远用主色，已结束灰色 */
export function countdownTagType(cd: ExamCountdown): 'danger' | 'warning' | 'primary' | 'info' {
  if (cd.status === 'ended') return 'info'
  if (cd.days <= 3) return 'danger'
  if (cd.days <= 7) return 'warning'
  return 'primary'
}

/**
 * 首页排序：未结束的按考试时间由近到远，已结束的排在后面（刚结束的在前），
 * 没有考试时间的历史数据放最后并保持原顺序。不修改传入的数组。
 */
export function sortByExamTime<T extends { examTime?: string | null }>(rows: T[], now: Date = new Date()): T[] {
  const items = rows.map((row, index) => {
    const exam = parseExamTime(row.examTime)
    const cd = examCountdown(row.examTime, now)
    const rank = !cd ? 2 : cd.status === 'ended' ? 1 : 0
    return { row, index, rank, time: exam ? exam.getTime() : 0 }
  })
  items.sort((a, b) => {
    if (a.rank !== b.rank) return a.rank - b.rank
    if (a.rank === 0) return a.time - b.time
    if (a.rank === 1) return b.time - a.time
    return a.index - b.index
  })
  return items.map((x) => x.row)
}

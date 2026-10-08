/**
 * 课程空间共用的纯函数：考勤状态映射、服务器时间字符串换算、文件大小、成绩权重。
 * 与 Web 端 vue/src/utils/courseSpace.ts 一致；不依赖组件与请求层，便于单测（tests/courseSpace.spec.js）。
 */

/** 考勤状态（数据库按中文存）→ 语言包键与标签配色 */
export const ATTENDANCE_STATUSES = [
  { value: '正常', key: 'normal', tag: 'xm-tag-success' },
  { value: '迟到', key: 'late', tag: 'xm-tag-warning' },
  { value: '早退', key: 'early', tag: 'xm-tag-warning' },
  { value: '缺勤', key: 'absent', tag: 'xm-tag-danger' },
  { value: '请假', key: 'leave', tag: 'xm-tag-info' },
] as const

export function attendanceMeta(status: string | null | undefined) {
  return ATTENDANCE_STATUSES.find((s) => s.value === status) || null
}

/**
 * 出勤率（百分比，一位小数），与后端首页口径一致：迟到、早退算出勤，缺勤不算，请假不计入分母。
 * 没有可计入的记录返回 null。
 */
export function attendanceRateOf(counts: Record<string, number> | null | undefined): number | null {
  if (!counts) return null
  let total = 0
  for (const value of Object.values(counts)) total += Number(value) || 0
  const counted = total - (Number(counts['请假']) || 0)
  if (counted <= 0) return null
  return Math.round(((counted - (Number(counts['缺勤']) || 0)) * 1000) / counted) / 10
}

/** 作业状态（后端返回中文）→ 语言包键与标签配色 */
export const ASSIGNMENT_STATUSES: Record<string, { key: string; tag: string }> = {
  未提交: { key: 'pending', tag: '' },
  已提交: { key: 'submitted', tag: 'xm-tag-info' },
  已批改: { key: 'graded', tag: 'xm-tag-success' },
  已逾期: { key: 'overdue', tag: 'xm-tag-danger' },
}

/**
 * 服务器给的墙上时间「yyyy-MM-dd HH:mm[:ss]」→ 毫秒数（按 UTC 承载，不做时区换算）。
 * 两个服务器时间相减就是真实间隔，和手机所在时区无关。格式不对返回 NaN。
 */
export function wallMs(text: string | null | undefined): number {
  const m = /^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})(?::(\d{2}))?/.exec(text || '')
  if (!m) return NaN
  return Date.UTC(+m[1], +m[2] - 1, +m[3], +m[4], +m[5], +(m[6] || 0))
}

/** 剩余秒数 → 「m:ss」；负数按 0 */
export function formatCountdown(seconds: number): string {
  const s = Math.max(0, Math.floor(seconds))
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`
}

/** 字节数 → 「1.2 MB」；空值返回 — */
export function formatSize(bytes: number | null | undefined): string {
  if (bytes == null || !Number.isFinite(bytes) || bytes < 0) return '—'
  if (bytes < 1024) return `${bytes} B`
  const units = ['KB', 'MB', 'GB']
  let value = bytes / 1024
  let unit = 0
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024
    unit += 1
  }
  return `${value >= 10 ? Math.round(value) : value.toFixed(1)} ${units[unit]}`
}

/** 文件扩展名（小写，不带点），用来挑图标 */
export function fileExt(name: string | null | undefined): string {
  const m = /\.([a-z0-9]+)$/i.exec(name || '')
  return m ? m[1].toLowerCase() : ''
}

/** 文件类别：决定资料列表里的图标颜色 */
export function fileKind(
  name: string | null | undefined,
): 'pdf' | 'doc' | 'sheet' | 'slide' | 'image' | 'archive' | 'text' | 'file' {
  const ext = fileExt(name)
  if (ext === 'pdf') return 'pdf'
  if (['doc', 'docx'].includes(ext)) return 'doc'
  if (['xls', 'xlsx'].includes(ext)) return 'sheet'
  if (['ppt', 'pptx'].includes(ext)) return 'slide'
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(ext)) return 'image'
  if (['zip', 'rar', '7z'].includes(ext)) return 'archive'
  if (['txt', 'md'].includes(ext)) return 'text'
  return 'file'
}

/** 姓名或学号包含关键字（忽略大小写与首尾空格） */
export function matchesPerson(person: { name?: string | null; username?: string | null }, keyword: string): boolean {
  const k = keyword.trim().toLowerCase()
  if (!k) return true
  return (person.name || '').toLowerCase().includes(k) || (person.username || '').toLowerCase().includes(k)
}

/** 成绩册四项权重：合计是否正好 100、每项是否 0~100 的整数 */
export function weightsValid(weights: Record<string, number | null | undefined>): boolean {
  const values = ['attendance', 'homework', 'ordinary', 'exam'].map((k) => weights[k])
  if (values.some((v) => v == null || !Number.isInteger(v) || (v as number) < 0 || (v as number) > 100)) return false
  return values.reduce<number>((sum, v) => sum + (v as number), 0) === 100
}

/**
 * 与后端 GradeCalculator.total 一致的总评：有权重的项都有分才算，保留一位小数；否则返回 null（待录）。
 * 老师在成绩册里改分时用来即时预览，保存后以后端算的为准。
 */
export function previewTotal(
  weights: { attendance: number; homework: number; ordinary: number; exam: number },
  values: { attendance: number | null; homework: number | null; ordinary: number | null; exam: number | null },
): number | null {
  let sum = 0
  for (const key of ['attendance', 'homework', 'ordinary', 'exam'] as const) {
    const w = weights[key]
    if (!w) continue
    const v = values[key]
    if (v == null || Number.isNaN(v)) return null
    sum += w * v
  }
  return Math.round((sum / 100) * 10) / 10
}

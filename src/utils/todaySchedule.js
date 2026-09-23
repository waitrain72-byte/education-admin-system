/**
 * 首页「今日课程」的纯函数：节次时间解析、上课状态判定、课表单元格拆分。
 * 不依赖 uni / 请求层，便于单测（tests/todaySchedule.spec.js）。
 */

/** 课表接口的星期列名，下标与 Date#getDay() 对应（0 = 周日） */
export const WEEKDAY_FIELDS = ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday']
/** 课程表 week 字段的中文值，下标同上 */
export const WEEKDAY_ZH = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']

/** 从「第一大节（08:30 ~ 10:10）」解析起止时间，解析不到返回 ['', ''] */
export function parseSegmentTime(segment) {
  const m = /(\d{1,2}:\d{2})\s*~\s*(\d{1,2}:\d{2})/.exec(segment || '')
  return m ? [m[1], m[2]] : ['', '']
}

export function toMinutes(hhmm) {
  const [h, m] = String(hhmm).split(':').map(Number)
  return (h || 0) * 60 + (m || 0)
}

/** 按当前时间判定：todo 未开始 / doing 进行中 / done 已结束；节次无时间时返回 '' */
export function classStatus(segment, now = new Date()) {
  const [start, end] = parseSegmentTime(segment)
  if (!start || !end) return ''
  const minutes = now.getHours() * 60 + now.getMinutes()
  if (minutes < toMinutes(start)) return 'todo'
  if (minutes > toMinutes(end)) return 'done'
  return 'doing'
}

/**
 * 拆分学生课表单元格。单元格形如「课程名（教师）\n其它信息」：
 * 拆成 name = 课程名、sub = 「教师 · 其它信息」，与演示稿「名称 / 信息」两行结构一致。
 */
export function parseCurriculumCell(cell) {
  const text = String(cell).trim()
  const lines = text.split('\n')
  const first = lines[0].trim()
  const m = /^(.*?)[（(]([^）(]*)[）)]\s*$/.exec(first)
  const name = m ? m[1].trim() : first
  const sub = [m && m[2].trim(), lines.slice(1).join(' ')].filter(Boolean).join(' · ')
  return { text, name, sub }
}

/** 补齐起止时间与状态，并按开始时间排序（不依赖后端返回顺序，保证「下一节」判定正确） */
export function decorateToday(courses, now = new Date()) {
  return courses
    .map((c) => {
      const [start, end] = parseSegmentTime(c.segment)
      return { ...c, start, end, status: classStatus(c.segment, now) }
    })
    .sort((a, b) => toMinutes(a.start || '23:59') - toMinutes(b.start || '23:59'))
}

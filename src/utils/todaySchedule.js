/**
 * 首页「今日课程」与「我的课表」周视图的纯函数：节次时间解析、上课状态判定、课表单元格拆分、
 * 周课表网格构建、当前时间线定位。不依赖 uni / 请求层，便于单测（tests/todaySchedule.spec.js）。
 */

/** 课表接口的星期列名，下标与 Date#getDay() 对应（0 = 周日） */
export const WEEKDAY_FIELDS = ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday']
/** 课程表 week 字段的中文值，下标同上 */
export const WEEKDAY_ZH = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
/** 周视图列顺序（周一在前） */
export const WEEK_COLUMNS = ['monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday']
/** 全部大节（与后端 SegmentEnum、课程表 segment 字段的中文值一致，数据库按中文存储） */
export const SEGMENTS = [
  '第一大节（08:30 ~ 10:10）',
  '第二大节（10:30 ~ 12:10）',
  '第三大节（14:00 ~ 15:40）',
  '第四大节（16:00 ~ 17:40）',
  '第五大节（19:00 ~ 20:40）',
]

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

/**
 * 由学生选课记录（/choice/selectAll，已联表带出课程的星期/大节/教室/教师）构建周课表网格：
 * rows[i] = { segment, start, end, cells: { monday: [课程...], ... } }。
 * - 行固定为 SEGMENTS 全部大节（空行也保留，保证网格稳定）；数据里出现的未知大节追加在末尾
 * - 同一格可能有多门课（选课冲突的历史数据），按数组保留，由界面决定如何展示
 */
export function buildWeekGrid(choices) {
  const list = Array.isArray(choices) ? choices : []
  const segments = [...SEGMENTS]
  list.forEach((c) => {
    if (c && c.segment && !segments.includes(c.segment)) segments.push(c.segment)
  })
  return segments.map((segment) => {
    const [start, end] = parseSegmentTime(segment)
    const cells = {}
    WEEK_COLUMNS.forEach((f) => (cells[f] = []))
    list.forEach((c) => {
      if (!c || c.segment !== segment) return
      const field = WEEKDAY_FIELDS[WEEKDAY_ZH.indexOf(c.week)]
      if (field) cells[field].push(c)
    })
    return { segment, start, end, cells }
  })
}

/** 周六、周日是否有课：没有时周视图默认收起周末两列，工作日列更宽 */
export function hasWeekendClass(rows) {
  return (rows || []).some((r) => r.cells.saturday.length || r.cells.sunday.length)
}

/**
 * 当前时间在网格中的纵向位置（单位：行）：第 i 节进行中返回 i + 已上课比例；
 * 两节之间的课间返回下一节的行首 i；第一节开始前、最后一节结束后返回 -1（不画时间线）。
 */
export function nowRowPosition(rows, now = new Date()) {
  const minutes = now.getHours() * 60 + now.getMinutes()
  for (let i = 0; i < (rows || []).length; i += 1) {
    const { start, end } = rows[i]
    if (!start || !end) continue
    const s = toMinutes(start)
    const e = toMinutes(end)
    if (minutes < s) return i === 0 ? -1 : i
    if (minutes <= e) return i + (minutes - s) / (e - s || 1)
  }
  return -1
}

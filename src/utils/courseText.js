import { t } from '@/i18n'
import { SEGMENTS, segmentIndex, segmentShortName, weekdayIndex } from './schedule'

/**
 * 课程字段的显示文字：星期、大节、课程类型、状态在库里按中文存，这里按当前语言翻译；
 * 认不出的值（历史数据）原样显示。课程卡片、课程空间、日程、教务后台共用。
 */

/** 「星期一」→「周一」/「Mon」；long 时「星期一」/「Monday」 */
export function weekLabel(week, long = false) {
  const i = weekdayIndex(week)
  if (i < 0) return week || ''
  return t(`timetable.${long ? 'weekdayLong' : 'weekday'}.${i + 1}`)
}

/** 「第一大节（08:30 ~ 10:10）」→「第一大节」/「Period 1」 */
export function segmentLabel(segment) {
  const i = segmentIndex(segment)
  if (i >= SEGMENTS.length) return segmentShortName(segment)
  return t(`timetable.segment.${i + 1}`)
}

/** 上课时间一行：「周一 第一大节 · 教室」；没排课时「未排课」 */
export function courseWhen(course, withRoom = true) {
  if (!course) return ''
  const parts = []
  if (course.week) parts.push([weekLabel(course.week), segmentLabel(course.segment)].filter(Boolean).join(' '))
  else parts.push(t('courses.unscheduled'))
  if (withRoom && course.room) parts.push(course.room)
  return parts.join(' · ')
}

const TYPE_KEYS = { 必修: 'required', 选修: 'elective' }

/** 课程类型「必修 / 选修」 */
export function courseTypeLabel(type) {
  return TYPE_KEYS[type] ? t(`timetable.type.${TYPE_KEYS[type]}`) : type || '—'
}

/** 课程状态 → 文字与标签配色；「已开课」是常态，课程卡片上不标 */
const STATUS = {
  未开课: { key: 'courses.status.notStarted', tag: 'xm-tag-info' },
  已开课: { key: 'courses.filter.active', tag: 'xm-tag-success' },
  已结课: { key: 'courses.status.finished', tag: '' },
}

export function courseStatusLabel(status) {
  return STATUS[status] ? t(STATUS[status].key) : status || ''
}

export function courseStatusTag(status) {
  return (STATUS[status] && STATUS[status].tag) || ''
}

import { t } from '@/i18n'
import { SEGMENTS } from './todaySchedule'

/**
 * 业务枚举表：数据库按中文存储的枚举值（与 Web 端一致）在这里集中定义一次。
 * - value：库存值（保持中文，与后端统计 / 筛选一致）
 * - label：展示文案的词条键（按当前语言翻译）
 * - tag：列表里状态标签的配色类（可选）
 *
 * 页面里的用法：
 *   {{ enumLabel('courseStatus', item.status) }}           展示文案
 *   :class="enumTag('applyStatus', row.status)"           标签配色
 *   <xm-picker v-model="form.type" :options="enumOptions('courseType')" />   下拉选择
 * 新增枚举只需在下面加一组，展示、配色、下拉三处自动一致。
 */
export const ENUMS = {
  courseType: [
    { value: '必修', label: 'pages.course.required' },
    { value: '选修', label: 'pages.course.elective' },
  ],
  courseStatus: [
    { value: '未开课', label: 'pages.course.notStarted', tag: 'xm-tag-info' },
    { value: '已开课', label: 'pages.course.started', tag: 'xm-tag-success' },
    { value: '已结课', label: 'pages.course.finished' },
  ],
  week: [
    { value: '星期一', label: 'pages.course.monday' },
    { value: '星期二', label: 'pages.course.tuesday' },
    { value: '星期三', label: 'pages.course.wednesday' },
    { value: '星期四', label: 'pages.course.thursday' },
    { value: '星期五', label: 'pages.course.friday' },
    { value: '星期六', label: 'pages.course.saturday' },
    { value: '星期日', label: 'pages.course.sunday' },
  ],
  segment: SEGMENTS.map((value, i) => ({ value, label: `pages.course.segment${i + 1}` })),
  warningLevel: [
    { value: '高风险', label: 'pages.warning.levelHigh', tag: 'xm-tag-danger' },
    { value: '中风险', label: 'pages.warning.levelMiddle', tag: 'xm-tag-warning' },
    { value: '低风险', label: 'pages.warning.levelLow', tag: 'xm-tag-info' },
    { value: '正常', label: 'pages.warning.levelNormal', tag: 'xm-tag-success' },
  ],
  applyStatus: [
    { value: '待审核', label: 'pages.apply.statusPending', tag: 'xm-tag-warning' },
    { value: '审核通过', label: 'pages.apply.statusApproved', tag: 'xm-tag-success' },
    { value: '审核不通过', label: 'pages.apply.statusRejected', tag: 'xm-tag-danger' },
  ],
  attendanceStatus: [
    { value: '正常', label: 'pages.attendance.statusNormal', tag: 'xm-tag-success' },
    { value: '迟到', label: 'pages.attendance.statusLate', tag: 'xm-tag-warning' },
    { value: '早退', label: 'pages.attendance.statusEarlyLeave', tag: 'xm-tag-warning' },
    { value: '缺勤', label: 'pages.attendance.statusAbsent', tag: 'xm-tag-danger' },
  ],
  roomStatus: [
    { value: '空闲', label: 'pages.roomplan.free', tag: 'xm-tag-success' },
    { value: '占用', label: 'pages.roomplan.occupied', tag: 'xm-tag-warning' },
  ],
  roomType: [
    { value: '授课教室', label: 'pages.roomplan.typeTeaching' },
    { value: '运动场馆', label: 'pages.roomplan.typeVenue' },
    { value: '固定占用', label: 'pages.roomplan.typeFixed' },
  ],
  role: [
    { value: 'ADMIN', label: 'login.roleAdmin' },
    { value: 'TEACHER', label: 'login.roleTeacher' },
    { value: 'STUDENT', label: 'login.roleStudent' },
  ],
}

const findOption = (kind, value) => (ENUMS[kind] || []).find((o) => o.value === value)

/** 库存值 → 当前语言文案；不在枚举表里的值原样返回（兼容历史数据） */
export function enumLabel(kind, value) {
  const o = findOption(kind, value)
  if (o) return t(o.label)
  return value == null ? '' : String(value)
}

/** 库存值 → 状态标签配色类（未配置时为空串，显示中性灰） */
export const enumTag = (kind, value) => (findOption(kind, value) || {}).tag || ''

/** 下拉选项：[{ value, label }]，label 已按当前语言翻译 */
export const enumOptions = (kind) => (ENUMS[kind] || []).map((o) => ({ value: o.value, label: t(o.label) }))

/** 上课时间一行：「星期一 · 第一大节（08:30 ~ 10:10）」，都没有时返回 '' */
export const scheduleText = (week, segment) =>
  [enumLabel('week', week), enumLabel('segment', segment)].filter(Boolean).join(' · ')

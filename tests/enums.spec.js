import { afterEach, describe, expect, it } from 'vitest'
import { ENUMS, enumLabel, enumOptions, enumTag, scheduleText } from '@/utils/enums'
import { SEGMENTS } from '@/utils/todaySchedule'
import { setLocale } from '@/i18n'

afterEach(() => setLocale('zh-CN'))

describe('enums 业务枚举表', () => {
  it('库存中文值 → 当前语言文案；未知值原样返回', () => {
    expect(enumLabel('courseStatus', '已开课')).toBe('已开课')
    expect(enumLabel('applyStatus', '审核不通过')).toBe('审核不通过')
    expect(enumLabel('courseStatus', '历史状态')).toBe('历史状态')
    expect(enumLabel('courseStatus', null)).toBe('')
  })

  it('切到英文后显示英文文案（证明走的是词条而不是原样返回）', () => {
    setLocale('en-US')
    expect(enumLabel('courseType', '选修')).toBe('Elective')
    expect(enumLabel('courseStatus', '已结课')).toBe('Completed')
    expect(enumLabel('week', '星期日')).toBe('Sunday')
    expect(enumLabel('segment', SEGMENTS[0])).toBe('Period 1 (08:30 ~ 10:10)')
    expect(enumLabel('roomType', '运动场馆')).toBe('Sports Venue')
    expect(enumLabel('role', 'TEACHER')).toBe('Teacher')
  })

  it('标签配色：配置了的返回类名，未配置 / 未知值为空串', () => {
    expect(enumTag('applyStatus', '审核通过')).toBe('xm-tag-success')
    expect(enumTag('attendanceStatus', '缺勤')).toBe('xm-tag-danger')
    expect(enumTag('courseStatus', '已结课')).toBe('')
    expect(enumTag('courseStatus', '未知')).toBe('')
  })

  it('下拉选项：保持枚举顺序，value 为库存值、label 已翻译', () => {
    expect(enumOptions('roomStatus')).toEqual([
      { value: '空闲', label: '空闲' },
      { value: '占用', label: '占用' },
    ])
    expect(enumOptions('segment').map((o) => o.value)).toEqual(SEGMENTS)
    expect(enumOptions('不存在')).toEqual([])
  })

  it('每个枚举项的词条都存在（中英文都能翻译出来，不会显示成键名）', () => {
    for (const lang of ['zh-CN', 'en-US']) {
      setLocale(lang)
      for (const [kind, list] of Object.entries(ENUMS)) {
        for (const o of list) expect(enumLabel(kind, o.value), `${lang} ${kind}.${o.value}`).not.toBe(o.label)
      }
    }
  })

  it('scheduleText：星期 + 大节组合成一行，缺省时为空串', () => {
    expect(scheduleText('星期一', SEGMENTS[0])).toBe('星期一 · 第一大节（08:30 ~ 10:10）')
    expect(scheduleText('', '')).toBe('')
  })
})

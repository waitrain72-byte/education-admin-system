import { describe, expect, it } from 'vitest'
import { isEmptyValue, pickerIndex, pickerText } from '@/utils/picker'

const courses = [
  { id: 1, name: '数据结构' },
  { id: 2, name: '软件工程' },
]

describe('picker 纯函数（xm-picker）', () => {
  it('空值判断：空串 / null / undefined 为空，0 不为空', () => {
    expect([isEmptyValue(''), isEmptyValue(null), isEmptyValue(undefined), isEmptyValue(0)]).toEqual([
      true,
      true,
      true,
      false,
    ])
  })

  it('pickerIndex：按 valueKey 找下标，空值或找不到为 -1', () => {
    expect(pickerIndex(courses, 2, 'id')).toBe(1)
    expect(pickerIndex(courses, 9, 'id')).toBe(-1)
    expect(pickerIndex(courses, null, 'id')).toBe(-1)
    expect(pickerIndex([{ value: 'a' }], 'a')).toBe(0)
  })

  it('pickerText：命中返回 labelKey 文案；空值返回空串（组件显示占位）', () => {
    expect(pickerText(courses, 1, 'name', 'id')).toBe('数据结构')
    expect(pickerText(courses, '', 'name', 'id')).toBe('')
  })

  it('pickerText：值不在选项里——字符串原样显示（旧枚举），数字 id 返回空串（显示占位）', () => {
    expect(pickerText([{ value: '正常', label: 'Present' }], '请假')).toBe('请假')
    expect(pickerText(courses, 99, 'name', 'id')).toBe('')
    expect(pickerText(undefined, 1)).toBe('')
  })
})

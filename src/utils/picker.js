/**
 * xm-picker 的纯函数（不依赖 uni / vue，便于单测）：空值判断、当前下标、展示文案。
 * options 是对象数组，labelKey / valueKey 指定文案与取值字段（默认 label / value），
 * 因此接口返回的原始列表（如课程 { id, name }）可以直接传入，不必先 map 成 label/value。
 */

export const isEmptyValue = (v) => v === '' || v === null || v === undefined

/** 当前值在选项里的下标，找不到返回 -1 */
export function pickerIndex(options, value, valueKey = 'value') {
  if (isEmptyValue(value)) return -1
  return (options || []).findIndex((o) => o && o[valueKey] === value)
}

/**
 * 当前值的展示文案；返回 '' 时组件显示占位文字。
 * 值不在选项里（选项还没加载、历史数据）时：字符串值原样显示（如旧的中文枚举），id 等数字值返回 ''。
 */
export function pickerText(options, value, labelKey = 'label', valueKey = 'value') {
  if (isEmptyValue(value)) return ''
  const hit = (options || []).find((o) => o && o[valueKey] === value)
  if (hit) return hit[labelKey] == null ? '' : String(hit[labelKey])
  return typeof value === 'string' ? value : ''
}

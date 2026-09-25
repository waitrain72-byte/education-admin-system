import { ref } from 'vue'

/**
 * 表单键盘「下一项」接力聚焦：input 设 confirm-type="next"，@confirm 时 focusTo(下一个字段名)，
 * 下一个字段绑定 :focus="focused === '字段名'" 与 @blur="onBlur('字段名')"。
 *
 * 小程序的 focus 属性只在 false → true 变化时生效：字段失焦后必须把状态清回去，否则再次跳到同一
 * 字段时属性值没变、键盘不会弹出。onBlur 只清「自己」——按下一项时本字段的 blur 晚于 confirm 触发，
 * 此时 focused 已指向下一个字段，不能把它清掉。
 */
export function useFocusChain() {
  const focused = ref('')
  const focusTo = (name) => {
    focused.value = name
  }
  const onBlur = (name) => {
    if (focused.value === name) focused.value = ''
  }
  return { focused, focusTo, onBlur }
}

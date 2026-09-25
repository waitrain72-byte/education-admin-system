import { describe, expect, it } from 'vitest'
import { useFocusChain } from '@/composables/useFocusChain'

describe('useFocusChain 键盘下一项接力聚焦', () => {
  it('按下一项：本字段 blur 晚于 confirm 触发，不能清掉已指向下一字段的焦点', () => {
    const { focused, focusTo, onBlur } = useFocusChain()
    focusTo('password')
    // 密码框按「下一项」：先 confirm 指向验证码，再触发密码框自己的 blur
    focusTo('captcha')
    onBlur('password')
    expect(focused.value).toBe('captcha')
  })

  it('字段失焦后清空状态，再次跳到同一字段时 focus 能重新由 false 变 true', () => {
    const { focused, focusTo, onBlur } = useFocusChain()
    focusTo('captcha')
    onBlur('captcha')
    expect(focused.value).toBe('')
    focusTo('captcha')
    expect(focused.value).toBe('captcha')
  })
})

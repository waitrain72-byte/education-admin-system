import { describe, expect, it } from 'vitest'
import { PRESET_COLORS, normalizeColor, buildThemeVars, buildThemeStyle, nativeChromeColors } from '@/utils/themeColor'

describe('normalizeColor 取值归一', () => {
  it('合法 #RRGGBB 转小写', () => {
    expect(normalizeColor('#409EFF')).toBe('#409eff')
  })

  it('空值、三位简写、非字符串、注入串一律视为默认（空串）', () => {
    for (const v of ['', null, undefined, '#fff', 123, 'red; background:url(x)', '409eff']) {
      expect(normalizeColor(v)).toBe('')
    }
  })
})

describe('buildThemeVars 页面层 CSS 变量', () => {
  it('浅色模式：品牌色即所选颜色，浅底向白色混 90%', () => {
    const vars = buildThemeVars('#409eff', false)
    expect(vars['--xm-brand']).toBe('#409eff')
    // 255*0.9 + 64*0.1 = 235.9 -> ec；与 Element Plus 的 light-9 同值
    expect(vars['--xm-brand-soft']).toBe('#ecf5ff')
    expect(vars['--xm-brand-grad']).toMatch(/^linear-gradient\(135deg, #409eff 0%, #[0-9a-f]{6} 100%\)$/)
  })

  it('深色模式：基色提亮 20%，浅底改为半透明', () => {
    const vars = buildThemeVars('#409eff', true)
    expect(vars['--xm-brand']).toBe('#66b1ff')
    expect(vars['--xm-brand-soft']).toMatch(/^rgba\(\d+, \d+, \d+, 0\.16\)$/)
  })

  it('跨端一致性：与 Web 端对同一颜色派生出的基色相同', () => {
    // Web 端 useThemeColor.spec.ts 断言 #409eff 深色模式下 --el-color-primary = #66b1ff，
    // 这里的 --xm-brand 必须是同一个值，否则同一账号在两端看到的颜色会不一样
    expect(buildThemeVars('#409eff', true)['--xm-brand']).toBe('#66b1ff')
    expect(buildThemeVars('#409eff', false)['--xm-brand']).toBe('#409eff')
  })

  it('默认色（空串）不产出任何覆盖变量，完全回落到 theme.scss', () => {
    expect(buildThemeVars('', false)).toEqual({})
    expect(buildThemeVars('', true)).toEqual({})
  })

  it('非法颜色不写入任何值，防止脏字符串进入样式', () => {
    expect(buildThemeVars('red; color: expression(x)', false)).toEqual({})
  })
})

describe('buildThemeStyle 样式字符串', () => {
  it('输出可直接绑定的 "--k: v; --k: v" 字符串', () => {
    const style = buildThemeStyle('#16a34a', false)
    expect(style).toContain('--xm-brand: #16a34a')
    expect(style.split('; ').length).toBe(3)
  })

  it('默认色时为空串（:style 绑空串 = 不覆盖）', () => {
    expect(buildThemeStyle('', false)).toBe('')
  })
})

describe('nativeChromeColors 原生导航栏 / tabBar', () => {
  it('浅色模式：导航栏底色与 tabBar 选中色都用主题色', () => {
    expect(nativeChromeColors('#e11d48', false)).toEqual({ navBg: '#e11d48', tabSelected: '#e11d48' })
  })

  it('深色模式：导航栏保持中性暗色（不返回 navBg），只改 tabBar 选中色', () => {
    const c = nativeChromeColors('#e11d48', true)
    expect(c).not.toHaveProperty('navBg')
    expect(c.tabSelected).toMatch(/^#[0-9a-f]{6}$/)
  })

  it('默认色返回 null，由调用方沿用内置配色', () => {
    expect(nativeChromeColors('', false)).toBeNull()
  })
})

describe('PRESET_COLORS 预设色板', () => {
  it('首项为默认（空串），其余为合法小写 #RRGGBB 且不重复', () => {
    expect(PRESET_COLORS[0].value).toBe('')
    const rest = PRESET_COLORS.slice(1).map((p) => p.value)
    rest.forEach((v) => expect(v).toMatch(/^#[0-9a-f]{6}$/))
    expect(new Set(rest).size).toBe(rest.length)
  })

  it('与 Web 端色板完全一致（顺序与取值）', () => {
    expect(PRESET_COLORS.map((p) => p.value)).toEqual([
      '',
      '#409eff',
      '#16a34a',
      '#0d9488',
      '#e11d48',
      '#ea580c',
      '#9333ea',
      '#475569',
    ])
  })

  it('文案全部走 i18n 键', () => {
    PRESET_COLORS.forEach((p) => expect(p.label).toMatch(/^layout\.themeColor\.preset\./))
  })
})

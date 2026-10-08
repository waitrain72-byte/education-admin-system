import { describe, expect, it } from 'vitest'
import {
  PRESET_COLORS,
  normalizeColor,
  buildThemeVars,
  buildThemeStyle,
  nativeChromeColors,
  onBrandColor,
} from '@/utils/themeColor'

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
  it('浅色模式：品牌色即所选颜色，浅底向白色混 90%，加深色向黑色混 20%', () => {
    const vars = buildThemeVars('#409eff', false)
    expect(vars['--xm-brand']).toBe('#409eff')
    // 255*0.9 + 64*0.1 = 235.9 -> ec；与 Element Plus 的 light-9 同值
    expect(vars['--xm-brand-soft']).toBe('#ecf5ff')
    // 64*0.8 = 51.2 -> 33；与 Web 端 --xm-brand-strong（dark-2）同值
    expect(vars['--xm-brand-strong']).toBe('#337ecc')
    expect(vars['--xm-brand-grad']).toBe('linear-gradient(135deg, #409eff 0%, #337ecc 100%)')
  })

  it('深色模式：基色提亮 20%，浅底向深色卡片表面混（实色，不再是半透明）', () => {
    const vars = buildThemeVars('#409eff', true)
    expect(vars['--xm-brand']).toBe('#66b1ff')
    expect(vars['--xm-brand-soft']).toMatch(/^#[0-9a-f]{6}$/)
    // 浅底必须比品牌色暗得多，压在深色卡片上才不刺眼
    const soft = parseInt(vars['--xm-brand-soft'].slice(1, 3), 16)
    expect(soft).toBeLessThan(0x40)
  })

  it('跨端一致性：与 Web 端对同一颜色派生出的基色相同', () => {
    // Web 端 useThemeColor.spec.ts 断言 #409eff 深色模式下 --el-color-primary = #66b1ff，
    // 这里的 --xm-brand 必须是同一个值，否则同一账号在两端看到的颜色会不一样
    expect(buildThemeVars('#409eff', true)['--xm-brand']).toBe('#66b1ff')
    expect(buildThemeVars('#409eff', false)['--xm-brand']).toBe('#409eff')
  })

  it('品牌色实底上的文字：深色系用白字，浅色系用近黑字', () => {
    expect(buildThemeVars('#475569', false)['--xm-on-brand']).toBe('#ffffff')
    expect(onBrandColor('#0f7b63')).toBe('#ffffff')
    expect(onBrandColor('#f5d90a')).toBe('#0c110f')
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
    expect(style.split('; ').length).toBe(5)
  })

  it('默认色时为空串（:style 绑空串 = 不覆盖）', () => {
    expect(buildThemeStyle('', false)).toBe('')
  })
})

describe('nativeChromeColors 原生 tabBar', () => {
  it('导航栏是与页面同色的中性底，不随主题色变；只改 tabBar 选中色', () => {
    expect(nativeChromeColors('#e11d48', false)).toEqual({ tabSelected: '#e11d48' })
    const dark = nativeChromeColors('#e11d48', true)
    expect(dark).not.toHaveProperty('navBg')
    expect(dark.tabSelected).toMatch(/^#[0-9a-f]{6}$/)
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
      '#6366f1',
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

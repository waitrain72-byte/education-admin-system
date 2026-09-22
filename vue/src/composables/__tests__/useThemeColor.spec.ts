import { describe, it, expect, beforeEach, vi } from 'vitest'
import { applyThemeColor, PRESET_COLORS } from '@/composables/useThemeColor'

// 本用例只测颜色派生这段纯计算，不触发与后端的同步
vi.mock('@/utils/request', () => ({ default: { get: vi.fn(), put: vi.fn() } }))

const root = () => document.documentElement
const cssVar = (name: string) => root().style.getPropertyValue(name)

describe('applyThemeColor', () => {
  beforeEach(() => {
    root().style.cssText = ''
  })

  it('浅色模式下写入基色与各级派生色', () => {
    applyThemeColor('#409eff', false)

    // 取 Element Plus 自己的默认蓝做基准：下面几个期望值与 EP 官方调色板逐字节相同，
    // 说明这里的派生算法与 Element Plus 内部一致，换任意颜色都能得到风格统一的色阶
    expect(cssVar('--el-color-primary')).toBe('#409eff')
    // 浅色模式向白色渐变：层级越高越浅
    expect(cssVar('--el-color-primary-light-3')).toBe('#79bbff')
    expect(cssVar('--el-color-primary-light-9')).toBe('#ecf5ff')
    // dark-2 向黑色渐变，用于 hover/active 态
    expect(cssVar('--el-color-primary-dark-2')).toBe('#337ecc')
  })

  it('深色模式下基色提亮，派生方向相反', () => {
    applyThemeColor('#409eff', true)

    // 基色按 20% 提亮，避免深色系自定义色在暗背景上看不见
    expect(cssVar('--el-color-primary')).toBe('#66b1ff')
    // 深色模式向黑色渐变：层级越高越深
    expect(cssVar('--el-color-primary-light-3')).toBe('#477cb3')
    // dark-2 反过来向白色渐变
    expect(cssVar('--el-color-primary-dark-2')).toBe('#85c1ff')
  })

  it('同一颜色在深浅模式下产生不同结果', () => {
    applyThemeColor('#16a34a', false)
    const light = cssVar('--el-color-primary-light-5')
    applyThemeColor('#16a34a', true)
    const dark = cssVar('--el-color-primary-light-5')

    expect(light).not.toBe(dark)
  })

  it('项目自有的品牌色变量同步跟随', () => {
    applyThemeColor('#9333ea', false)

    expect(cssVar('--xm-brand')).toBe('#9333ea')
    expect(cssVar('--xm-brand-strong')).toBeTruthy()
    expect(cssVar('--xm-brand-strong')).not.toBe('#9333ea')
  })

  it('传空串时清除全部内联变量，回落到样式表默认配色', () => {
    applyThemeColor('#e11d48', false)
    expect(cssVar('--el-color-primary')).toBe('#e11d48')

    applyThemeColor('', false)

    expect(cssVar('--el-color-primary')).toBe('')
    expect(cssVar('--el-color-primary-light-3')).toBe('')
    expect(cssVar('--el-color-primary-light-9')).toBe('')
    expect(cssVar('--el-color-primary-dark-2')).toBe('')
    expect(cssVar('--xm-brand')).toBe('')
    expect(cssVar('--xm-brand-strong')).toBe('')
  })

  it('非法颜色按恢复默认处理，不会把脏值写进 CSS', () => {
    applyThemeColor('#409eff', false)

    applyThemeColor('red; background: url(x)', false)

    expect(cssVar('--el-color-primary')).toBe('')
  })

  it('三位简写等非 #RRGGBB 格式一律视为默认', () => {
    applyThemeColor('#fff', false)
    expect(cssVar('--el-color-primary')).toBe('')
  })

  it('输出始终是补零到两位的 6 位十六进制', () => {
    // 深色模式下 #000000 会被提亮，检查每段都补零成两位
    applyThemeColor('#000000', true)
    expect(cssVar('--el-color-primary')).toMatch(/^#[0-9a-f]{6}$/)

    applyThemeColor('#ffffff', false)
    expect(cssVar('--el-color-primary-light-9')).toMatch(/^#[0-9a-f]{6}$/)
  })
})

describe('PRESET_COLORS', () => {
  it('首项是「默认」（空串），其余均为合法 #RRGGBB', () => {
    expect(PRESET_COLORS[0].value).toBe('')
    for (const preset of PRESET_COLORS.slice(1)) {
      expect(preset.value).toMatch(/^#[0-9a-f]{6}$/)
    }
  })

  it('每个预设都带 i18n 文案键，不出现硬编码中文', () => {
    for (const preset of PRESET_COLORS) {
      expect(preset.label).toMatch(/^layout\.themeColor\.preset\./)
    }
  })

  it('预设色值不重复', () => {
    const values = PRESET_COLORS.map((p) => p.value)
    expect(new Set(values).size).toBe(values.length)
  })
})

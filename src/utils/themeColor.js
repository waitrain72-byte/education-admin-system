/**
 * 自定义主题色的纯计算工具（不依赖 Vue / uni 运行时，便于单测）。
 *
 * 与 Web 端 vue/src/composables/useThemeColor.ts 使用同一套派生算法，
 * 保证同一个账号在两端看到的是同一种颜色：
 * - mix(base, target, w) = target*w + base*(1-w)，即 Element Plus 的混色公式
 * - 深色模式下先把基色向白色提亮 20%，否则深色系自定义色在暗背景上几乎看不见
 * - 浅色层级：浅色模式向白色混、深色模式向深色卡片表面混（与 Web 端 light-9 同值）
 *
 * 空串表示「使用 theme.scss 里的内置配色（玉绿）」，此时不产出任何覆盖变量。
 */

/** 本地存储键：与 Web 端同名（各端各自存储，登录后都以后端为准） */
export const STORAGE_KEY = 'xm-theme-color'

const HEX_RE = /^#[0-9a-fA-F]{6}$/

/** 预设色板：与 Web 端完全一致，value 为空串表示「默认（玉绿）」 */
export const PRESET_COLORS = [
  { value: '', label: 'layout.themeColor.preset.default' },
  { value: '#409eff', label: 'layout.themeColor.preset.blue' },
  { value: '#16a34a', label: 'layout.themeColor.preset.green' },
  { value: '#6366f1', label: 'layout.themeColor.preset.indigo' },
  { value: '#e11d48', label: 'layout.themeColor.preset.red' },
  { value: '#ea580c', label: 'layout.themeColor.preset.orange' },
  { value: '#9333ea', label: 'layout.themeColor.preset.purple' },
  { value: '#475569', label: 'layout.themeColor.preset.gray' },
]

/** 默认品牌色（theme.scss / theme.json 里的玉绿），预设色板的「默认」色块用它显示 */
export const DEFAULT_BRAND = { light: '#0f7b63', dark: '#3fb08f' }

const WHITE = [255, 255, 255]
const BLACK = [0, 0, 0]
/** 深色卡片表面（theme.scss 的 --xm-bg-card 附近）：深色模式下浅色层级向它混色，而不是纯黑 */
const DARK_SURFACE = [20, 27, 24]
/** 品牌色实底上的两种文字色：白字，或深色模式下的近黑字（theme.scss 的 --xm-on-brand） */
const ON_LIGHT = '#ffffff'
const ON_DARK = '#0c110f'

/** 合法的 #RRGGBB 返回小写形式，其余一律返回空串（= 默认色） */
export function normalizeColor(value) {
  return typeof value === 'string' && HEX_RE.test(value) ? value.toLowerCase() : ''
}

function parseHex(hex) {
  if (!HEX_RE.test(hex)) return null
  return [parseInt(hex.slice(1, 3), 16), parseInt(hex.slice(3, 5), 16), parseInt(hex.slice(5, 7), 16)]
}

function toHex(rgb) {
  return (
    '#' +
    rgb
      .map((v) =>
        Math.round(Math.min(255, Math.max(0, v)))
          .toString(16)
          .padStart(2, '0'),
      )
      .join('')
  )
}

function mixRgb(base, target, weight) {
  return [0, 1, 2].map((i) => target[i] * weight + base[i] * (1 - weight))
}

/** 深浅模式下实际使用的基色（深色模式提亮 20%） */
function baseOf(rgb, dark) {
  return dark ? mixRgb(rgb, WHITE, 0.2) : rgb
}

/** WCAG 相对亮度 */
function luminance(rgb) {
  const [r, g, b] = rgb.map((v) => {
    const c = Math.round(v) / 255
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4
  })
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

function contrast(a, b) {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x)
  return (hi + 0.05) / (lo + 0.05)
}

/** 压在这个颜色实底上的文字用白字还是近黑字：取对比度更高的一个 */
export function onBrandColor(hex) {
  const rgb = parseHex(normalizeColor(hex))
  if (!rgb) return ON_LIGHT
  return contrast(rgb, WHITE) >= contrast(rgb, parseHex(ON_DARK)) ? ON_LIGHT : ON_DARK
}

/**
 * 页面层 CSS 变量（覆盖 theme.scss 中的品牌色变量）。
 * @returns {Record<string,string>} 空对象表示使用内置配色
 */
export function buildThemeVars(hex, dark) {
  const rgb = parseHex(normalizeColor(hex))
  if (!rgb) return {}
  const base = baseOf(rgb, dark)
  const baseHex = toHex(base)
  // 加深层级：浅色模式向黑色、深色模式向白色（与 Web 端 --xm-brand-strong 相同）
  const strong = toHex(mixRgb(base, dark ? WHITE : BLACK, 0.2))
  const soft = toHex(mixRgb(base, dark ? DARK_SURFACE : WHITE, 0.9))
  return {
    '--xm-brand': baseHex,
    '--xm-brand-strong': strong,
    '--xm-brand-grad': `linear-gradient(135deg, ${baseHex} 0%, ${strong} 100%)`,
    '--xm-brand-soft': soft,
    '--xm-on-brand': onBrandColor(baseHex),
  }
}

/** 转成 :style 可直接绑定的字符串（小程序端字符串形式最稳，对象里的 -- 变量各端支持不一） */
export function buildThemeStyle(hex, dark) {
  return Object.entries(buildThemeVars(hex, dark))
    .map(([k, v]) => `${k}: ${v}`)
    .join('; ')
}

/**
 * 原生层（tabBar）配色覆盖：原生组件读不到 CSS 变量，只能经 uni API 单独设置。
 * 导航栏是跟页面同色的中性底，不随主题色变化；只有 tabBar 选中文字用主题色。
 * @returns {{tabSelected: string} | null} null 表示使用内置配色
 */
export function nativeChromeColors(hex, dark) {
  const rgb = parseHex(normalizeColor(hex))
  if (!rgb) return null
  return { tabSelected: toHex(baseOf(rgb, dark)) }
}

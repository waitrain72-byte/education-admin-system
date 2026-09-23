/**
 * 自定义主题色的纯计算工具（不依赖 Vue / uni 运行时，便于单测）。
 *
 * 与 Web 端 vue/src/composables/useThemeColor.ts 使用同一套派生算法，
 * 保证同一个账号在两端看到的是同一种颜色：
 * - mix(base, target, w) = target*w + base*(1-w)，即 Element Plus 的混色公式
 * - 深色模式下先把基色向白色提亮 20%，否则深色系自定义色在暗背景上几乎看不见
 *
 * 空串表示「使用 theme.scss 里的内置配色」，此时不产出任何覆盖变量。
 */

/** 本地存储键：与 Web 端同名（各端各自存储，登录后都以后端为准） */
export const STORAGE_KEY = 'xm-theme-color'

const HEX_RE = /^#[0-9a-fA-F]{6}$/

/** 预设色板：与 Web 端完全一致，value 为空串表示「默认」 */
export const PRESET_COLORS = [
  { value: '', label: 'layout.themeColor.preset.default' },
  { value: '#409eff', label: 'layout.themeColor.preset.blue' },
  { value: '#16a34a', label: 'layout.themeColor.preset.green' },
  { value: '#0d9488', label: 'layout.themeColor.preset.teal' },
  { value: '#e11d48', label: 'layout.themeColor.preset.red' },
  { value: '#ea580c', label: 'layout.themeColor.preset.orange' },
  { value: '#9333ea', label: 'layout.themeColor.preset.purple' },
  { value: '#475569', label: 'layout.themeColor.preset.gray' },
]

const WHITE = [255, 255, 255]
const BLACK = [0, 0, 0]

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

/**
 * 页面层 CSS 变量（覆盖 theme.scss 中的三个品牌色变量）。
 * @returns {Record<string,string>} 空对象表示使用内置配色
 */
export function buildThemeVars(hex, dark) {
  const rgb = parseHex(normalizeColor(hex))
  if (!rgb) return {}
  const base = baseOf(rgb, dark)
  const baseHex = toHex(base)
  // 渐变终点：浅色模式向黑色加深、深色模式向白色提亮，保持同色系的层次感
  const end = toHex(mixRgb(base, dark ? WHITE : BLACK, 0.18))
  const soft = dark ? `rgba(${base.map(Math.round).join(', ')}, 0.16)` : toHex(mixRgb(base, WHITE, 0.9))
  return {
    '--xm-brand': baseHex,
    '--xm-brand-grad': `linear-gradient(135deg, ${baseHex} 0%, ${end} 100%)`,
    '--xm-brand-soft': soft,
  }
}

/** 转成 :style 可直接绑定的字符串（小程序端字符串形式最稳，对象里的 -- 变量各端支持不一） */
export function buildThemeStyle(hex, dark) {
  return Object.entries(buildThemeVars(hex, dark))
    .map(([k, v]) => `${k}: ${v}`)
    .join('; ')
}

/**
 * 原生层（导航栏 / tabBar）配色覆盖：原生组件读不到 CSS 变量，只能经 uni API 单独设置。
 * - 浅色模式：导航栏底色 = 主题色，tabBar 选中色 = 主题色
 * - 深色模式：导航栏保持中性暗色不变（整页是暗的，亮色导航栏会很刺眼），只改 tabBar 选中色
 * @returns {{navBg?: string, tabSelected: string} | null} null 表示使用内置配色
 */
export function nativeChromeColors(hex, dark) {
  const rgb = parseHex(normalizeColor(hex))
  if (!rgb) return null
  const base = toHex(baseOf(rgb, dark))
  return dark ? { tabSelected: base } : { navBg: base, tabSelected: base }
}

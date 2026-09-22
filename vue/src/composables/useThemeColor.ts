import { computed, ref, watch } from 'vue'
import { useDebounceFn } from '@vueuse/core'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { isDark } from '@/composables/useTheme'

/**
 * 自定义主题色（品牌色 / Element Plus primary）。
 *
 * <p>与 useTheme（深浅模式）、useLocale（界面语言）同一套模式：
 * 本地存 localStorage 保证刷新不丢、防抖推送到后端保证换终端也跟着走。
 * 存储粒度是账号，所以 lisi 选了绿色，下次任何终端用 lisi 登录都是绿色。</p>
 *
 * <p>空串表示「使用系统内置默认色」——此时不写任何内联变量，
 * 完全回落到 assets/css/theme.css 里那套手工调过的靛蓝配色。</p>
 */

/** localStorage 键名：index.html 的防闪烁脚本会读取同一个键 */
const STORAGE_KEY = 'xm-theme-color'

/** 预设色板：兼顾浅色/深色两种模式下的对比度 */
export const PRESET_COLORS: ReadonlyArray<{ value: string; label: string }> = [
    { value: '', label: 'layout.themeColor.preset.default' },
    { value: '#409eff', label: 'layout.themeColor.preset.blue' },
    { value: '#16a34a', label: 'layout.themeColor.preset.green' },
    { value: '#0d9488', label: 'layout.themeColor.preset.teal' },
    { value: '#e11d48', label: 'layout.themeColor.preset.red' },
    { value: '#ea580c', label: 'layout.themeColor.preset.orange' },
    { value: '#9333ea', label: 'layout.themeColor.preset.purple' },
    { value: '#475569', label: 'layout.themeColor.preset.gray' },
]

// ========== 颜色计算 ==========

/** #RRGGBB -> [r, g, b]；非法输入返回 null */
function parseHex(hex: string): [number, number, number] | null {
    if (!/^#[0-9a-fA-F]{6}$/.test(hex)) return null
    return [
        parseInt(hex.slice(1, 3), 16),
        parseInt(hex.slice(3, 5), 16),
        parseInt(hex.slice(5, 7), 16),
    ]
}

function toHex(rgb: [number, number, number]): string {
    return '#' + rgb.map((v) => Math.round(Math.min(255, Math.max(0, v))).toString(16).padStart(2, '0')).join('')
}

/**
 * 按 Element Plus 的算法混色：mix(target, base, weight) = target*weight + base*(1-weight)。
 * EP 用它派生 primary-light-N（浅色模式混白、深色模式混黑）与 primary-dark-2。
 */
function mix(base: [number, number, number], target: [number, number, number], weight: number): string {
    return toHex([
        target[0] * weight + base[0] * (1 - weight),
        target[1] * weight + base[1] * (1 - weight),
        target[2] * weight + base[2] * (1 - weight),
    ])
}

const WHITE: [number, number, number] = [255, 255, 255]
const BLACK: [number, number, number] = [0, 0, 0]

/** 需要派生的 Element Plus 浅色层级 */
const LIGHT_LEVELS = [3, 5, 7, 8, 9]

/** 本组件写入的全部 CSS 变量名（恢复默认时按这份清单清除，不会误删别处的变量） */
const MANAGED_VARS = [
    '--el-color-primary',
    ...LIGHT_LEVELS.map((i) => `--el-color-primary-light-${i}`),
    '--el-color-primary-dark-2',
    '--xm-brand',
    '--xm-brand-strong',
]

/**
 * 把主题色写成 <html> 上的内联 CSS 变量。
 *
 * <p>用内联样式而不是动态 &lt;style&gt;：内联优先级高于 :root 与 html.dark 两条规则，
 * 无需关心选择器权重；清除时把这几个属性移除即可自动回落到样式表里的默认配色。</p>
 *
 * @param hex  #RRGGBB，空串表示恢复默认
 * @param dark 当前是否深色模式（深色下基色要提亮，派生方向也相反）
 */
export function applyThemeColor(hex: string, dark: boolean): void {
    const root = document.documentElement
    const rgb = hex ? parseHex(hex) : null
    if (!rgb) {
        // 恢复默认：移除内联变量，回落到 theme.css 中手工调过的配色
        MANAGED_VARS.forEach((name) => root.style.removeProperty(name))
        return
    }

    // 深色模式下把基色提亮 20%，否则深色系自定义色在暗背景上几乎看不见
    // （内置配色 #6366f1 -> #818cf8 正是这个关系）
    const base = dark ? parseHex(mix(rgb, WHITE, 0.2))! : rgb
    // 浅色模式向白色渐变、深色模式向黑色渐变，与 Element Plus 自身的明/暗主题一致
    const lightTarget = dark ? BLACK : WHITE
    const darkTarget = dark ? WHITE : BLACK

    root.style.setProperty('--el-color-primary', toHex(base))
    LIGHT_LEVELS.forEach((i) => {
        root.style.setProperty(`--el-color-primary-light-${i}`, mix(base, lightTarget, i / 10))
    })
    root.style.setProperty('--el-color-primary-dark-2', mix(base, darkTarget, 0.2))

    // 项目自有的品牌色变量同步跟随（登录页、侧边栏高亮等在用）
    root.style.setProperty('--xm-brand', toHex(base))
    root.style.setProperty('--xm-brand-strong', mix(base, darkTarget, 0.2))
}

// ========== 状态 ==========

function readStored(): string {
    try {
        const saved = localStorage.getItem(STORAGE_KEY) || ''
        return /^#[0-9a-fA-F]{6}$/.test(saved) ? saved.toLowerCase() : ''
    } catch {
        return ''
    }
}

/** 当前主题色（''=默认色）。模块级单例，全应用共享 */
const themeColor = ref<string>(readStored())

/** 供 UI 读取/写入的响应式引用 */
export function useThemeColor() {
    return themeColor
}

/** 当前是否为自定义色（用于 UI 上「恢复默认」按钮的禁用态） */
export const isCustomThemeColor = computed(() => themeColor.value !== '')

/** 设置主题色；传空串恢复默认。UI 直接调用本方法 */
export function setThemeColor(hex: string): void {
    const next = hex && /^#[0-9a-fA-F]{6}$/.test(hex) ? hex.toLowerCase() : ''
    themeColor.value = next
}

// ========== 与后端同步 ==========

/** 最近一次与后端一致的值，用于防止「拉取回显 → 触发推送」的回环 */
let serverThemeColor = ''

function isLoggedIn(): boolean {
    try {
        return useUserStore().isLoggedIn
    } catch {
        return false
    }
}

/**
 * 安装主题色同步：在 App.vue 调用一次。
 * - 颜色或深浅模式变化时立即重算并应用到 CSS 变量
 * - 颜色变化防抖 500ms 后推送到后端（深浅模式切换不触发推送，它由 useTheme 自己管）
 */
export function installThemeColorSync(): void {
    // 首屏立即应用一次（index.html 的防闪烁脚本已抢先写过基础色，这里补齐全部派生层级）
    applyThemeColor(themeColor.value, isDark.value)

    const push = useDebounceFn(async () => {
        if (!isLoggedIn() || themeColor.value === serverThemeColor) return
        const next = themeColor.value
        try {
            await request.put('/themeColor', { themeColor: next })
            serverThemeColor = next
        } catch {
            // 同步失败不打断使用，下次切换会重新尝试
        }
    }, 500)

    watch(themeColor, (value) => {
        try {
            localStorage.setItem(STORAGE_KEY, value)
        } catch {
            // 隐私模式等存储不可用的场景：仅本次会话生效
        }
        applyThemeColor(value, isDark.value)
        push()
    })

    // 深浅模式切换时要按新模式重算派生色（深色下基色提亮、渐变方向相反）
    watch(isDark, (dark) => applyThemeColor(themeColor.value, dark))
}

/**
 * 从后端拉取当前用户保存的主题色并覆盖本地。
 * 登录成功后调用：换个账号登录立刻变成那个账号自己的配色。
 */
export async function pullThemeColorFromServer(): Promise<void> {
    if (!isLoggedIn()) return
    try {
        const value = await request.get<string>('/themeColor')
        const next = typeof value === 'string' && /^#[0-9a-fA-F]{6}$/.test(value) ? value.toLowerCase() : ''
        serverThemeColor = next
        if (themeColor.value !== next) {
            themeColor.value = next
        } else {
            // 值相同不会触发 watch，这里手动保证 CSS 变量已按当前模式写好
            applyThemeColor(next, isDark.value)
        }
    } catch {
        // 拉取失败时保留本地颜色
    }
}

/**
 * 退出登录时把主题色恢复为默认：
 * 否则下一个在同一浏览器登录的账号会先看到上一个账号的配色，直到拉取完成才闪回自己的。
 */
export function resetThemeColorOnLogout(): void {
    serverThemeColor = ''
    setThemeColor('')
}

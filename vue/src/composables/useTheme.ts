import { watch } from 'vue'
import { useColorMode, useDebounceFn } from '@vueuse/core'
import type { UseColorModeReturn } from '@vueuse/core'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

/**
 * 主题偏好：本地三种模式。
 * auto 表示跟随系统（VueUse 会通过 prefers-color-scheme 媒体查询实时感知系统切换），
 * 其余为手动指定的浅色/深色。
 */
export type ThemeMode = 'light' | 'dark' | 'auto'

/** 本地模式与后端存储值的映射（后端用 system 表达"跟随系统"） */
const LOCAL_TO_SERVER: Record<ThemeMode, string> = { light: 'light', dark: 'dark', auto: 'system' }
const SERVER_TO_LOCAL: Record<string, ThemeMode> = { light: 'light', dark: 'dark', system: 'auto' }

/** localStorage 键名：页面加载前 index.html 的防闪烁脚本会读取同一个键 */
const STORAGE_KEY = 'xm-color-mode'

/** 模块级单例：全应用共享同一个 colorMode 实例，避免重复绑定 storage/媒体查询监听 */
let colorMode: UseColorModeReturn<ThemeMode> | null = null

/**
 * 主题模式组合式 API：
 * - 通过 useColorMode 在 <html> 上维护 dark 类（Element Plus 暗色变量与自定义 CSS 变量都挂在它上面）
 * - 模式值持久化在 localStorage，刷新后不丢失
 * - auto 模式下由浏览器媒体查询驱动，系统切换深浅色时页面实时跟随
 */
export function useTheme() {
    if (!colorMode) {
        colorMode = useColorMode({
            selector: 'html',
            attribute: 'class',
            storageKey: STORAGE_KEY,
        }) as UseColorModeReturn<ThemeMode>
    }
    return colorMode
}

/** 最近一次从后端拉取的值，用于防止"拉取回显 → 触发推送"的回环 */
let serverTheme = ''

/** 判断是否处于登录态（独立小函数，避免模块加载时依赖 Pinia 生命周期） */
function isLoggedIn(): boolean {
    try {
        return useUserStore().isLoggedIn
    } catch {
        return false
    }
}

/**
 * 安装主题同步：模式变化后防抖推送到后端，实现多端一致性。
 * 在 App.vue 中调用一次即可；未登录时只写 localStorage，登录后自动开始同步。
 */
export function installThemeSync() {
    const mode = useTheme()
    const push = useDebounceFn(async () => {
        const next = LOCAL_TO_SERVER[mode.value as ThemeMode] || 'system'
        if (!isLoggedIn() || next === serverTheme) return
        try {
            await request.put('/theme', { theme: next })
            serverTheme = next
        } catch {
            // 主题同步失败不打断使用，下次切换会重新尝试
        }
    }, 500)
    watch(mode, push)
}

/**
 * 从后端拉取当前用户的主题偏好并覆盖本地模式。
 * 在登录成功后调用：用户在别的终端设置过的主题会同步到当前终端。
 */
export async function pullThemeFromServer() {
    const mode = useTheme()
    if (!isLoggedIn()) return
    try {
        const res = await request.get('/theme')
        const value = res.data?.data
        if (typeof value !== 'string' || !(value in SERVER_TO_LOCAL)) return
        serverTheme = value
        const local = SERVER_TO_LOCAL[value]
        if (mode.value !== local) {
            mode.value = local
        }
    } catch {
        // 拉取失败时保留本地主题
    }
}

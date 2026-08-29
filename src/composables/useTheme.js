import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { get, put } from '@/utils/request'

/**
 * 主题偏好（与 Web 端 useTheme 语义一致）：
 * - light / dark / auto 三档，localStorage 持久化
 * - auto 通过 uni.onThemeChange 跟随系统深浅色
 * - 页面根节点绑定 :class="themeClass"（由 main.js 全局混入提供）
 * - 登录后与后端同步（GET/PUT /theme），与 Web 端共用同一份偏好
 */
const STORAGE_KEY = 'xm-color-mode'

export const themeMode = ref(uni.getStorageSync(STORAGE_KEY) || 'auto')
const systemDark = ref(false)

if (typeof uni.onThemeChange === 'function') {
  uni.onThemeChange((res) => {
    systemDark.value = res.theme === 'dark'
  })
}

export const isDark = computed(() => {
  if (themeMode.value === 'dark') return true
  if (themeMode.value === 'light') return false
  return systemDark.value
})

export const themeClass = computed(() => (isDark.value ? 'theme-dark' : 'theme-light'))

let serverTheme = ''
let pushTimer = null

function pushTheme() {
  try {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) return
    const next = themeMode.value === 'auto' ? 'system' : themeMode.value
    if (next === serverTheme) return
    if (pushTimer) clearTimeout(pushTimer)
    pushTimer = setTimeout(() => {
      put('/theme', { theme: next })
        .then(() => {
          serverTheme = next
        })
        .catch(() => {})
    }, 500)
  } catch {
    // Pinia 未就绪时忽略
  }
}

export function setThemeMode(mode) {
  if (!['light', 'dark', 'auto'].includes(mode)) return
  themeMode.value = mode
  uni.setStorageSync(STORAGE_KEY, mode)
  pushTheme()
}

export function cycleTheme() {
  setThemeMode(themeMode.value === 'light' ? 'dark' : themeMode.value === 'dark' ? 'auto' : 'light')
}

/** 登录成功后调用：用后端保存的主题偏好覆盖本地 */
export async function pullThemeFromServer() {
  try {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) return
    const res = await get('/theme')
    const value = res && res.data && res.data.data
    if (value !== 'light' && value !== 'dark' && value !== 'system') return
    serverTheme = value
    const local = value === 'system' ? 'auto' : value
    if (themeMode.value !== local) {
      themeMode.value = local
      uni.setStorageSync(STORAGE_KEY, local)
    }
  } catch {
    // 拉取失败时保留本地主题
  }
}

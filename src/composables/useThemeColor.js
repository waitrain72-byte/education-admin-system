import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { get, put } from '@/utils/request'
import { isDark, syncNativeChrome } from './useTheme'
import { STORAGE_KEY, normalizeColor, buildThemeStyle, PRESET_COLORS } from '@/utils/themeColor'

/**
 * 自定义主题色（与 Web 端 useThemeColor 语义一致，共用后端 /themeColor 与账号表 theme_color 列）：
 * - 本地 storage 持久化保证冷启动即生效，登录后以后端为准，修改后防抖推送
 * - 页面层：全局混入的 themeStyle 绑在每个页面根节点，覆盖 theme.scss 的品牌色 CSS 变量
 *   （小程序无法在运行时给 page 根元素设样式，所以挂在各页面根 view 上，变量会继承到所有子组件）
 * - 原生层：导航栏 / tabBar 读不到 CSS 变量，由 useTheme 的 syncNativeChrome 统一处理
 *
 * 与 Web 端的差异：小程序没有原生取色器，这里只提供预设色板点选；
 * 但在 Web 端选的任意颜色同样会同步过来并正确显示。
 */

export { PRESET_COLORS }

function readStored() {
  try {
    return normalizeColor(uni.getStorageSync(STORAGE_KEY))
  } catch {
    return ''
  }
}

/** 当前主题色（'' = 内置默认色）。模块级单例，全应用共享 */
export const themeColor = ref(readStored())

/** 供全局混入绑定到页面根节点的样式字符串（空串 = 不覆盖） */
export const themeStyle = computed(() => buildThemeStyle(themeColor.value, isDark.value))

/** 当前颜色是否为自定义（非默认），用于「恢复默认」按钮的禁用态 */
export const isCustomThemeColor = computed(() => themeColor.value !== '')

/** 最近一次与后端一致的值，用于防止「拉取回显 → 触发推送」的回环 */
let serverThemeColor = ''
let pushTimer = null

function persistAndApply(value) {
  try {
    uni.setStorageSync(STORAGE_KEY, value)
  } catch {
    // 存储不可用时仅本次会话生效
  }
  // 原生层（导航栏 / tabBar）读 storage 取色，所以要先写 storage 再同步
  syncNativeChrome()
}

function pushThemeColor() {
  try {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn || themeColor.value === serverThemeColor) return
    if (pushTimer) clearTimeout(pushTimer)
    const next = themeColor.value
    pushTimer = setTimeout(() => {
      put('/themeColor', { themeColor: next }, { loading: false })
        .then(() => {
          serverThemeColor = next
        })
        .catch(() => {
          // 同步失败不打断使用，下次切换会重新尝试
        })
    }, 500)
  } catch {
    // Pinia 未就绪时忽略
  }
}

/** 设置主题色；传空串恢复默认 */
export function setThemeColor(value) {
  const next = normalizeColor(value)
  if (next === themeColor.value) return
  themeColor.value = next
  persistAndApply(next)
  pushThemeColor()
}

/** 登录成功后调用：用后端保存的主题色覆盖本地（换账号登录即变成那个账号的配色） */
export async function pullThemeColorFromServer() {
  try {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) return
    const next = normalizeColor(await get('/themeColor', undefined, { loading: false }))
    serverThemeColor = next
    if (themeColor.value !== next) {
      themeColor.value = next
      persistAndApply(next)
    }
  } catch {
    // 拉取失败时保留本地颜色
  }
}

/**
 * 退出登录时恢复默认：否则下一个在本机登录的账号会先看到上一个账号的配色，
 * 直到 /themeColor 拉取完成才闪回自己的。
 */
export function resetThemeColorOnLogout() {
  serverThemeColor = ''
  if (pushTimer) {
    clearTimeout(pushTimer)
    pushTimer = null
  }
  if (themeColor.value !== '') {
    themeColor.value = ''
    persistAndApply('')
  }
}

// 注：深浅模式切换时的原生层重算由 useTheme 自身的 watch(isDark, syncNativeChrome) 负责，
// syncNativeChrome 每次都从 storage 读取主题色，这里无需重复监听

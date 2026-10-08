import { ref, computed, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { SILENT } from '@/utils/request'
import { accountApi } from '@/api'
import { STORAGE_KEY as THEME_COLOR_KEY, nativeChromeColors, normalizeColor } from '@/utils/themeColor'
import { TABS, tabItem } from '@/utils/tabbar'
import { locale } from '@/i18n'

/**
 * 主题偏好（与 Web 端 useTheme 语义一致）：
 * - light / dark / auto 三档，localStorage 持久化
 * - auto 通过 uni.onThemeChange 跟随系统深浅色
 * - 页面根节点绑定 :class="themeClass"（由 main.js 全局混入提供）
 * - 登录后与后端同步（GET/PUT /theme），与 Web 端共用同一份偏好
 */
const STORAGE_KEY = 'xm-color-mode'

export const themeMode = ref(uni.getStorageSync(STORAGE_KEY) || 'auto')

/**
 * 冷启动时读取一次系统当前深浅色：onThemeChange 只在「切换」时回调，
 * 不读初值的话，手机本来就是深色模式时「跟随系统」会一直显示浅色。
 * 微信小程序需 app.json 开启 darkmode（manifest.json mp-weixin.darkmode）才会返回 theme。
 */
export function readSystemDark() {
  try {
    const info = typeof uni.getAppBaseInfo === 'function' ? uni.getAppBaseInfo() : uni.getSystemInfoSync()
    return !!info && info.theme === 'dark'
  } catch {
    return false
  }
}

const systemDark = ref(readSystemDark())

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

/** 三档切换按钮的图标名（xm-icon）：浅色 / 深色 / 跟随系统 */
export const themeModeIcon = computed(() => ({ light: 'sun', dark: 'moon' })[themeMode.value] || 'theme-auto')

/** 与 theme.json 取值一致：导航栏（与页面同色的中性底）/ 窗口背景 / tabBar 的深浅两套配色 */
const NATIVE_CHROME = {
  light: {
    navBg: '#f3f5f4',
    navFront: '#000000',
    tabBg: '#ffffff',
    tabColor: '#7d8a84',
    tabSelected: '#0f7b63',
    tabBorder: 'white',
  },
  dark: {
    navBg: '#0e1311',
    navFront: '#ffffff',
    tabBg: '#151c19',
    tabColor: '#7d8a84',
    tabSelected: '#3fb08f',
    tabBorder: 'black',
  },
}

/** 最近一次成功应用到 tabBar 的「角色 | 深浅 | 自定义色 | 语言」，没变化时不重复设置 */
let appliedTabs = ''

/**
 * tabBar 五个位置的文字（随语言）与图标（随角色、深浅色、是否自定义主题色）。
 * tabBar 接口只能在 tabBar 页面调用，其他页面会失败——失败不记录，下次进入 tabBar 页面再设。
 */
function syncTabBarItems(dark, custom) {
  let role = ''
  try {
    role = useUserStore().role
  } catch {
    // Pinia 未就绪时按未登录处理
  }
  const key = [role, dark, custom, locale.value].join('|')
  if (key === appliedTabs) return
  TABS.forEach((_, index) => {
    try {
      uni.setTabBarItem({
        ...tabItem(index, role, { dark, custom }),
        success: () => {
          appliedTabs = key
        },
        fail: () => {},
      })
    } catch {
      // 平台不支持时忽略
    }
  })
}

/**
 * 原生导航栏 / 窗口背景 / tabBar 跟随应用内主题，
 * 避免出现「页面已变暗、导航栏还是浅色」的割裂（H5 端部分接口不存在，静默忽略）。
 * 模块加载、主题切换、每个页面 onShow（main.js 全局混入）、App onShow 都会调用。
 */
export function syncNativeChrome() {
  // 叠加自定义主题色：原生层读不到 CSS 变量，只能在这里按主题色覆盖 tabBar 选中色。
  // 从 storage 读取（useThemeColor 总是先写 storage 再调本函数），避免两个模块互相 import 形成循环依赖
  let brand = ''
  try {
    brand = normalizeColor(uni.getStorageSync(THEME_COLOR_KEY))
  } catch {
    // 存储不可用时按内置配色
  }
  const dark = isDark.value
  const c = { ...(dark ? NATIVE_CHROME.dark : NATIVE_CHROME.light), ...nativeChromeColors(brand, dark) }
  try {
    uni.setNavigationBarColor({ frontColor: c.navFront, backgroundColor: c.navBg, fail: () => {} })
  } catch {
    // 平台不支持时忽略
  }
  try {
    if (typeof uni.setBackgroundColor === 'function') {
      uni.setBackgroundColor({
        backgroundColor: c.navBg,
        backgroundColorTop: c.navBg,
        backgroundColorBottom: c.navBg,
        fail: () => {},
      })
    }
  } catch {
    // 平台不支持时忽略
  }
  try {
    uni.setTabBarStyle({
      backgroundColor: c.tabBg,
      color: c.tabColor,
      selectedColor: c.tabSelected,
      borderStyle: c.tabBorder,
      fail: () => {},
    })
  } catch {
    // 平台不支持时忽略
  }
  syncTabBarItems(dark, !!brand)
}

// 深浅色切换要换整套原生配色；语言切换要换 tabBar 文字
watch([isDark, locale], syncNativeChrome)
// 冷启动时立即应用一次（首屏原生层即与主题一致）
syncNativeChrome()

let serverTheme = ''
let pushTimer = null

function pushTheme() {
  try {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) return
    const next = themeMode.value === 'auto' ? 'system' : themeMode.value
    if (next === serverTheme) return
    if (pushTimer) clearTimeout(pushTimer)
    // 后台静默同步：不弹加载蒙层（否则切换后半秒会闪一下全屏「加载中」）
    pushTimer = setTimeout(() => {
      accountApi
        .updateTheme(next, SILENT)
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
    const value = await accountApi.getTheme(SILENT)
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

import { useUserStore } from '@/stores/user'

/**
 * 登录守卫（对照 unibest / uniapp-vue3-template 的做法：路由拦截 + 页面级兜底两层）。
 *
 * 为什么两层都要：
 * - 路由拦截（uni.addInterceptor）能拦住代码发起的 navigateTo 等跳转，未登录时直接改道登录页，
 *   目标页根本不会被加载 —— 比「页面先渲染、onShow 里再跳走」更干净，也不会先发出一堆 401 请求
 * - 但它拦不住两种入口：用户直接点原生 tabBar、以及冷启动 / 分享卡片直接打开某页。
 *   这两种情况只能靠页面 onShow 里的 ensureLoggedIn() 兜底
 */

const LOGIN_PAGE = '/pages/login/login'

/** 无需登录即可访问的页面 */
const PUBLIC_PAGES = ['/pages/login/login', '/pages/register/register']

/** 'pages/x/x?a=1' / '/pages/x/x' → '/pages/x/x' */
function pathOf(url) {
  return (
    '/' +
    String(url || '')
      .replace(/^\//, '')
      .split('?')[0]
  )
}

function isLoggedIn() {
  try {
    return useUserStore().isLoggedIn
  } catch {
    return false
  }
}

export function isPublicPage(url) {
  return PUBLIC_PAGES.includes(pathOf(url))
}

/**
 * 页面级兜底：在需要登录的页面 onShow 开头调用 `if (!ensureLoggedIn()) return`。
 * 未登录时跳转登录页并返回 false。
 */
export function ensureLoggedIn() {
  if (isLoggedIn()) return true
  uni.reLaunch({ url: LOGIN_PAGE })
  return false
}

/** 拦截器对象（导出供单测直接调用 invoke） */
export const authInterceptor = {
  invoke(args) {
    if (isLoggedIn() || isPublicPage(args && args.url)) return true
    // 这里再次调用 reLaunch 会重入本拦截器，但登录页在白名单内，会直接放行，不会死循环
    uni.reLaunch({ url: LOGIN_PAGE })
    // 返回 false 终止原跳转
    return false
  },
}

const GUARDED_APIS = ['navigateTo', 'redirectTo', 'reLaunch', 'switchTab']
let installed = false

/** 在 App.vue onLaunch 调用一次 */
export function installAuthInterceptor() {
  if (installed) return
  installed = true
  GUARDED_APIS.forEach((api) => uni.addInterceptor(api, authInterceptor))
}

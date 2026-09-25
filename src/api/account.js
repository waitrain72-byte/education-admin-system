import { get, post, put, saveCookie, clearCookie } from '@/utils/request'
import { baseUrl } from '@/utils/config'

/**
 * 账号与个人偏好接口（后端 WebController / PermissionController）。
 * 偏好（语言 / 主题 / 主题色）按账号存后端，与 Web 端双向同步。
 */
export const accountApi = {
  /** 登录：{ username, password, role, captcha }，成功返回账号信息（含 token） */
  login: (data, opts) => post('/login', data, opts),
  /** 学生自助注册：{ username, password, role } */
  register: (data, opts) => post('/register', data, opts),
  /** 修改当前账号密码 */
  updatePassword: (data, opts) => put('/updatePassword', data, opts),

  /**
   * 获取一张新的图形验证码，返回可直接给 <image> 用的 data URL。
   * 验证码按 Session 校验：先清掉旧会话 Cookie，取图时保存新会话 Cookie，登录请求再由请求层带上；
   * 需要拿到响应头里的 Cookie，所以直接用 uni.request 取二进制图片，而不是 <image> 直接加载地址。
   */
  async captcha() {
    clearCookie()
    const res = await new Promise((resolve, reject) => {
      uni.request({
        url: `${baseUrl}/captcha?t=${Date.now()}`,
        method: 'GET',
        responseType: 'arraybuffer',
        success: resolve,
        fail: reject,
      })
    })
    saveCookie(res)
    return 'data:image/gif;base64,' + uni.arrayBufferToBase64(res.data)
  },

  /** 当前账号的 RBAC 权限码（首页菜单按权限过滤） */
  getPermissions: (opts) => get('/permission/my', undefined, opts),

  /** 语言偏好：'zh-CN' | 'en-US' */
  getLocale: (opts) => get('/locale', undefined, opts),
  updateLocale: (locale, opts) => put('/locale', { locale }, opts),
  /** 主题模式：'light' | 'dark' | 'system' */
  getTheme: (opts) => get('/theme', undefined, opts),
  updateTheme: (theme, opts) => put('/theme', { theme }, opts),
  /** 主题色：'#RRGGBB'，'' 表示默认色 */
  getThemeColor: (opts) => get('/themeColor', undefined, opts),
  updateThemeColor: (themeColor, opts) => put('/themeColor', { themeColor }, opts),
}

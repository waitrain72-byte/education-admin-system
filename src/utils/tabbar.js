import { t } from '@/i18n'

/**
 * 底部标签栏（与 pages.json tabBar.list 顺序一致）：首页 / 课程 / 日程 / 消息 / 我的。
 * 与 Web 端顶部主导航对应——日程只给学生和教师，管理员的第三个位置是「教务后台」。
 * 小程序的 tabBar 页面路径不能按角色替换，所以第三个位置是同一个页面（pages/schedule/schedule），
 * 页面按角色渲染日程或后台入口，这里只把文字和图标换掉。
 *
 * 图标（static/tabbar）：<icon>.png 未选中（中性灰，深浅色通用）；
 * <icon>-on(-dark).png 选中（默认玉绿，深色模式用提亮的一版）；
 * <icon>-ink(-dark).png 选中（用户自定义了主题色时用中性墨色，避免玉绿图标配别的颜色文字）。
 */
export const TABS = [
  { key: 'home', page: 'pages/home/home', text: 'tab.home', icon: 'home' },
  { key: 'courses', page: 'pages/courses/courses', text: 'tab.courses', icon: 'courses' },
  {
    key: 'schedule',
    page: 'pages/schedule/schedule',
    text: 'tab.schedule',
    icon: 'schedule',
    admin: { text: 'tab.console', icon: 'console' },
  },
  { key: 'messages', page: 'pages/messages/messages', text: 'tab.messages', icon: 'messages' },
  { key: 'mine', page: 'pages/mine/mine', text: 'tab.mine', icon: 'mine' },
]

/** 消息 tab 的位置（未读角标挂在这里） */
export const MESSAGES_TAB_INDEX = TABS.findIndex((tab) => tab.key === 'messages')

/**
 * 某个位置在当前角色、主题下的文字与图标（uni.setTabBarItem 的参数）。
 * @param {number} index
 * @param {string} role
 * @param {{ dark: boolean, custom: boolean }} look 深色模式；是否自定义了主题色
 */
export function tabItem(index, role, look) {
  const tab = TABS[index]
  const item = role === 'ADMIN' && tab.admin ? { ...tab, ...tab.admin } : tab
  const variant = (look.custom ? 'ink' : 'on') + (look.dark ? '-dark' : '')
  return {
    index,
    text: t(item.text),
    iconPath: `/static/tabbar/${item.icon}.png`,
    selectedIconPath: `/static/tabbar/${item.icon}-${variant}.png`,
  }
}

/** 是否是 tabBar 页面（路由不带开头的 /） */
export function isTabPage(route) {
  return TABS.some((tab) => tab.page === String(route || '').replace(/^\//, ''))
}

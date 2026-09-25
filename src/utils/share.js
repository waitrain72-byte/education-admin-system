import { t } from '@/i18n'

/**
 * 右上角「转发」：main.js 的全局混入给所有页面统一注册 onShareAppMessage（不注册时转发按钮是灰的）。
 * - 公开信息类页面转发当前页（通知、考试安排等），其余页面（个人信息、管理页等）转发首页
 * - 接收者打开后未登录会先进入登录页（登录拦截统一处理）
 * - 不开放「分享到朋友圈」：朋友圈打开的是单页模式，无法登录，页面没有可看的内容
 */
const SHARE_PAGES = {
  'pages/notice/notice': 'menu.notice',
  'pages/examplan/examplan': 'menu.examplan',
  'pages/roomplan/roomplan': 'menu.roomplan',
  'pages/course/course': 'menu.course',
}

/** 按页面路由（不带开头的 /）生成转发内容 { title, path } */
export function shareMessage(route) {
  const appName = t('layout.title')
  const menu = SHARE_PAGES[route]
  if (!menu) return { title: appName, path: '/pages/home/home' }
  return { title: `${t(menu)} - ${appName}`, path: `/${route}` }
}

/** 当前页面路由（onShareAppMessage 总是由栈顶页面触发） */
export function currentRoute() {
  const pages = getCurrentPages()
  return pages.length ? pages[pages.length - 1].route : ''
}

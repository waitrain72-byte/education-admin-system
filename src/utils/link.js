/**
 * 站内链接 → 小程序页面。
 *
 * 后端给站内消息带的跳转地址是 Web 端路由（如 /course/8/assignments?open=3、/schedule?view=month、/grades），
 * 小程序的页面结构不同，这里统一换算；纯函数 resolveLink 便于单测（tests/link.spec.js）。
 * tabBar 页面不能带参数（switchTab 不支持 query）：参数先寄存在这里，页面 onShow 时用 takeTabQuery 取走。
 */

/** 课程空间的分区（Web 端 /course/:id/<section>） */
export const COURSE_SECTIONS = ['overview', 'attendance', 'assignments', 'grades', 'evaluation', 'resources', 'members']

/** 教务后台页面：Web 端 /admin/<path> → 小程序 pages-admin 下的页面 */
const ADMIN_PAGES = ['courses', 'rooms', 'leaves', 'exams', 'warnings', 'org', 'people', 'semester']
/** Web 端已合并的旧后台地址 */
const ADMIN_ALIASES = {
  colleges: ['org'],
  specialities: ['org'],
  classes: ['org'],
  students: ['people', { tab: 'students' }],
  teachers: ['people', { tab: 'teachers' }],
  admins: ['people', { tab: 'admins' }],
}

const TAB_ROUTES = {
  home: '/pages/home/home',
  courses: '/pages/courses/courses',
  schedule: '/pages/schedule/schedule',
  messages: '/pages/messages/messages',
}

export function parseQuery(text) {
  const query = {}
  String(text || '')
    .split('&')
    .filter(Boolean)
    .forEach((pair) => {
      const [k, v = ''] = pair.split('=')
      try {
        query[decodeURIComponent(k)] = decodeURIComponent(v)
      } catch {
        query[k] = v
      }
    })
  return query
}

export function buildQuery(query) {
  const parts = Object.keys(query || {})
    .filter((k) => query[k] !== undefined && query[k] !== null && query[k] !== '')
    .map((k) => `${encodeURIComponent(k)}=${encodeURIComponent(query[k])}`)
  return parts.length ? '?' + parts.join('&') : ''
}

/**
 * 换算一个 Web 端路由。
 * @returns {{ url: string, tab: boolean, query: Record<string,string> } | null} 认不出的地址返回 null
 */
export function resolveLink(link) {
  if (!link || typeof link !== 'string' || !link.startsWith('/')) return null
  const [path, queryText] = link.split('?')
  const query = parseQuery(queryText)
  const parts = path.split('/').filter(Boolean)
  const [head, second, third] = parts

  if (!head) return { url: TAB_ROUTES.home, tab: true, query }
  if (TAB_ROUTES[head] && !second) return { url: TAB_ROUTES[head], tab: true, query }

  if (head === 'course' && /^\d+$/.test(second || '')) {
    const tab = third && COURSE_SECTIONS.includes(third) ? third : 'overview'
    return { url: '/pages-course/space/space', tab: false, query: { id: second, tab, ...query } }
  }
  if (head === 'square' && !second) return { url: '/pages-course/square/square', tab: false, query }
  if (head === 'grades' && !second) return { url: '/pages-course/transcript/transcript', tab: false, query }
  if (head === 'profile' && !second) return { url: '/pages/person/person', tab: false, query }

  if (head === 'admin') {
    if (!second) return { url: TAB_ROUTES.schedule, tab: true, query }
    const alias = ADMIN_ALIASES[second]
    const page = alias ? alias[0] : second
    if (!ADMIN_PAGES.includes(page)) return null
    return { url: `/pages-admin/${page}/${page}`, tab: false, query: { ...query, ...(alias && alias[1]) } }
  }
  return null
}

/** 寄存的 tabBar 页面参数：键是页面路由（/pages/xxx/xxx） */
const pendingTabQuery = {}

/** tabBar 页面 onShow 时取走寄存给自己的参数（取一次即清空），没有返回 null */
export function takeTabQuery(url) {
  const query = pendingTabQuery[url] || null
  delete pendingTabQuery[url]
  return query
}

/**
 * 打开一个站内链接（消息、通知、首页卡片里的跳转都走这里）。
 * @returns {boolean} 认不出的地址返回 false，调用方可以停在原页
 */
export function openLink(link) {
  const target = resolveLink(link)
  if (!target) return false
  if (target.tab) {
    pendingTabQuery[target.url] = target.query
    // 目标 tab 已经是当前页时 onShow 不会再触发，广播一下让它自己取参数
    uni.$emit('xm:tab-query', target.url)
    uni.switchTab({ url: target.url })
  } else {
    uni.navigateTo({ url: target.url + buildQuery(target.query) })
  }
  return true
}

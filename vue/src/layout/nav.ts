import type { RouteRecordRaw } from 'vue-router'

/**
 * 顶部导航与教务后台侧栏的数据：按角色裁剪，路由表是唯一数据源（meta.roles / meta.name）。
 */

export interface NavItem {
    key: string
    /** i18n 键 */
    label: string
    to: string
    /** 当前路径以这些前缀开头时高亮 */
    prefixes: string[]
}

const MAIN_NAV: Array<NavItem & { roles?: string[] }> = [
    { key: 'home', label: 'nav.home', to: '/home', prefixes: ['/home'] },
    { key: 'courses', label: 'nav.courses', to: '/courses', prefixes: ['/courses', '/course/'] },
    { key: 'messages', label: 'nav.messages', to: '/messages', prefixes: ['/messages'] },
    { key: 'admin', label: 'nav.admin', to: '/admin', prefixes: ['/admin'], roles: ['ADMIN'] },
]

/** 顶部主导航：首页 / 课程 / 消息，管理员多一个「教务后台」 */
export function mainNav(role: string): NavItem[] {
    return MAIN_NAV.filter((item) => !item.roles || item.roles.includes(role))
}

export function isNavActive(item: NavItem, path: string): boolean {
    return item.prefixes.some((p) => (p.endsWith('/') ? path.startsWith(p) : path === p || path.startsWith(p + '/')))
}

export interface LinkItem {
    path: string
    label: string
    section?: string
}

/**
 * 某个路由节点下、当前角色能进入的子页面（顶栏「全部功能」与后台侧栏都用它）。
 * 子路由的 meta.roles 缺省时继承父路由的限制（父路由已在外层过滤）。
 */
export function childLinks(routes: readonly RouteRecordRaw[], parentPath: string, role: string, filter?: (meta: Record<string, any>) => boolean): LinkItem[] {
    const prefix = parentPath.replace(/\/$/, '')
    const links: LinkItem[] = []
    for (const route of routes) {
        const meta = (route.meta || {}) as Record<string, any>
        if (!meta.name || meta.hidden || route.redirect) continue
        if (meta.roles && !meta.roles.includes(role)) continue
        if (filter && !filter(meta)) continue
        links.push({ path: `${prefix}/${route.path}`, label: meta.name, section: meta.section })
    }
    return links
}

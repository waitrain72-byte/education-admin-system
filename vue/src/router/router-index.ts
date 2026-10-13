import { createRouter, createWebHistory, type RouteMeta, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

/**
 * 路由表：meta.name 是页面名称的 i18n 键（导航、命令面板、后台侧栏都从这里取，单一数据源）；
 * meta.roles 限定可访问的角色（子路由继承父路由的限制）；meta.permission 是需要的权限码。
 *
 * meta.legacy 标记沿用旧版界面的教务后台页面（教学记录、考试安排、学业预警、权限与日志）：
 * 它们没有自己的页头，由教务后台布局补标题（meta.section 决定侧栏分组）。
 */

/** 改版前学生、教师用的旧页面地址（书签、旧消息里可能还有）→ 新页面 */
const LEGACY_REDIRECTS: Record<string, string> = {
    course: '/courses',
    choice: '/square',
    curriculum: '/schedule',
    score: '/grades',
    attendance: '/courses',
    homework: '/courses',
    comment: '/courses',
    apply: '/schedule?view=month',
}

const routes: RouteRecordRaw[] = [
    {
        path: '/',
        component: () => import('@/layout/AppLayout.vue'),
        redirect: '/home',
        children: [
            { path: 'home', name: 'Home', meta: { name: 'nav.home' }, component: () => import('@/views/home/HomePage.vue') },
            {
                path: 'courses',
                name: 'Courses',
                meta: { name: 'nav.courses' },
                component: () => import('@/views/courses/CoursesPage.vue'),
            },
            {
                path: 'course/:id(\\d+)',
                component: () => import('@/views/course/CourseSpaceLayout.vue'),
                meta: { name: 'nav.courses', hidden: true },
                // 消息里的链接会带查询参数（如 /course/8?post=3），跳到概览时要保留
                redirect: (to) => ({ path: `/course/${to.params.id}/overview`, query: to.query }),
                children: [
                    {
                        path: 'overview',
                        name: 'CourseOverview',
                        meta: { name: 'space.tabs.overview' },
                        component: () => import('@/views/course/CourseOverview.vue'),
                    },
                    {
                        path: 'attendance',
                        name: 'CourseAttendance',
                        meta: { name: 'space.tabs.attendance' },
                        component: () => import('@/views/course/CourseAttendance.vue'),
                    },
                    {
                        path: 'assignments',
                        name: 'CourseAssignments',
                        meta: { name: 'space.tabs.assignments' },
                        component: () => import('@/views/course/CourseAssignments.vue'),
                    },
                    {
                        path: 'grades',
                        name: 'CourseGrades',
                        meta: { name: 'space.tabs.grades' },
                        component: () => import('@/views/course/CourseGrades.vue'),
                    },
                    {
                        path: 'evaluation',
                        name: 'CourseEvaluation',
                        meta: { name: 'space.tabs.evaluation' },
                        component: () => import('@/views/course/CourseEvaluation.vue'),
                    },
                    {
                        path: 'resources',
                        name: 'CourseResources',
                        meta: { name: 'space.tabs.resources' },
                        component: () => import('@/views/course/CourseResources.vue'),
                    },
                    {
                        path: 'members',
                        name: 'CourseMembers',
                        meta: { name: 'space.tabs.members' },
                        component: () => import('@/views/course/CourseMembers.vue'),
                    },
                ],
            },
            {
                path: 'square',
                name: 'Square',
                meta: { name: 'nav.square' },
                component: () => import('@/views/square/CourseSquarePage.vue'),
            },
            {
                path: 'grades',
                name: 'Transcript',
                meta: { name: 'transcript.title', roles: ['STUDENT'] },
                component: () => import('@/views/grades/TranscriptPage.vue'),
            },
            {
                path: 'schedule',
                name: 'Schedule',
                meta: { name: 'nav.schedule', roles: ['STUDENT', 'TEACHER'] },
                component: () => import('@/views/schedule/SchedulePage.vue'),
            },
            {
                path: 'messages',
                name: 'Messages',
                meta: { name: 'nav.messages' },
                component: () => import('@/views/messages/MessagesPage.vue'),
            },
            {
                path: 'profile',
                name: 'Profile',
                meta: { name: 'shell.profile', hidden: true },
                component: () => import('@/views/profile/ProfilePage.vue'),
            },

            {
                path: 'legacy/:page(.*)*',
                redirect: (to) => {
                    const page = String([to.params.page].flat()[0] || '')
                    // 成绩单只有学生有，老师的旧成绩页转到课程（成绩册在课程空间里）
                    if (page === 'score' && useUserStore().role !== 'STUDENT') return '/courses'
                    return LEGACY_REDIRECTS[page] || '/home'
                },
            },

            {
                path: 'admin',
                component: () => import('@/layout/AdminLayout.vue'),
                meta: { name: 'nav.admin', roles: ['ADMIN'], hidden: true },
                redirect: '/admin/courses',
                children: [
                    // 教学
                    { path: 'courses', name: 'AdminCourses', meta: { name: 'admin.menu.courses', section: 'teaching' }, component: () => import('@/views/admin/CourseAdmin.vue') },
                    { path: 'rooms', name: 'AdminRooms', meta: { name: 'admin.menu.rooms', section: 'teaching' }, component: () => import('@/views/admin/RoomsAdmin.vue') },
                    { path: 'leaves', name: 'AdminLeaves', meta: { name: 'admin.menu.leaves', section: 'teaching' }, component: () => import('@/views/admin/LeaveBoard.vue') },
                    { path: 'exams', name: 'AdminExams', meta: { name: 'menu.examplan', section: 'teaching', legacy: true }, component: () => import('@/views/manager/Examplan.vue') },
                    { path: 'warnings', name: 'AdminWarnings', meta: { name: 'menu.warning', section: 'teaching', legacy: true }, component: () => import('@/views/manager/Warning.vue') },
                    // 教学记录
                    { path: 'choices', name: 'AdminChoices', meta: { name: 'admin.menu.choices', section: 'records', legacy: true }, component: () => import('@/views/manager/Choice.vue') },
                    { path: 'scores', name: 'AdminScores', meta: { name: 'admin.menu.scores', section: 'records', legacy: true }, component: () => import('@/views/manager/Score.vue') },
                    { path: 'attendance', name: 'AdminAttendance', meta: { name: 'menu.attendance', section: 'records', legacy: true }, component: () => import('@/views/manager/Attendance.vue') },
                    { path: 'homework', name: 'AdminHomework', meta: { name: 'admin.menu.homework', section: 'records', legacy: true }, component: () => import('@/views/manager/Homework.vue') },
                    { path: 'comments', name: 'AdminComments', meta: { name: 'menu.comment', section: 'records', legacy: true }, component: () => import('@/views/manager/Comment.vue') },
                    // 档案
                    { path: 'org', name: 'AdminOrg', meta: { name: 'admin.menu.org', section: 'archives' }, component: () => import('@/views/admin/OrgAdmin.vue') },
                    { path: 'people', name: 'AdminPeople', meta: { name: 'admin.menu.people', section: 'archives' }, component: () => import('@/views/admin/PeopleAdmin.vue') },
                    // 旧地址（书签、旧消息里的链接）转到合并后的新页面；带 redirect 的不进侧栏
                    { path: 'colleges', redirect: '/admin/org' },
                    { path: 'specialities', redirect: '/admin/org' },
                    { path: 'classes', redirect: '/admin/org' },
                    { path: 'students', redirect: (to) => ({ path: '/admin/people', query: { ...to.query, tab: 'students' } }) },
                    { path: 'teachers', redirect: (to) => ({ path: '/admin/people', query: { ...to.query, tab: 'teachers' } }) },
                    { path: 'admins', redirect: (to) => ({ path: '/admin/people', query: { ...to.query, tab: 'admins' } }) },
                    // 系统
                    { path: 'semester', name: 'AdminSemester', meta: { name: 'admin.menu.semester', section: 'system' }, component: () => import('@/views/admin/SemesterSettings.vue') },
                    { path: 'permission', name: 'AdminPermission', meta: { name: 'menu.permission', section: 'system', legacy: true }, component: () => import('@/views/manager/Permission.vue') },
                    { path: 'operlog', name: 'AdminOperLog', meta: { name: 'menu.operlog', section: 'system', legacy: true }, component: () => import('@/views/manager/OperLog.vue') },
                    { path: 'loginlog', name: 'AdminLoginLog', meta: { name: 'menu.loginlog', section: 'system', legacy: true }, component: () => import('@/views/manager/LoginLog.vue') },
                ],
            },
        ],
    },
    { path: '/login', name: 'Login', component: () => import('@/views/auth/LoginPage.vue') },
    { path: '/register', name: 'Register', component: () => import('@/views/auth/RegisterPage.vue') },
    {
        path: '/dashboard',
        name: 'Dashboard',
        // 管理员看全校、教师看本人任课；学生即使被授了权限码也进不来（后端同样拒绝）
        meta: { permission: 'dashboard:view', roles: ['ADMIN', 'TEACHER'] },
        component: () => import('@/views/Dashboard.vue'),
    },
    { path: '/403', name: 'Forbidden', component: () => import('@/views/manager/403.vue') },
    { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/404.vue') },
]

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes,
    // 切页回到顶部；浏览器前进后退保留原来的滚动位置
    scrollBehavior(_to, _from, savedPosition) {
        return savedPosition || { top: 0 }
    },
})

/**
 * 当前账号能否进入带这份 meta 的页面：meta.roles 存在时角色必须在允许列表内；
 * meta.permission（如 dashboard:view）存在时必须有该权限码（ADMIN 放行）。
 */
function canEnter(meta: RouteMeta, role: string, permissions: string[]): boolean {
    const roles = meta.roles as string[] | undefined
    if (roles && roles.length && !roles.includes(role)) return false
    const perm = meta.permission as string | undefined
    return !perm || role === 'ADMIN' || permissions.includes(perm)
}

// ========== 路由守卫 ==========
router.beforeEach((to, _from, next) => {
    // 从统一状态管理读取登录态
    const userStore = useUserStore()
    const isLoggedIn = userStore.isLoggedIn

    // 登录页、注册页：已登录直接回首页
    if (to.path === '/login' || to.path === '/register') {
        if (isLoggedIn) {
            next('/home')
            return
        }
        next()
        return
    }

    if (!isLoggedIn) {
        next('/login')
        return
    }

    // 路由级权限：角色与权限码（子路由的 meta 已合并父路由）
    if (!canEnter(to.meta, userStore.role, userStore.permissions)) {
        next('/403')
        return
    }

    next()
})

// ========== 页面预取 ==========
/**
 * 登录后趁浏览器空闲，把当前账号能进的页面代码提前加载好。
 *
 * 路由都是懒加载：第一次切到某个页面要先加载它的代码（开发模式下 Vite 还要现场编译），
 * 会明显顿一下，之后再切就不卡了。提前加载过的页面，第一次切换也一样顺。
 * 只取登录后布局下的页面（不含登录页、数据大屏），逐个加载，不和当前页面抢资源。
 */
export function prefetchRoutes() {
    const { role, permissions } = useUserStore()
    const loaders: Array<() => Promise<unknown>> = []
    const collect = (records: RouteRecordRaw[], inherited: RouteMeta) => {
        for (const record of records) {
            // 子路由继承父路由的 meta，和守卫里看到的 to.meta 一致
            const meta = { ...inherited, ...record.meta }
            if (!canEnter(meta, role, permissions)) continue
            // 路由组件都写成 () => import(...)：调用一次就加载，之后切页时直接命中模块缓存
            if (typeof record.component === 'function') loaders.push(record.component as () => Promise<unknown>)
            if (record.children) collect(record.children, meta)
        }
    }
    collect(routes.find((r) => r.path === '/')?.children ?? [], {})

    const run = async () => {
        for (const load of loaders) {
            // 预取失败不提示，切到该页时照常加载
            await load().catch(() => undefined)
        }
    }
    if ('requestIdleCallback' in window) window.requestIdleCallback(run, { timeout: 3000 })
    else setTimeout(run, 1500)
}

export default router

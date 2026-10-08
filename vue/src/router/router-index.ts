import { createRouter, createWebHistory, type RouteRecordRaw, type RouteRecordSingleView } from 'vue-router'
import { useUserStore } from '@/stores/user'

/**
 * 路由表：meta.name 是页面名称的 i18n 键（导航、命令面板、后台侧栏都从这里取，单一数据源）；
 * meta.roles 限定可访问的角色（子路由继承父路由的限制）；meta.permission 是需要的权限码。
 *
 * meta.legacy 标记「还没迁到新界面的旧页面」：学生、教师在顶栏「全部功能」里进入，
 * 管理员的旧页面都挂在教务后台侧栏下（meta.section 决定侧栏分组）。
 */
const Course = () => import('@/views/manager/Course.vue')
const Choice = () => import('@/views/manager/Choice.vue')
const Score = () => import('@/views/manager/Score.vue')
const Attendance = () => import('@/views/manager/Attendance.vue')
const Homework = () => import('@/views/manager/Homework.vue')
const Comment = () => import('@/views/manager/Comment.vue')
const Apply = () => import('@/views/manager/Apply.vue')
const Warning = () => import('@/views/manager/Warning.vue')
const Examplan = () => import('@/views/manager/Examplan.vue')
const Roomplan = () => import('@/views/manager/Roomplan.vue')

/** 学生、教师过渡期的旧页面 */
const legacy = (path: string, name: string, component: RouteRecordSingleView['component'], roles: string[]): RouteRecordSingleView => ({
    path: `legacy/${path}`,
    name: `Legacy${name}`,
    meta: { name: `menu.${path}`, roles, legacy: true },
    component,
})

const BOTH = ['TEACHER', 'STUDENT']

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

            legacy('course', 'Course', Course, BOTH),
            legacy('choice', 'Choice', Choice, BOTH),
            legacy('curriculum', 'Curriculum', () => import('@/views/manager/Curriculum.vue'), ['STUDENT']),
            legacy('score', 'Score', Score, BOTH),
            legacy('attendance', 'Attendance', Attendance, BOTH),
            legacy('homework', 'Homework', Homework, BOTH),
            legacy('comment', 'Comment', Comment, BOTH),
            legacy('apply', 'Apply', Apply, BOTH),
            legacy('warning', 'Warning', Warning, BOTH),
            legacy('examplan', 'Examplan', Examplan, BOTH),
            legacy('roomplan', 'Roomplan', Roomplan, BOTH),

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
                    { path: 'exams', name: 'AdminExams', meta: { name: 'menu.examplan', section: 'teaching', legacy: true }, component: Examplan },
                    { path: 'warnings', name: 'AdminWarnings', meta: { name: 'menu.warning', section: 'teaching', legacy: true }, component: Warning },
                    // 教学记录
                    { path: 'choices', name: 'AdminChoices', meta: { name: 'admin.menu.choices', section: 'records', legacy: true }, component: Choice },
                    { path: 'scores', name: 'AdminScores', meta: { name: 'admin.menu.scores', section: 'records', legacy: true }, component: Score },
                    { path: 'attendance', name: 'AdminAttendance', meta: { name: 'menu.attendance', section: 'records', legacy: true }, component: Attendance },
                    { path: 'homework', name: 'AdminHomework', meta: { name: 'admin.menu.homework', section: 'records', legacy: true }, component: Homework },
                    { path: 'comments', name: 'AdminComments', meta: { name: 'menu.comment', section: 'records', legacy: true }, component: Comment },
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
        meta: { permission: 'dashboard:view' },
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

    // 路由级权限：meta.roles 存在时，当前角色必须在允许列表内（子路由的 meta 已合并父路由）
    const roles = to.meta?.roles as string[] | undefined
    if (roles && roles.length && !roles.includes(userStore.role)) {
        next('/403')
        return
    }

    // 权限码校验：meta.permission（如 dashboard:view）存在时，当前用户必须拥有该权限码（ADMIN 放行）
    const perm = to.meta?.permission as string | undefined
    if (perm && userStore.role !== 'ADMIN' && !userStore.permissions.includes(perm)) {
        next('/403')
        return
    }

    next()
})

export default router

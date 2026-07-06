import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
// import { ElMessage } from 'element-plus'

const routes: RouteRecordRaw[] = [
    {
        path: '/',
        component: () => import('@/views/Manager.vue'),
        redirect: '/home',
        children: [
            { path: 'home',        name: 'Home',          meta: { name: '系统首页' }, component: () => import('@/views/manager/Home.vue') },
            { path: 'notice',      name: 'Notice',        meta: { name: '教务通知' }, component: () => import('@/views/manager/Notice.vue') },
            { path: 'examplan',    name: 'Examplan',      meta: { name: '考试安排' }, component: () => import('@/views/manager/Examplan.vue') },
            { path: 'roomplan',    name: 'Roomplan',      meta: { name: '教室安排' }, component: () => import('@/views/manager/Roomplan.vue') },
            { path: 'college',     name: 'College',       meta: { name: '学院信息' }, component: () => import('@/views/manager/College.vue') },
            { path: 'speciality',  name: 'Speciality',    meta: { name: '专业信息' }, component: () => import('@/views/manager/Speciality.vue') },
            { path: 'classes',     name: 'Classes',       meta: { name: '班级信息' }, component: () => import('@/views/manager/Classes.vue') },
            { path: 'course',      name: 'Course',        meta: { name: '课程信息' }, component: () => import('@/views/manager/Course.vue') },
            { path: 'choice',      name: 'Choice',        meta: { name: '我的选课' }, component: () => import('@/views/manager/Choice.vue') },
            { path: 'curriculum',  name: 'Curriculum',    meta: { name: '我的课表' }, component: () => import('@/views/manager/Curriculum.vue') },
            { path: 'score',       name: 'Score',         meta: { name: '我的成绩' }, component: () => import('@/views/manager/Score.vue') },
            { path: 'comment',     name: 'Comment',       meta: { name: '网上评教' }, component: () => import('@/views/manager/Comment.vue') },
            { path: 'apply',       name: 'Apply',         meta: { name: '请假申请' }, component: () => import('@/views/manager/Apply.vue') },
            { path: 'homework',    name: 'Homework',      meta: { name: '作业提交' }, component: () => import('@/views/manager/Homework.vue') },
            { path: 'attendance',  name: 'Attendance',    meta: { name: '考勤信息' }, component: () => import('@/views/manager/Attendance.vue') },
            { path: 'admin',       name: 'Admin',         meta: { name: '管理员信息' }, component: () => import('@/views/manager/Admin.vue') },
            { path: 'teacher',     name: 'Teacher',       meta: { name: '教师信息' }, component: () => import('@/views/manager/Teacher.vue') },
            { path: 'student',     name: 'Student',       meta: { name: '学生信息' }, component: () => import('@/views/manager/Student.vue') },
            { path: 'adminPerson',   name: 'AdminPerson',   meta: { name: '个人信息' }, component: () => import('@/views/manager/AdminPerson.vue') },
            { path: 'teacherPerson', name: 'TeacherPerson', meta: { name: '个人信息' }, component: () => import('@/views/manager/TeacherPerson.vue') },
            { path: 'studentPerson', name: 'StudentPerson', meta: { name: '个人信息' }, component: () => import('@/views/manager/StudentPerson.vue') },
            { path: 'password',    name: 'Password',      meta: { name: '修改密码' }, component: () => import('@/views/manager/Password.vue') },
        ],
    },
    { path: '/login',    name: 'Login',    component: () => import('@/views/Login.vue') },
    { path: '/register', name: 'Register', component: () => import('@/views/Register.vue') },
    { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/404.vue') },
]

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes,
})

// ========== 路由守卫 ==========
// ✅ 使用 _from 表示参数有意不使用
router.beforeEach((to, _from, next) => {
    // 从 localStorage 获取用户信息
    const userStr: string | null = localStorage.getItem('xm-user')
    const isLoggedIn = userStr && JSON.parse(userStr).id

    console.log('路由守卫 - 目标:', to.path)
    console.log('路由守卫 - 登录状态:', !!isLoggedIn)

    // 如果访问的是登录页或注册页
    if (to.path === '/login' || to.path === '/register') {
        // 如果已登录，跳转到首页
        if (isLoggedIn) {
            next('/home')
            return
        }
        next()
        return
    }

    // 如果访问的是需要登录的页面
    if (!isLoggedIn) {
        // ElMessage.warning('请先登录')
        next('/login')
        return
    }

    // 已登录，正常访问
    next()
})

export default router

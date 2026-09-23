<template>
  <!-- 功能入口（按角色过滤，与 Web 端路由权限一致） -->
  <view class="xm-card">
    <view class="xm-card-title">{{ $t('home.quickEntry') }}</view>
    <view class="xm-grid">
      <view
        v-for="item in menuItems"
        :key="item.path"
        class="xm-grid-item"
        @click="go(item.path)"
      >
        <text class="xm-grid-item-emoji">{{ item.icon }}</text>
        <view class="xm-grid-item-label">{{ $t(item.name) }}</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 功能入口：与 Web 端路由 meta.roles / meta.permission 保持一致
// perm 为 RBAC 权限码：权限已拉取时按码过滤；未拉取到（空）时退化为仅按 roles 过滤
// 管理类页面位于 pages-admin 分包；个人中心/修改密码入口移至「我的」Tab
const allMenus = [
  { path: '/pages/notice/notice', name: 'menu.notice', icon: '📢', perm: 'notice:view' },
  { path: '/pages/examplan/examplan', name: 'menu.examplan', icon: '📝', perm: 'examplan:view' },
  { path: '/pages/roomplan/roomplan', name: 'menu.roomplan', icon: '🏫', perm: 'roomplan:view' },
  { path: '/pages-admin/college/college', name: 'menu.college', icon: '🏛', roles: ['ADMIN'], perm: 'college:view' },
  {
    path: '/pages-admin/speciality/speciality',
    name: 'menu.speciality',
    icon: '📚',
    roles: ['ADMIN'],
    perm: 'speciality:view',
  },
  { path: '/pages-admin/classes/classes', name: 'menu.classes', icon: '👨‍👩‍👧', roles: ['ADMIN'], perm: 'classes:view' },
  { path: '/pages/course/course', name: 'menu.course', icon: '📖', perm: 'course:view' },
  { path: '/pages/choice/choice', name: 'menu.choice', icon: '🧾', perm: 'choice:view' },
  {
    path: '/pages/curriculum/curriculum',
    name: 'menu.curriculum',
    icon: '🗓',
    roles: ['STUDENT'],
    perm: 'curriculum:view',
  },
  { path: '/pages/score/score', name: 'menu.score', icon: '💯', perm: 'score:view' },
  { path: '/pages/comment/comment', name: 'menu.comment', icon: '⭐', perm: 'comment:view' },
  { path: '/pages/apply/apply', name: 'menu.apply', icon: '📮', perm: 'apply:view' },
  { path: '/pages/homework/homework', name: 'menu.homework', icon: '📒', perm: 'homework:view' },
  { path: '/pages/attendance/attendance', name: 'menu.attendance', icon: '🕐', perm: 'attendance:view' },
  { path: '/pages-admin/admin/admin', name: 'menu.admin', icon: '👤', roles: ['ADMIN'], perm: 'admin:view' },
  { path: '/pages-admin/teacher/teacher', name: 'menu.teacher', icon: '👨‍🏫', roles: ['ADMIN'], perm: 'teacher:view' },
  { path: '/pages-admin/student/student', name: 'menu.student', icon: '🎓', roles: ['ADMIN'], perm: 'student:view' },
]

const menuItems = computed(() => {
  const role = userStore.role
  const perms = userStore.permissions
  const isAdmin = role === 'ADMIN'
  return allMenus.filter((m) => {
    if (m.roles && !m.roles.includes(role)) return false
    // RBAC：权限码已拉取且非管理员时，隐藏无权限的入口（ADMIN 固定放行）
    if (m.perm && !isAdmin && perms.length && !perms.includes(m.perm)) return false
    return true
  })
})

const go = (path) => uni.navigateTo({ url: path })
</script>

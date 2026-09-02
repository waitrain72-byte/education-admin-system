<template>
  <view
    class="xm-page"
    :class="themeClass"
  >
    <!-- 欢迎 + 偏好切换 -->
    <view class="xm-card">
      <view class="xm-between">
        <view
          class="xm-value"
          style="font-weight: bold"
          >{{ $t('home.welcome', { name: userName }) }}</view
        >
        <view class="xm-row">
          <button
            class="xm-btn xm-btn-plain pref-btn"
            @click="toggleLocale"
          >
            {{ isZhLocale() ? 'EN' : '中' }}
          </button>
          <button
            class="xm-btn xm-btn-plain pref-btn"
            @click="cycleTheme"
          >
            {{ themeMode === 'light' ? '☀' : themeMode === 'dark' ? '☾' : '◐' }}
          </button>
        </view>
      </view>
    </view>

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
          {{ $t(item.name) }}
        </view>
      </view>
    </view>

    <!-- 教务通知 / 考试安排（首页仅展示最新 3 条，避免数据多时页面过长） -->
    <view class="xm-card">
      <view class="xm-card-title">{{ $t('home.notice') }}</view>
      <view
        v-if="!notices.length"
        class="xm-empty"
        >{{ $t('common.empty') }}</view
      >
      <view
        v-for="item in noticeList"
        :key="item.id"
        class="notice-item"
      >
        <view class="xm-value">{{ item.title }}</view>
        <view class="xm-label">{{ item.time }}</view>
      </view>
      <view
        v-if="notices.length"
        class="xm-label card-more"
        @click="go('/pages/notice/notice')"
        >{{ $t('home.viewAll') }}</view
      >
    </view>

    <view class="xm-card">
      <view class="xm-card-title">{{ $t('home.examplan') }}</view>
      <view
        v-if="!examplans.length"
        class="xm-empty"
        >{{ $t('common.empty') }}</view
      >
      <view
        v-for="item in examplanList"
        :key="item.id"
        class="notice-item"
      >
        <view class="xm-value">{{ item.name }}</view>
        <view
          class="xm-label"
          v-if="item.time"
          >{{ item.time }}</view
        >
      </view>
      <view
        v-if="examplans.length"
        class="xm-label card-more"
        @click="go('/pages/examplan/examplan')"
        >{{ $t('home.viewAll') }}</view
      >
    </view>

    <!-- 考勤统计（数据来自 /attendance/getPie，以占比条形式呈现） -->
    <view class="xm-card">
      <view class="xm-card-title">{{ $t('home.attendanceStats') }}</view>
      <view
        v-if="!attendanceTotal"
        class="xm-empty"
        >{{ $t('common.empty') }}</view
      >
      <view
        v-for="s in attendanceRows"
        :key="s.label"
        class="stat-row"
      >
        <text class="stat-label">{{ s.label }}</text>
        <view class="xm-bar-wrap">
          <view
            class="xm-bar"
            :style="{ width: s.percent + '%', background: s.color }"
          ></view>
        </view>
        <text class="stat-num">{{ s.value }}</text>
      </view>
    </view>

    <!-- 成绩统计（数据来自 /score/getLine，以条形图形式呈现） -->
    <view class="xm-card">
      <view class="xm-card-title">{{ $t('home.scoreStats') }}</view>
      <view
        v-if="!scoreRows.length"
        class="xm-empty"
        >{{ $t('common.empty') }}</view
      >
      <view
        v-for="s in scoreRows"
        :key="s.label"
        class="stat-row"
      >
        <text class="stat-label">{{ s.label }}</text>
        <view class="xm-bar-wrap">
          <view
            class="xm-bar"
            :style="{ width: s.percent + '%' }"
          ></view>
        </view>
        <text class="stat-num">{{ s.value }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { usePermission } from '@/composables/usePermission'
import { get } from '@/utils/request'
import { t, apiMessage } from '@/i18n'
import { isZhLocale, toggleLocale } from '@/composables/useLocale'
import { cycleTheme, themeMode, themeClass } from '@/composables/useTheme'

const userStore = useUserStore()
const { pullPermissions } = usePermission()
const userName = computed(() => userStore.user.name || userStore.user.username || t('layout.guest'))

// 功能入口：与 Web 端路由 meta.roles / meta.permission 保持一致
// perm 为 RBAC 权限码：权限已拉取时按码过滤；未拉取到（空）时退化为仅按 roles 过滤
// 管理类页面位于 pages-admin 分包；个人中心/修改密码入口移至「我的」Tab
const allMenus = [
  { path: '/pages/notice/notice', name: 'menu.notice', icon: '📢', perm: 'notice:view' },
  { path: '/pages/examplan/examplan', name: 'menu.examplan', icon: '📝', perm: 'examplan:view' },
  { path: '/pages/roomplan/roomplan', name: 'menu.roomplan', icon: '🏫', perm: 'roomplan:view' },
  { path: '/pages-admin/college/college', name: 'menu.college', icon: '🏛', roles: ['ADMIN'], perm: 'college:view' },
  { path: '/pages-admin/speciality/speciality', name: 'menu.speciality', icon: '📚', roles: ['ADMIN'], perm: 'speciality:view' },
  { path: '/pages-admin/classes/classes', name: 'menu.classes', icon: '👨‍👩‍👧', roles: ['ADMIN'], perm: 'classes:view' },
  { path: '/pages/course/course', name: 'menu.course', icon: '📖', perm: 'course:view' },
  { path: '/pages/choice/choice', name: 'menu.choice', icon: '🧾', perm: 'choice:view' },
  { path: '/pages/curriculum/curriculum', name: 'menu.curriculum', icon: '🗓', roles: ['STUDENT'], perm: 'curriculum:view' },
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

const notices = ref([])
const examplans = ref([])

// 首页仅展示最新 3 条，完整列表在对应页面分页浏览
const HOME_LIST_LIMIT = 3
const noticeList = computed(() => notices.value.slice(0, HOME_LIST_LIMIT))
const examplanList = computed(() => examplans.value.slice(0, HOME_LIST_LIMIT))

// 考勤统计
const attendanceStats = ref({ late: 0, absent: 0, earlyLeave: 0, normal: 0 })
const attendanceTotal = computed(() => {
  const s = attendanceStats.value
  return s.late + s.absent + s.earlyLeave + s.normal
})
const attendanceRows = computed(() => {
  const s = attendanceStats.value
  const total = attendanceTotal.value
  if (!total) return []
  const mk = (label, value, color) => ({
    label,
    value,
    color,
    percent: Math.round((value / total) * 100),
  })
  return [
    mk(t('home.statusNormal'), s.normal, '#67c23a'),
    mk(t('home.statusLate'), s.late, '#e6a23c'),
    mk(t('home.statusEarlyLeave'), s.earlyLeave, '#409eff'),
    mk(t('home.statusAbsent'), s.absent, '#f56c6c'),
  ]
})

// 成绩统计
const scoreStats = ref({ excellent: 0, good: 0, fail: 0 })
const scoreRows = computed(() => {
  const s = scoreStats.value
  const total = s.excellent + s.good + s.fail
  if (!total) return []
  const mk = (label, value) => ({ label, value, percent: Math.round((value / total) * 100) })
  return [mk(t('home.bandExcellent'), s.excellent), mk(t('home.bandGood'), s.good), mk(t('home.bandFail'), s.fail)]
})

onShow(() => {
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  // 动态设置导航栏标题，跟随语言切换
  uni.setNavigationBarTitle({ title: t('menu.home') })

  // 权限码缺失（如旧版本登录留下的缓存）时补拉，保证菜单过滤与 Web 端授权一致
  if (userStore.role !== 'ADMIN' && !userStore.permissions.length) {
    pullPermissions()
  }

  get('/notice/selectAll').then((res) => {
    notices.value = (res.data && res.data.data) || []
  })
  get('/examplan/selectAll').then((res) => {
    examplans.value = (res.data && res.data.data) || []
  })
  get('/attendance/getPie').then((res) => {
    if (res.data.code !== '200') return
    const data = (res.data.data && res.data.data.data) || []
    const stats = { late: 0, absent: 0, earlyLeave: 0, normal: 0 }
    // 后端按中文状态分组统计，这里按中文键匹配（数据库存储值为中文）
    data.forEach((item) => {
      if (item.name === '迟到') stats.late = item.value || 0
      else if (item.name === '缺勤') stats.absent = item.value || 0
      else if (item.name === '早退') stats.earlyLeave = item.value || 0
      else if (item.name === '正常') stats.normal = item.value || 0
    })
    attendanceStats.value = stats
  })
  get('/score/getLine').then((res) => {
    if (res.data.code !== '200') return
    const yAxis = (res.data.data && res.data.data.yAxis) || []
    if (yAxis.length >= 5) {
      scoreStats.value = {
        excellent: yAxis[0] || 0,
        good: yAxis[1] || 0,
        fail: yAxis[yAxis.length - 1] || 0,
      }
    }
  })
})
</script>

<style lang="scss" scoped>
.pref-btn {
  height: 56rpx;
  line-height: 56rpx;
  padding: 0 22rpx;
  font-size: 24rpx;
}

.notice-item {
  padding: 16rpx 0;
  border-bottom: 1rpx solid var(--xm-border);
}

.notice-item:last-child {
  border-bottom: none;
}

/* 「查看全部」入口：右对齐，主题色 */
.card-more {
  text-align: right;
  padding-top: 12rpx;
  color: var(--xm-brand);
}

.stat-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 16rpx;
}

.stat-label {
  width: 110rpx;
  font-size: 26rpx;
  color: var(--xm-text-2);
}

.stat-num {
  width: 60rpx;
  text-align: right;
  font-size: 26rpx;
}
</style>

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

    <!-- 教务通知 / 考试安排 -->
    <view class="xm-card">
      <view class="xm-card-title">{{ $t('home.notice') }}</view>
      <view
        v-if="!notices.length"
        class="xm-empty"
        >{{ $t('common.empty') }}</view
      >
      <view
        v-for="item in notices"
        :key="item.id"
        class="notice-item"
      >
        <view class="xm-value">{{ item.title }}</view>
        <view class="xm-label">{{ item.time }}</view>
      </view>
    </view>

    <view class="xm-card">
      <view class="xm-card-title">{{ $t('home.examplan') }}</view>
      <view
        v-if="!examplans.length"
        class="xm-empty"
        >{{ $t('common.empty') }}</view
      >
      <view
        v-for="item in examplans"
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
import { get } from '@/utils/request'
import { t, apiMessage } from '@/i18n'
import { isZhLocale, toggleLocale } from '@/composables/useLocale'
import { cycleTheme, themeMode, themeClass } from '@/composables/useTheme'

const userStore = useUserStore()
const userName = computed(() => userStore.user.name || userStore.user.username || t('layout.guest'))

// 功能入口：与 Web 端路由 meta.roles 保持一致
const allMenus = [
  { path: '/pages/notice/notice', name: 'menu.notice', icon: '📢', roles: null },
  { path: '/pages/examplan/examplan', name: 'menu.examplan', icon: '📝', roles: null },
  { path: '/pages/roomplan/roomplan', name: 'menu.roomplan', icon: '🏫', roles: null },
  { path: '/pages/college/college', name: 'menu.college', icon: '🏛', roles: ['ADMIN'] },
  { path: '/pages/speciality/speciality', name: 'menu.speciality', icon: '📚', roles: ['ADMIN'] },
  { path: '/pages/classes/classes', name: 'menu.classes', icon: '👨‍👩‍👧', roles: ['ADMIN'] },
  { path: '/pages/course/course', name: 'menu.course', icon: '📖', roles: null },
  { path: '/pages/choice/choice', name: 'menu.choice', icon: '🧾', roles: null },
  { path: '/pages/curriculum/curriculum', name: 'menu.curriculum', icon: '🗓', roles: ['STUDENT'] },
  { path: '/pages/score/score', name: 'menu.score', icon: '💯', roles: null },
  { path: '/pages/comment/comment', name: 'menu.comment', icon: '⭐', roles: null },
  { path: '/pages/apply/apply', name: 'menu.apply', icon: '📮', roles: null },
  { path: '/pages/homework/homework', name: 'menu.homework', icon: '📒', roles: null },
  { path: '/pages/attendance/attendance', name: 'menu.attendance', icon: '🕐', roles: null },
  { path: '/pages/admin/admin', name: 'menu.admin', icon: '👤', roles: ['ADMIN'] },
  { path: '/pages/teacher/teacher', name: 'menu.teacher', icon: '👨‍🏫', roles: ['ADMIN'] },
  { path: '/pages/student/student', name: 'menu.student', icon: '🎓', roles: ['ADMIN'] },
  { path: '/pages/person/person', name: 'menu.person', icon: '🧑', roles: null },
  { path: '/pages/password/password', name: 'menu.password', icon: '🔑', roles: null },
]

const menuItems = computed(() => {
  const role = userStore.role
  return allMenus.filter((m) => !m.roles || m.roles.includes(role))
})

const go = (path) => uni.navigateTo({ url: path })

const notices = ref([])
const examplans = ref([])

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

<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <home-hero />
    <home-quick-entry />

    <!-- 骨架屏：无缓存首次加载时的占位（有缓存则秒开，不经过此分支） -->
    <view
      v-if="firstLoading"
      class="xm-card"
    >
      <view class="skeleton skeleton-title"></view>
      <view class="skeleton skeleton-line"></view>
      <view class="skeleton skeleton-line"></view>
      <view class="skeleton skeleton-line short"></view>
    </view>

    <template v-else>
      <home-today-card
        v-if="userStore.role === 'STUDENT' || userStore.role === 'TEACHER'"
        :courses="todayCourses"
      />

      <!-- 分组标签页：动态（推荐/通知/考试）与 统计（考勤/成绩） -->
      <view class="home-tabs">
        <view
          class="home-tab"
          :class="{ on: homeTab === 'feed' }"
          @click="homeTab = 'feed'"
          >{{ $t('home.tabFeed') }}</view
        >
        <view
          class="home-tab"
          :class="{ on: homeTab === 'stats' }"
          @click="homeTab = 'stats'"
          >{{ $t('home.tabStats') }}</view
        >
      </view>
      <home-feed-panel
        v-if="homeTab === 'feed'"
        :recommends="userStore.role === 'STUDENT' ? recommends : []"
        :notices="notices"
        :examplans="examplans"
      />
      <home-stats-panel
        v-else
        :attendance-stats="attendanceStats"
        :score-stats="scoreStats"
      />
    </template>
    <xm-loader />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow, onHide } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { ensureLoggedIn } from '@/utils/authGuard'
import { useMessageStore } from '@/stores/message'
import { usePermission } from '@/composables/usePermission'
import { useTodayCourses } from '@/composables/useTodayCourses'
import { get } from '@/utils/request'
import { resetWsUnread } from '@/utils/websocket'
import { t } from '@/i18n'
import { themeClass } from '@/composables/useTheme'
import HomeHero from './components/home-hero.vue'
import HomeQuickEntry from './components/home-quick-entry.vue'
import HomeTodayCard from './components/home-today-card.vue'
import HomeFeedPanel from './components/home-feed-panel.vue'
import HomeStatsPanel from './components/home-stats-panel.vue'

/**
 * 系统首页：只负责数据拉取、本地缓存与标签页切换，各区块的展示与样式在 ./components 下：
 * 头卡 home-hero / 功能入口 home-quick-entry / 今日课程 home-today-card /
 * 动态 home-feed-panel / 统计 home-stats-panel
 */
const userStore = useUserStore()
const messageStore = useMessageStore()
const { pullPermissions } = usePermission()
const { todayCourses, loadTodayCourses } = useTodayCourses(userStore)

// 首页压缩（方案 B）：动态 / 统计 分组标签页
const homeTab = ref('feed')

const notices = ref([])
const examplans = ref([])
// 课程推荐（基于物品的协同过滤）：仅学生角色请求与展示
const recommends = ref([])
const attendanceStats = ref({ late: 0, absent: 0, earlyLeave: 0, normal: 0 })
const scoreStats = ref({ excellent: 0, good: 0, fail: 0 })

// 教务通知拉取：进入首页与收到 WebSocket 推送（新教务通知）时都会调用（返回 Promise 供缓存写回时机使用）
const loadNotices = () =>
  get('/notice/selectAll').then((rows) => {
    notices.value = rows || []
  })
const onWsPush = () => loadNotices().catch(() => {})

const loadExamplans = () =>
  get('/examplan/selectAll').then((rows) => {
    examplans.value = rows || []
  })

// 后端按中文状态分组统计，这里按中文键匹配（数据库存储值为中文）
const ATTENDANCE_KEYS = { 迟到: 'late', 缺勤: 'absent', 早退: 'earlyLeave', 正常: 'normal' }
const loadAttendanceStats = () =>
  get('/attendance/getPie').then((pie) => {
    const stats = { late: 0, absent: 0, earlyLeave: 0, normal: 0 }
    ;((pie && pie.data) || []).forEach((item) => {
      const key = ATTENDANCE_KEYS[item.name]
      if (key) stats[key] = item.value || 0
    })
    attendanceStats.value = stats
  })

const loadScoreStats = () =>
  get('/score/getLine').then((line) => {
    const yAxis = (line && line.yAxis) || []
    if (yAxis.length >= 5) {
      scoreStats.value = {
        excellent: yAxis[0] || 0,
        good: yAxis[1] || 0,
        fail: yAxis[yAxis.length - 1] || 0,
      }
    }
  })

const loadRecommends = () =>
  get('/course/recommend', { limit: 4 }).then((rows) => {
    recommends.value = rows || []
  })

// 首页数据本地缓存（按用户 ID 隔离，防止切换账号闪现他人数据）：
// 进入首页先渲染缓存（秒开不白屏），静默刷新完成后写回；无缓存时显示骨架屏
const HOME_CACHE_KEY = 'xm-home-cache-'
const firstLoading = ref(true)

const applyCache = () => {
  try {
    const cached = uni.getStorageSync(HOME_CACHE_KEY + userStore.user.id)
    if (cached && typeof cached === 'object' && Array.isArray(cached.notices)) {
      notices.value = cached.notices || []
      examplans.value = cached.examplans || []
      if (cached.attendanceStats) attendanceStats.value = cached.attendanceStats
      if (cached.scoreStats) scoreStats.value = cached.scoreStats
      if (Array.isArray(cached.recommends)) recommends.value = cached.recommends
      return true
    }
  } catch {}
  return false
}

const saveCache = () => {
  try {
    uni.setStorageSync(HOME_CACHE_KEY + userStore.user.id, {
      notices: notices.value,
      examplans: examplans.value,
      attendanceStats: attendanceStats.value,
      scoreStats: scoreStats.value,
      recommends: recommends.value,
    })
  } catch {}
}

onShow(() => {
  if (!ensureLoggedIn()) return
  // 动态设置导航栏标题，跟随语言切换
  uni.setNavigationBarTitle({ title: t('menu.home') })

  // 载入当前用户的推送消息历史（铃铛未读角标）
  messageStore.loadForUser(userStore.user.id)
  // 回到首页即视为已读：清掉推送未读角标
  resetWsUnread()
  // 收到 WebSocket 推送时实时刷新首页通知列表（先解绑再绑定，防止页面反复进出后重复触发）
  uni.$off('ws:push', onWsPush)
  uni.$on('ws:push', onWsPush)

  // 权限码缺失（如旧版本登录留下的缓存）时补拉，保证菜单过滤与 Web 端授权一致
  if (userStore.role !== 'ADMIN' && !userStore.permissions.length) {
    pullPermissions()
  }

  // 首页数据：先渲染缓存（秒开），再静默刷新；无缓存时显示骨架屏。
  // 每个请求单独 catch（网络失败不应打断其它请求），全部结束后写缓存并撤骨架屏
  firstLoading.value = !applyCache()
  const tasks = [loadNotices(), loadExamplans(), loadAttendanceStats(), loadScoreStats(), loadTodayCourses()]
  // 学生角色追加课程推荐（协同过滤）
  if (userStore.role === 'STUDENT') tasks.push(loadRecommends())
  Promise.all(tasks.map((p) => p.catch(() => {}))).then(() => {
    saveCache()
    firstLoading.value = false
  })
})

onHide(() => {
  uni.$off('ws:push', onWsPush)
})
</script>

<style lang="scss" scoped>
/* 动态 / 统计 分组标签页 */
.home-tabs {
  display: flex;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.home-tab {
  flex: 1;
  text-align: center;
  padding: 14rpx 0;
  border-radius: 14rpx;
  font-size: 26rpx;
  color: var(--xm-text-2);
  background: var(--xm-bg-card);
  border: 1rpx solid var(--xm-border);
}

.home-tab.on {
  background: var(--xm-brand);
  color: #ffffff;
  font-weight: bold;
  border-color: var(--xm-brand);
}
</style>

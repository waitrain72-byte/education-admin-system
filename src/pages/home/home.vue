<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 没有缓存的首次加载：骨架屏占位 -->
    <view v-if="!data && loading">
      <view class="xm-card">
        <view class="skeleton skeleton-title" />
        <view class="skeleton skeleton-line" />
        <view class="skeleton skeleton-line short" />
      </view>
      <view class="xm-card">
        <view class="skeleton skeleton-title" />
        <view class="skeleton skeleton-line" />
        <view class="skeleton skeleton-line" />
        <view class="skeleton skeleton-line short" />
      </view>
    </view>
    <view
      v-else-if="!data && failed"
      class="failed"
    >
      <view class="failed-text">{{ $t('workbench.loadFailed') }}</view>
      <button
        class="xm-btn xm-btn-primary"
        @click="load(false)"
      >
        {{ $t('workbench.retry') }}
      </button>
    </view>
    <template v-else-if="data">
      <student-home
        v-if="data.role === 'STUDENT'"
        :data="data"
      />
      <teacher-home
        v-else-if="data.role === 'TEACHER'"
        ref="teacherRef"
        :data="data"
      />
      <admin-home
        v-else-if="data.role === 'ADMIN'"
        :data="data"
      />
    </template>
    <xm-loader />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onHide, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { workbenchApi } from '@/api'
import { SILENT } from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { ensureLoggedIn } from '@/utils/authGuard'
import { readUserCache, writeUserCache } from '@/utils/userCache'
import { syncServerClock } from '@/composables/useServerClock'
import { usePermission } from '@/composables/usePermission'
import { t } from '@/i18n'
import StudentHome from './StudentHome.vue'
import TeacherHome from './TeacherHome.vue'
import AdminHome from './AdminHome.vue'

const userStore = useUserStore()
const { pullPermissions } = usePermission()

/** /workbench/summary 按角色返回不同内容；先渲染本账号上次的缓存（秒开、弱网也有内容），再静默刷新 */
const data = ref(null)
const loading = ref(false)
const failed = ref(false)
const teacherRef = ref(null)
let cachedFor = ''

const applyCache = () => {
  const account = userStore.accountKey
  if (cachedFor === account) return
  cachedFor = account
  const cached = readUserCache('home', account)
  // 角色对不上的缓存（理论上不会出现）不用
  data.value = cached && cached.role === userStore.role ? cached : null
}

const load = async (silent = true) => {
  loading.value = true
  failed.value = false
  try {
    const summary = await workbenchApi.summary(silent ? SILENT : undefined)
    syncServerClock(summary && summary.now)
    data.value = summary
    writeUserCache('home', userStore.accountKey, summary)
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}

/** 静默刷新：签到开始 / 结束、布置作业、成绩发布时后端推事件，首页跟着变；同一批事件只刷新一次 */
let refreshTimer = null
const scheduleRefresh = () => {
  if (refreshTimer) return
  refreshTimer = setTimeout(() => {
    refreshTimer = null
    load()
  }, 600)
}
const onCourseEvent = (message) => {
  if (['attendance', 'assignments'].includes(String(message && message.event))) scheduleRefresh()
}

onShow(() => {
  if (!ensureLoggedIn()) return
  uni.setNavigationBarTitle({ title: t('nav.home') })
  // 权限码缺失（如旧版本登录留下的缓存）时补拉，保证按权限显示的入口与 Web 端授权一致
  if (userStore.role !== 'ADMIN' && !userStore.permissions.length) pullPermissions()
  applyCache()
  load()
  uni.$off('ws:course', onCourseEvent)
  uni.$on('ws:course', onCourseEvent)
  uni.$off('ws:push', scheduleRefresh)
  uni.$on('ws:push', scheduleRefresh)
})

onHide(() => {
  uni.$off('ws:course', onCourseEvent)
  uni.$off('ws:push', scheduleRefresh)
  if (refreshTimer) {
    clearTimeout(refreshTimer)
    refreshTimer = null
  }
})

onPullDownRefresh(async () => {
  await load()
  if (teacherRef.value && teacherRef.value.reloadExtras) teacherRef.value.reloadExtras()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
.failed {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24rpx;
  padding: 160rpx 0;
}

.failed-text {
  font-size: 28rpx;
  color: var(--xm-text-2);
}
</style>

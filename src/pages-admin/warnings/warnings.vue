<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 学业预警（与 Web 端教务后台「学业预警」一致）：按成绩与考勤算出的学业风险，风险高的在前，可以给学生推送提醒 -->
    <view class="desc">{{ $t('pages.warning.desc') }}</view>
    <view class="filters">
      <xm-search
        v-model="keyword"
        :placeholder="$t('space.members.search')"
      />
      <xm-chips
        v-model="level"
        :options="levelOptions"
      />
    </view>

    <view
      v-if="loading && !list.length"
      class="xm-card"
    >
      <view class="skeleton skeleton-line" />
      <view class="skeleton skeleton-line short" />
    </view>
    <xm-empty
      v-else-if="!shown.length"
      icon="alert-triangle"
      :text="$t('pages.warning.noData')"
    />
    <view
      v-else
      class="xm-list"
    >
      <view
        v-for="s in shown"
        :key="s.studentId"
        class="xm-card student"
      >
        <view class="student-head">
          <xm-user-avatar
            :name="s.studentName"
            :size="64"
          />
          <view class="student-main">
            <view class="student-name">{{ s.studentName }}</view>
            <view class="student-risk xm-num">{{ $t('pages.warning.riskIndex') }} {{ s.riskIndex }}</view>
          </view>
          <text
            class="xm-tag"
            :class="LEVEL_TAG[s.level] || 'xm-tag-success'"
            >{{ levelLabel(s.level) }}</text
          >
        </view>
        <view class="metrics">
          <view class="metric">
            <text class="metric-label">{{ $t('pages.warning.courseCount') }}</text>
            <text class="metric-value xm-num">{{ s.courseCount }}</text>
          </view>
          <view class="metric">
            <text class="metric-label">{{ $t('pages.warning.avg') }}</text>
            <text class="metric-value xm-num">{{ s.avgScore == null ? '—' : s.avgScore }}</text>
          </view>
          <view class="metric">
            <text class="metric-label">{{ $t('pages.warning.failed') }}</text>
            <text class="metric-value xm-num">{{ s.failedCount }}</text>
          </view>
          <view class="metric">
            <text class="metric-label">{{ $t('pages.warning.absentRate') }}</text>
            <text class="metric-value xm-num">{{ s.absentRate }}</text>
          </view>
        </view>
        <view
          v-if="s.suggestion"
          class="xm-quote"
          >{{ s.suggestion }}</view
        >
        <view
          v-if="s.level !== '正常'"
          class="xm-actions"
        >
          <button
            class="xm-btn xm-btn-soft xm-btn-sm"
            :disabled="sending === s.studentId || sent.includes(s.studentId)"
            @click="notify(s)"
          >
            {{ $t('pages.warning.notify') }}
          </button>
        </view>
      </view>
    </view>
    <xm-loader />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { warningApi } from '@/api'
import { SILENT } from '@/utils/request'
import { ensureRole } from '@/utils/authGuard'
import { t } from '@/i18n'

const LEVELS = ['高风险', '中风险', '低风险', '正常']
const LEVEL_KEY = { 高风险: 'high', 中风险: 'medium', 低风险: 'low' }
const LEVEL_TAG = { 高风险: 'xm-tag-danger', 中风险: 'xm-tag-warning', 低风险: 'xm-tag-info' }

/** 后端返回的等级是中文原值，显示时翻译 */
const levelLabel = (value) => {
  if (LEVEL_KEY[value]) return t('workbench.attention.levels.' + LEVEL_KEY[value])
  if (value === '正常') return t('mobile.warningNormal')
  return value || '—'
}

const list = ref([])
const loading = ref(false)
const keyword = ref('')
const level = ref('')
const sending = ref(null)
const sent = ref([])

const levelOptions = computed(() => [
  { value: '', label: t('common.all'), count: list.value.length },
  ...LEVELS.map((value) => ({
    value,
    label: levelLabel(value),
    count: list.value.filter((s) => s.level === value).length,
  })),
])

const shown = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  return list.value
    .filter((s) => !level.value || s.level === level.value)
    .filter(
      (s) =>
        !k ||
        String(s.studentName || '')
          .toLowerCase()
          .includes(k),
    )
})

const load = async () => {
  loading.value = true
  try {
    list.value = (await warningApi.list(SILENT)) || []
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    loading.value = false
  }
}

const notify = async (s) => {
  sending.value = s.studentId
  try {
    await warningApi.notify(s.studentId)
    sent.value.push(s.studentId)
    uni.showToast({ title: t('pages.warning.notifyOk'), icon: 'none' })
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    sending.value = null
  }
}

onShow(() => {
  if (!ensureRole(['ADMIN'])) return
  uni.setNavigationBarTitle({ title: t('menu.warning') })
  load()
})

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
.desc {
  margin-bottom: 20rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: var(--xm-text-2);
}

.filters {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.student-head {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.student-main {
  flex: 1;
  min-width: 0;
}

.student-name {
  font-size: 30rpx;
  font-weight: 600;
  color: var(--xm-text);
}

.student-risk {
  margin-top: 2rpx;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8rpx;
  margin-top: 16rpx;
}

.metric {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
}

.metric-label {
  font-size: 20rpx;
  text-align: center;
  color: var(--xm-text-2);
}

.metric-value {
  font-size: 30rpx;
  font-weight: 600;
  color: var(--xm-text);
}
</style>

<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 模型说明：风险指数怎么算、谁能看到哪些学生（与 Web 端一致） -->
    <view class="xm-card warning-desc">
      <xm-icon
        name="alert-triangle"
        :size="32"
      />
      <text class="warning-desc-text">{{ $t('pages.warning.desc') }}</text>
    </view>

    <!-- 等级快捷筛选（教师 / 管理员；学生只有本人一条，不需要筛选） -->
    <view
      v-if="isStaff && list.length"
      class="xm-card xm-tabs"
    >
      <view
        v-for="tab in levelTabs"
        :key="tab.value"
        class="xm-tab"
        :class="{ active: level === tab.value }"
        @click="level = tab.value"
      >
        <text>{{ tab.label }}</text>
        <text class="tab-count">{{ tab.count }}</text>
      </view>
    </view>

    <xm-empty
      v-if="loaded && !filtered.length"
      icon="shield"
      :text="$t('pages.warning.noData')"
      :action-text="$t('common.reload')"
      @action="load()"
    />

    <!-- 列表：按风险指数降序（后端已排好）；手机单列，平板两列 / 三列 -->
    <view class="xm-list">
      <view
        v-for="row in filtered"
        :key="row.studentId"
        class="xm-card"
      >
        <view class="xm-between">
          <text class="xm-card-name xm-ellipsis">{{ row.studentName }}</text>
          <text
            class="xm-tag"
            :class="enumTag('warningLevel', row.level)"
            >{{ enumLabel('warningLevel', row.level) }}</text
          >
        </view>

        <!-- 风险指数 0~100：进度条 + 数值，颜色随等级 -->
        <view class="risk-row">
          <text class="xm-label">{{ $t('pages.warning.riskIndex') }}</text>
          <view class="risk-bar">
            <view
              class="risk-fill"
              :class="'tone-' + toneOf(row.level)"
              :style="{ width: clampPercent(row.riskIndex) + '%' }"
            />
          </view>
          <text
            class="risk-value"
            :class="'tone-' + toneOf(row.level)"
            >{{ row.riskIndex }}</text
          >
        </view>

        <!-- 三项指标的原始数据 -->
        <view class="stat-grid">
          <view class="stat">
            <text class="stat-value">{{ row.avgScore }}</text>
            <text class="stat-label">{{ $t('pages.warning.avg') }}</text>
          </view>
          <view class="stat">
            <text
              class="stat-value"
              :class="{ 'is-bad': row.failedCount > 0 }"
              >{{ row.failedCount }}</text
            >
            <text class="stat-label">{{ $t('pages.warning.failed') }}</text>
          </view>
          <view class="stat">
            <text class="stat-value">{{ row.courseCount }}</text>
            <text class="stat-label">{{ $t('pages.warning.courseCount') }}</text>
          </view>
          <view class="stat">
            <text
              class="stat-value"
              :class="{ 'is-bad': row.absentRate > 0 }"
              >{{ row.absentRate }}%</text
            >
            <text class="stat-label">{{ $t('pages.warning.abnormal') }}</text>
          </view>
        </view>

        <view
          v-if="row.suggestion"
          class="suggestion"
        >
          <text class="suggestion-label">{{ $t('pages.warning.suggestion') }}</text>
          <text>{{ row.suggestion }}</text>
        </view>

        <!-- 教师 / 管理员：实时推送预警提醒给该学生 -->
        <view
          v-if="isStaff"
          class="xm-actions"
        >
          <button
            class="xm-btn xm-btn-plain"
            :loading="notifying === row.studentId"
            :disabled="notifying === row.studentId"
            @click="notify(row)"
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
import { ref, computed } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { ensureLoggedIn } from '@/utils/authGuard'
import { warningApi } from '@/api'
import { SILENT } from '@/utils/request'
import { t } from '@/i18n'
import { enumLabel, enumTag, enumOptions } from '@/utils/enums'

/**
 * 学业预警：后端按成绩与考勤的多指标加权模型算出 0~100 的风险指数（见后端 WarningService），
 * 管理员看全部学生、教师看本人任课学生、学生只看本人；教师 / 管理员可向学生实时推送提醒。
 * 列表一次返回全部（不分页），等级筛选在前端完成。
 */
const userStore = useUserStore()
const isStaff = computed(() => userStore.role !== 'STUDENT')

const list = ref([])
const loaded = ref(false)
// 当前筛选的等级：'' = 全部
const level = ref('')

const levelTabs = computed(() => [
  { value: '', label: t('common.all'), count: list.value.length },
  ...enumOptions('warningLevel').map((o) => ({
    ...o,
    count: list.value.filter((r) => r.level === o.value).length,
  })),
])
const filtered = computed(() => (level.value ? list.value.filter((r) => r.level === level.value) : list.value))

// 等级 → 配色（与标签同色系）：danger / warning / info / success
const toneOf = (value) => (enumTag('warningLevel', value) || 'xm-tag-success').replace('xm-tag-', '')
const clampPercent = (n) => Math.max(0, Math.min(100, Number(n) || 0))

const load = (silent = false) =>
  warningApi
    .list(silent ? SILENT : undefined)
    .then((rows) => {
      list.value = rows || []
    })
    .catch(() => {
      // 提示已由请求层统一弹出；保留已有数据
    })
    .finally(() => {
      loaded.value = true
    })

// 推送提醒：同一时间只处理一条，按钮转圈防连点
const notifying = ref(null)
const notify = (row) => {
  if (notifying.value !== null) return
  notifying.value = row.studentId
  warningApi
    .notify(row.studentId)
    .then(() => uni.showToast({ title: t('pages.warning.notifyOk'), icon: 'success' }))
    .catch(() => {
      // 「该学生当前无预警数据」等提示已由请求层统一弹出
    })
    .finally(() => {
      notifying.value = null
    })
}

onShow(() => {
  uni.setNavigationBarTitle({ title: t('menu.warning') })
  if (!ensureLoggedIn()) return
  // 首次进入走加载动画；之后回到页面静默刷新（成绩 / 考勤可能已变化）
  load(loaded.value)
})

onPullDownRefresh(() => {
  load(true).finally(() => uni.stopPullDownRefresh())
})
</script>

<style lang="scss" scoped>
.warning-desc {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  color: var(--xm-warning);
}

.warning-desc-text {
  flex: 1;
  min-width: 0;
  font-size: 24rpx;
  line-height: 1.6;
  color: var(--xm-text-2);
}

/* 等级名换行时（英文 Medium Risk）各标签的人数仍底部对齐 */
.xm-tab {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.tab-count {
  display: block;
  font-size: 22rpx;
  opacity: 0.85;
}

.risk-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 20rpx;
}

.risk-bar {
  flex: 1;
  height: 14rpx;
  border-radius: 999rpx;
  overflow: hidden;
  background: var(--xm-bg-input);
}

.risk-fill {
  height: 100%;
  border-radius: 999rpx;
}

.risk-value {
  min-width: 56rpx;
  text-align: right;
  font-size: 34rpx;
  font-weight: bold;
}

.risk-fill.tone-danger {
  background: var(--xm-danger);
}

.risk-fill.tone-warning {
  background: var(--xm-warning);
}

.risk-fill.tone-info {
  background: var(--xm-info);
}

.risk-fill.tone-success {
  background: var(--xm-success);
}

.risk-value.tone-danger {
  color: var(--xm-danger);
}

.risk-value.tone-warning {
  color: var(--xm-warning);
}

.risk-value.tone-info {
  color: var(--xm-info);
}

.risk-value.tone-success {
  color: var(--xm-success);
}

/* 指标四宫格 */
.stat-grid {
  display: flex;
  gap: 12rpx;
  margin-top: 20rpx;
}

.stat {
  flex: 1;
  min-width: 0;
  padding: 14rpx 0;
  text-align: center;
  border-radius: 12rpx;
  background: var(--xm-bg-input);
}

.stat-value {
  display: block;
  font-size: 30rpx;
  font-weight: bold;
  color: var(--xm-text);
}

.stat-value.is-bad {
  color: var(--xm-danger);
}

.stat-label {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

/* 建议气泡 */
.suggestion {
  margin-top: 16rpx;
  padding: 16rpx 20rpx;
  border-radius: 12rpx;
  font-size: 26rpx;
  line-height: 1.6;
  color: var(--xm-text);
  background: var(--xm-bg-input);
}

.suggestion-label {
  margin-right: 12rpx;
  font-weight: bold;
  color: var(--xm-warning);
}
</style>

<template>
  <!-- 近期考试（与 Web 端 views/home/ExamsPanel.vue 一致）：日历撕页样的日期 + 考试名 + 倒计时标签 -->
  <view class="xm-card">
    <view class="xm-section-head">
      <text class="xm-section-title">{{ $t('workbench.exams') }}</text>
      <text
        v-if="moreUrl"
        class="xm-link"
        @click="openMore"
        >{{ $t('home.viewAll') }} ›</text
      >
    </view>
    <view v-if="exams.length">
      <view
        v-for="e in exams"
        :key="e.id"
        class="exam-row"
      >
        <view class="exam-date xm-num">
          <text class="exam-day">{{ day(e.examTime) }}</text>
          <text class="exam-month">{{ month(e.examTime) }}</text>
        </view>
        <view class="exam-info">
          <view class="exam-name">{{ e.name }}</view>
          <view class="exam-time xm-num">{{ e.examTime || '—' }}</view>
        </view>
        <text
          v-if="tagOf(e)"
          class="xm-tag"
          :class="tagOf(e).cls"
          >{{ tagOf(e).text }}</text
        >
      </view>
    </view>
    <view
      v-else
      class="exam-empty"
      >{{ $t('workbench.noExams') }}</view
    >
  </view>
</template>

<script setup>
import { countdownTag } from '@/utils/examCountdown'
import { isZh } from '@/i18n'

const props = defineProps({
  exams: { type: Array, default: () => [] },
  /** 「查看全部」打开的页面（管理员首页才有） */
  moreUrl: { type: String, default: '' },
})

const MONTH_EN = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

/** 考试时间「2026-11-02 09:00」→ 日 02、月「11月」/「Nov」 */
const day = (time) => (time || '').slice(8, 10) || '—'
const month = (time) => {
  const m = Number((time || '').slice(5, 7))
  if (!m) return ''
  return isZh() ? `${m}月` : MONTH_EN[m - 1]
}

const tagOf = (e) => countdownTag(e.examTime)

const openMore = () => uni.navigateTo({ url: props.moreUrl })
</script>

<style lang="scss" scoped>
.exam-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 16rpx 0;
}

.exam-row + .exam-row {
  border-top: 1rpx solid var(--xm-border);
}

.exam-date {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 84rpx;
  height: 84rpx;
  flex-shrink: 0;
  border-radius: 18rpx;
  background: var(--xm-brand-soft);
  color: var(--xm-brand);
  line-height: 1.1;
}

.exam-day {
  font-size: 32rpx;
  font-weight: bold;
}

.exam-month {
  font-size: 20rpx;
}

.exam-info {
  flex: 1;
  min-width: 0;
}

.exam-name {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--xm-text);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.exam-time {
  margin-top: 4rpx;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.exam-empty {
  padding: 16rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: var(--xm-text-2);
}
</style>

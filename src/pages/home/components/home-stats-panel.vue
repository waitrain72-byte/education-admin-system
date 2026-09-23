<template>
  <view>
    <!-- 考勤统计（数据来自 /attendance/getPie，纯 CSS 环形图 + 图例） -->
    <view class="xm-card">
      <view class="xm-card-title">{{ $t('home.attendanceStats') }}</view>
      <xm-empty
        v-if="!attendanceTotal"
        icon="🕐"
      />
      <template v-else>
        <view class="donut-wrap">
          <view
            class="donut"
            :style="{ background: donutGradient }"
          >
            <view class="donut-hole">
              <text class="donut-num">{{ attendanceTotal }}</text>
              <text class="donut-label">{{ $t('home.donutTotal') }}</text>
            </view>
          </view>
        </view>
        <view
          v-for="r in attendanceRows"
          :key="r.label"
          class="stat-row"
        >
          <view
            class="stat-dot"
            :style="{ background: r.color }"
          ></view>
          <text class="stat-label">{{ r.label }}</text>
          <view class="stat-track">
            <view
              class="stat-fill"
              :style="{ width: r.percent + '%', background: r.color }"
            ></view>
          </view>
          <text class="stat-num">{{ r.value }}</text>
        </view>
      </template>
    </view>

    <!-- 成绩统计（数据来自 /score/getLine，纯 CSS 柱状图） -->
    <view class="xm-card">
      <view class="xm-card-title">{{ $t('home.scoreStats') }}</view>
      <xm-empty
        v-if="!scoreRows.length"
        icon="💯"
      />
      <view
        v-else
        class="vbar-chart"
      >
        <view
          v-for="(r, i) in scoreRows"
          :key="r.label"
          class="vbar-col"
        >
          <text class="vbar-num">{{ r.value }}</text>
          <view class="vbar-track">
            <view
              class="vbar-bar"
              :style="{ height: vbarHeight(r), background: bandColors[i % bandColors.length] }"
            ></view>
          </view>
          <text class="vbar-label">{{ r.label }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { t } from '@/i18n'

/**
 * 首页「统计」标签页：考勤环形图 + 成绩柱状图。
 * 图表实现：纯 CSS（conic-gradient 环形图 + 柱状图），零依赖、小程序 100% 渲染、深浅主题自适应
 */
const props = defineProps({
  attendanceStats: { type: Object, default: () => ({ late: 0, absent: 0, earlyLeave: 0, normal: 0 }) },
  scoreStats: { type: Object, default: () => ({ excellent: 0, good: 0, fail: 0 }) },
})

const attendanceTotal = computed(() => {
  const s = props.attendanceStats
  return s.late + s.absent + s.earlyLeave + s.normal
})
const attendanceRows = computed(() => {
  const s = props.attendanceStats
  const total = attendanceTotal.value
  if (!total) return []
  const mk = (label, value, color) => ({
    label,
    value,
    color,
    percent: Math.round((value / total) * 100),
  })
  return [
    mk(t('home.statusNormal'), s.normal, '#22b866'),
    mk(t('home.statusLate'), s.late, '#f59e0b'),
    mk(t('home.statusEarlyLeave'), s.earlyLeave, '#3b82f6'),
    mk(t('home.statusAbsent'), s.absent, '#f0555f'),
  ]
})

const scoreRows = computed(() => {
  const s = props.scoreStats
  const total = s.excellent + s.good + s.fail
  if (!total) return []
  const mk = (label, value) => ({ label, value, percent: Math.round((value / total) * 100) })
  return [mk(t('home.bandExcellent'), s.excellent), mk(t('home.bandGood'), s.good), mk(t('home.bandFail'), s.fail)]
})

const bandColors = ['#22b866', '#5b6cff', '#f0555f']

/** 环形图锥形渐变：按各状态精确占比分段（用原始值计算，避免四舍五入留下缝隙） */
const donutGradient = computed(() => {
  const total = attendanceTotal.value
  if (!total) return 'transparent'
  let acc = 0
  const stops = attendanceRows.value
    .filter((r) => r.value > 0)
    .map((r) => {
      const start = (acc / total) * 100
      acc += r.value
      const end = (acc / total) * 100
      return `${r.color} ${start}% ${end}%`
    })
  return `conic-gradient(${stops.join(', ')})`
})

const vbarMax = computed(() => Math.max(...scoreRows.value.map((r) => r.value), 1))

const vbarHeight = (r) => Math.max(12, Math.round((r.value / vbarMax.value) * 100)) + '%'
</script>

<style lang="scss" scoped>
/* ===== 统计图表：纯 CSS 环形图 + 柱状图 ===== */
.donut-wrap {
  display: flex;
  justify-content: center;
  padding: 8rpx 0 24rpx;
}

.donut {
  width: 280rpx;
  height: 280rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.donut-hole {
  width: 176rpx;
  height: 176rpx;
  border-radius: 50%;
  background: var(--xm-bg-card);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.donut-num {
  font-size: 40rpx;
  font-weight: bold;
  color: var(--xm-text);
}

.donut-label {
  font-size: 20rpx;
  color: var(--xm-text-2);
  margin-top: 2rpx;
}

.stat-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 14rpx;
}

.stat-dot {
  width: 18rpx;
  height: 18rpx;
  border-radius: 50%;
  flex-shrink: 0;
}

.stat-label {
  width: 110rpx;
  font-size: 26rpx;
  color: var(--xm-text-2);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-shrink: 0;
}

.stat-track {
  flex: 1;
  height: 14rpx;
  border-radius: 999rpx;
  background: var(--xm-bg-input);
  overflow: hidden;
}

.stat-fill {
  height: 100%;
  border-radius: 999rpx;
}

.stat-num {
  width: 60rpx;
  text-align: right;
  font-size: 26rpx;
  flex-shrink: 0;
}

.vbar-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  padding: 12rpx 8rpx 4rpx;
}

.vbar-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
  width: 140rpx;
}

.vbar-num {
  font-size: 24rpx;
  color: var(--xm-text);
}

.vbar-track {
  width: 64rpx;
  height: 240rpx;
  border-radius: 999rpx;
  background: var(--xm-bg-input);
  display: flex;
  align-items: flex-end;
  overflow: hidden;
}

.vbar-bar {
  width: 100%;
  border-radius: 999rpx;
  transition: height 0.3s ease;
}

.vbar-label {
  font-size: 24rpx;
  color: var(--xm-text-2);
}
</style>

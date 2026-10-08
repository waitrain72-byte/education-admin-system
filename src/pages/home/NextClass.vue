<template>
  <!-- 下一节课（与 Web 端 views/home/NextClassCard.vue 一致）：正在上的优先，其次今天最近的一节 -->
  <view
    class="next"
    :style="barStyle"
  >
    <template v-if="next">
      <view class="next-label">
        <view
          class="next-dot"
          :class="{ 'is-live': state === 'ongoing' }"
        />
        {{ state === 'ongoing' ? $t('workbench.next.ongoing') : $t('workbench.next.upcoming', { time: next.start }) }}
      </view>
      <view class="next-main">
        <view class="next-body">
          <view
            class="next-name"
            @click="openCourse(next.id)"
            >{{ next.name }}</view
          >
          <view class="next-meta">{{ meta }}</view>
          <view class="next-foot xm-num">
            <template v-if="state === 'ongoing'">{{ $t('workbench.next.endsAt', { time: next.end }) }}</template>
            <template v-else>{{ countdown }}</template>
          </view>
        </view>
        <button
          v-if="enterLabel"
          class="xm-btn xm-btn-primary xm-btn-sm"
          @click="openCourse(next.id)"
        >
          {{ enterLabel }}
        </button>
      </view>
    </template>
    <template v-else>
      <view class="next-label">{{ $t('workbench.next.title') }}</view>
      <view class="next-none">
        {{ courses.length ? $t('workbench.next.allDone') : $t('workbench.next.noneToday') }}
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { classState, minutesUntil, nextClass } from '@/utils/schedule'
import { courseColor } from '@/utils/courseColor'
import { useServerClock } from '@/composables/useServerClock'
import { t } from '@/i18n'

const props = defineProps({
  courses: { type: Array, default: () => [] },
  showTeacher: { type: Boolean, default: true },
  /** 右侧按钮文字（教师端「进入课堂」），不传不显示 */
  enterLabel: { type: String, default: '' },
})

const clock = useServerClock()
const next = computed(() => nextClass(props.courses, clock.value.hhmm))
const state = computed(() => (next.value ? classState(next.value.segment, clock.value.hhmm) : ''))
const barStyle = computed(() => (next.value ? `--cc: ${courseColor(next.value.name)};` : ''))

const meta = computed(() =>
  [next.value.room, props.showTeacher ? next.value.teacherName : null].filter(Boolean).join(' · '),
)

/** 距离上课：一小时以内显示分钟，再远显示「X 小时 Y 分」 */
const countdown = computed(() => {
  if (!next.value) return ''
  const minutes = minutesUntil(next.value.segment, clock.value.hhmm)
  if (minutes === null) return ''
  if (minutes < 60) return t('workbench.next.inMinutes', { n: minutes })
  return t('workbench.next.inHours', { h: Math.floor(minutes / 60), m: minutes % 60 })
})

const openCourse = (id) => uni.navigateTo({ url: `/pages-course/space/space?id=${id}` })
</script>

<style lang="scss" scoped>
.next {
  position: relative;
  padding: 24rpx 24rpx 24rpx 36rpx;
  border: 1rpx solid var(--xm-border);
  border-radius: 24rpx;
  background: var(--xm-bg-card);
  box-shadow: var(--xm-shadow-lg);
  overflow: hidden;
}

/* 左侧一条课程色竖条，与课程卡片、课表里同一门课的颜色一致 */
.next::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 10rpx;
  background: var(--cc, var(--xm-border));
}

.next-label {
  display: flex;
  align-items: center;
  gap: 10rpx;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.next-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: var(--xm-info);
}

.next-dot.is-live {
  background: var(--xm-success);
  animation: next-pulse 1.6s ease-in-out infinite;
}

@keyframes next-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(29, 128, 72, 0.45);
  }

  50% {
    box-shadow: 0 0 0 10rpx rgba(29, 128, 72, 0);
  }
}

.next-main {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.next-body {
  flex: 1;
  min-width: 0;
}

.next-name {
  margin-top: 8rpx;
  font-size: 34rpx;
  font-weight: bold;
  color: var(--xm-text);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.next-meta {
  margin-top: 4rpx;
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.next-foot {
  margin-top: 8rpx;
  font-size: 24rpx;
  font-weight: 600;
  color: var(--xm-brand);
}

.next-none {
  margin-top: 10rpx;
  font-size: 28rpx;
  color: var(--xm-text-2);
}
</style>

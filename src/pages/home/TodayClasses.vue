<template>
  <!-- 今天的课（与 Web 端 views/home/TodayClasses.vue 一致）：按节次排序，上课中高亮、已下课变淡，签到中的课直达签到页 -->
  <view
    v-if="courses.length"
    class="today"
  >
    <view
      v-for="c in sorted"
      :key="c.id + '-' + c.segment"
      class="today-row"
      :class="'is-' + (stateOf(c) || 'unknown')"
      @click="open(c)"
    >
      <view class="today-time xm-num">
        <text class="today-start">{{ c.start || '—' }}</text>
        <text class="today-end">{{ c.end }}</text>
      </view>
      <view
        class="today-bar"
        :style="'background:' + courseColor(c.name)"
      />
      <view class="today-info">
        <view class="today-name">{{ c.name }}</view>
        <view class="today-meta">{{ metaOf(c) }}</view>
      </view>
      <text
        v-if="c.signing"
        class="xm-tag xm-tag-danger"
        @click.stop="openAttendance(c)"
        >{{ $t('workbench.signing') }} ›</text
      >
      <text
        v-else
        class="xm-tag"
        :class="tagOf(c)"
        >{{ stateLabel(c) }}</text
      >
    </view>
  </view>
  <view
    v-else
    class="today-empty"
    >{{ $t('workbench.noClassToday') }}</view
  >
</template>

<script setup>
import { computed } from 'vue'
import { classState, minutesUntil, segmentIndex } from '@/utils/schedule'
import { courseColor } from '@/utils/courseColor'
import { useServerClock } from '@/composables/useServerClock'
import { t } from '@/i18n'

const props = defineProps({
  courses: { type: Array, default: () => [] },
  showTeacher: { type: Boolean, default: true },
})

const clock = useServerClock()
const sorted = computed(() => [...props.courses].sort((a, b) => segmentIndex(a.segment) - segmentIndex(b.segment)))
const stateOf = (c) => classState(c.segment, clock.value.hhmm)

const metaOf = (c) => [c.room, props.showTeacher ? c.teacherName : null].filter(Boolean).join(' · ') || '—'

const stateLabel = (c) => {
  const state = stateOf(c)
  if (state === 'ongoing') return t('workbench.state.ongoing')
  if (state === 'done') return t('workbench.state.done')
  if (state === 'upcoming') {
    const minutes = minutesUntil(c.segment, clock.value.hhmm)
    return minutes !== null && minutes <= 60 ? t('workbench.state.soon', { n: minutes }) : t('workbench.state.upcoming')
  }
  return '—'
}

const tagOf = (c) => {
  const state = stateOf(c)
  if (state === 'ongoing') return 'xm-tag-brand'
  if (state === 'upcoming') return 'xm-tag-info'
  return ''
}

const open = (c) => uni.navigateTo({ url: `/pages-course/space/space?id=${c.id}` })
const openAttendance = (c) => uni.navigateTo({ url: `/pages-course/space/space?id=${c.id}&tab=attendance` })
</script>

<style lang="scss" scoped>
.today-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 20rpx;
  margin: 0 -20rpx;
  border-radius: 20rpx;
}

.today-row + .today-row {
  margin-top: 4rpx;
}

.today-row.is-ongoing {
  background: var(--xm-brand-soft);
}

.today-row.is-done {
  opacity: 0.6;
}

.today-time {
  display: flex;
  flex-direction: column;
  width: 84rpx;
  flex-shrink: 0;
  line-height: 1.25;
}

.today-start {
  font-size: 30rpx;
  font-weight: 600;
  color: var(--xm-text);
}

.today-end {
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.today-bar {
  align-self: stretch;
  width: 8rpx;
  border-radius: 4rpx;
  flex-shrink: 0;
}

.today-info {
  flex: 1;
  min-width: 0;
}

.today-name {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--xm-text);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.today-meta {
  margin-top: 4rpx;
  font-size: 24rpx;
  color: var(--xm-text-2);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.today-empty {
  padding: 24rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: var(--xm-text-2);
}
</style>

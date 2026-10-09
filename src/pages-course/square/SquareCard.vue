<template>
  <!-- 课程广场的卡片（与 Web 端 views/square/SquareCard.vue 一致）：上半截课程色封面（点进课程概览），
       下半截是教师、简介 / 推荐理由、名额和选课按钮 -->
  <view
    class="sq"
    :class="{ 'is-ended': ended }"
  >
    <view
      class="sq-cover"
      :style="'background-color:' + color"
      @click="openCourse"
    >
      <view class="sq-tags">
        <text class="sq-tag">{{ courseTypeLabel(course.type) }}</text>
        <text class="sq-tag xm-num">{{
          $t('courses.credits', { n: course.credit == null ? '—' : course.credit })
        }}</text>
        <text
          v-if="course.status !== '已开课'"
          class="sq-tag"
          >{{ courseStatusLabel(course.status) || '—' }}</text
        >
      </view>
      <view class="sq-name">{{ course.name }}</view>
      <view class="sq-time">{{ courseWhen(course) }}</view>
    </view>

    <view class="sq-body">
      <view class="sq-teacher">{{ course.teacherName || '—' }}</view>
      <view
        v-if="course.intro"
        class="sq-intro"
        >{{ course.intro }}</view
      >
      <view
        v-if="course.reason"
        class="sq-reason"
        >{{ course.reason }}</view
      >

      <view class="sq-seats">
        <view class="sq-bar">
          <view
            class="sq-bar-fill"
            :class="{ 'is-full': full }"
            :style="'width:' + fillPercent + '%'"
          />
        </view>
        <text class="sq-seat-text xm-num">{{ seatText }}</text>
      </view>

      <view class="sq-foot">
        <text
          v-if="course.enrolled"
          class="xm-tag xm-tag-success"
          >{{ $t('square.enrolled') }}</text
        >
        <text
          v-else-if="course.conflict"
          class="xm-tag xm-tag-warning sq-conflict"
          >{{ $t('square.conflict', { name: course.conflict }) }}</text
        >
        <view class="sq-spacer" />
        <template v-if="canEnroll">
          <button
            v-if="course.enrolled && course.canDrop"
            class="xm-btn xm-btn-plain xm-btn-sm"
            :disabled="busy"
            @click="$emit('drop', course)"
          >
            {{ $t('square.drop') }}
          </button>
          <button
            v-else-if="!course.enrolled && !ended"
            class="xm-btn xm-btn-primary xm-btn-sm"
            :disabled="full || !!course.conflict || busy"
            :loading="busy"
            @click="$emit('enroll', course)"
          >
            {{ $t('square.enroll') }}
          </button>
        </template>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { courseColor } from '@/utils/courseColor'
import { courseStatusLabel, courseTypeLabel, courseWhen } from '@/utils/courseText'
import { t } from '@/i18n'

const props = defineProps({
  course: { type: Object, required: true },
  canEnroll: { type: Boolean, default: false },
  busy: { type: Boolean, default: false },
})

defineEmits(['enroll', 'drop'])

const color = computed(() => courseColor(props.course.name))
const ended = computed(() => props.course.status === '已结课')
const full = computed(() => props.course.seatsLeft != null && props.course.seatsLeft <= 0)
const fillPercent = computed(() => {
  const cap = Number(props.course.capacity) || 0
  if (!cap) return 0
  return Math.min(100, Math.round((Number(props.course.studentCount) / cap) * 100))
})
const seatText = computed(() => {
  if (props.course.seatsLeft == null) return t('square.noLimit')
  if (full.value) return t('square.full')
  return t('square.seatsLeft', { n: props.course.seatsLeft })
})

const openCourse = () => uni.navigateTo({ url: `/pages-course/space/space?id=${props.course.id}` })
</script>

<style lang="scss" scoped>
.sq {
  display: flex;
  flex-direction: column;
  border: 1rpx solid var(--xm-card-border);
  border-radius: 24rpx;
  background: var(--xm-bg-card);
  box-shadow: var(--xm-shadow);
  overflow: hidden;
}

.sq-cover {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
  padding: 24rpx 28rpx 26rpx;
  color: #ffffff;
  background-image:
    radial-gradient(circle at 92% 8%, rgba(255, 255, 255, 0.18) 0 80rpx, transparent 82rpx),
    repeating-linear-gradient(90deg, rgba(255, 255, 255, 0.06) 0 1rpx, transparent 1rpx 52rpx);
}

.sq.is-ended .sq-cover {
  filter: saturate(0.45);
}

.sq-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
}

.sq-tag {
  padding: 2rpx 14rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.2);
  font-size: 22rpx;
}

.sq-name {
  margin-top: 18rpx;
  font-size: 32rpx;
  font-weight: bold;
  line-height: 1.3;
}

.sq-time {
  font-size: 24rpx;
  opacity: 0.9;
}

.sq-body {
  padding: 20rpx 28rpx 24rpx;
}

.sq-teacher {
  font-size: 26rpx;
  color: var(--xm-text);
}

.sq-intro {
  margin-top: 8rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: var(--xm-text-2);
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.sq-reason {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: var(--xm-brand);
}

.sq-seats {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 16rpx;
}

.sq-bar {
  flex: 1;
  height: 10rpx;
  border-radius: 999rpx;
  background: var(--xm-bg-sunken);
  overflow: hidden;
}

.sq-bar-fill {
  height: 100%;
  background: var(--xm-brand);
}

.sq-bar-fill.is-full {
  background: var(--xm-danger);
}

.sq-seat-text {
  flex-shrink: 0;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.sq-foot {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 18rpx;
}

.sq-conflict {
  max-width: 380rpx;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sq-spacer {
  flex: 1;
}
</style>

<template>
  <!-- 课程卡片（与 Web 端 components/CourseCard.vue 同一设计）：课程色封面 + 类型 / 状态标签 + 课名 + 上课时间，
       底栏默认是教师与学分，教师端用 foot 插槽换成人数、待批作业 -->
  <view
    class="course-card"
    :class="{ 'is-finished': finished, 'is-compact': compact }"
    @click="open"
  >
    <view
      class="course-card-cover"
      :style="coverStyle"
    >
      <view class="course-card-tags">
        <text class="course-card-tag">{{ courseTypeLabel(course.type) }}</text>
        <text
          v-if="statusLabel"
          class="course-card-tag course-card-tag-status"
          >{{ statusLabel }}</text
        >
      </view>
      <view class="course-card-name">{{ course.name }}</view>
      <view class="course-card-time">{{ courseWhen(course) }}</view>
    </view>
    <view class="course-card-foot">
      <slot name="foot">
        <text class="xm-ellipsis">{{ course.teacherName || '—' }}</text>
        <text
          v-if="course.credit != null"
          class="xm-num"
          >{{ $t('courses.credits', { n: course.credit }) }}</text
        >
      </slot>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { courseColor } from '@/utils/courseColor'
import { courseTypeLabel, courseWhen } from '@/utils/courseText'
import { t } from '@/i18n'

const props = defineProps({
  course: { type: Object, required: true },
  /** 点击后打开的页面；不传则只触发 tap 事件 */
  url: { type: String, default: '' },
  /** 首页横向滑动列表里的窄卡片 */
  compact: { type: Boolean, default: false },
})

const emit = defineEmits(['tap'])

const finished = computed(() => props.course.status === '已结课')

/** 正在上的课不标状态（默认就是它），只标「未开课」「已结课」 */
const statusLabel = computed(() => {
  if (props.course.status === '未开课') return t('courses.status.notStarted')
  if (props.course.status === '已结课') return t('courses.status.finished')
  return ''
})

const coverStyle = computed(() => `background-color: ${courseColor(props.course.name)};`)

const open = () => {
  emit('tap', props.course)
  if (props.url) uni.navigateTo({ url: props.url })
}
</script>

<style lang="scss" scoped>
.course-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  border: 1rpx solid var(--xm-card-border);
  border-radius: 24rpx;
  background: var(--xm-bg-card);
  box-shadow: var(--xm-shadow);
  overflow: hidden;
  color: var(--xm-text);
}

.course-card:active {
  opacity: 0.9;
}

.course-card-cover {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
  min-height: 200rpx;
  padding: 24rpx 28rpx 26rpx;
  box-sizing: border-box;
  color: #ffffff;
  /* 右上角的大圆和细竖线：课表网格的意象 */
  background-image:
    radial-gradient(circle at 92% 8%, rgba(255, 255, 255, 0.18) 0 80rpx, transparent 82rpx),
    repeating-linear-gradient(90deg, rgba(255, 255, 255, 0.06) 0 1rpx, transparent 1rpx 52rpx);
}

.is-compact .course-card-cover {
  min-height: 180rpx;
}

.course-card.is-finished .course-card-cover {
  filter: saturate(0.45);
}

.course-card-tags {
  display: flex;
  gap: 10rpx;
  margin-bottom: auto;
}

.course-card-tag {
  padding: 2rpx 14rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.2);
  font-size: 22rpx;
}

.course-card-tag-status {
  background: rgba(0, 0, 0, 0.22);
}

.course-card-name {
  margin-top: 22rpx;
  font-size: 32rpx;
  font-weight: bold;
  line-height: 1.3;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.course-card-time {
  font-size: 24rpx;
  opacity: 0.9;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.course-card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 20rpx 28rpx;
  font-size: 24rpx;
  color: var(--xm-text-2);
}
</style>

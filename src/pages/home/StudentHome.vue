<template>
  <!-- 学生首页（与 Web 端 views/home/StudentHome.vue 同一组内容）：问候 + 学业数字 + 下一节课、预警、今天的课、
       我的课程、学业进度、待交作业、近期考试 -->
  <view>
    <home-hero
      :eyebrow="semesterLine"
      :title="$t('workbench.hello', { name: user.name || user.username, greeting })"
    >
      <text>
        {{ $t('workbench.stats.credits') }}
        <text class="stat xm-num">{{ data.stats.credits }}</text>
        <text
          v-if="data.stats.requiredCredits"
          class="xm-num"
        >
          / {{ data.stats.requiredCredits }}</text
        >
      </text>
      <text>
        {{ $t('workbench.stats.gpa') }}
        <text class="stat xm-num">{{ data.stats.gpa == null ? '—' : data.stats.gpa }}</text>
      </text>
      <text>
        {{ $t('workbench.stats.attendance') }}
        <text class="stat xm-num">{{ rateText }}</text>
      </text>
      <template #aside>
        <next-class :courses="data.todayCourses || []" />
      </template>
    </home-hero>

    <view
      v-if="data.warning"
      class="warning-bar"
    >
      <text
        class="xm-tag"
        :class="data.warning.level === '高风险' ? 'xm-tag-danger' : 'xm-tag-warning'"
        >{{ data.warning.level }}</text
      >
      <text class="warning-text"
        >{{ $t('workbench.warning', { index: data.warning.riskIndex }) }}{{ data.warning.suggestion }}</text
      >
    </view>

    <view class="xm-card">
      <view class="xm-section-head">
        <text class="xm-section-title">{{ $t('workbench.todayTitle') }}</text>
        <text class="xm-label">{{ weekLabel(data.semester.weekday, true) }}</text>
      </view>
      <today-classes :courses="data.todayCourses || []" />
    </view>

    <view class="xm-card">
      <view class="xm-section-head">
        <text class="xm-section-title">{{ $t('workbench.myCourses') }}</text>
        <text
          class="xm-link"
          @click="goCourses"
          >{{ $t('workbench.allCourses') }} ›</text
        >
      </view>
      <scroll-view
        v-if="activeCourses.length"
        scroll-x
        class="strip"
        :show-scrollbar="false"
      >
        <view
          v-for="c in activeCourses.slice(0, 6)"
          :key="c.id"
          class="strip-item"
        >
          <xm-course-card
            :course="c"
            :url="'/pages-course/space/space?id=' + c.id"
            compact
          />
        </view>
      </scroll-view>
      <view
        v-else
        class="empty-note"
      >
        {{ $t('workbench.noCourses') }}
        <text
          class="xm-link"
          @click="goSquare"
          >{{ $t('workbench.goSelect') }} ›</text
        >
      </view>
    </view>

    <view class="xm-card">
      <view class="xm-section-head">
        <text class="xm-section-title">{{ $t('workbench.progress') }}</text>
        <text
          class="xm-link"
          @click="goTranscript"
          >{{ $t('transcript.entry') }} ›</text
        >
      </view>
      <view class="progress">
        <xm-progress-ring
          :value="Number(data.stats.credits) || 0"
          :max="Number(data.stats.requiredCredits) || 0"
          :size="200"
          :stroke="18"
        >
          <text class="progress-value xm-num">{{ data.stats.credits }}</text>
          <text class="progress-unit">{{
            data.stats.requiredCredits
              ? $t('workbench.ofCredits', { n: data.stats.requiredCredits })
              : $t('workbench.creditsUnit')
          }}</text>
        </xm-progress-ring>
        <view class="progress-list">
          <view class="progress-item">
            <text class="xm-label">{{ $t('workbench.stats.gpa') }}</text>
            <text class="progress-num xm-num">{{ data.stats.gpa == null ? '—' : data.stats.gpa }}</text>
          </view>
          <view class="progress-item">
            <text class="xm-label">{{ $t('workbench.stats.attendance') }}</text>
            <text class="progress-num xm-num">{{ rateText }}</text>
          </view>
          <view class="progress-item">
            <text class="xm-label">{{ $t('workbench.stats.ongoing') }}</text>
            <text class="progress-num xm-num">{{ data.stats.ongoingCourses }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="xm-card">
      <view class="xm-section-head">
        <text class="xm-section-title">{{ $t('workbench.dueTitle') }}</text>
      </view>
      <view v-if="data.todos && data.todos.length">
        <view
          v-for="todo in data.todos"
          :key="todo.id"
          class="due"
          @click="openTodo(todo)"
        >
          <view
            class="due-bar"
            :style="'background:' + courseColor(todo.courseName)"
          />
          <view class="due-text">
            <view class="due-title">{{ todo.title }}</view>
            <view class="due-meta">
              {{ todo.courseName }} ·
              <text class="xm-num">{{ $t('workbench.dueAt', { time: (todo.deadline || '').slice(5) }) }}</text>
            </view>
          </view>
          <view class="xm-cell-arrow">
            <xm-icon
              name="chevron-right"
              :size="32"
            />
          </view>
        </view>
      </view>
      <view
        v-else
        class="empty-note"
        >{{ $t('workbench.noDue') }}</view
      >
    </view>

    <exams-card :exams="data.exams || []" />
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { courseColor } from '@/utils/courseColor'
import { weekLabel } from '@/utils/courseText'
import { useHomeText } from './useHomeText'
import HomeHero from './HomeHero.vue'
import NextClass from './NextClass.vue'
import TodayClasses from './TodayClasses.vue'
import ExamsCard from './ExamsCard.vue'

const props = defineProps({
  data: { type: Object, required: true },
})

const user = computed(() => useUserStore().user)
const { greeting, semesterLine } = useHomeText(() => props.data.semester)

/** 首页只放还在上的课（未开课、已开课），已结课的去「课程」页看 */
const activeCourses = computed(() => (props.data.courses || []).filter((c) => c.status !== '已结课'))

const rateText = computed(() => (props.data.stats.attendanceRate == null ? '—' : props.data.stats.attendanceRate + '%'))

const goCourses = () => uni.switchTab({ url: '/pages/courses/courses' })
const goSquare = () => uni.navigateTo({ url: '/pages-course/square/square' })
const goTranscript = () => uni.navigateTo({ url: '/pages-course/transcript/transcript' })
const openTodo = (todo) =>
  uni.navigateTo({ url: `/pages-course/space/space?id=${todo.courseId}&tab=assignments&open=${todo.id}` })
</script>

<style lang="scss" scoped>
.stat {
  margin-left: 6rpx;
  font-size: 30rpx;
  font-weight: bold;
  color: var(--xm-text);
}

.warning-bar {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  margin-bottom: 24rpx;
  padding: 20rpx 24rpx;
  border: 1rpx solid var(--xm-border);
  border-radius: 20rpx;
  background: var(--xm-bg-card);
  font-size: 26rpx;
  line-height: 1.6;
  color: var(--xm-text-2);
}

.warning-text {
  flex: 1;
  min-width: 0;
}

/* 横向滑动的课程卡片 */
.strip {
  white-space: nowrap;
  margin: 0 -28rpx;
  padding: 0 28rpx;
  width: calc(100% + 56rpx);
  box-sizing: border-box;
}

.strip-item {
  display: inline-block;
  width: 500rpx;
  margin-right: 20rpx;
  vertical-align: top;
  white-space: normal;
}

.strip-item:last-child {
  margin-right: 28rpx;
}

.progress {
  display: flex;
  align-items: center;
  gap: 36rpx;
}

.progress-value {
  font-size: 48rpx;
  font-weight: bold;
  line-height: 1;
  color: var(--xm-text);
}

.progress-unit {
  margin-top: 6rpx;
  font-size: 20rpx;
  color: var(--xm-text-2);
}

.progress-list {
  flex: 1;
  min-width: 0;
}

.progress-item {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12rpx;
  padding: 10rpx 0;
}

.progress-num {
  font-size: 34rpx;
  font-weight: 600;
  color: var(--xm-text);
}

.due {
  display: flex;
  align-items: stretch;
  gap: 18rpx;
  padding: 16rpx 0;
}

.due + .due {
  border-top: 1rpx solid var(--xm-border);
}

.due-bar {
  width: 8rpx;
  border-radius: 4rpx;
  flex-shrink: 0;
}

.due-text {
  flex: 1;
  min-width: 0;
}

.due-title {
  font-size: 28rpx;
  color: var(--xm-text);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.due-meta {
  margin-top: 4rpx;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.due .xm-cell-arrow {
  align-self: center;
}

.empty-note {
  padding: 16rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: var(--xm-text-2);
}
</style>

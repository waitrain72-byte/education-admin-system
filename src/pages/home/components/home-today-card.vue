<template>
  <!-- 今日课程（学生=选课课表今日列；教师=本人授课课程取今天星期）：严格按方案 A 演示稿样式 -->
  <view class="xm-card">
    <view class="xm-card-title">
      {{ $t('home.todayTitle') }}
      <text class="today-date">{{ todayTitleText }}</text>
    </view>
    <xm-empty
      v-if="!courses.length"
      icon="🎉"
      :text="$t('home.noClassToday')"
    />
    <template v-else>
      <!-- 收起状态：一行摘要，点击展开完整时间轴 -->
      <view
        v-if="!expanded"
        class="today-summary"
        @click="expanded = true"
      >
        <view class="summary-main">
          <view class="summary-line1">
            {{ todayCountText }}<template v-if="doingCourse"> · {{ doingCourse.name }} {{ statusText.doing }}</template>
          </view>
          <view
            v-if="nextCourse"
            class="summary-line2"
          >
            {{ $t('home.nextClass') }} {{ nextCourse.start }} {{ nextCourse.name
            }}<template v-if="nextCourse.sub"> · {{ nextCourse.sub }}</template>
          </view>
        </view>
        <text class="summary-arrow">{{ $t('home.expand') }} ▾</text>
      </view>
      <!-- 展开：完整时间轴（与方案 A 演示稿一致） -->
      <template v-else>
        <view
          v-for="c in courses"
          :key="c.key"
          class="today-item"
        >
          <view class="today-time">
            <text class="today-start">{{ c.start || '--:--' }}</text>
            <text class="today-end">{{ c.end }}</text>
          </view>
          <view class="today-rail">
            <view
              class="today-dot"
              :class="c.status ? 'is-' + c.status : ''"
            ></view>
          </view>
          <view
            class="today-block"
            :class="{ 'is-done': c.status === 'done' }"
            :style="courseBlockStyle(c.name)"
          >
            <view class="today-name">
              <text class="today-course">{{ c.name }}</text>
              <!-- 严格按演示稿：进行中=绿色胶囊，已结束=弱化文字，未开始不挂标签 -->
              <text
                v-if="c.status === 'doing'"
                class="today-status status-doing"
                >{{ statusText.doing }}</text
              >
              <text
                v-else-if="c.status === 'done'"
                class="today-status status-done"
                >{{ statusText.done }}</text
              >
            </view>
            <text
              v-if="c.sub"
              class="today-sub"
              >{{ c.sub }}</text
            >
          </view>
        </view>
        <view
          class="today-collapse"
          @click="expanded = false"
        >
          <text class="summary-arrow">{{ $t('home.collapse') }} ▴</text>
        </view>
      </template>
    </template>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { t, isZh } from '@/i18n'
import { courseBlockStyle } from '@/utils/courseColor'

/** 今日课程卡：courses 已由 decorateToday 补齐 start/end/status 并按开始时间排好序 */
const props = defineProps({
  courses: { type: Array, default: () => [] },
})

// 默认展开（严格按演示稿），可收起为一行摘要条
const expanded = ref(true)

const statusText = computed(() => ({
  doing: t('home.classDoing'),
  todo: t('home.classTodo'),
  done: t('home.classDone'),
}))

/** 标题右侧日期：中文「9月18日 周四」/ 英文「9/18 Thu」 */
const todayTitleText = computed(() => {
  const d = new Date()
  if (isZh()) {
    return `${d.getMonth() + 1}月${d.getDate()}日 周${'日一二三四五六'[d.getDay()]}`
  }
  return `${d.getMonth() + 1}/${d.getDate()} ${['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'][d.getDay()]}`
})
const todayCountText = computed(() => t('home.todayCount', { n: props.courses.length }))
/** 当前正在上的课（进行中） */
const doingCourse = computed(() => props.courses.find((c) => c.status === 'doing') || null)
/** 下一节未开始的课（已按开始时间排序，取第一个未开始） */
const nextCourse = computed(() => props.courses.find((c) => c.status === 'todo') || null)
</script>

<style lang="scss" scoped>
/* ===== 今日课程：时间轴彩色块 ===== */

/* 标题右侧日期（与演示稿一致：9月18日 周四） */
.today-date {
  font-weight: normal;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

/* 今日课程摘要条（收起态，点击展开完整时间轴） */
.today-summary {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.summary-main {
  flex: 1;
  min-width: 0;
}

.summary-line1 {
  font-size: 26rpx;
  color: var(--xm-text);
}

.summary-line2 {
  font-size: 22rpx;
  color: var(--xm-text-2);
  margin-top: 4rpx;
}

.summary-arrow {
  font-size: 22rpx;
  color: var(--xm-text-2);
  flex-shrink: 0;
}

/* 展开态底部的「收起」行 */
.today-collapse {
  display: flex;
  justify-content: flex-end;
  padding-top: 8rpx;
}

.today-item {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.today-item:last-child {
  margin-bottom: 4rpx;
}

.today-time {
  width: 96rpx;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-end;
  gap: 2rpx;
}

.today-start {
  font-size: 26rpx;
  font-weight: bold;
  color: var(--xm-text);
}

.today-end {
  font-size: 20rpx;
  color: var(--xm-text-2);
}

.today-rail {
  width: 24rpx;
  flex-shrink: 0;
  display: flex;
  justify-content: center;
}

.today-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: var(--xm-brand);
  margin-top: 22rpx;
}

.today-dot.is-done {
  background: var(--xm-border);
}

.today-dot.is-doing {
  background: var(--xm-success);
  animation: today-pulse 2s infinite;
}

@keyframes today-pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(34, 184, 102, 0.3);
  }

  70% {
    box-shadow: 0 0 0 12rpx rgba(34, 184, 102, 0);
  }

  100% {
    box-shadow: 0 0 0 0 rgba(34, 184, 102, 0);
  }
}

.today-block {
  flex: 1;
  border-radius: 16rpx;
  padding: 18rpx 22rpx;
  border: 1rpx solid transparent;
  /* 单节课（只有名称一行）也保持与演示稿相近的块高度 */
  min-height: 92rpx;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.today-block.is-done {
  opacity: 0.55;
}

.today-name {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.today-course {
  font-size: 28rpx;
  font-weight: bold;
}

.today-sub {
  display: block;
  font-size: 22rpx;
  margin-top: 6rpx;
  opacity: 0.85;
}

.today-status {
  font-size: 20rpx;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
  font-weight: normal;
  flex-shrink: 0;
}

.status-doing {
  background: var(--xm-success);
  color: #ffffff;
}

.status-done {
  color: var(--xm-text-2);
}
</style>

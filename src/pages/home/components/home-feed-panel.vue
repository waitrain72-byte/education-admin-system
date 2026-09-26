<template>
  <view>
    <!-- 为你推荐（协同过滤课程推荐，仅学生角色；点击进入选课页） -->
    <view
      v-if="recommends.length"
      class="xm-card"
    >
      <view class="xm-card-title">{{ $t('home.recommend') }}</view>
      <view
        v-for="c in recommends"
        :key="c.id"
        class="notice-item"
        @click="go('/pages/choice/choice')"
      >
        <view class="xm-between">
          <view class="xm-value">{{ c.name }}</view>
          <view class="xm-label">{{ c.teacherName }}</view>
        </view>
        <view class="recommend-reason">{{ c.reason }}</view>
      </view>
    </view>

    <!-- 教务通知 / 考试安排（首页仅展示最新 3 条，避免数据多时页面过长） -->
    <view class="xm-card">
      <view class="xm-card-title">{{ $t('home.notice') }}</view>
      <xm-empty
        v-if="!notices.length"
        icon="megaphone"
      />
      <view
        v-for="item in noticeList"
        :key="item.id"
        class="notice-item"
      >
        <view class="xm-value">{{ item.title }}</view>
        <view class="xm-label">{{ item.time }}</view>
      </view>
      <view
        v-if="notices.length"
        class="xm-label card-more"
        @click="go('/pages/notice/notice')"
        >{{ $t('home.viewAll') }}</view
      >
    </view>

    <view class="xm-card">
      <view class="xm-card-title">{{ $t('home.examplan') }}</view>
      <xm-empty
        v-if="!examplans.length"
        icon="clipboard"
      />
      <!-- 即将开考的排最前（由近到远），已结束的其次；显示考试时间与倒计时，历史数据没有考试时间时显示发布时间 -->
      <view
        v-for="item in examplanList"
        :key="item.id"
        class="notice-item"
      >
        <view class="xm-between">
          <view class="xm-value xm-ellipsis exam-name">{{ item.name }}</view>
          <text
            v-if="item.countdown"
            class="xm-tag exam-countdown"
            :class="item.countdown.cls"
            >{{ item.countdown.text }}</text
          >
        </view>
        <view
          class="xm-label"
          v-if="item.examTime || item.time"
          >{{ item.examTime || item.time }}</view
        >
      </view>
      <view
        v-if="examplans.length"
        class="xm-label card-more"
        @click="go('/pages/examplan/examplan')"
        >{{ $t('home.viewAll') }}</view
      >
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { sortByExamTime, countdownTag } from '@/utils/examCountdown'

/** 首页「动态」标签页：课程推荐 + 教务通知 + 考试安排 */
const props = defineProps({
  // 仅学生角色有数据，其它角色传空数组即不显示推荐卡
  recommends: { type: Array, default: () => [] },
  notices: { type: Array, default: () => [] },
  examplans: { type: Array, default: () => [] },
})

// 首页仅展示最新 3 条，完整列表在对应页面分页浏览
const HOME_LIST_LIMIT = 3
const noticeList = computed(() => props.notices.slice(0, HOME_LIST_LIMIT))
// 考试安排按离开考的远近排序后取前几条，并算好倒计时（首页每次显示都会重新拉数据，基准时间随之更新）
const examplanList = computed(() => {
  const now = new Date()
  return sortByExamTime(props.examplans, now)
    .slice(0, HOME_LIST_LIMIT)
    .map((row) => ({ ...row, countdown: countdownTag(row.examTime, now) }))
})

const go = (path) => uni.navigateTo({ url: path })
</script>

<style lang="scss" scoped>
.notice-item {
  padding: 16rpx 0;
  border-bottom: 1rpx solid var(--xm-border);
}

.notice-item:last-child {
  border-bottom: none;
}

/* 考试名过长时省略，右侧倒计时标签不被挤压 */
.exam-name {
  flex: 1;
  min-width: 0;
}

.exam-countdown {
  flex-shrink: 0;
  margin-left: 12rpx;
}

/* 推荐理由：品牌色弱化小字 */
.recommend-reason {
  font-size: 24rpx;
  color: var(--xm-brand);
  margin-top: 4rpx;
}

/* 「查看全部」入口：右对齐，主题色 */
.card-more {
  text-align: right;
  padding-top: 12rpx;
  color: var(--xm-brand);
}
</style>

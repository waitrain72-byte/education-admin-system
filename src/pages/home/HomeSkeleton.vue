<template>
  <!-- 首页骨架（与 Web 端 views/home/HomeSkeleton.vue 同一思路）：按身份画出三种首页各自的版式，
       卡片标题、「全部课程」这类入口照常显示，只有数据位置是占位块 -->
  <view class="sk-region">
    <!-- 管理员页头没有数字一行，下面是快捷入口（分两个写：小程序里按条件有无的插槽不可靠） -->
    <home-hero v-if="role === 'ADMIN'">
      <template #eyebrow>
        <view
          class="sk sk-s sk-on-brand hs-eyebrow"
          style="width: 62%"
        />
      </template>
      <template #title>
        <view
          class="sk sk-xl sk-on-brand hs-title"
          style="width: 46%"
        />
      </template>
      <template #aside>
        <view class="hs-quick">
          <view
            v-for="i in 3"
            :key="i"
            class="hs-quick-item"
          >
            <view class="sk hs-quick-icon" />
            <view
              class="sk sk-s"
              style="width: 70%"
            />
          </view>
        </view>
      </template>
    </home-hero>
    <home-hero v-else>
      <template #eyebrow>
        <view
          class="sk sk-s sk-on-brand hs-eyebrow"
          style="width: 62%"
        />
      </template>
      <template #title>
        <view
          class="sk sk-xl sk-on-brand hs-title"
          style="width: 46%"
        />
      </template>
      <view
        v-for="i in 3"
        :key="i"
        class="sk sk-on-brand hs-stat"
      />
      <template #aside>
        <view class="hs-next">
          <view
            class="sk sk-s"
            style="width: 40%"
          />
          <view
            class="sk sk-h"
            style="width: 55%"
          />
          <view
            class="sk sk-s"
            style="width: 35%"
          />
        </view>
      </template>
    </home-hero>

    <!-- 学生、教师 -->
    <template v-if="role !== 'ADMIN'">
      <view class="xm-card">
        <view class="xm-section-head">
          <text class="xm-section-title">{{ $t('workbench.todayTitle') }}</text>
          <view class="sk sk-s hs-hint" />
        </view>
        <view
          v-for="i in 2"
          :key="i"
          class="hs-today"
        >
          <view class="sk-stack hs-today-time">
            <view class="sk" />
            <view
              class="sk sk-s"
              style="width: 80%"
            />
          </view>
          <view class="sk sk-bar" />
          <view class="sk-stack">
            <view
              class="sk"
              :style="{ width: i === 1 ? '62%' : '74%' }"
            />
            <view
              class="sk sk-s"
              :style="{ width: i === 1 ? '40%' : '32%' }"
            />
          </view>
          <view class="sk sk-tag" />
        </view>
      </view>

      <view class="xm-card">
        <view class="xm-section-head">
          <text class="xm-section-title">{{
            $t(role === 'TEACHER' ? 'workbench.teachingCourses' : 'workbench.myCourses')
          }}</text>
          <text class="xm-link">{{ $t('workbench.allCourses') }} ›</text>
        </view>
        <view class="hs-strip">
          <view
            v-for="i in 2"
            :key="i"
            class="hs-strip-item"
          >
            <xm-course-card-skeleton
              :index="i"
              compact
            />
          </view>
        </view>
      </view>

      <template v-if="role === 'STUDENT'">
        <view class="xm-card">
          <view class="xm-section-head">
            <text class="xm-section-title">{{ $t('workbench.progress') }}</text>
            <text class="xm-link">{{ $t('transcript.entry') }} ›</text>
          </view>
          <view class="hs-progress">
            <view class="sk hs-ring" />
            <view class="hs-progress-list">
              <view
                v-for="key in ['gpa', 'attendance', 'ongoing']"
                :key="key"
                class="hs-progress-item"
              >
                <text class="xm-label">{{ $t('workbench.stats.' + key) }}</text>
                <view class="sk sk-h hs-progress-num" />
              </view>
            </view>
          </view>
        </view>
        <view class="xm-card">
          <view class="xm-section-head">
            <text class="xm-section-title">{{ $t('workbench.dueTitle') }}</text>
          </view>
          <xm-skeleton-list
            :rows="2"
            lead="bar"
            divided
          />
        </view>
      </template>

      <template v-else>
        <view class="xm-card">
          <view class="xm-section-head">
            <text class="xm-section-title">{{ $t('workbench.todos') }}</text>
          </view>
          <xm-skeleton-list
            :rows="2"
            :lines="1"
            trail="text"
          />
        </view>
        <view class="xm-card">
          <view class="xm-section-head">
            <text class="xm-section-title">{{ $t('workbench.attention.title') }}</text>
          </view>
          <xm-skeleton-list
            :rows="2"
            lead="avatar"
            trail="btn"
          />
        </view>
      </template>
    </template>

    <!-- 管理员 -->
    <template v-else>
      <view class="hs-kpis">
        <view
          v-for="i in 7"
          :key="i"
          class="hs-kpi"
        >
          <view
            class="sk sk-s"
            style="width: 50%"
          />
          <view
            class="sk sk-num"
            :style="{ width: i % 3 === 0 ? '28%' : '40%' }"
          />
        </view>
      </view>
      <view class="xm-card">
        <view class="xm-section-head">
          <text class="xm-section-title">{{ $t('workbench.admin.pendingLeaves') }}</text>
          <text class="xm-link">{{ $t('workbench.admin.goReview') }} ›</text>
        </view>
        <xm-skeleton-list
          :rows="3"
          lead="avatar"
          divided
        />
      </view>
      <view class="xm-card">
        <view class="xm-section-head">
          <text class="xm-section-title">{{ $t('workbench.admin.unscheduled') }}</text>
          <text class="xm-link">{{ $t('workbench.admin.goSchedule') }} ›</text>
        </view>
        <xm-skeleton-list
          :rows="2"
          lead="bar"
          divided
        />
      </view>
    </template>

    <view class="xm-card">
      <view class="xm-section-head">
        <text class="xm-section-title">{{ $t('workbench.exams') }}</text>
        <text
          v-if="role === 'ADMIN'"
          class="xm-link"
          >{{ $t('home.viewAll') }} ›</text
        >
      </view>
      <xm-skeleton-list
        :rows="2"
        lead="date"
        trail="tag"
        divided
      />
    </view>

    <view
      v-if="role === 'ADMIN'"
      class="xm-card"
    >
      <view class="xm-section-head">
        <text class="xm-section-title">{{ $t('workbench.admin.notices') }}</text>
        <text class="xm-link">{{ $t('home.viewAll') }} ›</text>
      </view>
      <xm-skeleton-list
        :rows="3"
        :lines="1"
        trail="text"
        divided
      />
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import HomeHero from './HomeHero.vue'

const userStore = useUserStore()
const role = computed(() => userStore.role)
</script>

<style lang="scss" scoped>
/* 占位块高度取字号，上下补回行高差，骨架换成内容时页头不跳 */
.hs-eyebrow {
  margin: 7rpx 0;
}

.hs-title {
  margin: 6rpx 0;
}

.hs-stat {
  width: 150rpx;
  height: 30rpx;
  margin: 4rpx 0;
}

/* 与 NextClass 同尺寸：左侧一条竖条 */
.hs-next {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 14rpx;
  padding: 24rpx 24rpx 24rpx 36rpx;
  border-radius: 24rpx;
  background: var(--xm-bg-card);
  overflow: hidden;
}

.hs-next::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 10rpx;
  background: var(--xm-sk-base);
}

/* 与管理员首页的快捷入口同尺寸 */
.hs-quick {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
}

.hs-quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14rpx;
  padding: 20rpx 8rpx;
  border: 1rpx solid var(--xm-border);
  border-radius: 20rpx;
  background: var(--xm-bg-card);
}

.hs-quick-icon {
  width: 40rpx;
  height: 40rpx;
}

.hs-kpis {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.hs-kpi {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  padding: 22rpx 24rpx;
  border: 1rpx solid var(--xm-card-border);
  border-radius: 20rpx;
  background: var(--xm-bg-card);
}

.hs-hint {
  width: 80rpx;
}

/* 与 TodayClasses 的行同尺寸：时间列、课程色条、课名与教室、状态标签 */
.hs-today {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 20rpx 0;
}

.hs-today-time {
  flex: none;
  width: 84rpx;
}

/* 与首页横向滑动的课程卡片同宽 */
.hs-strip {
  display: flex;
  gap: 20rpx;
  margin-right: -28rpx;
  overflow: hidden;
}

.hs-strip-item {
  flex: none;
  width: 500rpx;
}

.hs-progress {
  display: flex;
  align-items: center;
  gap: 36rpx;
}

/* 学分环：200rpx 的圆挖空成环（环宽 18rpx，与 xm-progress-ring 一致） */
.hs-ring {
  width: 200rpx;
  height: 200rpx;
  border-radius: 50%;
  -webkit-mask: radial-gradient(circle, transparent 81rpx, #000 82rpx);
  mask: radial-gradient(circle, transparent 81rpx, #000 82rpx);
}

.hs-progress-list {
  flex: 1;
  min-width: 0;
}

.hs-progress-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  padding: 10rpx 0;
}

.hs-progress-num {
  width: 80rpx;
}
</style>

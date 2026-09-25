<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 首次加载失败（断网/超时）：给出重新加载入口，而不是留白 -->
    <xm-empty
      v-if="loadFailed"
      :action-text="$t('common.reload')"
      @action="load()"
    />
    <xm-empty
      v-else-if="loaded && !hasAnyClass"
      icon="calendar"
      :text="$t('pages.curriculum.empty')"
      :action-text="$t('pages.curriculum.goChoose')"
      @action="goChoose"
    />

    <template v-else-if="loaded">
      <!-- 周视图：一屏放下整周（对齐 ColorTimetable / 常见课表 App），周末无课时收起周末两列 -->
      <view class="xm-card grid-card">
        <view class="grid-head">
          <view class="head-time"></view>
          <view
            v-for="d in days"
            :key="d.field"
            class="head-day"
            :class="{ today: d.field === todayField }"
            >{{ d.short }}</view
          >
        </view>

        <view class="grid-body">
          <view
            v-for="(row, i) in grid"
            :key="row.segment"
            class="grid-row"
            :style="{ height: rpx(ROW_HEIGHT) }"
          >
            <view
              class="time-cell"
              :class="{ now: nowRow === i }"
            >
              <text class="time-no">{{ i + 1 }}</text>
              <text class="time-range">{{ row.start }}</text>
              <text class="time-range">{{ row.end }}</text>
            </view>
            <view
              v-for="d in days"
              :key="d.field"
              class="day-cell"
              :class="{ today: d.field === todayField }"
            >
              <view
                v-if="row.cells[d.field].length"
                class="course-block"
                :class="{ doing: d.field === todayField && nowRow === i }"
                :style="courseBlockStyle(row.cells[d.field][0].name)"
                @click="openDetail(row, d)"
              >
                <text class="block-name">{{ row.cells[d.field][0].name }}</text>
                <text
                  v-if="row.cells[d.field][0].room"
                  class="block-room"
                  >@{{ row.cells[d.field][0].room }}</text
                >
                <text
                  v-if="row.cells[d.field].length > 1"
                  class="block-more"
                  >+{{ row.cells[d.field].length - 1 }}</text
                >
              </view>
            </view>
          </view>

          <!-- 当前时间线：按大节起止时间折算到行内位置，每分钟刷新 -->
          <view
            v-if="nowPos >= 0"
            class="now-line"
            :style="{ top: rpx(nowPos * ROW_HEIGHT) }"
          ></view>
        </view>
      </view>

      <view class="xm-between grid-tip">
        <text class="xm-label">{{ $t('pages.curriculum.tapTip') }}</text>
        <text
          v-if="!weekendHasClass"
          class="weekend-toggle"
          @click="showWeekend = !showWeekend"
          >{{ showWeekend ? $t('pages.curriculum.hideWeekend') : $t('pages.curriculum.showWeekend') }}</text
        >
      </view>
    </template>

    <!-- 课程详情（底部弹层）：教师 / 教室 / 时间 / 类型 / 学分 / 状态 -->
    <view
      v-if="detail"
      class="xm-mask"
      @click="detail = null"
      @touchmove.stop.prevent="noop"
    ></view>
    <view
      v-if="detail"
      class="xm-popup"
    >
      <view class="xm-popup-title">{{ detail.dayLabel }} · {{ detail.segmentLabel }}</view>
      <view
        v-if="detail.courses.length > 1"
        class="xm-label slot-count"
        >{{ $t('pages.curriculum.slotCount', { n: detail.courses.length }) }}</view
      >
      <view
        v-for="c in detail.courses"
        :key="c.id"
        class="detail-item"
      >
        <view class="xm-between">
          <text
            class="detail-name"
            :style="{ color: courseColor(c.name) }"
            >{{ c.name }}</text
          >
          <view class="xm-row detail-tags">
            <text
              v-if="c.type"
              class="xm-tag xm-tag-brand"
              >{{ enumLabel('courseType', c.type) }}</text
            >
            <text
              v-if="c.status"
              class="xm-tag"
              :class="enumTag('courseStatus', c.status)"
              >{{ enumLabel('courseStatus', c.status) }}</text
            >
          </view>
        </view>
        <view class="detail-line">
          <text class="xm-label">{{ $t('pages.course.teacher') }}</text>
          <text class="xm-value">{{ c.teacherName || '-' }}</text>
        </view>
        <view class="detail-line">
          <text class="xm-label">{{ $t('pages.course.room') }}</text>
          <text class="xm-value">{{ c.room || '-' }}</text>
        </view>
        <view class="detail-line">
          <text class="xm-label">{{ $t('pages.course.score') }}</text>
          <text class="xm-value">{{ c.score != null ? c.score : '-' }}</text>
        </view>
      </view>
      <button
        class="xm-btn xm-btn-primary xm-btn-block"
        @click="detail = null"
      >
        {{ $t('pages.curriculum.gotIt') }}
      </button>
    </view>
    <xm-loader />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onHide, onUnload, onPullDownRefresh } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { ensureLoggedIn } from '@/utils/authGuard'
import { SILENT } from '@/utils/request'
import { choiceApi } from '@/api'
import { readUserCache, writeUserCache } from '@/utils/userCache'
import { courseBlockStyle, courseColor } from '@/utils/courseColor'
import {
  WEEK_COLUMNS,
  WEEKDAY_FIELDS,
  buildWeekGrid,
  classStatus,
  hasWeekendClass,
  nowRowPosition,
} from '@/utils/todaySchedule'
import { t } from '@/i18n'
import { rpx } from '@/composables/useScreen'
import { enumLabel, enumTag } from '@/utils/enums'

/** 网格行高（rpx）：固定行高才能把「当前时间」按比例换算成时间线的纵向位置 */
const ROW_HEIGHT = 168

const userStore = useUserStore()
const choices = ref([])
const loaded = ref(false)
const loadFailed = ref(false)
const showWeekend = ref(false)
const detail = ref(null)

const grid = computed(() => buildWeekGrid(choices.value))
const weekendHasClass = computed(() => hasWeekendClass(grid.value))
// 以网格为准判断是否有课：选了但还没排时间（无星期/大节）的课画不进课表，不能让整张空表代替空态
const hasAnyClass = computed(() => grid.value.some((r) => WEEK_COLUMNS.some((f) => r.cells[f].length)))
const days = computed(() =>
  WEEK_COLUMNS.filter((f) => weekendHasClass.value || showWeekend.value || !['saturday', 'sunday'].includes(f)).map(
    (field) => ({ field, short: t(`pages.curriculum.${field}Short`), full: t(`pages.curriculum.${field}`) }),
  ),
)

// 当前时间：每分钟刷新一次，驱动今日列高亮、进行中课程描边与时间线位置
const now = ref(new Date())
let timer = null
const todayField = computed(() => WEEKDAY_FIELDS[now.value.getDay()])
const nowPos = computed(() => nowRowPosition(grid.value, now.value))
// 正在上课的大节（课间为 -1）：时间列序号变主色，今日该节课程块描边
const nowRow = computed(() => grid.value.findIndex((r) => classStatus(r.segment, now.value) === 'doing'))

const openDetail = (row, day) => {
  detail.value = {
    dayLabel: day.full,
    segmentLabel: enumLabel('segment', row.segment),
    courses: row.cells[day.field],
  }
}

const goChoose = () => uni.navigateTo({ url: '/pages/course/course' })
// 遮罩拦截 touchmove：详情打开时背后的课表不跟着滚动
const noop = () => {}

// 本地缓存（按账号隔离）：进入页面先画缓存的课表（秒开，弱网 / 断网也能看），再静默刷新写回
const CACHE_NAME = 'curriculum'
const applyCache = () => {
  const cached = readUserCache(CACHE_NAME, userStore.accountKey)
  if (!Array.isArray(cached)) return false
  choices.value = cached
  loaded.value = true
  return true
}

// 选课记录已联表带出星期/大节/教室/教师/类型/学分，一次请求即可画出整周课表与详情
const load = (silent = false) =>
  choiceApi
    .selectAll({ studentId: userStore.user.id }, silent ? SILENT : undefined)
    .then((rows) => {
      choices.value = rows || []
      loaded.value = true
      loadFailed.value = false
      writeUserCache(CACHE_NAME, userStore.accountKey, choices.value)
    })
    .catch(() => {
      // 提示已由请求层统一弹出；已有数据时保留旧课表，只有从未加载成功才显示重试
      if (!loaded.value) loadFailed.value = true
    })

onShow(() => {
  uni.setNavigationBarTitle({ title: t('menu.curriculum') })
  if (!ensureLoggedIn()) return
  // 仅学生可访问（与 Web 端路由 meta.roles 一致）
  if (!['STUDENT'].includes(userStore.role)) {
    uni.showToast({ title: t('forbidden.message'), icon: 'none' })
    setTimeout(() => uni.navigateBack(), 800)
    return
  }
  // 有缓存先画缓存；首次进入且无缓存才走加载动画，其余情况静默刷新（网格固定，不存在滚动位置丢失的问题）
  if (!loaded.value) applyCache()
  load(loaded.value)
  now.value = new Date()
  clearInterval(timer)
  timer = setInterval(() => {
    now.value = new Date()
  }, 60 * 1000)
})

const stopTimer = () => {
  clearInterval(timer)
  timer = null
}
onHide(stopTimer)
onUnload(stopTimer)

onPullDownRefresh(() => {
  load(true).finally(() => uni.stopPullDownRefresh())
})
</script>

<style lang="scss" scoped>
.grid-card {
  padding: 12rpx 8rpx 8rpx;
}

.grid-head {
  display: flex;
  align-items: center;
  height: 64rpx;
}

.head-time,
.time-cell {
  width: 64rpx;
  flex-shrink: 0;
}

.head-day {
  flex: 1;
  min-width: 0;
  margin: 0 2rpx;
  padding: 8rpx 0;
  border-radius: 12rpx;
  text-align: center;
  font-size: 24rpx;
  color: var(--xm-text-2);
}

/* 今日：表头主色胶囊 + 整列软底 */
.head-day.today {
  color: #ffffff;
  background: var(--xm-brand);
  font-weight: bold;
}

.grid-body {
  position: relative;
}

.grid-row {
  display: flex;
}

.time-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-size: 18rpx;
  line-height: 1.4;
  color: var(--xm-text-2);
}

.time-no {
  font-size: 28rpx;
  font-weight: bold;
  color: var(--xm-text);
}

.time-cell.now .time-no {
  color: var(--xm-brand);
}

.day-cell {
  flex: 1;
  min-width: 0;
  height: 100%;
  padding: 4rpx;
  box-sizing: border-box;
}

.day-cell.today {
  background: var(--xm-brand-soft);
}

/* 课程块：填满格子，颜色由 courseBlockStyle 按课程名注入（与首页今日课程同色） */
.course-block {
  position: relative;
  height: 100%;
  box-sizing: border-box;
  padding: 8rpx 6rpx;
  border-radius: 12rpx;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 进行中：同色描边强调 */
.course-block.doing {
  box-shadow: inset 0 0 0 3rpx currentColor;
}

.block-name {
  font-size: 22rpx;
  font-weight: bold;
  line-height: 1.3;
  word-break: break-all;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
  overflow: hidden;
}

.block-room {
  margin-top: 4rpx;
  font-size: 18rpx;
  line-height: 1.3;
  opacity: 0.85;
  word-break: break-all;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

/* 同一时段多门课：右下角计数角标 */
.block-more {
  position: absolute;
  right: 4rpx;
  bottom: 4rpx;
  padding: 0 8rpx;
  border-radius: 999rpx;
  font-size: 18rpx;
  line-height: 1.6;
  color: #ffffff;
  background: rgba(0, 0, 0, 0.4);
}

/* 当前时间线：左端圆点落在时间列，横贯整周 */
.now-line {
  position: absolute;
  left: 56rpx;
  right: 0;
  height: 0;
  border-top: 3rpx solid var(--xm-danger);
  z-index: 2;
  pointer-events: none;
}

.now-line::before {
  content: '';
  position: absolute;
  left: -8rpx;
  top: -9rpx;
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: var(--xm-danger);
}

.grid-tip {
  padding: 0 8rpx;
}

.weekend-toggle {
  flex-shrink: 0;
  font-size: 24rpx;
  color: var(--xm-brand);
}

.slot-count {
  text-align: center;
  margin: -12rpx 0 16rpx;
}

.detail-item {
  padding: 20rpx 24rpx;
  margin-bottom: 20rpx;
  border-radius: 16rpx;
  background: var(--xm-bg-input);
}

.detail-name {
  flex: 1;
  min-width: 0;
  font-size: 30rpx;
  font-weight: bold;
}

.detail-tags {
  flex-shrink: 0;
  gap: 8rpx;
}

.detail-line {
  display: flex;
  justify-content: space-between;
  gap: 24rpx;
  margin-top: 12rpx;
}
</style>

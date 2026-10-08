<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 管理员没有日程，这个 tab 是教务后台入口（tab 文字、图标由 utils/tabbar 换成「后台」） -->
    <console-home v-if="isAdmin" />

    <!-- 日程（与 Web 端 views/schedule/SchedulePage.vue 一致）：周课表、月历；学生在月历上选日期请假 -->
    <template v-else>
      <view class="xm-seg view-seg">
        <view
          class="xm-seg-item"
          :class="{ on: view === 'week' }"
          @click="setView('week')"
          >{{ $t('schedule.week') }}</view
        >
        <view
          class="xm-seg-item"
          :class="{ on: view === 'month' }"
          @click="setView('month')"
          >{{ $t('schedule.month') }}</view
        >
      </view>

      <view class="nav">
        <view
          class="nav-btn"
          @click="shift(-1)"
        >
          <xm-icon
            name="chevron-left"
            :size="32"
          />
        </view>
        <view
          class="nav-today"
          @click="goToday"
          >{{ view === 'week' ? $t('schedule.thisWeek') : $t('schedule.thisMonth') }}</view
        >
        <view
          class="nav-btn"
          @click="shift(1)"
        >
          <xm-icon
            name="chevron-right"
            :size="32"
          />
        </view>
        <text class="nav-label xm-num">{{ periodLabel }}</text>
        <text
          v-if="view === 'week' && weekData && weekData.weekNo > 0"
          class="xm-tag xm-tag-brand"
          >{{ $t('schedule.weekNo', { n: weekData.weekNo }) }}</text
        >
      </view>

      <view
        v-if="loading && !current"
        class="xm-card"
      >
        <view class="skeleton skeleton-title" />
        <view class="skeleton skeleton-line" />
        <view class="skeleton skeleton-line" />
        <view class="skeleton skeleton-line short" />
      </view>

      <!-- ========== 周课表 ========== -->
      <template v-else-if="view === 'week' && weekData">
        <week-grid
          :data="weekData"
          :show-teacher="isStudent"
          @open-course="openCourse"
        />
        <view
          v-if="!weekData.courses || !weekData.courses.length"
          class="note"
          >{{ $t('schedule.noCourses') }}</view
        >
        <view class="xm-card events">
          <view class="xm-section-head">
            <text class="xm-section-title">{{ $t('schedule.events') }}</text>
          </view>
          <view v-if="weekItems.length">
            <view
              v-for="item in weekItems"
              :key="item.key"
              class="event"
              @click="openItem(item)"
            >
              <text class="event-day xm-num">{{ item.dayText }}</text>
              <text
                class="xm-tag"
                :class="item.tag"
                >{{ item.label }}</text
              >
              <view class="event-body">
                <view class="event-title">{{ item.title }}</view>
                <view
                  v-if="item.meta"
                  class="event-meta"
                  >{{ item.meta }}</view
                >
              </view>
            </view>
          </view>
          <view
            v-else
            class="note"
            >{{ $t('schedule.noEvents') }}</view
          >
        </view>
      </template>

      <!-- ========== 月历 ========== -->
      <template v-else-if="view === 'month' && monthData">
        <view
          v-if="isStudent"
          class="select-bar"
          :class="{ 'is-active': selection.from }"
        >
          <text class="select-text">{{ selectionText }}</text>
          <view
            v-if="selection.from"
            class="select-actions"
          >
            <button
              class="xm-btn xm-btn-plain xm-btn-sm"
              @click="clearSelection"
            >
              {{ $t('leave.clear') }}
            </button>
            <button
              class="xm-btn xm-btn-primary xm-btn-sm"
              @click="leaveOpen = true"
            >
              {{ $t('leave.request') }}
            </button>
          </view>
        </view>

        <month-grid
          :data="monthData"
          :selection="selection"
          :focus="focusDate"
          @pick="pick"
        />

        <!-- 点过的那一天：课程与事件明细（格子里只有圆点，靠这里看详情） -->
        <view
          v-if="focusDay"
          class="xm-card day"
        >
          <view class="xm-section-head">
            <text class="xm-section-title">{{ dayText(focusDay.date) }}</text>
          </view>
          <view
            v-if="!focusDay.classes.length && !focusEvents.length"
            class="note"
            >—</view
          >
          <view
            v-for="c in focusDay.classes"
            :key="'c' + c.courseId + c.segment"
            class="day-item"
            @click="openCourse(c.courseId)"
          >
            <view
              class="day-bar"
              :style="'background:' + courseColor(c.courseName)"
            />
            <text class="day-time xm-num">{{ c.start }}–{{ c.end }}</text>
            <text class="day-name">{{ c.courseName }}</text>
            <text class="day-meta">{{ c.room || '—' }}</text>
          </view>
          <view
            v-for="ev in focusEvents"
            :key="ev.type + ev.id"
            class="day-item"
            @click="openEvent(ev)"
          >
            <text
              class="xm-tag"
              :class="eventTag(ev)"
              >{{ eventLabel(ev) }}</text
            >
            <text class="day-time xm-num">{{ ev.time }}</text>
            <text class="day-name">{{ ev.title }}</text>
            <text class="day-meta">{{ ev.courseName || '' }}</text>
          </view>
        </view>

        <view
          v-if="isStudent"
          class="xm-card"
        >
          <view class="xm-section-head">
            <text class="xm-section-title">{{ $t('leave.mine') }}</text>
          </view>
          <view
            v-if="!myLeaves.length"
            class="note"
            >{{ $t('leave.mineEmpty') }}</view
          >
          <view
            v-for="l in myLeaves"
            :key="l.id"
            class="leave"
          >
            <view class="leave-head">
              <text class="xm-num">{{ dayText(l.time) }}</text>
              <text class="leave-days">{{ $t('leave.days', { n: l.day }) }}</text>
              <text
                class="xm-tag"
                :class="leaveTag(l.status)"
                >{{ $t('leave.status.' + leaveKey(l.status)) }}</text
              >
            </view>
            <view class="leave-reason">{{ l.content }}</view>
            <view
              v-if="l.descr"
              class="leave-note"
              >{{ $t('leave.reviewNote', { text: l.descr }) }}</view
            >
            <text
              v-if="l.status === '待审核'"
              class="leave-withdraw"
              @click="withdraw(l)"
              >{{ $t('leave.withdraw') }}</text
            >
          </view>
        </view>
      </template>

      <leave-sheet
        v-if="selection.from && selection.to"
        :visible="leaveOpen"
        :from="selection.from"
        :to="selection.to"
        @close="leaveOpen = false"
        @submitted="onSubmitted"
      />
    </template>
    <xm-loader />
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onHide, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { applyApi, scheduleApi } from '@/api'
import { SILENT } from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { ensureLoggedIn } from '@/utils/authGuard'
import { takeTabQuery } from '@/utils/link'
import { courseColor } from '@/utils/courseColor'
import { addDays, addMonths, daysBetween, formatDay, formatMonth } from '@/utils/schedule'
import { syncServerClock } from '@/composables/useServerClock'
import { confirm } from '@/utils/confirm'
import { isZh, t } from '@/i18n'
import ConsoleHome from './ConsoleHome.vue'
import WeekGrid from './WeekGrid.vue'
import MonthGrid from './MonthGrid.vue'
import LeaveSheet from './LeaveSheet.vue'

const PAGE_URL = '/pages/schedule/schedule'
/** 一次最多请 30 天（与后端一致） */
const MAX_LEAVE_DAYS = 30

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'ADMIN')
const isStudent = computed(() => userStore.role === 'STUDENT')

const view = ref('week')
/** 周视图锚定的日期（那一周里的任意一天），空 = 本周 */
const anchor = ref('')
/** 月视图的月份 yyyy-MM，空 = 本月 */
const month = ref('')

const weekData = ref(null)
const monthData = ref(null)
const loading = ref(false)
const current = computed(() => (view.value === 'week' ? weekData.value : monthData.value))

const dayText = (iso) => formatDay(iso, isZh(), true)

const periodLabel = computed(() => {
  if (view.value === 'week' && weekData.value) {
    const monday = weekData.value.monday
    return `${formatDay(monday, isZh())} – ${formatDay(addDays(monday, 6), isZh())}`
  }
  if (view.value === 'month' && monthData.value) return formatMonth(monthData.value.month, isZh())
  return ''
})

const load = async () => {
  if (isAdmin.value) return
  loading.value = true
  try {
    if (view.value === 'week') {
      const data = await scheduleApi.week(anchor.value ? { date: anchor.value } : undefined, SILENT)
      weekData.value = data
      anchor.value = data.monday
      syncServerClock(data.now)
    } else {
      const data = await scheduleApi.month(month.value ? { month: month.value } : undefined, SILENT)
      monthData.value = data
      month.value = data.month
      syncServerClock(data.now)
      if (isStudent.value) await loadMyLeaves()
    }
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    loading.value = false
  }
}

const setView = (next) => {
  if (view.value === next) return
  view.value = next
  load()
}

const shift = (n) => {
  if (view.value === 'week') {
    anchor.value = addDays(anchor.value || (weekData.value && weekData.value.monday), n * 7)
  } else {
    month.value = addMonths(month.value || (monthData.value && monthData.value.month), n)
  }
  load()
}

const goToday = () => {
  anchor.value = ''
  month.value = ''
  load()
}

// ---------- 周视图：本周的考试、作业截止、请假，按日期列出 ----------
const eventLabel = (ev) =>
  ev.type === 'exam' ? t('schedule.exam') : ev.done ? t('schedule.done') : t('schedule.deadline')
const eventTag = (ev) => (ev.type === 'exam' ? 'xm-tag-danger' : ev.done ? 'xm-tag-success' : 'xm-tag-warning')

const leaveKey = (status) => (status === '审核通过' ? 'approved' : status === '审核不通过' ? 'rejected' : 'pending')
const leaveTag = (status) =>
  status === '审核通过' ? 'xm-tag-success' : status === '审核不通过' ? 'xm-tag-danger' : 'xm-tag-warning'

const weekItems = computed(() => {
  if (!weekData.value) return []
  const events = (weekData.value.events || []).map((ev) => ({
    key: ev.type + ev.id,
    sort: ev.date + ' ' + (ev.time || ''),
    dayText: formatDay(ev.date, isZh(), true),
    label: eventLabel(ev),
    tag: eventTag(ev),
    title: ev.title,
    meta: [ev.time, ev.courseName].filter(Boolean).join(' · '),
    event: ev,
  }))
  const leaves = (weekData.value.leaves || [])
    .filter((l) => l.status !== '审核不通过')
    .map((l) => ({
      key: 'leave' + l.id,
      sort: l.from,
      dayText: formatDay(l.from, isZh(), true),
      label: t('schedule.leave'),
      tag: 'xm-tag-info',
      title: l.content,
      meta: `${t('leave.days', { n: l.days })} · ${t('leave.status.' + leaveKey(l.status))}`,
    }))
  return [...events, ...leaves].sort((a, b) => a.sort.localeCompare(b.sort))
})

const openCourse = (id) => uni.navigateTo({ url: `/pages-course/space/space?id=${id}` })

/** 作业截止 → 那门课的作业；考试只是提示，不跳转 */
const openEvent = (ev) => {
  if (ev.type === 'deadline' && ev.courseId) {
    uni.navigateTo({ url: `/pages-course/space/space?id=${ev.courseId}&tab=assignments&open=${ev.id}` })
  }
}
const openItem = (item) => {
  if (item.event) openEvent(item.event)
  else setView('month')
}

// ---------- 月视图：点一天看明细；学生点两下选请假区间 ----------
const selection = reactive({ from: null, to: null })
/** 已经点了开始日期、等着点结束日期 */
const picking = ref(false)
const leaveOpen = ref(false)
const myLeaves = ref([])
const focusDate = ref('')

const focusDay = computed(
  () => ((monthData.value && monthData.value.days) || []).find((d) => d.date === focusDate.value) || null,
)
const focusEvents = computed(() =>
  ((monthData.value && monthData.value.events) || []).filter((e) => e.date === focusDate.value),
)

const selectedDays = computed(() =>
  selection.from && selection.to ? daysBetween(selection.from, selection.to) + 1 : 0,
)

const selectionText = computed(() => {
  if (!selection.from) return t('leave.requestHint')
  if (selection.from === selection.to && picking.value) {
    return t('leave.selecting', { from: dayText(selection.from), n: 1 })
  }
  return t('leave.selected', { from: dayText(selection.from), to: dayText(selection.to), n: selectedDays.value })
})

const pick = (date) => {
  focusDate.value = date
  // 老师只看明细；学生点日期同时是在选请假区间
  if (!isStudent.value) return
  if (!selection.from || !picking.value) {
    selection.from = date
    selection.to = date
    picking.value = true
    return
  }
  // 第二下：结束日期（点在开始日期前面就把两头换一下）
  const [from, to] = date < selection.from ? [date, selection.from] : [selection.from, date]
  if (daysBetween(from, to) + 1 > MAX_LEAVE_DAYS) {
    uni.showToast({ title: t('errors.5028'), icon: 'none' })
    return
  }
  selection.from = from
  selection.to = to
  picking.value = false
}

const clearSelection = () => {
  selection.from = null
  selection.to = null
  picking.value = false
}

const loadMyLeaves = async () => {
  try {
    const list = (await applyApi.selectAll(undefined, SILENT)) || []
    myLeaves.value = list.sort((a, b) => String(b.time).localeCompare(String(a.time)) || b.id - a.id)
  } catch {
    myLeaves.value = []
  }
}

const onSubmitted = async () => {
  leaveOpen.value = false
  clearSelection()
  await load()
}

const withdraw = async (leave) => {
  if (!(await confirm(t('leave.withdrawConfirm'), { title: t('leave.withdraw') }))) return
  try {
    await applyApi.delete(leave.id)
    uni.showToast({ title: t('leave.withdrawn'), icon: 'none' })
    await load()
  } catch {
    // 提示已由请求层统一弹出
  }
}

// ---------- 生命周期 ----------
/** 别的页面寄存过来的参数：{ view: 'month', month: '2026-10' } / { date: '2026-10-08' } */
const applyQuery = (query) => {
  if (!query) return false
  if (query.view === 'month' || query.month) {
    view.value = 'month'
    month.value = query.month || ''
  } else if (query.view === 'week' || query.date) {
    view.value = 'week'
    anchor.value = query.date || ''
  }
  return true
}

const onTabQuery = (url) => {
  if (url === PAGE_URL && applyQuery(takeTabQuery(PAGE_URL))) load()
}

/** 老师布置、删除作业时后端推课程事件：日程里的作业截止跟着刷新 */
let refreshTimer = null
const onCourseEvent = (message) => {
  if (!message || message.event !== 'assignments' || refreshTimer) return
  refreshTimer = setTimeout(() => {
    refreshTimer = null
    load()
  }, 600)
}

onShow(() => {
  if (!ensureLoggedIn()) return
  uni.setNavigationBarTitle({ title: isAdmin.value ? t('nav.admin') : t('schedule.title') })
  if (isAdmin.value) return
  applyQuery(takeTabQuery(PAGE_URL))
  load()
  uni.$off('xm:tab-query', onTabQuery)
  uni.$on('xm:tab-query', onTabQuery)
  uni.$off('ws:course', onCourseEvent)
  uni.$on('ws:course', onCourseEvent)
})

onHide(() => {
  uni.$off('xm:tab-query', onTabQuery)
  uni.$off('ws:course', onCourseEvent)
  if (refreshTimer) {
    clearTimeout(refreshTimer)
    refreshTimer = null
  }
})

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
.view-seg {
  margin-bottom: 20rpx;
}

.nav {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 20rpx;
}

.nav-btn,
.nav-today {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 60rpx;
  border: 1rpx solid var(--xm-border);
  border-radius: 14rpx;
  background: var(--xm-bg-card);
  color: var(--xm-text);
  box-sizing: border-box;
}

.nav-btn {
  width: 60rpx;
}

.nav-today {
  padding: 0 20rpx;
  font-size: 24rpx;
}

.nav-label {
  flex: 1;
  min-width: 0;
  font-size: 26rpx;
  font-weight: 600;
  color: var(--xm-text);
  text-align: right;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.note {
  padding: 16rpx 0;
  text-align: center;
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.events {
  margin-top: 24rpx;
}

.event {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 0;
}

.event + .event {
  border-top: 1rpx solid var(--xm-border);
}

.event-day {
  width: 150rpx;
  flex-shrink: 0;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.event-body {
  flex: 1;
  min-width: 0;
}

.event-title {
  font-size: 26rpx;
  color: var(--xm-text);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.event-meta {
  margin-top: 2rpx;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.select-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx 16rpx;
  margin-bottom: 20rpx;
  padding: 18rpx 22rpx;
  border: 1rpx dashed var(--xm-border);
  border-radius: 20rpx;
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.select-bar.is-active {
  border-style: solid;
  border-color: var(--xm-brand);
  background: var(--xm-brand-soft);
  color: var(--xm-text);
}

.select-text {
  flex: 1;
  min-width: 300rpx;
  line-height: 1.5;
}

.select-actions {
  display: flex;
  gap: 12rpx;
}

.day {
  margin-top: 24rpx;
}

.day-item {
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 14rpx 0;
  font-size: 24rpx;
}

.day-item + .day-item {
  border-top: 1rpx solid var(--xm-border);
}

.day-bar {
  width: 8rpx;
  height: 36rpx;
  border-radius: 4rpx;
  flex-shrink: 0;
}

.day-time {
  flex-shrink: 0;
  color: var(--xm-text-2);
}

.day-name {
  flex: 1;
  min-width: 0;
  font-size: 26rpx;
  color: var(--xm-text);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.day-meta {
  flex-shrink: 0;
  max-width: 200rpx;
  color: var(--xm-text-2);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.leave {
  padding: 18rpx 0;
}

.leave + .leave {
  border-top: 1rpx solid var(--xm-border);
}

.leave-head {
  display: flex;
  align-items: center;
  gap: 14rpx;
  font-size: 26rpx;
  color: var(--xm-text);
}

.leave-days {
  flex: 1;
  color: var(--xm-text-2);
}

.leave-reason {
  margin-top: 8rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: var(--xm-text-2);
}

.leave-note {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: var(--xm-text-3);
}

.leave-withdraw {
  display: inline-block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: var(--xm-danger);
}
</style>

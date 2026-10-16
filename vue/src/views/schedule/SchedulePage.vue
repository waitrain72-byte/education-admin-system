<template>
  <div class="sched">
    <div class="page-head">
      <div>
        <h1 class="page-title">{{ $t('schedule.title') }}</h1>
        <p class="page-sub">{{ $t('schedule.sub') }}</p>
      </div>
      <el-radio-group v-model="view" @change="onViewChange">
        <el-radio-button value="week">{{ $t('schedule.week') }}</el-radio-button>
        <el-radio-button value="month">{{ $t('schedule.month') }}</el-radio-button>
      </el-radio-group>
    </div>

    <div class="sched-nav">
      <el-button :aria-label="$t('schedule.prev')" @click="shift(-1)">‹</el-button>
      <el-button @click="goToday">{{ view === 'week' ? $t('schedule.thisWeek') : $t('schedule.thisMonth') }}</el-button>
      <el-button :aria-label="$t('schedule.next')" @click="shift(1)">›</el-button>
      <span class="sched-nav__label">{{ periodLabel }}</span>
      <span v-if="view === 'week' && weekData && weekData.weekNo > 0" class="pill pill--brand">
        {{ $t('schedule.weekNo', { n: weekData.weekNo }) }}
      </span>
    </div>

    <!-- 骨架：周视图画成课表格子（几格有课块），月视图画成月历格子 -->
    <SkeletonRegion v-if="skeleton">
      <div v-if="view === 'week'" class="sched-sk-scroll">
        <div class="sched-sk sched-sk--week" aria-hidden="true">
          <span></span>
          <span v-for="d in 7" :key="'h' + d" class="sched-sk__head">
            <SkBlock :w="28" :h="11" />
            <SkBlock :w="44" :h="15" />
          </span>
          <template v-for="p in 5" :key="'p' + p">
            <span class="sched-sk__time">
              <SkBlock :w="36" :h="12" />
              <SkBlock :w="30" :h="10" />
            </span>
            <span v-for="d in 7" :key="d" class="sched-sk__cell">
              <SkBlock v-if="SKELETON_BUSY.has(`${p}-${d}`)" w="100%" h="100%" />
            </span>
          </template>
        </div>
      </div>
      <div v-else class="sched-month">
        <div class="sched-month__main">
          <div v-if="isStudent" class="sched-select">{{ $t('leave.requestHint') }}</div>
          <div class="sched-sk sched-sk--month" aria-hidden="true">
            <span v-for="d in 7" :key="'h' + d" class="sched-sk__mhead"><SkBlock :w="24" :h="12" /></span>
            <span v-for="i in 35" :key="i" class="sched-sk__day">
              <SkBlock :w="18" :h="14" />
              <SkBlock v-if="i % 5 === 2" w="80%" :h="16" />
            </span>
          </div>
        </div>
        <aside v-if="isStudent" class="panel sched-leaves">
          <div class="panel__head">
            <span class="panel__title">{{ $t('leave.mine') }}</span>
          </div>
          <div class="panel__body"><SkeletonList :rows="2" trail="pill" /></div>
        </aside>
      </div>
    </SkeletonRegion>

    <template v-else-if="view === 'week' && weekData">
      <WeekTimetable :data="weekData" :show-teacher="isStudent" />
      <p v-if="!weekData.courses.length" class="sched-note">{{ $t('schedule.noCourses') }}</p>
    </template>

    <div v-else-if="view === 'month' && monthData" class="sched-month">
      <div class="sched-month__main">
        <div v-if="isStudent" class="sched-select" :class="{ 'is-active': selection.from }">
          <span class="sched-select__text">
            <template v-if="!selection.from">{{ $t('leave.requestHint') }}</template>
            <template v-else-if="selection.from === selection.to && picking">
              {{ $t('leave.selecting', { from: fmt(selection.from), n: 1 }) }}
            </template>
            <template v-else>
              {{ $t('leave.selected', { from: fmt(selection.from), to: fmt(selection.to!), n: selectedDays }) }}
            </template>
          </span>
          <template v-if="selection.from">
            <el-button size="small" @click="clearSelection">{{ $t('leave.clear') }}</el-button>
            <el-button size="small" type="primary" @click="leaveOpen = true">{{ $t('leave.request') }}</el-button>
          </template>
        </div>
        <MonthCalendar :data="monthData" :selectable="true" :selection="selection" @pick="pick" />

        <!-- 点过的那一天：课程与事件明细（手机上格子里只有圆点，靠这里看详情） -->
        <section v-if="focusDay" class="panel sched-day">
          <div class="panel__head">
            <span class="panel__title">{{ fmt(focusDay.date) }}</span>
          </div>
          <div class="panel__body">
            <div v-if="!focusDay.classes.length && !focusEvents.length" class="empty-note">—</div>
            <ul v-else class="sched-day__list">
              <li v-for="c in focusDay.classes" :key="'c' + c.courseId + c.segment">
                <router-link :to="`/course/${c.courseId}`" class="sched-day__item">
                  <span class="sched-day__bar" :style="{ background: courseColor(c.courseName) }"></span>
                  <span class="num sched-day__time">{{ c.start }}–{{ c.end }}</span>
                  <span class="sched-day__name">{{ c.courseName }}</span>
                  <span class="sched-day__meta">{{ c.room || '—' }}</span>
                </router-link>
              </li>
              <li v-for="ev in focusEvents" :key="ev.type + ev.id" class="sched-day__item">
                <span class="pill" :class="ev.type === 'exam' ? 'pill--bad' : ev.done ? 'pill--ok' : 'pill--warn'">
                  {{ ev.type === 'exam' ? $t('schedule.exam') : ev.done ? $t('schedule.done') : $t('schedule.deadline') }}
                </span>
                <span class="num sched-day__time">{{ ev.time }}</span>
                <span class="sched-day__name">{{ ev.title }}</span>
                <span class="sched-day__meta">{{ ev.courseName || '' }}</span>
              </li>
            </ul>
          </div>
        </section>
      </div>

      <aside v-if="isStudent" class="panel sched-leaves">
        <div class="panel__head">
          <span class="panel__title">{{ $t('leave.mine') }}</span>
        </div>
        <div class="panel__body">
          <div v-if="!myLeaves.length" class="empty-note">{{ $t('leave.mineEmpty') }}</div>
          <ul v-else class="sched-leaves__list">
            <li v-for="l in myLeaves" :key="l.id" class="sched-leaves__item">
              <div class="sched-leaves__head">
                <span class="num">{{ fmt(l.time) }}</span>
                <span class="sched-leaves__days">{{ $t('leave.days', { n: l.day }) }}</span>
                <span class="pill" :class="leavePill(l.status)">{{ $t('leave.status.' + leaveKey(l.status)) }}</span>
              </div>
              <p class="sched-leaves__reason">{{ l.content }}</p>
              <p v-if="l.descr" class="sched-leaves__note">{{ $t('leave.reviewNote', { text: l.descr }) }}</p>
              <el-button v-if="l.status === '待审核'" link type="danger" size="small" @click="withdraw(l)">
                {{ $t('leave.withdraw') }}
              </el-button>
            </li>
          </ul>
        </div>
      </aside>
    </div>

    <LeaveDialog
      v-if="selection.from && selection.to"
      v-model="leaveOpen"
      :from="selection.from"
      :to="selection.to"
      @submitted="onSubmitted"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import WeekTimetable from './WeekTimetable.vue'
import MonthCalendar from './MonthCalendar.vue'
import LeaveDialog from './LeaveDialog.vue'
import request from '@/utils/request'
import { useUser } from '@/components/useUser'
import { ElMessage, ElMessageBox } from '@/utils/element-plus'
import { addDays, addMonths, daysBetween, formatDay } from '@/utils/schedule'
import { courseColor } from '@/utils/courseColor'
import { syncServerClock } from '@/composables/useServerClock'
import { onSocketMessage } from '@/composables/useNoticeSocket'
import { currentLocale } from '@/composables/useLocale'
import { useSkeleton } from '@/composables/useSkeleton'
import SkBlock from '@/components/skeleton/SkBlock.vue'
import SkeletonList from '@/components/skeleton/SkeletonList.vue'
import SkeletonRegion from '@/components/skeleton/SkeletonRegion.vue'
import { t } from '@/i18n'

/** 周视图骨架里画课块的格子（第几大节-星期几），看起来像一张排了课的课表 */
const SKELETON_BUSY = new Set(['1-1', '1-3', '2-2', '2-4', '3-2', '3-5', '4-4'])

defineOptions({ name: 'SchedulePage' })

/** 一次最多请 30 天（与后端一致） */
const MAX_LEAVE_DAYS = 30

const route = useRoute()
const router = useRouter()
const { user } = useUser()
const isStudent = computed(() => user.value.role === 'STUDENT')

const view = ref<'week' | 'month'>(route.query.view === 'month' ? 'month' : 'week')
/** 周视图锚定的日期（那一周里的任意一天），空 = 今天 */
const anchor = ref<string>(typeof route.query.date === 'string' ? route.query.date : '')
/** 月视图的月份 yyyy-MM，空 = 本月 */
const month = ref<string>(typeof route.query.month === 'string' ? route.query.month : '')

const weekData = ref<Record<string, any> | null>(null)
const monthData = ref<Record<string, any> | null>(null)
const loading = ref(false)
const current = computed(() => (view.value === 'week' ? weekData.value : monthData.value))

const locale = computed(() => currentLocale())
const fmt = (iso: string) => formatDay(iso, locale.value, { month: 'short', day: 'numeric', weekday: 'short' })

const periodLabel = computed(() => {
  if (view.value === 'week' && weekData.value) {
    const monday = weekData.value.monday as string
    return `${formatDay(monday, locale.value)} – ${formatDay(addDays(monday, 6), locale.value)}`
  }
  if (view.value === 'month' && monthData.value) {
    return formatDay(`${monthData.value.month}-01`, locale.value, { year: 'numeric', month: 'long' })
  }
  return ''
})

// 学生自己的请假记录（月历右侧）。要写在 load 前面：load 在下面的 watch 里立即执行，月历和它同时取
const myLeaves = ref<Record<string, any>[]>([])
const loadMyLeaves = async () => {
  try {
    const list = (await request.get<Record<string, any>[]>('/apply/selectAll')) || []
    myLeaves.value = list.sort((a, b) => String(b.time).localeCompare(String(a.time)) || b.id - a.id)
  } catch {
    myLeaves.value = []
  }
}

const load = async () => {
  loading.value = true
  try {
    if (view.value === 'week') {
      const data = await request.get<Record<string, any>>('/schedule/week', { params: anchor.value ? { date: anchor.value } : undefined })
      weekData.value = data
      anchor.value = data.monday
    } else {
      // 月历与「我的请假」一起取、一起显示：先显示月历再补请假，中间会闪一下「还没有请假」
      const [data] = await Promise.all([
        request.get<Record<string, any>>('/schedule/month', { params: month.value ? { month: month.value } : undefined }),
        isStudent.value ? loadMyLeaves() : null,
      ])
      monthData.value = data
      month.value = data.month
    }
    syncUrl()
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    loading.value = false
  }
}

/** 把当前视图写回地址栏（刷新、分享链接都能回到同一页） */
const syncUrl = () => {
  const query: Record<string, string> = { view: view.value }
  if (view.value === 'week' && anchor.value) query.date = anchor.value
  if (view.value === 'month' && month.value) query.month = month.value
  router.replace({ query })
}

const onViewChange = () => load()

const shift = (n: number) => {
  if (view.value === 'week') {
    anchor.value = addDays(anchor.value || (weekData.value?.monday as string), n * 7)
  } else {
    month.value = addMonths(month.value || (monthData.value?.month as string), n)
  }
  load()
}

const goToday = () => {
  anchor.value = ''
  month.value = ''
  load()
}

watch(
  () => user.value.role,
  () => load(),
  { immediate: true },
)

// 放在首次加载之后创建，第一次渲染就是骨架；翻周、翻月时上一页还在，不出骨架
const skeleton = useSkeleton(() => loading.value && !current.value)

// 老师布置、删除作业时后端会推课程事件：日程里的作业截止跟着刷新
let refreshTimer: ReturnType<typeof setTimeout> | null = null
const offSocket = onSocketMessage((message) => {
  if (message?.type !== 'course' || message.event !== 'assignments' || refreshTimer) return
  refreshTimer = setTimeout(() => {
    refreshTimer = null
    load()
  }, 600)
})
onBeforeUnmount(() => {
  offSocket()
  if (refreshTimer) clearTimeout(refreshTimer)
})

// ---------- 学生：在月历上选日期请假 ----------
const selection = reactive<{ from: string | null; to: string | null }>({ from: null, to: null })
/** 已经点了开始日期、等着点结束日期 */
const picking = ref(false)
const leaveOpen = ref(false)

const selectedDays = computed(() => (selection.from && selection.to ? daysBetween(selection.from, selection.to) + 1 : 0))

/** 月历里最近点的那一天（看当天明细用） */
const focusDate = ref<string | null>(null)
const focusDay = computed(() => (monthData.value?.days || []).find((d: Record<string, any>) => d.date === focusDate.value) || null)
const focusEvents = computed(() => (monthData.value?.events || []).filter((e: Record<string, any>) => e.date === focusDate.value))

const pick = (date: string) => {
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
    ElMessage.warning(t('errors.5028'))
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

const onSubmitted = async () => {
  clearSelection()
  await load()
}

const withdraw = async (leave: Record<string, any>) => {
  try {
    await ElMessageBox.confirm(t('leave.withdrawConfirm'), t('leave.withdraw'), { type: 'warning' })
  } catch {
    return
  }
  try {
    await request.delete(`/apply/delete/${leave.id}`)
    ElMessage.success(t('leave.withdrawn'))
    await load()
  } catch {
    // 错误提示已由拦截器统一处理
  }
}

const leaveKey = (status: string) => (status === '审核通过' ? 'approved' : status === '审核不通过' ? 'rejected' : 'pending')
const leavePill = (status: string) => (status === '审核通过' ? 'pill--ok' : status === '审核不通过' ? 'pill--bad' : 'pill--warn')

// 用服务器时间校准时钟（周课表里「正在上的课」按服务器时间判断，不用浏览器时区）
watch(current, (data) => {
  if (data?.now) syncServerClock(data.now)
})
</script>

<style scoped>
.sched {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
}

.sched .page-head {
  margin-bottom: 0;
}

.sched-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.sched-nav .el-button + .el-button {
  margin-left: 0;
}

.sched-nav__label {
  margin-left: 6px;
  font-size: 16px;
  font-weight: 600;
  color: var(--xm-text-primary);
}

.sched-note {
  font-size: 13px;
  color: var(--xm-text-secondary);
}

/* ---------- 骨架：尺寸与 WeekTimetable / MonthCalendar 的格子一致 ---------- */
.sched-sk-scroll {
  overflow-x: auto;
}

.sched-sk {
  display: grid;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius-lg);
  background: var(--xm-bg-card);
  overflow: hidden;
}

.sched-sk--week {
  grid-template-columns: 72px repeat(7, minmax(104px, 1fr));
  min-width: 800px;
}

.sched-sk__head {
  display: grid;
  align-content: start;
  gap: 6px;
  min-height: 74px;
  padding: 10px 8px 8px;
  border-left: 1px solid var(--xm-border);
}

.sched-sk__time {
  display: grid;
  align-content: center;
  justify-items: center;
  gap: 6px;
  padding: 8px 4px;
  border-top: 1px solid var(--xm-border);
}

.sched-sk__cell {
  display: grid;
  min-height: 84px;
  padding: 5px;
  border-top: 1px solid var(--xm-border);
  border-left: 1px solid var(--xm-border);
}

.sched-sk--month {
  grid-template-columns: repeat(7, minmax(0, 1fr));
}

.sched-sk__mhead {
  padding: 8px 10px;
}

.sched-sk__day {
  display: grid;
  align-content: start;
  gap: 8px;
  min-height: 108px;
  padding: 8px 6px 6px;
  border-top: 1px solid var(--xm-border);
  border-left: 1px solid var(--xm-border);
}

/* 每行第一格不画左边线（前 7 个子元素是星期表头） */
.sched-sk__day:nth-child(7n + 8) {
  border-left: 0;
}

.sched-month {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 16px;
  align-items: start;
}

.sched-month__main {
  display: grid;
  gap: 12px;
  min-width: 0;
}

.sched-select {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 10px 14px;
  border: 1px dashed var(--xm-border);
  border-radius: var(--xm-radius);
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.sched-select.is-active {
  border-style: solid;
  border-color: var(--xm-brand);
  background: var(--xm-brand-soft);
  color: var(--xm-text-primary);
}

.sched-select__text {
  flex: 1;
  min-width: 200px;
}

.sched-day__list {
  display: grid;
  gap: 6px;
  list-style: none;
}

.sched-day__item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--xm-text-primary);
}

.sched-day__bar {
  width: 4px;
  height: 18px;
  border-radius: 2px;
  flex-shrink: 0;
}

.sched-day__time {
  color: var(--xm-text-secondary);
  white-space: nowrap;
}

.sched-day__name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.sched-day__meta {
  color: var(--xm-text-secondary);
  white-space: nowrap;
}

.sched-leaves__list {
  display: grid;
  gap: 12px;
  list-style: none;
}

.sched-leaves__item {
  display: grid;
  gap: 4px;
  justify-items: start;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--xm-border);
}

.sched-leaves__item:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.sched-leaves__head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--xm-text-primary);
}

.sched-leaves__days {
  color: var(--xm-text-secondary);
}

.sched-leaves__reason {
  font-size: 13px;
  color: var(--xm-text-regular);
  overflow-wrap: anywhere;
}

.sched-leaves__note {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

@media (max-width: 1080px) {
  .sched-month {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>

<template>
  <div class="screen">
    <!-- 顶部标题栏 -->
    <div class="screen-header">
      <div class="header-side left">
        <span class="clock">{{ clock }}</span>
      </div>
      <div class="header-title">{{ $t('pages.dashboard.title') }}</div>
      <div class="header-side right">
        <button class="screen-btn" @click="goBack">{{ $t('pages.dashboard.exit') }}</button>
      </div>
    </div>

    <!-- 指标卡 -->
    <div class="metric-row">
      <div v-for="m in metrics" :key="m.key" class="metric-card">
        <div class="metric-num">{{ m.value }}</div>
        <div class="metric-label">{{ $t(m.label) }}</div>
      </div>
    </div>

    <!-- 图表 3 x 2 -->
    <div class="chart-grid">
      <div class="panel"><div class="panel-title">{{ $t('pages.dashboard.scoreChart') }}</div><div ref="chartScore" class="chart"></div></div>
      <div class="panel"><div class="panel-title">{{ $t('pages.dashboard.attendanceChart') }}</div><div ref="chartAttendance" class="chart"></div></div>
      <div class="panel"><div class="panel-title">{{ $t('pages.dashboard.collegeChart') }}</div><div ref="chartCollege" class="chart"></div></div>
      <div class="panel"><div class="panel-title">{{ $t('pages.dashboard.courseTopChart') }}</div><div ref="chartCourseTop" class="chart"></div></div>
      <div class="panel"><div class="panel-title">{{ $t('pages.dashboard.titleChart') }}</div><div ref="chartTitle" class="chart"></div></div>
      <div class="panel"><div class="panel-title">{{ $t('pages.dashboard.loginTrendChart') }}</div><div ref="chartLoginTrend" class="chart"></div></div>
    </div>

    <!-- 底部：通知轮播 + 待办 -->
    <div class="bottom-row">
      <div class="notice-strip">
        <span class="notice-badge">{{ $t('pages.dashboard.notices') }}</span>
        <div class="notice-text">{{ currentNotice }}</div>
      </div>
      <div class="todo-strip">
        <span class="todo-item">{{ $t('pages.dashboard.pendingApply') }}：{{ stats.pendingApply ?? 0 }}</span>
        <span class="todo-item">{{ $t('pages.dashboard.ungradedHomework') }}：{{ stats.ungradedHomework ?? 0 }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import request from '@/utils/request'

echarts.use([BarChart, LineChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

const router = useRouter()
const stats = ref<Record<string, any>>({})
const notices = ref<any[]>([])
const noticeIndex = ref(0)
const currentNotice = computed(() => {
  const list = notices.value
  if (!list.length) return ''
  return list[noticeIndex.value % list.length].title || ''
})

const goBack = () => {
  window.history.length > 1 ? router.back() : router.push('/home')
}

// ========== 指标卡 ==========
const metrics = computed(() => [
  { key: 'student', value: stats.value.studentCount ?? '-', label: 'pages.dashboard.studentCount' },
  { key: 'teacher', value: stats.value.teacherCount ?? '-', label: 'pages.dashboard.teacherCount' },
  { key: 'course', value: stats.value.courseCount ?? '-', label: 'pages.dashboard.courseCount' },
  { key: 'choice', value: stats.value.choiceCount ?? '-', label: 'pages.dashboard.choiceCount' },
  { key: 'loginToday', value: stats.value.loginToday ?? '-', label: 'pages.dashboard.loginToday' },
  { key: 'loginWeek', value: stats.value.loginWeek ?? '-', label: 'pages.dashboard.loginWeek' },
])

// ========== 时钟 ==========
const clock = ref('')
let clockTimer: ReturnType<typeof setInterval> | null = null
const updateClock = () => {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  clock.value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

// ========== 图表 ==========
const chartScore = ref<HTMLElement>()
const chartAttendance = ref<HTMLElement>()
const chartCollege = ref<HTMLElement>()
const chartCourseTop = ref<HTMLElement>()
const chartTitle = ref<HTMLElement>()
const chartLoginTrend = ref<HTMLElement>()
const instances: echarts.ECharts[] = []

// 放大的全局字号
const AXIS_FONT = 18
const LEGEND_FONT = 18
const TOOLTIP_FONT = 18
const AXUS = 'rgba(140, 180, 255, 0.9)'
const SPLIT = 'rgba(80, 120, 200, 0.25)'
const baseGrid = { left: 70, right: 30, top: 36, bottom: 46 }
const categoryAxis = (data: any[], rotate = 0) => ({
  type: 'category',
  data,
  axisLabel: { color: AXUS, fontSize: AXIS_FONT, interval: 0, rotate },
  axisLine: { lineStyle: { color: SPLIT } },
})
const valueAxis = () => ({
  type: 'value',
  minInterval: 1,
  axisLabel: { color: AXUS, fontSize: AXIS_FONT },
  splitLine: { lineStyle: { color: SPLIT } },
})

const handleResize = () => instances.forEach((inst) => inst.resize())

function initChart(el: HTMLElement | undefined, option: echarts.EChartsCoreOption) {
  if (!el) return
  const inst = echarts.init(el)
  inst.setOption(option)
  instances.push(inst)
}

const renderCharts = () => {
  // 成绩分布（柱状）
  request.get('/score/getLine').then((res: any) => {
    if (res.data.code !== '200') return
    const xAxis: string[] = res.data.data.xAxis || []
    const values: number[] = res.data.data.yAxis || []
    initChart(chartScore.value, {
      grid: baseGrid,
      tooltip: { textStyle: { fontSize: TOOLTIP_FONT } },
      xAxis: categoryAxis(xAxis),
      yAxis: valueAxis(),
      series: [{ type: 'bar', data: values, barWidth: 34, itemStyle: { borderRadius: [6, 6, 0, 0], color: '#2f7cff' } }],
    })
  })

  // 考勤占比（环形）
  request.get('/attendance/getPie').then((res: any) => {
    if (res.data.code !== '200') return
    const data = res.data.data.data || []
    initChart(chartAttendance.value, {
      tooltip: { trigger: 'item', textStyle: { fontSize: TOOLTIP_FONT } },
      legend: { bottom: 0, textStyle: { color: AXUS, fontSize: LEGEND_FONT }, itemWidth: 16, itemHeight: 16 },
      series: [{
        type: 'pie', radius: ['38%', '64%'], center: ['50%', '44%'],
        data,
        label: { color: AXUS, fontSize: AXIS_FONT },
        itemStyle: { borderRadius: 6, borderColor: '#0a1628', borderWidth: 2 },
      }],
    })
  })

  const s = stats.value

  // 各学院学生人数（柱状）
  initChart(chartCollege.value, {
    grid: baseGrid,
    tooltip: { textStyle: { fontSize: TOOLTIP_FONT } },
    xAxis: categoryAxis((s.collegeDist || []).map((i: any) => i.name), 20),
    yAxis: valueAxis(),
    series: [{ type: 'bar', data: (s.collegeDist || []).map((i: any) => i.value), barWidth: 34, itemStyle: { borderRadius: [6, 6, 0, 0], color: '#00d4ff' } }],
  })

  // 选课热度 TOP5（横向条形）
  const top = [...(s.courseTop || [])].reverse()
  initChart(chartCourseTop.value, {
    grid: { left: 190, right: 40, top: 26, bottom: 30 },
    tooltip: { textStyle: { fontSize: TOOLTIP_FONT } },
    xAxis: valueAxis(),
    yAxis: { type: 'category', data: top.map((i: any) => i.name), axisLabel: { color: AXUS, fontSize: AXIS_FONT }, axisLine: { lineStyle: { color: SPLIT } } },
    series: [{ type: 'bar', data: top.map((i: any) => i.value), barWidth: 22, itemStyle: { borderRadius: [0, 10, 10, 0], color: '#22e0a1' } }],
  })

  // 教师职称结构（饼图）
  initChart(chartTitle.value, {
    tooltip: { trigger: 'item', textStyle: { fontSize: TOOLTIP_FONT } },
    legend: { bottom: 0, textStyle: { color: AXUS, fontSize: LEGEND_FONT }, itemWidth: 16, itemHeight: 16 },
    series: [{
      type: 'pie', radius: '60%', center: ['50%', '44%'],
      data: s.titleDist || [],
      label: { color: AXUS, fontSize: AXIS_FONT, formatter: '{b} {c}' },
      itemStyle: { borderRadius: 4, borderColor: '#0a1628', borderWidth: 2 },
    }],
  })

  // 近 7 天登录趋势（折线）
  initChart(chartLoginTrend.value, {
    grid: baseGrid,
    tooltip: { trigger: 'axis', textStyle: { fontSize: TOOLTIP_FONT } },
    xAxis: { ...categoryAxis((s.loginTrend || []).map((i: any) => i.date)), boundaryGap: false },
    yAxis: valueAxis(),
    series: [{
      type: 'line', data: (s.loginTrend || []).map((i: any) => i.value),
      smooth: true, symbol: 'circle', symbolSize: 10,
      lineStyle: { color: '#ffb54d', width: 4 },
      itemStyle: { color: '#ffb54d' },
      areaStyle: { color: 'rgba(255, 181, 77, 0.15)' },
    }],
  })
}

// ========== 通知轮播 ==========
let noticeTimer: ReturnType<typeof setInterval> | null = null

const loadNotices = () => {
  request.get('/notice/selectAll').then((res: any) => {
    notices.value = res.data?.data || []
  })
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  updateClock()
  clockTimer = setInterval(updateClock, 1000)

  await nextTick()
  const res: any = await request.get('/dashboard/stats')
  if (res.data.code === '200') {
    stats.value = res.data.data || {}
  }
  renderCharts()
  loadNotices()
  noticeTimer = setInterval(() => {
    noticeIndex.value++
  }, 5000)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (clockTimer) clearInterval(clockTimer)
  if (noticeTimer) clearInterval(noticeTimer)
  instances.forEach((inst) => inst.dispose())
})
</script>

<style lang="scss" scoped>
/* 全屏铺满：视口单位布局，任意分辨率无黑边、无变形 */
.screen {
  width: 100vw;
  height: 100vh;
  background:
    radial-gradient(ellipse at 20% 0%, rgba(47, 124, 255, 0.18) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 100%, rgba(0, 212, 255, 0.12) 0%, transparent 50%),
    linear-gradient(160deg, #050b17 0%, #0a1628 60%, #081222 100%);
  color: #d6e7ff;
  font-size: 18px;
  padding: 20px 30px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 18px;
  overflow: hidden;
}

.screen-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72px;
  flex-shrink: 0;
  border-bottom: 1px solid rgba(0, 212, 255, 0.25);

  .header-title {
    font-size: 42px;
    font-weight: bold;
    letter-spacing: 6px;
    color: #ffffff;
    text-shadow: 0 0 24px rgba(0, 212, 255, 0.6);
  }

  .header-side {
    width: 340px;
    display: flex;
  }

  .header-side.left {
    justify-content: flex-start;
  }

  .header-side.right {
    justify-content: flex-end;
  }

  .clock {
    font-size: 24px;
    color: #8cb4ff;
    font-variant-numeric: tabular-nums;
  }
}

.screen-btn {
  background: transparent;
  border: 1px solid rgba(0, 212, 255, 0.5);
  color: #00d4ff;
  padding: 10px 26px;
  border-radius: 6px;
  font-size: 18px;
  cursor: pointer;

  &:hover {
    background: rgba(0, 212, 255, 0.15);
  }
}

.metric-row {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 18px;
  height: 17vh;
  min-height: 120px;
  flex-shrink: 0;
}

.metric-card {
  background: rgba(13, 35, 70, 0.6);
  border: 1px solid rgba(0, 212, 255, 0.25);
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  box-shadow: inset 0 0 30px rgba(0, 100, 255, 0.08);

  .metric-num {
    font-size: 54px;
    font-weight: bold;
    color: #00d4ff;
    font-variant-numeric: tabular-nums;
    text-shadow: 0 0 18px rgba(0, 212, 255, 0.5);
    line-height: 1;
  }

  .metric-label {
    font-size: 22px;
    color: #8cb4ff;
  }
}

.chart-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(2, 1fr);
  gap: 18px;
  min-height: 0;
}

.panel {
  background: rgba(13, 35, 70, 0.45);
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 10px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.panel-title {
  font-size: 24px;
  font-weight: bold;
  color: #eaf4ff;
  padding-left: 14px;
  border-left: 5px solid #00d4ff;
  margin-bottom: 8px;
}

.chart {
  flex: 1;
  min-height: 0;
}

.bottom-row {
  display: flex;
  gap: 18px;
  height: 72px;
  flex-shrink: 0;
  align-items: stretch;
}

.notice-strip {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 18px;
  background: rgba(13, 35, 70, 0.45);
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 10px;
  padding: 0 24px;
  overflow: hidden;

  .notice-badge {
    flex-shrink: 0;
    background: rgba(0, 212, 255, 0.15);
    color: #00d4ff;
    border: 1px solid rgba(0, 212, 255, 0.4);
    border-radius: 6px;
    padding: 6px 16px;
    font-size: 18px;
  }

  .notice-text {
    font-size: 22px;
    color: #d6e7ff;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.todo-strip {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: space-around;
  background: rgba(13, 35, 70, 0.45);
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 10px;

  .todo-item {
    font-size: 22px;
    color: #ffb54d;
  }
}
</style>

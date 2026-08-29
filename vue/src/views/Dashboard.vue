<template>
  <div class="screen-viewport">
    <div class="screen" :style="screenStyle">
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

// 1920 x 1080 设计稿等比缩放
const DESIGN_W = 1920
const DESIGN_H = 1080
const scale = ref(1)
const screenStyle = computed(() => ({
  transform: `translate(-50%, -50%) scale(${scale.value})`,
}))
const fit = () => {
  scale.value = Math.min(window.innerWidth / DESIGN_W, window.innerHeight / DESIGN_H)
}

const metrics = computed(() => [
  { key: 'student', value: stats.value.studentCount ?? '-', label: 'pages.dashboard.studentCount' },
  { key: 'teacher', value: stats.value.teacherCount ?? '-', label: 'pages.dashboard.teacherCount' },
  { key: 'course', value: stats.value.courseCount ?? '-', label: 'pages.dashboard.courseCount' },
  { key: 'choice', value: stats.value.choiceCount ?? '-', label: 'pages.dashboard.choiceCount' },
  { key: 'loginToday', value: stats.value.loginToday ?? '-', label: 'pages.dashboard.loginToday' },
  { key: 'loginWeek', value: stats.value.loginWeek ?? '-', label: 'pages.dashboard.loginWeek' },
])

const goBack = () => {
  window.history.length > 1 ? router.back() : router.push('/home')
}

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

const AXUS = 'rgba(140, 180, 255, 0.8)'
const SPLIT = 'rgba(80, 120, 200, 0.25)'

const baseGrid = { left: 50, right: 20, top: 30, bottom: 30 }

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
      tooltip: {},
      xAxis: { type: 'category', data: xAxis, axisLabel: { color: AXUS, fontSize: 12, interval: 0 }, axisLine: { lineStyle: { color: SPLIT } } },
      yAxis: { type: 'value', minInterval: 1, axisLabel: { color: AXUS }, splitLine: { lineStyle: { color: SPLIT } } },
      series: [{ type: 'bar', data: values, barWidth: 22, itemStyle: { borderRadius: [4, 4, 0, 0], color: '#2f7cff' } }],
    })
  })

  // 考勤占比（环形）
  request.get('/attendance/getPie').then((res: any) => {
    if (res.data.code !== '200') return
    const data = res.data.data.data || []
    initChart(chartAttendance.value, {
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, textStyle: { color: AXUS }, itemWidth: 12, itemHeight: 12 },
      series: [{
        type: 'pie', radius: ['40%', '68%'], center: ['50%', '45%'],
        data,
        label: { color: AXUS },
        itemStyle: { borderRadius: 6, borderColor: '#0a1628', borderWidth: 2 },
      }],
    })
  })

  const s = stats.value

  // 各学院学生人数（柱状）
  initChart(chartCollege.value, {
    grid: baseGrid,
    tooltip: {},
    xAxis: { type: 'category', data: (s.collegeDist || []).map((i: any) => i.name), axisLabel: { color: AXUS, fontSize: 11, interval: 0, rotate: 20 }, axisLine: { lineStyle: { color: SPLIT } } },
    yAxis: { type: 'value', minInterval: 1, axisLabel: { color: AXUS }, splitLine: { lineStyle: { color: SPLIT } } },
    series: [{ type: 'bar', data: (s.collegeDist || []).map((i: any) => i.value), barWidth: 24, itemStyle: { borderRadius: [4, 4, 0, 0], color: '#00d4ff' } }],
  })

  // 选课热度 TOP5（横向条形）
  const top = [...(s.courseTop || [])].reverse()
  initChart(chartCourseTop.value, {
    grid: { left: 130, right: 30, top: 20, bottom: 20 },
    tooltip: {},
    xAxis: { type: 'value', minInterval: 1, axisLabel: { color: AXUS }, splitLine: { lineStyle: { color: SPLIT } } },
    yAxis: { type: 'category', data: top.map((i: any) => i.name), axisLabel: { color: AXUS, fontSize: 12 }, axisLine: { lineStyle: { color: SPLIT } } },
    series: [{ type: 'bar', data: top.map((i: any) => i.value), barWidth: 16, itemStyle: { borderRadius: [0, 8, 8, 0], color: '#22e0a1' } }],
  })

  // 教师职称结构（饼图）
  initChart(chartTitle.value, {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: AXUS }, itemWidth: 12, itemHeight: 12 },
    series: [{
      type: 'pie', radius: '62%', center: ['50%', '45%'],
      data: s.titleDist || [],
      label: { color: AXUS, formatter: '{b} {c}' },
      itemStyle: { borderRadius: 4, borderColor: '#0a1628', borderWidth: 2 },
    }],
  })

  // 近 7 天登录趋势（折线）
  initChart(chartLoginTrend.value, {
    grid: baseGrid,
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: (s.loginTrend || []).map((i: any) => i.date), axisLabel: { color: AXUS }, axisLine: { lineStyle: { color: SPLIT } }, boundaryGap: false },
    yAxis: { type: 'value', minInterval: 1, axisLabel: { color: AXUS }, splitLine: { lineStyle: { color: SPLIT } } },
    series: [{
      type: 'line', data: (s.loginTrend || []).map((i: any) => i.value),
      smooth: true, symbol: 'circle', symbolSize: 8,
      lineStyle: { color: '#ffb54d', width: 3 },
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
  fit()
  window.addEventListener('resize', fit)
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
  window.removeEventListener('resize', fit)
  if (clockTimer) clearInterval(clockTimer)
  if (noticeTimer) clearInterval(noticeTimer)
  instances.forEach((inst) => inst.dispose())
})
</script>

<style lang="scss" scoped>
.screen-viewport {
  width: 100vw;
  height: 100vh;
  background: #03080f;
  overflow: hidden;
  position: relative;
}

.screen {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 1920px;
  height: 1080px;
  transform-origin: center;
  background:
    radial-gradient(ellipse at 20% 0%, rgba(47, 124, 255, 0.18) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 100%, rgba(0, 212, 255, 0.12) 0%, transparent 50%),
    linear-gradient(160deg, #050b17 0%, #0a1628 60%, #081222 100%);
  color: #d6e7ff;
  font-size: 16px;
  padding: 20px 30px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.screen-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  border-bottom: 1px solid rgba(0, 212, 255, 0.25);

  .header-title {
    font-size: 40px;
    font-weight: bold;
    letter-spacing: 6px;
    color: #ffffff;
    text-shadow: 0 0 24px rgba(0, 212, 255, 0.6);
  }

  .header-side {
    width: 320px;
    display: flex;
  }

  .header-side.left {
    justify-content: flex-start;
  }

  .header-side.right {
    justify-content: flex-end;
  }

  .clock {
    font-size: 22px;
    color: #8cb4ff;
    font-variant-numeric: tabular-nums;
  }
}

.screen-btn {
  background: transparent;
  border: 1px solid rgba(0, 212, 255, 0.5);
  color: #00d4ff;
  padding: 8px 22px;
  border-radius: 6px;
  font-size: 16px;
  cursor: pointer;

  &:hover {
    background: rgba(0, 212, 255, 0.15);
  }
}

.metric-row {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 18px;
  height: 130px;
}

.metric-card {
  background: rgba(13, 35, 70, 0.6);
  border: 1px solid rgba(0, 212, 255, 0.25);
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: inset 0 0 30px rgba(0, 100, 255, 0.08);

  .metric-num {
    font-size: 44px;
    font-weight: bold;
    color: #00d4ff;
    font-variant-numeric: tabular-nums;
    text-shadow: 0 0 18px rgba(0, 212, 255, 0.5);
  }

  .metric-label {
    font-size: 18px;
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
  padding: 14px;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.panel-title {
  font-size: 20px;
  font-weight: bold;
  color: #eaf4ff;
  padding-left: 12px;
  border-left: 4px solid #00d4ff;
  margin-bottom: 6px;
}

.chart {
  flex: 1;
  min-height: 0;
}

.bottom-row {
  display: flex;
  gap: 18px;
  height: 64px;
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
    padding: 4px 14px;
    font-size: 15px;
  }

  .notice-text {
    font-size: 18px;
    color: #d6e7ff;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.todo-strip {
  width: 460px;
  display: flex;
  align-items: center;
  justify-content: space-around;
  background: rgba(13, 35, 70, 0.45);
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 10px;

  .todo-item {
    font-size: 18px;
    color: #ffb54d;
  }
}
</style>

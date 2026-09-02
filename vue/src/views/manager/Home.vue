<template>
  <div>
    <!-- 欢迎条 -->
    <div class="card home-hello">
      <span>{{ $t('home.welcome', { name: user && user.name }) }}</span>
      <el-button type="primary" size="small" @click="router.push('/dashboard')">{{ $t('pages.dashboard.entry') }}</el-button>
    </div>

    <!-- ========== 通知和考试安排（最多各 5 条，右上角可查看全部） ========== -->
    <div class="home-cols">
      <div class="card">
        <div class="card-title">
          <span>{{ $t('home.notice') }}</span>
          <router-link class="card-more" to="/notice">{{ $t('home.viewAll') }} ›</router-link>
        </div>
        <el-timeline v-if="notices.length" reverse>
          <el-timeline-item v-for="item in notices.slice(0, 5)" :key="item.id" :timestamp="item.time">
            <el-popover placement="right" width="200" trigger="hover" :content="item.content">
              <template #reference><span class="tl-item">{{ item.title }}</span></template>
            </el-popover>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else :image-size="60" :description="$t('common.empty')" />
      </div>

      <div class="card">
        <div class="card-title">
          <span>{{ $t('home.examplan') }}</span>
          <router-link class="card-more" to="/examplan">{{ $t('home.viewAll') }} ›</router-link>
        </div>
        <el-timeline v-if="examplans.length" reverse>
          <el-timeline-item v-for="item in examplans.slice(0, 5)" :key="item.id" :timestamp="item.time">
            <el-popover placement="right" width="200" trigger="hover" :content="item.content">
              <template #reference><span class="tl-item">{{ item.name }}</span></template>
            </el-popover>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else :image-size="60" :description="$t('common.empty')" />
      </div>
    </div>

    <!-- ========== 图表（考勤饼图 / 成绩折线） ========== -->
    <div class="home-cols">
      <div class="card chart-card">
        <div class="card-title">
          <span>{{ chartPieTitle }}</span>
          <span v-if="chartPieSub" class="chart-sub">{{ chartPieSub }}</span>
        </div>
        <div v-loading="pieLoading" class="chart-body">
          <div ref="pieEl" v-show="!pieEmpty" class="chart"></div>
          <el-empty v-if="pieEmpty" :image-size="80" :description="$t('common.empty')" />
        </div>
      </div>

      <div class="card chart-card">
        <div class="card-title">
          <span>{{ chartLineTitle }}</span>
          <span class="chart-sub">{{ lineSubText }}</span>
        </div>
        <div v-loading="lineLoading" class="chart-body">
          <div ref="lineEl" v-show="!lineEmpty" class="chart"></div>
          <el-empty v-if="lineEmpty" :image-size="80" :description="$t('common.empty')" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Home' })

import { ref, computed, watch, onMounted, onBeforeUnmount, onActivated, nextTick } from 'vue'
import * as echarts from 'echarts/core'
import { PieChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import request from '@/utils/request'
import { useUser } from '@/components/useUser.ts'
import { useTheme } from '@/composables/useTheme'
import { t } from '@/i18n'
import { useRouter } from 'vue-router'

const router = useRouter()

// 按需注册图表组件，避免打包整个 echarts（体积从约 1MB 降至数百 KB）
echarts.use([
  PieChart,
  LineChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  CanvasRenderer,
])

const { user } = useUser()
const notices = ref<any[]>([])
const examplans = ref<any[]>([])
const pieLoading = ref(false)
const lineLoading = ref(false)
const pieEmpty = ref(false)
const lineEmpty = ref(false)

const pieEl = ref<HTMLDivElement>()
const lineEl = ref<HTMLDivElement>()
const pieChart = ref<ReturnType<typeof echarts.init>>()
const lineChart = ref<ReturnType<typeof echarts.init>>()

// 图表卡片标题（原接口返回的 text/subtext，改为显示在卡片标题栏而非图内，排版更清爽）
const chartPieTitle = ref('')
const chartPieSub = ref('')
const chartLineTitle = ref('')
const chartLineSub = ref('')
/** 折线卡片副标题：后端统计维度说明 + Y 轴含义（避免轴名画进画布被裁剪） */
const lineSubText = computed(() =>
    [chartLineSub.value, t('home.axisYNote')].filter(Boolean).join(' · '),
)

// 图表配色随主题切换（浅色/深色文字、网格线 + 品牌色序列）
const mode = useTheme()
const dark = computed(() => mode.value === 'dark')
const chartTheme = computed(() => {
  const d = dark.value
  return {
    textColor: d ? '#cfd3dc' : '#303133',
    subTextColor: d ? '#a3a6ad' : '#909399',
    axisLineColor: d ? '#363b44' : '#e4e7ed',
    splitLineColor: d ? '#2a2f37' : '#e4e7ed',
    tooltipBg: d ? '#2a2f37' : '#ffffff',
    tooltipBorder: d ? '#363b44' : '#e4e7ed',
  }
})
// 品牌色系：蓝紫主色 + 语义色（正常/迟到/早退/缺勤）
const palette = ['#6366f1', '#67c23a', '#e6a23c', '#409eff', '#f56c6c', '#0ea5e9']

const themed = (opts: any) => {
  const t = chartTheme.value
  return {
    ...opts,
    textStyle: { color: t.textColor },
    color: palette,
    tooltip: { ...opts.tooltip, backgroundColor: t.tooltipBg, borderColor: t.tooltipBorder, textStyle: { color: t.textColor } },
    // 合并而非替换：保留各图自定义的 axisLabel 配置（interval/hideOverlap 等）
    legend: opts.legend ? { ...opts.legend, textStyle: { color: t.textColor } } : undefined,
    xAxis: opts.xAxis
        ? {
            ...opts.xAxis,
            axisLabel: { ...opts.xAxis.axisLabel, color: t.textColor },
            axisLine: { ...opts.xAxis.axisLine, lineStyle: { ...opts.xAxis.axisLine?.lineStyle, color: t.axisLineColor } },
          }
        : undefined,
    yAxis: opts.yAxis
        ? {
            ...opts.yAxis,
            axisLabel: { ...opts.yAxis.axisLabel, color: t.textColor },
            splitLine: { ...opts.yAxis.splitLine, lineStyle: { ...opts.yAxis.splitLine?.lineStyle, color: t.splitLineColor } },
          }
        : undefined,
  }
}

// ---------- 数据加载（home 在多标签页 keep-alive 中缓存，onActivated 每次切回刷新） ----------
let firstActivate = true
onActivated(async () => {
  // 首次挂载由 onMounted 负责；后续每次切回首页都刷新数据并校正图表尺寸
  if (firstActivate) {
    firstActivate = false
    return
  }
  loadAll()
  await nextTick()
  pieChart.value?.resize()
  lineChart.value?.resize()
})

const loadNotices = () => {
  request.get('/notice/selectAll').then((res: any) => {
    notices.value = res.data?.data || []
  })
}

const loadExamplans = () => {
  request.get('/examplan/selectAll').then((res: any) => {
    examplans.value = res.data?.data || []
  })
}

const loadAll = () => {
  loadNotices()
  loadExamplans()
  loadPie()
  loadLine()
}

// 最近一次图表数据缓存：主题切换时据此按新主题重建图表
const pieRows = ref<any[]>([])
const lineX = ref<string[]>([])
const lineY = ref<number[]>([])

/** 考勤饼图：同一接口只请求一次（原实现中 getAttendanceStats 与 getPie 重复请求同接口） */
const loadPie = async () => {
  pieLoading.value = true
  try {
    const res: any = await request.get('/attendance/getPie')
    if (res.data.code === '200') {
      const data = res.data.data
      chartPieTitle.value = data?.text || ''
      chartPieSub.value = data?.subtext || ''
      const rows: any[] = data?.data || []
      pieEmpty.value = !rows.length
      pieRows.value = rows
      if (rows.length) {
        // 容器可能刚从 v-show 隐藏态转为显示，等 DOM 更新后再初始化图表，避免 0 尺寸
        await nextTick()
        renderPie()
      }
    }
  } catch {
    // 请求层已统一提示
  } finally {
    pieLoading.value = false
  }
}

/** 成绩折线：同上，合并原 getScoreStats/getLine 两次重复请求 */
const loadLine = async () => {
  lineLoading.value = true
  try {
    const res: any = await request.get('/score/getLine')
    if (res.data.code === '200') {
      const data = res.data.data
      chartLineTitle.value = data?.text || ''
      chartLineSub.value = data?.subtext || ''
      const x: string[] = data?.xAxis || []
      const y: number[] = data?.yAxis || []
      lineEmpty.value = !y.length
      lineX.value = x
      lineY.value = y
      if (y.length) {
        await nextTick()
        renderLine()
      }
    }
  } catch {
    // 请求层已统一提示
  } finally {
    lineLoading.value = false
  }
}

const renderPie = () => {
  const el = pieEl.value
  if (!el || !pieRows.value.length) return
  pieChart.value = pieChart.value || echarts.init(el)
  const t = chartTheme.value
  pieChart.value.setOption(themed({
    tooltip: {
      trigger: 'item',
      formatter: '{b}：{c} 人次（{d}%）',
      // appendToBody：提示框挂到 body，避免被卡片/画布裁剪导致悬停看不到
      appendToBody: true,
      confine: true,
    },
    legend: { orient: 'horizontal', bottom: 0, icon: 'circle', itemWidth: 10, itemHeight: 10 },
    series: [{
      name: '',
      type: 'pie',
      radius: ['38%', '58%'],          // 环形图
      center: ['50%', '45%'],
      // 数值常驻显示：每个扇区外拉引导线直接标注「名称 + 人次」，不悬停也能看见
      avoidLabelOverlap: false,
      label: {
        show: true,
        position: 'outer',
        formatter: (p: any) => `${p.name}\n${p.value} 人次`,
        lineHeight: 18,
        color: t.textColor,
        fontSize: 13,
      },
      labelLine: { show: true, length: 14, length2: 10, lineStyle: { width: 1 } },
      labelLayout: { hideOverlap: false },
      itemStyle: { borderRadius: 6, borderColor: t.tooltipBg, borderWidth: 2 },
      emphasis: {
        scaleSize: 6,
        label: { show: true, fontWeight: 'bold' },
        itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.25)' },
      },
      data: pieRows.value,
    }],
  }), true)
}

const renderLine = () => {
  const el = lineEl.value
  if (!el || !lineY.value.length) return
  lineChart.value = lineChart.value || echarts.init(el)
  const ct = chartTheme.value
  // X 轴原始文案较长（如「优（90分-100分）」），轴上仅显示段名避免遮挡，完整分数区间放入悬停提示
  const xShort = lineX.value.map((s: string) => s.split('（')[0])
  lineChart.value.setOption(themed({
    tooltip: {
      trigger: 'axis',
      // appendToBody：提示框挂到 body，避免被卡片/画布裁剪导致悬停看不到
      appendToBody: true,
      // 单竖线指针：不用 cross（其 Y 轴标签会随鼠标显示插值小数，人数轴必须是整数）
      axisPointer: { type: 'line', lineStyle: { color: ct.splitLineColor, type: 'dashed' } },
      formatter: (params: any) => {
        const p = Array.isArray(params) ? params[0] : params
        return `${lineX.value[p.dataIndex] ?? p.name}：${Math.round(p.value)} 人`
      },
    },
    // containLabel：网格自动容纳轴标签，边缘文字不再被裁剪；
    // 轴含义（X=成绩段 / Y=人数）在卡片标题栏副标题说明，画布内不放轴名避免挤压
    grid: { left: 8, right: 24, top: 32, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: xShort,
      axisLabel: { interval: 0, hideOverlap: false },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
    },
    series: [{
      data: lineY.value,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { width: 3 },
      // 数值常驻显示：每个数据点正上方直接标注人数，不悬停也能看见
      label: {
        show: true,
        position: 'top',
        formatter: (p: any) => `${p.value}`,
        color: ct.textColor,
        fontSize: 14,
        fontWeight: 600,
        distance: 8,
      },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: dark.value ? 'rgba(99,102,241,0.35)' : 'rgba(99,102,241,0.25)' },
            { offset: 1, color: 'rgba(99,102,241,0.02)' },
          ],
        },
      },
    }],
  }), true)
}

// 主题切换：按最新缓存数据 + 新主题色重建图表（不能直接回设旧 option，颜色已被烘焙进去）
watch(mode, () => {
  renderPie()
  renderLine()
})

const handleResize = () => {
  pieChart.value?.resize()
  lineChart.value?.resize()
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  pieChart.value?.dispose()
  lineChart.value?.dispose()
})
</script>

<style scoped>
.card {
  background: var(--xm-bg-card);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 20px;
}

.home-hello {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

/* 双栏排版：窄屏自动折行成单栏 */
.home-cols {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 10px;
}

.home-cols > .card {
  flex: 1 1 360px;
  min-width: 0;
}

.card-title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 18px;
  font-size: 16px;
  font-weight: bold;
}

.card-more {
  font-size: 13px;
  font-weight: normal;
  color: var(--xm-brand, #409eff);
  text-decoration: none;
}

.card-more:hover {
  text-decoration: underline;
}

.tl-item {
  cursor: default;
}

.chart-sub {
  font-size: 12px;
  font-weight: normal;
  color: var(--xm-text-secondary);
}

.chart-body {
  position: relative;
  height: 330px;
}

.chart {
  width: 100%;
  height: 100%;
}
</style>

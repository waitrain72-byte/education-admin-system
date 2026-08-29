<template>
  <div>
    <div class="card" style="padding: 15px; display: flex; align-items: center; justify-content: space-between">
      <span>{{ $t('home.welcome', { name: user && user.name }) }}</span>
      <el-button type="primary" size="small" @click="router.push('/dashboard')">{{ $t('pages.dashboard.entry') }}</el-button>
    </div>


    <!-- ========== 通知和考试安排 ========== -->
    <div style="display: flex; margin: 10px 0">
      <div style="width: 50%;" class="card">
        <div style="margin-bottom: 30px; font-size: 20px; font-weight: bold">{{ $t('home.notice') }}</div>
        <el-timeline reverse>
          <el-timeline-item v-for="item in notices" :key="item.id" :timestamp="item.time">
            <el-popover placement="right" width="200" trigger="hover" :content="item.content">
              <template #reference><span>{{ item.title }}</span></template>
            </el-popover>
          </el-timeline-item>
        </el-timeline>
      </div>

      <div style="width: 50%;" class="card">
        <div style="margin-bottom: 30px; font-size: 20px; font-weight: bold">{{ $t('home.examplan') }}</div>
        <el-timeline reverse>
          <el-timeline-item v-for="item in examplans" :key="item.id" :timestamp="item.time">
            <el-popover placement="right" width="200" trigger="hover" :content="item.content">
              <template #reference><span>{{ item.name }}</span></template>
            </el-popover>
          </el-timeline-item>
        </el-timeline>
      </div>
    </div>

    <!-- ========== 图表 ========== -->
    <div style="display: flex">
      <div id="pie" class="card" style="height: 400px; width: 50%"></div>
      <div id="line" class="card" style="height: 400px; width: 50%"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts/core'
import { PieChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import request from '@/utils/request'
import { useUser } from '@/components/useUser.ts'
import { useTheme } from '@/composables/useTheme'
import { apiMessage } from '@/i18n'
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
const pieChart = ref<ReturnType<typeof echarts.init>>()
const lineChart = ref<ReturnType<typeof echarts.init>>()

// 考勤统计数据
const attendanceStats = reactive({
  late: 0,
  absent: 0,
  earlyLeave: 0,
  normal: 0
})

// 成绩统计数据
const scoreStats = reactive({
  excellent: 0,
  good: 0,
  fail: 0
})

// 从饼图接口获取考勤统计
const getAttendanceStats = () => {
  request.get('/attendance/getPie').then((res: any) => {
    if (res.data.code === '200') {
      const data = res.data.data.data || []
      data.forEach((item: any) => {
        if (item.name === '迟到') attendanceStats.late = item.value || 0
        else if (item.name === '缺勤') attendanceStats.absent = item.value || 0
        else if (item.name === '早退') attendanceStats.earlyLeave = item.value || 0
        else if (item.name === '正常') attendanceStats.normal = item.value || 0
      })
    }
  })
}

// 从折线图接口获取成绩统计
const getScoreStats = () => {
  request.get('/score/getLine').then((res: any) => {
    if (res.data.code === '200') {
      const yAxisData = res.data.data.yAxis || []
      if (yAxisData.length >= 5) {
        scoreStats.excellent = yAxisData[0] || 0
        scoreStats.good = yAxisData[1] || 0
        scoreStats.fail = yAxisData[yAxisData.length - 1] || 0
      }
    }
  })
}

const pieOptions: any = {
  title: { text: '', subtext: '', left: 'center' },
  tooltip: { trigger: 'item', formatter: '{a} <br/>{b} : {c} ({d}%)' },
  legend: { orient: 'vertical', left: 'left' },
  series: [{ name: '', type: 'pie', radius: '50%', center: ['50%', '60%'], data: [] }]
}

const lineOptions: any = {
  title: { text: '', subtext: '', left: 'center' },
  xAxis: { type: 'category', data: [] },
  yAxis: { type: 'value' },
  tooltip: { trigger: 'item' },
  series: [{ data: [], type: 'line' }]
}

// 图表配色跟随主题：echarts 画布不感知 CSS 变量，需在暗色下显式替换文字与网格线颜色
const mode = useTheme()
const chartTheme = computed(() => {
  const dark = mode.value === 'dark'
  return {
    textColor: dark ? '#cfd3dc' : '#303133',
    subTextColor: dark ? '#a3a6ad' : '#909399',
    axisLineColor: dark ? '#363b44' : '#e4e7ed',
    splitLineColor: dark ? '#2a2f37' : '#e4e7ed',
  }
})

const themeOptions = (opts: any): any => {
  const t = chartTheme.value
  const themed: any = { ...opts, textStyle: { color: t.textColor } }
  if (themed.title) {
    themed.title = { ...themed.title, textStyle: { color: t.textColor }, subtextStyle: { color: t.subTextColor } }
  }
  if (themed.legend) {
    themed.legend = { ...themed.legend, textStyle: { color: t.textColor } }
  }
  if (themed.xAxis) {
    themed.xAxis = {
      ...themed.xAxis,
      axisLabel: { ...themed.xAxis.axisLabel, color: t.textColor },
      axisLine: { ...themed.xAxis.axisLine, lineStyle: { ...themed.xAxis.axisLine?.lineStyle, color: t.axisLineColor } },
    }
  }
  if (themed.yAxis) {
    themed.yAxis = {
      ...themed.yAxis,
      axisLabel: { ...themed.yAxis.axisLabel, color: t.textColor },
      splitLine: { ...themed.yAxis.splitLine, lineStyle: { ...themed.yAxis.splitLine?.lineStyle, color: t.splitLineColor } },
    }
  }
  return themed
}

// 主题切换时重绘两块图表
watch(mode, () => {
  pieChart.value?.setOption(themeOptions(pieOptions))
  lineChart.value?.setOption(themeOptions(lineOptions))
})

const getPie = () => {
  request.get('/attendance/getPie').then((res: any) => {
    if (res.data.code === '200') {
      const chartDom = document.getElementById('pie')
      if (!chartDom) return
      // 同一 DOM 只初始化一次，避免重复 init 告警
      pieChart.value = pieChart.value || echarts.init(chartDom)
      pieOptions.title.text = res.data.data.text
      pieOptions.title.subtext = res.data.data.subtext
      pieOptions.series[0].name = res.data.data.name
      pieOptions.series[0].data = res.data.data.data
      pieChart.value.setOption(themeOptions(pieOptions))
    } else {
      ElMessage.error(apiMessage(res.data))
    }
  })
}

const getLine = () => {
  request.get('/score/getLine').then((res: any) => {
    if (res.data.code === '200') {
      const chartDom = document.getElementById('line')
      if (!chartDom) return
      lineChart.value = lineChart.value || echarts.init(chartDom)
      lineOptions.title.text = res.data.data.text
      lineOptions.title.subtext = res.data.data.subtext
      lineOptions.xAxis.data = res.data.data.xAxis
      lineOptions.series[0].data = res.data.data.yAxis
      lineChart.value.setOption(themeOptions(lineOptions))
    } else {
      ElMessage.error(apiMessage(res.data))
    }
  })
}

const handleResize = () => {
  pieChart.value?.resize()
  lineChart.value?.resize()
}

onMounted(() => {
  request.get('/notice/selectAll').then((res: any) => {
    notices.value = res.data?.data || []
  })
  request.get('/examplan/selectAll').then((res: any) => {
    examplans.value = res.data?.data || []
  })

  getAttendanceStats()
  getScoreStats()
  getPie()
  getLine()

  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  // 组件卸载时移除监听并销毁图表实例，避免内存泄漏
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
</style>

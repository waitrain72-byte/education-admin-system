<template>
  <div>
    <div class="card" style="padding: 15px">
      您好，{{ user && user.name }}！欢迎使用本系统
    </div>
    

    <!-- ========== 通知和考试安排 ========== -->
    <div style="display: flex; margin: 10px 0">
      <div style="width: 50%;" class="card">
        <div style="margin-bottom: 30px; font-size: 20px; font-weight: bold">教务通知</div>
        <el-timeline reverse>
          <el-timeline-item v-for="item in notices" :key="item.id" :timestamp="item.time">
            <el-popover placement="right" width="200" trigger="hover" :content="item.content">
              <template #reference><span>{{ item.title }}</span></template>
            </el-popover>
          </el-timeline-item>
        </el-timeline>
      </div>

      <div style="width: 50%;" class="card">
        <div style="margin-bottom: 30px; font-size: 20px; font-weight: bold">考试安排</div>
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
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts/core'
import { PieChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import request from '@/utils/request'
import { useUser } from '@/components/useUser.ts'

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
      pieChart.value.setOption(pieOptions)
    } else {
      ElMessage.error(res.data.msg)
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
      lineChart.value.setOption(lineOptions)
    } else {
      ElMessage.error(res.data.msg)
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
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 20px;
}
</style>

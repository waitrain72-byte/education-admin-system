<template>
  <div>
    <div class="card" style="padding: 15px">
      您好，{{ user && user.name }}！欢迎使用本系统
    </div>

    <!-- ========== 统计卡片区域（同一行） ========== -->
    <div style="display: flex; margin: 10px 0; gap: 10px">
      <!-- 考勤统计 -->
      <div class="card" style="flex: 1; padding: 10px; text-align: center">
        <div style="font-size: 14px; color: #909399; margin-bottom: 5px">
          <el-icon style="font-size: 20px; vertical-align: middle"><User /></el-icon>
          迟到人数
        </div>
        <div style="font-size: 28px; font-weight: bold; color: #E6A23C">{{ attendanceStats.late }}</div>
      </div>

      <div class="card" style="flex: 1; padding: 10px; text-align: center">
        <div style="font-size: 14px; color: #909399; margin-bottom: 5px">
          <el-icon style="font-size: 20px; vertical-align: middle"><CircleClose /></el-icon>
          缺勤人数
        </div>
        <div style="font-size: 28px; font-weight: bold; color: #F56C6C">{{ attendanceStats.absent }}</div>
      </div>

      <div class="card" style="flex: 1; padding: 10px; text-align: center">
        <div style="font-size: 14px; color: #909399; margin-bottom: 5px">
          <el-icon style="font-size: 20px; vertical-align: middle"><Clock /></el-icon>
          早退人数
        </div>
        <div style="font-size: 28px; font-weight: bold; color: #909399">{{ attendanceStats.earlyLeave }}</div>
      </div>

      <div class="card" style="flex: 1; padding: 10px; text-align: center">
        <div style="font-size: 14px; color: #909399; margin-bottom: 5px">
          <el-icon style="font-size: 20px; vertical-align: middle"><Check /></el-icon>
          正常人数
        </div>
        <div style="font-size: 28px; font-weight: bold; color: #67C23A">{{ attendanceStats.normal }}</div>
      </div>

      <!-- 成绩统计 -->
      <div class="card" style="flex: 1; padding: 10px; text-align: center">
        <div style="font-size: 14px; color: #909399; margin-bottom: 5px">
          <el-icon style="font-size: 20px; vertical-align: middle"><Star /></el-icon>
          优秀人数
        </div>
        <div style="font-size: 28px; font-weight: bold; color: #67C23A">{{ scoreStats.excellent }}</div>
      </div>

      <div class="card" style="flex: 1; padding: 10px; text-align: center">
        <div style="font-size: 14px; color: #909399; margin-bottom: 5px">
          <el-icon style="font-size: 20px; vertical-align: middle"><Medal /></el-icon>
          良好人数
        </div>
        <div style="font-size: 28px; font-weight: bold; color: #409EFF">{{ scoreStats.good }}</div>
      </div>

      <div class="card" style="flex: 1; padding: 10px; text-align: center">
        <div style="font-size: 14px; color: #909399; margin-bottom: 5px">
          <el-icon style="font-size: 20px; vertical-align: middle"><Warning /></el-icon>
          不及格人数
        </div>
        <div style="font-size: 28px; font-weight: bold; color: #F56C6C">{{ scoreStats.fail }}</div>
      </div>
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
      <div class="card" id="pie" style="height: 400px; width: 50%"></div>
      <div class="card" id="line" style="height: 400px; width: 50%"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import request from '@/utils/request'

const user = reactive(JSON.parse(localStorage.getItem('xm-user') || '{}'))
const notices = ref<any[]>([])
const examplans = ref<any[]>([])

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
      const chartDom = document.getElementById('pie')!
      const myChart = echarts.init(chartDom)
      pieOptions.title.text = res.data.data.text
      pieOptions.title.subtext = res.data.data.subtext
      pieOptions.series[0].name = res.data.data.name
      pieOptions.series[0].data = res.data.data.data
      myChart.setOption(pieOptions)
      window.addEventListener('resize', () => myChart.resize())
    } else {
      ElMessage.error(res.data.msg)
    }
  })
}

const getLine = () => {
  request.get('/score/getLine').then((res: any) => {
    if (res.data.code === '200') {
      const chartDom = document.getElementById('line')!
      const myChart = echarts.init(chartDom)
      lineOptions.title.text = res.data.data.text
      lineOptions.title.subtext = res.data.data.subtext
      lineOptions.xAxis.data = res.data.data.xAxis
      lineOptions.series[0].data = res.data.data.yAxis
      myChart.setOption(lineOptions)
      window.addEventListener('resize', () => myChart.resize())
    } else {
      ElMessage.error(res.data.msg)
    }
  })
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

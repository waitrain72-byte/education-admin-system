<template>
  <div>
    <div class="table">
      <el-table :data="tableData" stripe border>
        <el-table-column prop="segment" label="时间" width="200" />
        <el-table-column prop="monday" label="星期一" show-overflow-tooltip />
        <el-table-column prop="tuesday" label="星期二" show-overflow-tooltip />
        <el-table-column prop="wednesday" label="星期三" show-overflow-tooltip />
        <el-table-column prop="thursday" label="星期四" show-overflow-tooltip />
        <el-table-column prop="friday" label="星期五" show-overflow-tooltip />
        <el-table-column prop="saturday" label="星期六" show-overflow-tooltip />
        <el-table-column prop="sunday" label="星期日" show-overflow-tooltip />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const tableData = ref([])

const load = () => {
  request.get('/choice/getCurriculum').then((res: any) => {
    if (res.data.code === '200') { tableData.value = res.data.data } else { ElMessage.error(res.data.msg) }
  })
}

onMounted(() => load())
</script>

<style scoped></style>

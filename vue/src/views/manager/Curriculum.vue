<template>
  <div>
    <div class="table">
      <el-table :data="tableData" stripe border>
        <el-table-column prop="segment" :label="$t('pages.curriculum.time')" width="200" />
        <el-table-column prop="monday" :label="$t('pages.curriculum.monday')" show-overflow-tooltip />
        <el-table-column prop="tuesday" :label="$t('pages.curriculum.tuesday')" show-overflow-tooltip />
        <el-table-column prop="wednesday" :label="$t('pages.curriculum.wednesday')" show-overflow-tooltip />
        <el-table-column prop="thursday" :label="$t('pages.curriculum.thursday')" show-overflow-tooltip />
        <el-table-column prop="friday" :label="$t('pages.curriculum.friday')" show-overflow-tooltip />
        <el-table-column prop="saturday" :label="$t('pages.curriculum.saturday')" show-overflow-tooltip />
        <el-table-column prop="sunday" :label="$t('pages.curriculum.sunday')" show-overflow-tooltip />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Curriculum' })

import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const tableData = ref<any[]>([])

const load = () => {
  request.get<any[]>('/choice/getCurriculum').then((rows) => {
    tableData.value = rows
  }).catch(() => {
    // 错误提示已由 axios 拦截器统一处理
  })
}

onMounted(() => load())
</script>

<style scoped></style>

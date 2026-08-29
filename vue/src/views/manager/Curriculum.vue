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
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { apiMessage } from '@/i18n'

const tableData = ref<any[]>([])

const load = () => {
  request.get('/choice/getCurriculum').then((res: any) => {
    if (res.data.code === '200') { tableData.value = res.data.data } else { ElMessage.error(apiMessage(res.data)) }
  })
}

onMounted(() => load())
</script>

<style scoped></style>

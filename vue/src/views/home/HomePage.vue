<template>
  <div>
    <div v-if="!data && loading" class="home-skeleton" aria-busy="true">
      <el-skeleton animated :rows="3" class="panel panel--pad" />
      <el-skeleton animated :rows="6" class="panel panel--pad" />
    </div>
    <el-result v-else-if="!data && failed" icon="warning" :title="$t('workbench.loadFailed')">
      <template #extra>
        <el-button type="primary" @click="load">{{ $t('workbench.retry') }}</el-button>
      </template>
    </el-result>
    <template v-else-if="data">
      <StudentHome v-if="data.role === 'STUDENT'" :data="data" />
      <TeacherHome v-else-if="data.role === 'TEACHER'" :data="data" />
      <AdminHome v-else-if="data.role === 'ADMIN'" :data="data" />
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import request from '@/utils/request'
import { syncServerClock } from '@/composables/useServerClock'
import StudentHome from './StudentHome.vue'
import TeacherHome from './TeacherHome.vue'
import AdminHome from './AdminHome.vue'

defineOptions({ name: 'HomePage' })

const data = ref<Record<string, any> | null>(null)
const loading = ref(false)
const failed = ref(false)

/** 首页数据一次取齐（/workbench/summary 按角色返回不同内容），并用返回的服务器时间校准时钟 */
const load = async () => {
  loading.value = true
  failed.value = false
  try {
    const summary = await request.get<Record<string, any>>('/workbench/summary')
    syncServerClock(summary?.now)
    data.value = summary
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.home-skeleton {
  display: grid;
  gap: 20px;
}
</style>

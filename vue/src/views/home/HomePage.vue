<template>
  <div>
    <HomeSkeleton v-if="skeleton" />
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
import { onBeforeUnmount, onMounted, ref } from 'vue'
import request from '@/utils/request'
import { syncServerClock } from '@/composables/useServerClock'
import { onSocketMessage } from '@/composables/useNoticeSocket'
import { useSkeleton } from '@/composables/useSkeleton'
import StudentHome from './StudentHome.vue'
import TeacherHome from './TeacherHome.vue'
import AdminHome from './AdminHome.vue'
import HomeSkeleton from './HomeSkeleton.vue'

defineOptions({ name: 'HomePage' })

const data = ref<Record<string, any> | null>(null)
const loading = ref(false)
const failed = ref(false)
const skeleton = useSkeleton(() => !data.value && loading.value)

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

/** 静默刷新：签到开始 / 结束、布置作业时后端推事件，首页的「签到中」「待交作业」跟着变；失败时保留旧数据 */
let refreshTimer: ReturnType<typeof setTimeout> | null = null
const off = onSocketMessage((message) => {
  if (message?.type !== 'course' || !['attendance', 'assignments'].includes(String(message.event))) return
  if (refreshTimer) return
  refreshTimer = setTimeout(async () => {
    refreshTimer = null
    try {
      const summary = await request.get<Record<string, any>>('/workbench/summary')
      syncServerClock(summary?.now)
      data.value = summary
    } catch {
      // 保留旧数据
    }
  }, 600)
})
onBeforeUnmount(() => {
  off()
  if (refreshTimer) clearTimeout(refreshTimer)
})
</script>

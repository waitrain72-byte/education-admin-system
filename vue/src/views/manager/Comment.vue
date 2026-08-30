<template>
  <div>
    <div class="search">
      <el-input v-model="teacher" :placeholder="$t('pages.comment.teacherPlaceholder')" style="width: 200px" />
      <el-input v-model="content" :placeholder="$t('pages.comment.contentPlaceholder')" style="width: 200px; margin-left: 5px" />
      <el-button type="info" plain style="margin-left: 10px" @click="load(1)">{{ $t('common.search') }}</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">{{ $t('common.reset') }}</el-button>
    </div>

    <CrudTable
        :data="tableData"
        :columns="columns"
        :page-num="pageNum"
        :page-size="pageSize"
        :total="total"
        :loading="loading"
        :show-actions="user.role === 'ADMIN'"
        @page-change="load"
    >
      <template #actions="{ row }">
        <el-button link type="danger" size="small" @click="del(row.id)">{{ $t('common.delete') }}</el-button>
      </template>
    </CrudTable>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Comment' })

import { ref, computed, onMounted } from 'vue'
import { useUser } from '@/components/useUser.ts'
import { useCrud } from '@/composables/useCrud'
import { t } from '@/i18n'
import CrudTable, { type CrudColumn } from '@/components/CrudTable.vue'

const { user } = useUser()
const teacher = ref('')
const content = ref('')

const { tableData, pageNum, pageSize, total, loading, load, del } = useCrud({
  url: '/comment',
  getParams: () => ({ teacher: teacher.value, content: content.value }),
})

const columns = computed<CrudColumn[]>(() => [
  { prop: 'id', label: t('pages.comment.id'), width: 80, align: 'center', sortable: true },
  { prop: 'name', label: t('pages.comment.courseName'), width: 200, showOverflowTooltip: true },
  { prop: 'teacher', label: t('pages.comment.teacherName'), width: 100 },
  { prop: 'content', label: t('pages.comment.content') },
  { prop: 'time', label: t('pages.comment.time'), width: 250 },
])

const reset = () => {
  teacher.value = ''
  content.value = ''
  load(1)
}

onMounted(() => load(1))
</script>

<style scoped></style>

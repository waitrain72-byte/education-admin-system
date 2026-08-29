<template>
  <div>
    <div class="search">
      <el-input v-model="teacher" placeholder="请输入教师姓名" style="width: 200px" />
      <el-input v-model="content" placeholder="请输入评教内容" style="width: 200px; margin-left: 5px" />
      <el-button type="info" plain style="margin-left: 10px" @click="load(1)">查询</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">重置</el-button>
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
        <el-button link type="danger" size="small" @click="del(row.id)">删除</el-button>
      </template>
    </CrudTable>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUser } from '@/components/useUser.ts'
import { useCrud } from '@/composables/useCrud'
import CrudTable, { type CrudColumn } from '@/components/CrudTable.vue'

const { user } = useUser()
const teacher = ref('')
const content = ref('')

const { tableData, pageNum, pageSize, total, loading, load, del } = useCrud({
  url: '/comment',
  getParams: () => ({ teacher: teacher.value, content: content.value }),
})

const columns: CrudColumn[] = [
  { prop: 'id', label: '序号', width: 80, align: 'center', sortable: true },
  { prop: 'name', label: '课程名称', width: 200, showOverflowTooltip: true },
  { prop: 'teacher', label: '授课教师', width: 100 },
  { prop: 'content', label: '评教内容' },
  { prop: 'time', label: '评教时间', width: 250 },
]

const reset = () => {
  teacher.value = ''
  content.value = ''
  load(1)
}

onMounted(() => load(1))
</script>

<style scoped></style>

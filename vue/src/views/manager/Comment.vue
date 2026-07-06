<template>
  <div>
    <div class="search">
      <el-input placeholder="请输入教师姓名" style="width: 200px" v-model="teacher" />
      <el-input placeholder="请输入评教内容" style="width: 200px; margin-left: 5px" v-model="content" />
      <el-button type="info" plain style="margin-left: 10px" @click="load(1)">查询</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">重置</el-button>
    </div>
    <div class="table">
      <el-table :data="tableData" stripe>
        <el-table-column prop="id" label="序号" width="80" align="center" sortable />
        <el-table-column prop="name" label="课程名称" show-overflow-tooltip width="200px" />
        <el-table-column prop="teacher" label="授课教师" width="100px" />
        <el-table-column prop="content" label="评教内容" />
        <el-table-column prop="time" label="评教时间" width="250px" />
        <el-table-column label="操作" width="180" align="center" v-if="user.role === 'ADMIN'">
          <template #default="scope">
            <el-button plain type="danger" size="small" @click="del(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination background @current-change="handleCurrentChange" :current-page="pageNum"
          :page-sizes="[5, 10, 20]" :page-size="pageSize" layout="total, prev, pager, next" :total="total" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const user = reactive(JSON.parse(localStorage.getItem('xm-user') || '{}'))
const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const teacher = ref(null)
const content = ref(null)

const del = (id: number) => {
  ElMessageBox.confirm('您确定删除吗？', '确认删除', { type: 'warning' }).then(() => {
    request.delete('/comment/delete/' + id).then((res: any) => {
      if (res.data.code === '200') { ElMessage.success('操作成功'); load(1) }
      else { ElMessage.error(res.data.msg) }
    })
  }).catch(() => {})
}
const load = (pNum?: number) => {
  if (pNum) pageNum.value = pNum
  request.get('/comment/selectPage', { params: { pageNum: pageNum.value, pageSize: pageSize.value, teacher: teacher.value, content: content.value } })
    .then((res: any) => { tableData.value = res.data?.data?.list; total.value = res.data?.data?.total })
}
const reset = () => { teacher.value = null; content.value = null; load(1) }
const handleCurrentChange = (pNum: number) => load(pNum)

onMounted(() => load(1))
</script>

<style scoped></style>

<template>
  <div>
    <div class="table">
      <el-table :data="tableData" stripe>
        <el-table-column prop="id" label="序号" width="80" align="center" sortable />
        <el-table-column prop="name" label="课程名称" show-overflow-tooltip />
        <el-table-column prop="type" label="课程类型" show-overflow-tooltip />
        <el-table-column prop="teacherName" label="授课教师" show-overflow-tooltip />
        <el-table-column prop="score" label="学分" show-overflow-tooltip />
        <el-table-column prop="num" label="上课人数" show-overflow-tooltip />
        <el-table-column prop="room" label="上课教室" show-overflow-tooltip />
        <el-table-column prop="week" label="周几" show-overflow-tooltip />
        <el-table-column prop="segment" label="第几大节" show-overflow-tooltip />
        <el-table-column prop="status" label="上课状态" show-overflow-tooltip />
        <el-table-column prop="studentName" label="选课学生" show-overflow-tooltip />
        <el-table-column label="操作" width="180" align="center" v-if="user.role === 'STUDENT'">
          <template #default="scope">
            <el-button plain type="danger" size="small" @click="del(scope.row.id)" :disabled="scope.row.status !== '未开课'">取消选课</el-button>
            <el-button plain type="primary" size="small" @click="initComment(scope.row)" :disabled="scope.row.status !== '已结课'">评教</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination background @current-change="handleCurrentChange" :current-page="pageNum"
          :page-sizes="[5, 10, 20]" :page-size="pageSize" layout="total, prev, pager, next" :total="total" />
      </div>
    </div>

    <el-dialog title="请填写评教信息" v-model="fromVisible" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form label-width="100px" style="padding-right: 50px" :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="content" label="评教内容">
          <el-input type="textarea" :rows="5" v-model="form.content" autocomplete="off" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fromVisible = false">取 消</el-button>
        <el-button type="primary" @click="save">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import request from '@/utils/request'

const user = reactive(JSON.parse(localStorage.getItem('xm-user') || '{}'))
const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const form = ref<Record<string, any>>({})
const formRef = ref<FormInstance>()
const fromVisible = ref(false)

const rules: FormRules = {
  content: [{ required: true, message: '请输入评教内容', trigger: 'blur' }],
}

const initComment = (row: any) => { form.value = JSON.parse(JSON.stringify(row)); fromVisible.value = true }
const save = () => {
  const data = { name: form.value.name, teacher: form.value.teacherName, student: user.name, content: form.value.content }
  request.post('/comment/add', data).then((res: any) => {
    if (res.data.code === '200') { ElMessage.success('评教成功'); fromVisible.value = false }
    else { ElMessage.error(res.data.msg) }
  })
}
const del = (id: number) => {
  ElMessageBox.confirm('您确定取消选这门课吗？这个老师的课不好选哦！！', '灵魂拷问', { type: 'warning' }).then(() => {
    request.delete('/choice/delete/' + id).then((res: any) => {
      if (res.data.code === '200') { ElMessage.success('操作成功'); load(1) }
      else { ElMessage.error(res.data.msg) }
    })
  }).catch(() => {})
}
const load = (pNum?: number) => {
  if (pNum) pageNum.value = pNum
  request.get('/choice/selectPage', { params: { pageNum: pageNum.value, pageSize: pageSize.value } })
    .then((res: any) => { tableData.value = res.data?.data?.list; total.value = res.data?.data?.total })
}
const handleCurrentChange = (pNum: number) => load(pNum)

onMounted(() => load(1))
</script>

<style scoped></style>

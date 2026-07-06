<template>
  <div>
    <div class="search">
      <el-input placeholder="请输入课程名称" style="width: 200px" v-model="name" />
      <el-button type="info" plain style="margin-left: 10px" @click="load(1)">查询</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">重置</el-button>
    </div>
    <div class="operation" v-if="user.role === 'ADMIN'">
      <el-button type="primary" plain @click="handleAdd">新增</el-button>
      <el-button type="danger" plain @click="delBatch">批量删除</el-button>
    </div>
    <div class="table">
      <el-table :data="tableData" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" v-if="user.role === 'ADMIN'" />
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
        <el-table-column label="操作" width="180" align="center">
          <template #default="scope">
            <el-button plain type="primary" @click="handleEdit(scope.row)" size="small" v-if="user.role !== 'STUDENT'">编辑</el-button>
            <el-button plain type="primary" @click="choiceCourse(scope.row)" size="small" v-if="user.role === 'STUDENT'" :disabled="scope.row.status !== '未开课'">选课</el-button>
            <el-button plain type="danger" size="small" @click="del(scope.row.id)" v-if="user.role === 'ADMIN'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination background @current-change="handleCurrentChange" :current-page="pageNum"
          :page-sizes="[5, 10, 20]" :page-size="pageSize" layout="total, prev, pager, next" :total="total" />
      </div>
    </div>

    <el-dialog title="信息" v-model="fromVisible" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form label-width="100px" style="padding-right: 50px" :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="name" label="课程名称">
          <el-input v-model="form.name" autocomplete="off" :disabled="user.role !== 'ADMIN'" />
        </el-form-item>
        <el-form-item prop="type" label="课程类型">
          <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%" :disabled="user.role !== 'ADMIN'">
            <el-option label="必修" value="必修" /><el-option label="选修" value="选修" />
          </el-select>
        </el-form-item>
        <el-form-item prop="teacherId" label="授课教师">
          <el-select v-model="form.teacherId" placeholder="请选择教师" style="width: 100%" :disabled="user.role !== 'ADMIN'">
            <el-option v-for="item in teacherData" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item prop="score" label="学分">
          <el-input v-model="form.score" autocomplete="off" :disabled="user.role !== 'ADMIN'" />
        </el-form-item>
        <el-form-item prop="num" label="上课人数">
          <el-input v-model="form.num" autocomplete="off" :disabled="user.role !== 'ADMIN'" />
        </el-form-item>
        <el-form-item prop="room" label="上课教室">
          <el-input v-model="form.room" autocomplete="off" :disabled="user.role !== 'ADMIN'" />
        </el-form-item>
        <el-form-item prop="week" label="周几">
          <el-select v-model="form.week" placeholder="请选择" style="width: 100%" :disabled="user.role !== 'ADMIN'">
            <el-option v-for="d in ['星期一','星期二','星期三','星期四','星期五','星期六','星期日']" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item prop="segment" label="第几大节">
          <el-select v-model="form.segment" placeholder="请选择" style="width: 100%" :disabled="user.role !== 'ADMIN'">
            <el-option label="第一大节（08:30 ~ 10:10）" value="第一大节（08:30 ~ 10:10）" />
            <el-option label="第二大节（10:30 ~ 12:10）" value="第二大节（10:30 ~ 12:10）" />
            <el-option label="第三大节（14:00 ~ 15:40）" value="第三大节（14:00 ~ 15:40）" />
            <el-option label="第四大节（16:00 ~ 17:40）" value="第四大节（16:00 ~ 17:40）" />
            <el-option label="第五大节（19:00 ~ 20:40）" value="第五大节（19:00 ~ 20:40）" />
          </el-select>
        </el-form-item>
        <el-form-item prop="status" label="上课状态">
          <el-select v-model="form.status" placeholder="请选择" style="width: 100%">
            <el-option label="未开课" value="未开课" /><el-option label="已开课" value="已开课" /><el-option label="已结课" value="已结课" />
          </el-select>
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
const name = ref(null)
const fromVisible = ref(false)
const form = ref<Record<string, any>>({})
const formRef = ref<FormInstance>()
const ids = ref<number[]>([])
const teacherData = ref([])

const rules: FormRules = { name: [{ required: true, message: '请输入课程名称', trigger: 'blur' }] }

const choiceCourse = (row: any) => {
  request.post('/choice/add', { studentId: user.id, teacherId: row.teacherId, courseId: row.id }).then((res: any) => {
    if (res.data.code === '200') { ElMessage.success('选课成功') } else { ElMessage.error(res.data.msg) }
  })
}
const loadTeacher = () => {
  request.get('/teacher/selectAll').then((res: any) => {
    if (res.data.code === '200') { teacherData.value = res.data.data } else { ElMessage.error(res.data.msg) }
  })
}
const handleAdd = () => { form.value = {}; fromVisible.value = true }
const handleEdit = (row: any) => { form.value = JSON.parse(JSON.stringify(row)); fromVisible.value = true }
const save = () => {
  formRef.value?.validate((valid) => {
    if (valid) {
      request({ url: form.value.id ? '/course/update' : '/course/add', method: form.value.id ? 'PUT' : 'POST', data: form.value })
        .then((res: any) => {
          if (res.data.code === '200') { ElMessage.success('保存成功'); load(1); fromVisible.value = false }
          else { ElMessage.error(res.data.msg) }
        })
    }
  })
}
const del = (id: number) => {
  ElMessageBox.confirm('您确定删除吗？', '确认删除', { type: 'warning' }).then(() => {
    request.delete('/course/delete/' + id).then((res: any) => {
      if (res.data.code === '200') { ElMessage.success('操作成功'); load(1) } else { ElMessage.error(res.data.msg) }
    })
  }).catch(() => {})
}
const handleSelectionChange = (rows: any[]) => { ids.value = rows.map(v => v.id) }
const delBatch = () => {
  if (!ids.value.length) { ElMessage.warning('请选择数据'); return }
  ElMessageBox.confirm('您确定批量删除这些数据吗？', '确认删除', { type: 'warning' }).then(() => {
    request.delete('/course/delete/batch', { data: ids.value }).then((res: any) => {
      if (res.data.code === '200') { ElMessage.success('操作成功'); load(1) } else { ElMessage.error(res.data.msg) }
    })
  }).catch(() => {})
}
const load = (pNum?: number) => {
  if (pNum) pageNum.value = pNum
  request.get('/course/selectPage', { params: { pageNum: pageNum.value, pageSize: pageSize.value, name: name.value } })
    .then((res: any) => { tableData.value = res.data?.data?.list; total.value = res.data?.data?.total })
}
const reset = () => { name.value = null; load(1) }
const handleCurrentChange = (pNum: number) => load(pNum)

onMounted(() => { load(1); loadTeacher() })
</script>

<style scoped></style>

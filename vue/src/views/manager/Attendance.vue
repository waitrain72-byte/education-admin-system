<template>
  <div>
    <div class="search">
      <el-select v-model="courseId" placeholder="请选择课程" style="width: 200px">
        <el-option v-for="item in courseSearchData" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-button type="info" plain style="margin-left: 10px" @click="load(1)">查询</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">重置</el-button>
    </div>

    <div class="operation" v-if="user.role === 'TEACHER'">
      <el-button type="primary" plain @click="handleAdd">添加考勤</el-button>
      <el-button type="danger" plain @click="delBatch">批量删除</el-button>
    </div>

    <div class="table">
      <el-table :data="tableData" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" v-if="user.role === 'TEACHER'" />
        <el-table-column prop="id" label="序号" width="80" align="center" sortable />
        <el-table-column prop="courseName" label="课程名称" show-overflow-tooltip />
        <el-table-column prop="teacherName" label="授课教师" show-overflow-tooltip />
        <el-table-column prop="studentName" label="学生姓名" show-overflow-tooltip />
        <el-table-column prop="time" label="上课时间" show-overflow-tooltip />
        <el-table-column prop="status" label="考勤状态" show-overflow-tooltip />
        <el-table-column label="操作" width="180" align="center" v-if="user.role === 'TEACHER'">
          <template #default="scope">
            <el-button plain type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button plain type="danger" size="small" @click="del(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination background @current-change="handleCurrentChange" :current-page="pageNum"
          :page-sizes="[5, 10, 20]" :page-size="pageSize" layout="total, prev, pager, next" :total="total" />
      </div>
    </div>

    <el-dialog title="考勤信息" v-model="fromVisible" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form label-width="100px" style="padding-right: 50px" :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="courseId" label="选择课程">
          <el-select v-model="form.courseId" placeholder="请选择课程" style="width: 100%" @change="getStudent">
            <el-option v-for="item in courseData" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item prop="studentId" label="选择学生">
          <el-select v-model="studentId" placeholder="请选择学生" style="width: 100%">
            <el-option v-for="item in studentData" :key="item.studentId" :label="item.studentName" :value="item.studentId" />
          </el-select>
        </el-form-item>
        <el-form-item prop="time" label="上课时间">
          <el-date-picker style="width: 100%" v-model="form.time" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item prop="status" label="考勤状态">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="正常" value="正常" />
            <el-option label="迟到" value="迟到" />
            <el-option label="早退" value="早退" />
            <el-option label="缺勤" value="缺勤" />
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
const courseId = ref(null)
const fromVisible = ref(false)
const form = ref<Record<string, any>>({})
const formRef = ref<FormInstance>()
const ids = ref<number[]>([])
const courseData = ref([])
const courseSearchData = ref([])
const studentData = ref([])
const studentId = ref(null)

const rules: FormRules = {
  time: [{ required: true, message: '请输入上课时间', trigger: 'blur' }],
  status: [{ required: true, message: '请输入考勤状态', trigger: 'blur' }],
}

const loadCourseSearch = () => {
  if (user.role === 'STUDENT') {
    request.get('/choice/selectAll?studentId=' + user.id).then((res: any) => {
      if (res.data.code === '200') {
        res.data.data.forEach((item: any) => { item.id = item.courseId })
        courseSearchData.value = res.data.data
      }
    })
  } else {
    const url = user.role === 'ADMIN' ? '/course/selectAll' : '/course/selectAll?teacherId=' + user.id
    request.get(url).then((res: any) => {
      if (res.data.code === '200') { courseSearchData.value = res.data.data }
      else { ElMessage.error(res.data.msg) }
    })
  }
}
const loadCourseByTeacher = () => {
  request.get('/course/selectAll', { params: { teacherId: user.id } }).then((res: any) => {
    if (res.data.code === '200') { courseData.value = res.data.data }
    else { ElMessage.error(res.data.msg) }
  })
}
const getStudent = (cId: any) => {
  request.get('/choice/selectAll', { params: { courseId: cId } }).then((res: any) => {
    if (res.data.code === '200') { studentData.value = res.data.data; studentId.value = null }
    else { ElMessage.error(res.data.msg) }
  })
}
const getStudentEdit = (cId: any) => {
  request.get('/choice/selectAll', { params: { courseId: cId } }).then((res: any) => {
    if (res.data.code === '200') { studentData.value = res.data.data; studentId.value = form.value.studentId; fromVisible.value = true }
    else { ElMessage.error(res.data.msg) }
  })
}
const handleAdd = () => { form.value = { teacherId: user.id }; fromVisible.value = true; studentId.value = null }
const handleEdit = (row: any) => { form.value = JSON.parse(JSON.stringify(row)); getStudentEdit(form.value.courseId) }
const save = () => {
  formRef.value?.validate((valid) => {
    if (valid) {
      form.value.studentId = studentId.value
      request({ url: form.value.id ? '/attendance/update' : '/attendance/add', method: form.value.id ? 'PUT' : 'POST', data: form.value })
        .then((res: any) => {
          if (res.data.code === '200') { ElMessage.success('保存成功'); load(1); fromVisible.value = false }
          else { ElMessage.error(res.data.msg) }
        })
    }
  })
}
const del = (id: number) => {
  ElMessageBox.confirm('您确定删除吗？', '灵魂拷问', { type: 'warning' }).then(() => {
    request.delete('/attendance/delete/' + id).then((res: any) => {
      if (res.data.code === '200') { ElMessage.success('操作成功'); load(1) }
      else { ElMessage.error(res.data.msg) }
    })
  }).catch(() => {})
}
const handleSelectionChange = (rows: any[]) => { ids.value = rows.map(v => v.id) }
const delBatch = () => {
  if (!ids.value.length) { ElMessage.warning('请选择数据'); return }
  ElMessageBox.confirm('您确定批量删除这些数据吗？', '确认删除', { type: 'warning' }).then(() => {
    request.delete('/attendance/delete/batch', { data: ids.value }).then((res: any) => {
      if (res.data.code === '200') { ElMessage.success('操作成功'); load(1) }
      else { ElMessage.error(res.data.msg) }
    })
  }).catch(() => {})
}
const load = (pNum?: number) => {
  if (pNum) pageNum.value = pNum
  request.get('/attendance/selectPage', { params: { pageNum: pageNum.value, pageSize: pageSize.value, courseId: courseId.value } })
    .then((res: any) => { tableData.value = res.data?.data?.list; total.value = res.data?.data?.total })
}
const reset = () => { courseId.value = null; load(1) }
const handleCurrentChange = (pNum: number) => load(pNum)

onMounted(() => { load(1); loadCourseByTeacher(); loadCourseSearch() })
</script>

<style scoped></style>

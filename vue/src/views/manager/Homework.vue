<template>
  <div>
    <div class="search">
      <el-input placeholder="请输入作业说明" style="width: 200px" v-model="content" />
      <el-button type="info" plain style="margin-left: 10px" @click="load(1)">查询</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">重置</el-button>
    </div>
    <div class="operation" v-if="user.role === 'STUDENT'">
      <el-button type="primary" plain @click="handleAdd">作业提交</el-button>
    </div>
    <div class="table">
      <el-table :data="tableData" stripe>
        <el-table-column prop="id" label="序号" width="80" align="center" sortable />
        <el-table-column prop="content" label="作业说明" show-overflow-tooltip />
        <el-table-column prop="studentName" label="学生姓名" show-overflow-tooltip />
        <el-table-column prop="courseName" label="课程名称" />
        <el-table-column prop="teacherName" label="授课教师" />
        <el-table-column prop="file" label="作业文件">
          <template #default="scope">
            <el-button type="warning" size="small" @click="down(scope.row.file)">下载查看</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="作业打分" />
        <el-table-column prop="descr" label="审核说明" />
        <el-table-column label="操作" width="180" align="center" v-if="user.role !== 'ADMIN'">
          <template #default="scope">
            <el-button plain type="primary" @click="handleEdit(scope.row)" size="small" v-if="user.role === 'STUDENT' && !scope.row.score">编辑</el-button>
            <el-button plain type="danger" size="small" @click="del(scope.row.id)" v-if="user.role === 'STUDENT'">删除</el-button>
            <el-button plain type="primary" size="small" @click="handleCheck(scope.row)" v-if="user.role === 'TEACHER'">打分</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination background @current-change="handleCurrentChange" :current-page="pageNum"
          :page-sizes="[5, 10, 20]" :page-size="pageSize" layout="total, prev, pager, next" :total="total" />
      </div>
    </div>

    <el-dialog title="作业信息" v-model="fromVisible" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form label-width="100px" style="padding-right: 50px" :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="content" label="作业说明">
          <el-input type="textarea" :rows="4" v-model="form.content" autocomplete="off" />
        </el-form-item>
        <el-form-item prop="courseId" label="选择课程">
          <el-select v-model="form.courseId" placeholder="请选择课程" style="width: 100%">
            <el-option v-for="item in courseData" :key="item.courseId" :label="item.name" :value="item.courseId" />
          </el-select>
        </el-form-item>
        <el-form-item label="作业文件">
          <el-upload class="avatar-uploader" :action="baseUrl + '/files/upload'"
            :headers="{ token: user.token }" list-type="text" :on-success="handleFileSuccess">
            <el-button type="primary">上传文件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fromVisible = false">取 消</el-button>
        <el-button type="primary" @click="save">确 定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="作业审核" v-model="checkVisible" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form label-width="100px" style="padding-right: 50px" :model="form">
        <el-form-item prop="score" label="作业打分">
          <el-input v-model="form.score" autocomplete="off" />
        </el-form-item>
        <el-form-item prop="descr" label="打分说明">
          <el-input type="textarea" :rows="4" v-model="form.descr" autocomplete="off" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="checkVisible = false">取 消</el-button>
        <el-button type="primary" @click="check">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import request from '@/utils/request'

const baseUrl = import.meta.env.VITE_BASE_URL || 'http://localhost:9090'
const user = reactive(JSON.parse(localStorage.getItem('xm-user') || '{}'))
const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const content = ref(null)
const fromVisible = ref(false)
const checkVisible = ref(false)
const form = ref<Record<string, any>>({})
const formRef = ref<FormInstance>()
const courseData = ref([])

const rules: FormRules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'blur' }],
  content: [{ required: true, message: '请输入作业说明', trigger: 'blur' }],
}

const loadCourse = () => {
  request.get('/choice/selectAll?studentId=' + user.id).then((res: any) => {
    if (res.data.code === '200') { courseData.value = res.data.data } else { ElMessage.error(res.data.msg) }
  })
}
const handleAdd = () => { form.value = { studentId: user.id }; fromVisible.value = true }
const handleEdit = (row: any) => { form.value = JSON.parse(JSON.stringify(row)); form.value.status = '待审核'; form.value.descr = ''; fromVisible.value = true }
const handleCheck = (row: any) => { form.value = JSON.parse(JSON.stringify(row)); checkVisible.value = true }
const check = () => {
  request.put('/homework/update', form.value).then((res: any) => {
    if (res.data.code === '200') { ElMessage.success('操作成功'); load(1); checkVisible.value = false }
    else { ElMessage.error(res.data.msg) }
  })
}
const save = () => {
  formRef.value?.validate((valid) => {
    if (valid) {
      request({ url: form.value.id ? '/homework/update' : '/homework/add', method: form.value.id ? 'PUT' : 'POST', data: form.value })
        .then((res: any) => {
          if (res.data.code === '200') { ElMessage.success('保存成功'); load(1); fromVisible.value = false }
          else { ElMessage.error(res.data.msg) }
        })
    }
  })
}
const del = (id: number) => {
  ElMessageBox.confirm('您确定删除作业吗？作业会跟你的平时分强挂钩哦！！', '灵魂拷问', { type: 'warning' }).then(() => {
    request.delete('/homework/delete/' + id).then((res: any) => {
      if (res.data.code === '200') { ElMessage.success('操作成功'); load(1) } else { ElMessage.error(res.data.msg) }
    })
  }).catch(() => {})
}
const load = (pNum?: number) => {
  if (pNum) pageNum.value = pNum
  request.get('/homework/selectPage', { params: { pageNum: pageNum.value, pageSize: pageSize.value, content: content.value } })
    .then((res: any) => { tableData.value = res.data?.data?.list; total.value = res.data?.data?.total })
}
const reset = () => { content.value = null; load(1) }
const handleCurrentChange = (pNum: number) => load(pNum)
const handleFileSuccess = (res: any) => { form.value.file = res.data }
const down = (url: string) => { location.href = url }

onMounted(() => { load(1); loadCourse() })
</script>

<style scoped></style>

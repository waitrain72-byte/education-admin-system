<template>
  <div>
    <div class="search">
      <el-input v-model="content" placeholder="请输入作业说明" style="width: 200px" />
      <el-button type="info" plain style="margin-left: 10px" @click="load(1)">查询</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">重置</el-button>
    </div>
    <div v-if="user.role === 'STUDENT'" class="operation">
      <el-button type="primary" plain @click="handleAdd">作业提交</el-button>
    </div>

    <CrudTable
        :data="tableData"
        :columns="columns"
        :page-num="pageNum"
        :page-size="pageSize"
        :total="total"
        :loading="loading"
        :show-actions="user.role !== 'ADMIN'"
        @page-change="load"
    >
      <template #file="{ row }">
        <el-button type="warning" size="small" @click="down(row.file)">下载查看</el-button>
      </template>
      <template #actions="{ row }">
        <el-button
v-if="user.role === 'STUDENT' && !row.score" link type="primary" size="small"
                   @click="handleEdit(row)">编辑</el-button>
        <el-button
v-if="user.role === 'STUDENT'" link type="danger" size="small"
                   @click="del(row.id)">删除</el-button>
        <el-button
v-if="user.role === 'TEACHER'" link type="primary" size="small"
                   @click="handleCheck(row)">打分</el-button>
      </template>
    </CrudTable>

    <el-dialog v-model="formVisible" title="作业信息" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" label-width="100px" style="padding-right: 50px" :model="form" :rules="rules">
        <el-form-item prop="content" label="作业说明">
          <el-input v-model="form.content" type="textarea" :rows="4" autocomplete="off" />
        </el-form-item>
        <el-form-item prop="courseId" label="选择课程">
          <el-select v-model="form.courseId" placeholder="请选择课程" style="width: 100%">
            <el-option v-for="item in courseData" :key="item.courseId" :label="item.name" :value="item.courseId" />
          </el-select>
        </el-form-item>
        <el-form-item label="作业文件">
          <el-upload
class="avatar-uploader" :action="baseUrl + '/files/upload'"
                     :headers="{ token: user.token }" list-type="text" :on-success="handleFileSuccess">
            <el-button type="primary">上传文件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取 消</el-button>
        <el-button type="primary" @click="save">确 定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="checkVisible" title="作业审核" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form label-width="100px" style="padding-right: 50px" :model="form">
        <el-form-item prop="score" label="作业打分">
          <el-input v-model="form.score" autocomplete="off" />
        </el-form-item>
        <el-form-item prop="descr" label="打分说明">
          <el-input v-model="form.descr" type="textarea" :rows="4" autocomplete="off" />
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
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUser } from '@/components/useUser.ts'
import { useCrud } from '@/composables/useCrud'
import CrudTable, { type CrudColumn } from '@/components/CrudTable.vue'

const baseUrl = import.meta.env.VITE_BASE_URL || 'http://localhost:9090'
const { user } = useUser()
const content = ref('')
const checkVisible = ref(false)
const courseData = ref<any[]>([])

const {
  tableData, pageNum, pageSize, total, loading,
  formVisible, form, formRef, rules,
  load, save, del,
} = useCrud({
  url: '/homework',
  deleteConfirmMessage: '您确定删除作业吗？作业会跟你的平时分强挂钩哦！！',
  rules: {
    courseId: [{ required: true, message: '请选择课程', trigger: 'blur' }],
    content: [{ required: true, message: '请输入作业说明', trigger: 'blur' }],
  },
  getParams: () => ({ content: content.value }),
})

const columns: CrudColumn[] = [
  { prop: 'id', label: '序号', width: 80, align: 'center', sortable: true },
  { prop: 'content', label: '作业说明', showOverflowTooltip: true },
  { prop: 'studentName', label: '学生姓名', showOverflowTooltip: true },
  { prop: 'courseName', label: '课程名称' },
  { prop: 'teacherName', label: '授课教师' },
  { prop: 'file', label: '作业文件' },
  { prop: 'score', label: '作业打分' },
  { prop: 'descr', label: '审核说明' },
]

const loadCourse = () => {
  request.get('/choice/selectAll?studentId=' + user.value.id).then((res: any) => {
    if (res.data.code === '200') {
      courseData.value = res.data.data
    } else {
      ElMessage.error(res.data.msg)
    }
  })
}

const handleAdd = () => {
  form.value = { studentId: user.value.id }
  formVisible.value = true
}

const handleEdit = (row: any) => {
  form.value = JSON.parse(JSON.stringify(row))
  form.value.status = '待审核'
  form.value.descr = ''
  formVisible.value = true
}

const handleCheck = (row: any) => {
  form.value = JSON.parse(JSON.stringify(row))
  checkVisible.value = true
}

const check = () => {
  request.put('/homework/update', form.value).then((res: any) => {
    if (res.data.code === '200') {
      ElMessage.success('操作成功')
      load(1)
      checkVisible.value = false
    } else {
      ElMessage.error(res.data.msg)
    }
  })
}

const handleFileSuccess = (res: any) => {
  form.value.file = res.data
}

const down = (url: string) => {
  location.href = url
}

const reset = () => {
  content.value = ''
  load(1)
}

onMounted(() => {
  load(1)
  loadCourse()
})
</script>

<style scoped></style>

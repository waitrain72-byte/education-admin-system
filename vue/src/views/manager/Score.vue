<template>
  <div>
    <div class="search">
      <el-select v-model="courseId" placeholder="请选择课程" style="width: 200px">
        <el-option v-for="item in courseData" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-button type="info" plain style="margin-left: 10px" @click="load(1)">查询</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">重置</el-button>
    </div>
    <div v-if="user.role === 'TEACHER'" class="operation">
      <el-button type="primary" plain @click="handleAdd">新增</el-button>
      <el-button type="danger" plain @click="delBatch">批量删除</el-button>
    </div>

    <CrudTable
        :data="tableData"
        :columns="columns"
        :page-num="pageNum"
        :page-size="pageSize"
        :total="total"
        :loading="loading"
        :selectable="user.role !== 'STUDENT'"
        :show-actions="user.role === 'TEACHER'"
        @selection-change="handleSelectionChange"
        @page-change="load"
    >
      <template #actions="{ row }">
        <el-button plain type="danger" size="small" @click="del(row.id)">删除</el-button>
      </template>
    </CrudTable>

    <el-dialog v-model="formVisible" title="信息" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" label-width="100px" style="padding-right: 50px" :model="form" :rules="rules">
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
        <el-form-item prop="ordinaryScore" label="平时分">
          <el-input v-model="form.ordinaryScore" autocomplete="off" />
        </el-form-item>
        <el-form-item prop="examScore" label="期末分">
          <el-input v-model="form.examScore" autocomplete="off" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取 消</el-button>
        <el-button type="primary" @click="save">确 定</el-button>
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

const { user } = useUser()
const courseId = ref('')
const courseData = ref<any[]>([])
const studentData = ref<any[]>([])
const studentId = ref(null)

const {
  tableData, pageNum, pageSize, total, loading,
  formVisible, form, formRef, rules,
  load, save, del, delBatch,
  handleSelectionChange,
} = useCrud({
  url: '/score',
  deleteConfirmMessage: '您确定删除吗？删除后该学生的学分也会相应的减少，您需要重新再次录入？',
  rules: {
    ordinaryScore: [{ required: true, message: '请输入平时分', trigger: 'blur' }],
    examScore: [{ required: true, message: '请输入期末分', trigger: 'blur' }],
  },
  getParams: () => ({ courseId: courseId.value }),
  beforeSave: (formData) => {
    formData.studentId = studentId.value
  },
})

const columns: CrudColumn[] = [
  { prop: 'id', label: '序号', width: 80, align: 'center', sortable: true },
  { prop: 'studentName', label: '学生姓名', showOverflowTooltip: true },
  { prop: 'courseName', label: '课程名称', showOverflowTooltip: true },
  { prop: 'teacherName', label: '授课教师', showOverflowTooltip: true },
  { prop: 'ordinaryScore', label: '平时分', showOverflowTooltip: true },
  { prop: 'examScore', label: '期末分', showOverflowTooltip: true },
  { prop: 'score', label: '总成绩', showOverflowTooltip: true },
]

const loadCourse = () => {
  request.get('/course/selectAll', { params: { teacherId: user.value.id } }).then((res: any) => {
    if (res.data.code === '200') {
      courseData.value = res.data.data
    } else {
      ElMessage.error(res.data.msg)
    }
  })
}

const getStudent = (cId: any) => {
  request.get('/choice/selectAll', { params: { courseId: cId } }).then((res: any) => {
    if (res.data.code === '200') {
      studentData.value = res.data.data
      studentId.value = null
    } else {
      ElMessage.error(res.data.msg)
    }
  })
}

const handleAdd = () => {
  form.value = { teacherId: user.value.id }
  studentId.value = null
  formVisible.value = true
}

const reset = () => {
  courseId.value = ''
  load(1)
}

onMounted(() => {
  load(1)
  loadCourse()
})
</script>

<style scoped></style>

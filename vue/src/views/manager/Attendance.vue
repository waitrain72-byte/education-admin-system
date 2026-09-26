<template>
  <div>
    <div class="search">
      <el-select v-model="courseId" :placeholder="$t('pages.attendance.coursePlaceholder')" style="width: 200px">
        <el-option v-for="item in courseSearchData" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-button type="info" plain style="margin-left: 10px" @click="load(1)">{{ $t('common.search') }}</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">{{ $t('common.reset') }}</el-button>
    </div>

    <div v-if="user.role === 'TEACHER'" class="operation">
      <el-button type="primary" plain @click="handleAdd">{{ $t('pages.attendance.addAttendance') }}</el-button>
      <el-button type="danger" plain @click="delBatch">{{ $t('common.batchDelete') }}</el-button>
    </div>

    <CrudTable
        :data="tableData"
        :columns="columns"
        :page-num="pageNum"
        :page-size="pageSize"
        :total="total"
        :loading="loading"
        :selectable="user.role === 'TEACHER'"
        :show-actions="user.role === 'TEACHER'"
        @selection-change="handleSelectionChange"
        @page-change="load"
    >
      <template #actions="{ row }">
        <el-button link type="primary" size="small" @click="handleEdit(row)">{{ $t('common.edit') }}</el-button>
        <el-button link type="danger" size="small" @click="del(row.id)">{{ $t('common.delete') }}</el-button>
      </template>
    </CrudTable>

    <el-dialog v-model="formVisible" :title="$t('pages.attendance.dialogTitle')" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" label-width="100px" style="padding-right: 50px" :model="form" :rules="rules">
        <el-form-item prop="courseId" :label="$t('pages.attendance.selectCourse')">
          <el-select v-model="form.courseId" :placeholder="$t('pages.attendance.coursePlaceholder')" style="width: 100%" @change="getStudent">
            <el-option v-for="item in courseData" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item prop="studentId" :label="$t('pages.attendance.selectStudent')">
          <el-select v-model="studentId" :placeholder="$t('pages.attendance.studentPlaceholder')" style="width: 100%">
            <el-option v-for="item in studentData" :key="item.studentId" :label="item.studentName" :value="item.studentId" />
          </el-select>
        </el-form-item>
        <el-form-item prop="time" :label="$t('pages.attendance.timeLabel')">
          <el-date-picker v-model="form.time" style="width: 100%" type="date" value-format="YYYY-MM-DD" :placeholder="$t('pages.attendance.datePlaceholder')" />
        </el-form-item>
        <el-form-item prop="status" :label="$t('pages.attendance.statusLabel')">
          <el-select v-model="form.status" :placeholder="$t('pages.attendance.statusPlaceholder')" style="width: 100%">
            <el-option :label="$t('pages.attendance.statusNormal')" value="正常" />
            <el-option :label="$t('pages.attendance.statusLate')" value="迟到" />
            <el-option :label="$t('pages.attendance.statusEarlyLeave')" value="早退" />
            <el-option :label="$t('pages.attendance.statusAbsent')" value="缺勤" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="save">{{ $t('common.ok') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Attendance' })

import { ref, computed, onMounted } from 'vue'
import type { FormRules } from 'element-plus'
import request from '@/utils/request'
import { t } from '@/i18n'
import { useUser } from '@/components/useUser.ts'
import { useCrud } from '@/composables/useCrud'
import CrudTable, { type CrudColumn } from '@/components/CrudTable.vue'

const { user } = useUser()
const courseId = ref('')
const courseData = ref<any[]>([])
const courseSearchData = ref<any[]>([])
const studentData = ref<any[]>([])
const studentId = ref(null)

const {
  tableData, pageNum, pageSize, total, loading,
  formVisible, form, formRef, rules,
  load, save, del, delBatch,
  handleSelectionChange,
} = useCrud({
  url: '/attendance',
  rules: computed<FormRules>(() => ({
    time: [{ required: true, message: t('pages.attendance.ruleTimeRequired'), trigger: 'blur' }],
    status: [{ required: true, message: t('pages.attendance.ruleStatusRequired'), trigger: 'blur' }],
  })),
  getParams: () => ({ courseId: courseId.value }),
  beforeSave: (formData) => {
    formData.studentId = studentId.value
  },
})

const columns = computed<CrudColumn[]>(() => [
  { prop: 'id', label: t('pages.attendance.id'), width: 80, align: 'center', sortable: true },
  { prop: 'courseName', label: t('pages.attendance.courseName'), showOverflowTooltip: true },
  { prop: 'teacherName', label: t('pages.attendance.teacherName'), showOverflowTooltip: true },
  { prop: 'studentName', label: t('pages.attendance.studentName'), showOverflowTooltip: true },
  { prop: 'time', label: t('pages.attendance.timeLabel'), showOverflowTooltip: true },
  { prop: 'status', label: t('pages.attendance.statusLabel'), showOverflowTooltip: true },
])

const loadCourseSearch = async () => {
  try {
    if (user.value.role === 'STUDENT') {
      // 学生端拿到的是选课记录，下拉要以课程 id 作为值
      const choices = await request.get<any[]>('/choice/selectAll', { params: { studentId: user.value.id } })
      choices.forEach((item) => {
        item.id = item.courseId
      })
      courseSearchData.value = choices
    } else {
      const params = user.value.role === 'ADMIN' ? {} : { teacherId: user.value.id }
      courseSearchData.value = await request.get<any[]>('/course/selectAll', { params })
    }
  } catch {
    // 错误提示已由 axios 拦截器统一处理
  }
}

const loadCourseByTeacher = async () => {
  try {
    courseData.value = await request.get<any[]>('/course/selectAll', { params: { teacherId: user.value.id } })
  } catch {
    // 错误提示已由 axios 拦截器统一处理
  }
}

/**
 * 加载某门课的学生名单。
 * openDialog 为 true 时用于「编辑」：回填已选学生并打开弹窗；否则用于「新增」时切换课程，清空已选学生。
 */
const loadStudents = async (cId: any, openDialog = false) => {
  try {
    studentData.value = await request.get<any[]>('/choice/selectAll', { params: { courseId: cId } })
    studentId.value = openDialog ? form.value.studentId : null
    if (openDialog) {
      formVisible.value = true
    }
  } catch {
    // 错误提示已由 axios 拦截器统一处理
  }
}

const getStudent = (cId: any) => loadStudents(cId)

const handleAdd = () => {
  form.value = { teacherId: user.value.id }
  studentId.value = null
  formVisible.value = true
}

const handleEdit = (row: any) => {
  form.value = JSON.parse(JSON.stringify(row))
  loadStudents(form.value.courseId, true)
}

const reset = () => {
  courseId.value = ''
  load(1)
}

onMounted(() => {
  load(1)
  // 新增 / 编辑只对教师开放：表单的课程下拉取本人任教的课程，搜索栏复用同一份，只取一次；
  // 其他角色没有表单，只加载搜索栏的课程（不能拿管理员 / 学生的 id 当教师 id 去查）
  if (user.value.role === 'TEACHER') {
    loadCourseByTeacher().then(() => {
      courseSearchData.value = courseData.value
    })
  } else {
    loadCourseSearch()
  }
})
</script>

<style scoped></style>

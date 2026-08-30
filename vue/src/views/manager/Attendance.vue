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
import { ElMessage, type FormRules } from 'element-plus'
import request from '@/utils/request'
import { apiMessage, t } from '@/i18n'
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

const loadCourseSearch = () => {
  if (user.value.role === 'STUDENT') {
    request.get('/choice/selectAll?studentId=' + user.value.id).then((res: any) => {
      if (res.data.code === '200') {
        res.data.data.forEach((item: any) => {
          item.id = item.courseId
        })
        courseSearchData.value = res.data.data
      }
    })
  } else {
    const url = user.value.role === 'ADMIN' ? '/course/selectAll' : '/course/selectAll?teacherId=' + user.value.id
    request.get(url).then((res: any) => {
      if (res.data.code === '200') {
        courseSearchData.value = res.data.data
      } else {
        ElMessage.error(apiMessage(res.data))
      }
    })
  }
}

const loadCourseByTeacher = () => {
  request.get('/course/selectAll', { params: { teacherId: user.value.id } }).then((res: any) => {
    if (res.data.code === '200') {
      courseData.value = res.data.data
    } else {
      ElMessage.error(apiMessage(res.data))
    }
  })
}

const getStudent = (cId: any) => {
  request.get('/choice/selectAll', { params: { courseId: cId } }).then((res: any) => {
    if (res.data.code === '200') {
      studentData.value = res.data.data
      studentId.value = null
    } else {
      ElMessage.error(apiMessage(res.data))
    }
  })
}

const getStudentEdit = (cId: any) => {
  request.get('/choice/selectAll', { params: { courseId: cId } }).then((res: any) => {
    if (res.data.code === '200') {
      studentData.value = res.data.data
      studentId.value = form.value.studentId
      formVisible.value = true
    } else {
      ElMessage.error(apiMessage(res.data))
    }
  })
}

const handleAdd = () => {
  form.value = { teacherId: user.value.id }
  studentId.value = null
  formVisible.value = true
}

const handleEdit = (row: any) => {
  form.value = JSON.parse(JSON.stringify(row))
  getStudentEdit(form.value.courseId)
}

const reset = () => {
  courseId.value = ''
  load(1)
}

onMounted(() => {
  load(1)
  loadCourseByTeacher()
  loadCourseSearch()
})
</script>

<style scoped></style>

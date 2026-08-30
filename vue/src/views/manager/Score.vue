<template>
  <div>
    <div class="search">
      <el-select v-model="courseId" :placeholder="$t('pages.score.coursePlaceholder')" style="width: 200px">
        <el-option v-for="item in courseData" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-button type="info" plain style="margin-left: 10px" @click="load(1)">{{ $t('common.search') }}</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">{{ $t('common.reset') }}</el-button>
    </div>
    <div v-if="user.role !== 'STUDENT'" class="operation">
      <el-button v-if="user.role === 'TEACHER'" type="primary" plain @click="handleAdd">{{ $t('common.add') }}</el-button>
      <el-button v-if="user.role === 'TEACHER'" type="danger" plain @click="delBatch">{{ $t('common.batchDelete') }}</el-button>
      <el-button type="success" plain @click="exportExcel">{{ $t('pages.score.exportExcel') }}</el-button>
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
        <el-button link type="danger" size="small" @click="del(row.id)">{{ $t('common.delete') }}</el-button>
      </template>
    </CrudTable>

    <el-dialog v-model="formVisible" :title="$t('pages.score.dialogTitle')" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" label-width="100px" style="padding-right: 50px" :model="form" :rules="rules">
        <el-form-item prop="courseId" :label="$t('pages.score.selectCourse')">
          <el-select v-model="form.courseId" :placeholder="$t('pages.score.coursePlaceholder')" style="width: 100%" @change="getStudent">
            <el-option v-for="item in courseData" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item prop="studentId" :label="$t('pages.score.selectStudent')">
          <el-select v-model="studentId" :placeholder="$t('pages.score.studentPlaceholder')" style="width: 100%">
            <el-option v-for="item in studentData" :key="item.studentId" :label="item.studentName" :value="item.studentId" />
          </el-select>
        </el-form-item>
        <el-form-item prop="ordinaryScore" :label="$t('pages.score.ordinaryScore')">
          <el-input v-model="form.ordinaryScore" autocomplete="off" />
        </el-form-item>
        <el-form-item prop="examScore" :label="$t('pages.score.examScore')">
          <el-input v-model="form.examScore" autocomplete="off" />
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
defineOptions({ name: 'Score' })

import { ref, computed, onMounted } from 'vue'
import { ElMessage, type FormRules } from 'element-plus'
import request from '@/utils/request'
import { apiMessage, t } from '@/i18n'
import { useUser } from '@/components/useUser.ts'
import { useCrud } from '@/composables/useCrud'
import CrudTable, { type CrudColumn } from '@/components/CrudTable.vue'

// 导出全部成绩为 Excel
const exportExcel = () => {
  request.get('/score/export', { responseType: 'blob' }).then((res: any) => {
    const url = URL.createObjectURL(res.data)
    const link = document.createElement('a')
    link.href = url
    link.download = '成绩列表.xlsx'
    link.click()
    URL.revokeObjectURL(url)
  })
}

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
  deleteConfirmMessage: t('pages.score.deleteConfirm'),
  rules: computed<FormRules>(() => ({
    ordinaryScore: [{ required: true, message: t('pages.score.ruleOrdinaryRequired'), trigger: 'blur' }],
    examScore: [{ required: true, message: t('pages.score.ruleExamRequired'), trigger: 'blur' }],
  })),
  getParams: () => ({ courseId: courseId.value }),
  beforeSave: (formData) => {
    formData.studentId = studentId.value
  },
})

const columns = computed<CrudColumn[]>(() => [
  { prop: 'id', label: t('pages.score.id'), width: 80, align: 'center', sortable: true },
  { prop: 'studentName', label: t('pages.score.studentName'), showOverflowTooltip: true },
  { prop: 'courseName', label: t('pages.score.courseName'), showOverflowTooltip: true },
  { prop: 'teacherName', label: t('pages.score.teacherName'), showOverflowTooltip: true },
  { prop: 'ordinaryScore', label: t('pages.score.ordinaryScore'), showOverflowTooltip: true },
  { prop: 'examScore', label: t('pages.score.examScore'), showOverflowTooltip: true },
  { prop: 'score', label: t('pages.score.totalScore'), showOverflowTooltip: true },
])

const loadCourse = () => {
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

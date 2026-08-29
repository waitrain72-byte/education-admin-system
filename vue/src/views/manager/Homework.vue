<template>
  <div>
    <div class="search">
      <el-input v-model="content" :placeholder="$t('pages.homework.contentPlaceholder')" style="width: 200px" />
      <el-button type="info" plain style="margin-left: 10px" @click="load(1)">{{ $t('common.search') }}</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">{{ $t('common.reset') }}</el-button>
    </div>
    <div v-if="user.role === 'STUDENT'" class="operation">
      <el-button type="primary" plain @click="handleAdd">{{ $t('pages.homework.submitHomework') }}</el-button>
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
        <el-button type="warning" size="small" @click="down(row.file)">{{ $t('pages.homework.download') }}</el-button>
      </template>
      <template #actions="{ row }">
        <el-button
v-if="user.role === 'STUDENT' && !row.score" link type="primary" size="small"
                   @click="handleEdit(row)">{{ $t('common.edit') }}</el-button>
        <el-button
v-if="user.role === 'STUDENT'" link type="danger" size="small"
                   @click="del(row.id)">{{ $t('common.delete') }}</el-button>
        <el-button
v-if="user.role === 'TEACHER'" link type="primary" size="small"
                   @click="handleCheck(row)">{{ $t('pages.homework.grade') }}</el-button>
      </template>
    </CrudTable>

    <el-dialog v-model="formVisible" :title="$t('pages.homework.dialogTitle')" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" label-width="100px" style="padding-right: 50px" :model="form" :rules="rules">
        <el-form-item prop="content" :label="$t('pages.homework.contentLabel')">
          <el-input v-model="form.content" type="textarea" :rows="4" autocomplete="off" />
        </el-form-item>
        <el-form-item prop="courseId" :label="$t('pages.homework.selectCourse')">
          <el-select v-model="form.courseId" :placeholder="$t('pages.homework.coursePlaceholder')" style="width: 100%">
            <el-option v-for="item in courseData" :key="item.courseId" :label="item.name" :value="item.courseId" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('pages.homework.fileLabel')">
          <el-upload
class="avatar-uploader" :action="baseUrl + '/files/upload'"
                     :headers="{ token: user.token }" list-type="text" :on-success="handleFileSuccess">
            <el-button type="primary">{{ $t('pages.homework.uploadFile') }}</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="save">{{ $t('common.ok') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="checkVisible" :title="$t('pages.homework.checkDialogTitle')" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form label-width="100px" style="padding-right: 50px" :model="form">
        <el-form-item prop="score" :label="$t('pages.homework.scoreLabel')">
          <el-input v-model="form.score" autocomplete="off" />
        </el-form-item>
        <el-form-item prop="descr" :label="$t('pages.homework.descrLabel')">
          <el-input v-model="form.descr" type="textarea" :rows="4" autocomplete="off" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="checkVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="check">{{ $t('common.ok') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, type FormRules } from 'element-plus'
import request from '@/utils/request'
import { apiMessage, t } from '@/i18n'
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
  deleteConfirmMessage: t('pages.homework.deleteConfirm'),
  rules: computed<FormRules>(() => ({
    courseId: [{ required: true, message: t('pages.homework.ruleCourseRequired'), trigger: 'blur' }],
    content: [{ required: true, message: t('pages.homework.ruleContentRequired'), trigger: 'blur' }],
  })),
  getParams: () => ({ content: content.value }),
})

const columns = computed<CrudColumn[]>(() => [
  { prop: 'id', label: t('pages.homework.id'), width: 80, align: 'center', sortable: true },
  { prop: 'content', label: t('pages.homework.contentLabel'), showOverflowTooltip: true },
  { prop: 'studentName', label: t('pages.homework.studentName'), showOverflowTooltip: true },
  { prop: 'courseName', label: t('pages.homework.courseName') },
  { prop: 'teacherName', label: t('pages.homework.teacherName') },
  { prop: 'file', label: t('pages.homework.fileLabel') },
  { prop: 'score', label: t('pages.homework.scoreLabel') },
  { prop: 'descr', label: t('pages.homework.descrLabel') },
])

const loadCourse = () => {
  request.get('/choice/selectAll?studentId=' + user.value.id).then((res: any) => {
    if (res.data.code === '200') {
      courseData.value = res.data.data
    } else {
      ElMessage.error(apiMessage(res.data))
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
      ElMessage.success(t('common.operationSuccess'))
      load(1)
      checkVisible.value = false
    } else {
      ElMessage.error(apiMessage(res.data))
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

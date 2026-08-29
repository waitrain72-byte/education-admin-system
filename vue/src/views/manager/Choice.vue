<template>
  <div>
    <CrudTable
        :data="tableData"
        :columns="columns"
        :page-num="pageNum"
        :page-size="pageSize"
        :total="total"
        :loading="loading"
        :show-actions="user.role === 'STUDENT'"
        @page-change="load"
    >
      <template #actions="{ row }">
        <el-button link type="danger" size="small" :disabled="row.status !== '未开课'" @click="del(row.id)">{{ $t('pages.choice.cancelChoice') }}</el-button>
        <el-button link type="primary" size="small" :disabled="row.status !== '已结课'" @click="initComment(row)">{{ $t('pages.choice.comment') }}</el-button>
      </template>
    </CrudTable>

    <el-dialog v-model="formVisible" :title="$t('pages.choice.dialogTitle')" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" label-width="100px" style="padding-right: 50px" :model="form" :rules="rules">
        <el-form-item prop="content" :label="$t('pages.choice.contentLabel')">
          <el-input v-model="form.content" type="textarea" :rows="5" autocomplete="off" />
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
import { ref, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import request from '@/utils/request'
import { apiMessage, t } from '@/i18n'
import { useUser } from '@/components/useUser.ts'
import { useCrud } from '@/composables/useCrud'
import CrudTable, { type CrudColumn } from '@/components/CrudTable.vue'

const { user } = useUser()

// 分页/删除复用通用 CRUD，取消选课的确认文案自定义
const { tableData, pageNum, pageSize, total, loading, load, del } = useCrud({
  url: '/choice',
  deleteConfirmMessage: t('pages.choice.deleteConfirm'),
})

const formVisible = ref(false)
const form = ref<Record<string, any>>({})
const formRef = ref<FormInstance>()

const rules = computed<FormRules>(() => ({
  content: [{ required: true, message: t('pages.choice.ruleContentRequired'), trigger: 'blur' }],
}))

const columns = computed<CrudColumn[]>(() => [
  { prop: 'id', label: t('pages.choice.id'), width: 80, align: 'center', sortable: true },
  { prop: 'name', label: t('pages.choice.courseName'), showOverflowTooltip: true },
  { prop: 'type', label: t('pages.choice.courseType'), showOverflowTooltip: true },
  { prop: 'teacherName', label: t('pages.choice.teacherName'), showOverflowTooltip: true },
  { prop: 'score', label: t('pages.choice.credit'), showOverflowTooltip: true },
  { prop: 'num', label: t('pages.choice.studentCount'), showOverflowTooltip: true },
  { prop: 'room', label: t('pages.choice.room'), showOverflowTooltip: true },
  { prop: 'week', label: t('pages.choice.week'), showOverflowTooltip: true },
  { prop: 'segment', label: t('pages.choice.segment'), showOverflowTooltip: true },
  { prop: 'status', label: t('pages.choice.courseStatus'), showOverflowTooltip: true },
  { prop: 'studentName', label: t('pages.choice.studentName'), showOverflowTooltip: true },
])

const initComment = (row: any) => {
  form.value = JSON.parse(JSON.stringify(row))
  formVisible.value = true
}

// 评教提交逻辑与普通 CRUD 不同（固定写 /comment/add），保持页面内实现
const save = () => {
  formRef.value?.validate((valid) => {
    if (!valid) return
    const data = {
      name: form.value.name,
      teacher: form.value.teacherName,
      student: user.value.name,
      content: form.value.content,
    }
    request.post('/comment/add', data).then((res: any) => {
      if (res.data.code === '200') {
        ElMessage.success(t('pages.choice.commentSuccess'))
        formVisible.value = false
      } else {
        ElMessage.error(apiMessage(res.data))
      }
    })
  })
}

onMounted(() => load(1))
</script>

<style scoped></style>

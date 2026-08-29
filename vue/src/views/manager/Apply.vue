<template>
  <div>
    <div class="search">
      <el-input v-model="content" :placeholder="$t('pages.apply.contentPlaceholder')" style="width: 200px" />
      <el-select v-model="status" :placeholder="$t('pages.apply.statusPlaceholder')" style="width: 200px; margin-left: 5px">
        <el-option :label="$t('pages.apply.statusPending')" value="待审核" />
        <el-option :label="$t('pages.apply.statusApproved')" value="审核通过" />
        <el-option :label="$t('pages.apply.statusRejected')" value="审核不通过" />
      </el-select>
      <el-button type="info" plain style="margin-left: 10px" @click="load(1)">{{ $t('common.search') }}</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">{{ $t('common.reset') }}</el-button>
    </div>

    <div v-if="user.role === 'STUDENT'" class="operation">
      <el-button type="primary" plain @click="handleAdd">{{ $t('pages.apply.applyLeave') }}</el-button>
    </div>

    <CrudTable
        :data="tableData"
        :columns="columns"
        :page-num="pageNum"
        :page-size="pageSize"
        :total="total"
        :loading="loading"
        @page-change="load"
    >
      <template #actions="{ row }">
        <el-button
v-if="user.role === 'STUDENT' && row.status !== '审核通过'" link type="primary" size="small"
                   @click="handleEdit(row)">{{ $t('common.edit') }}</el-button>
        <el-button
v-if="user.role === 'ADMIN' && row.status === '待审核'" link type="primary" size="small"
                   @click="handleCheck(row)">{{ $t('pages.apply.review') }}</el-button>
        <el-button
v-if="user.role === 'STUDENT' && row.status === '待审核'" link type="danger" size="small"
                   @click="del(row.id)">{{ $t('pages.apply.withdraw') }}</el-button>
      </template>
    </CrudTable>

    <el-dialog v-model="formVisible" :title="$t('pages.apply.dialogTitle')" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" label-width="100px" style="padding-right: 50px" :model="form" :rules="rules">
        <el-form-item prop="content" :label="$t('pages.apply.contentLabel')">
          <el-input v-model="form.content" type="textarea" :rows="4" autocomplete="off" />
        </el-form-item>
        <el-form-item prop="time" :label="$t('pages.apply.timeLabel')">
          <el-date-picker
v-model="form.time" style="width: 100%" value-format="YYYY-MM-DD"
                          type="date" :placeholder="$t('pages.apply.datePlaceholder')" />
        </el-form-item>
        <el-form-item prop="day" :label="$t('pages.apply.dayLabel')">
          <el-input v-model="form.day" autocomplete="off" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="save">{{ $t('common.ok') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="checkVisible" :title="$t('pages.apply.checkDialogTitle')" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form label-width="100px" style="padding-right: 50px" :model="form">
        <el-form-item prop="status" :label="$t('pages.apply.statusLabel')">
          <el-select v-model="form.status" :placeholder="$t('pages.apply.statusPlaceholder')" style="width: 100%">
            <el-option :label="$t('pages.apply.statusPending')" value="待审核" />
            <el-option :label="$t('pages.apply.statusApproved')" value="审核通过" />
            <el-option :label="$t('pages.apply.statusRejected')" value="审核不通过" />
          </el-select>
        </el-form-item>
        <el-form-item prop="descr" :label="$t('pages.apply.descrLabel')">
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

const { user } = useUser()
const content = ref('')
const status = ref('')
const checkVisible = ref(false)

const {
  tableData, pageNum, pageSize, total, loading,
  formVisible, form, formRef, rules,
  load, save, del,
} = useCrud({
  url: '/apply',
  deleteConfirmMessage: t('pages.apply.deleteConfirm'),
  rules: computed<FormRules>(() => ({
    time: [{ required: true, message: t('pages.apply.ruleTimeRequired'), trigger: 'blur' }],
    content: [{ required: true, message: t('pages.apply.ruleContentRequired'), trigger: 'blur' }],
    day: [{ required: true, message: t('pages.apply.ruleDayRequired'), trigger: 'blur' }],
  })),
  getParams: () => ({ status: status.value, content: content.value }),
})

const columns = computed<CrudColumn[]>(() => [
  { prop: 'id', label: t('pages.apply.id'), width: 80, align: 'center', sortable: true },
  { prop: 'studentName', label: t('pages.apply.studentName'), showOverflowTooltip: true },
  { prop: 'content', label: t('pages.apply.contentLabel'), showOverflowTooltip: true },
  { prop: 'time', label: t('pages.apply.timeLabel') },
  { prop: 'day', label: t('pages.apply.dayLabel') },
  { prop: 'status', label: t('pages.apply.statusLabel') },
  { prop: 'descr', label: t('pages.apply.descrLabel') },
])

const handleAdd = () => {
  form.value = { studentId: user.value.id, status: '待审核' }
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
  request.put('/apply/update', form.value).then((res: any) => {
    if (res.data.code === '200') {
      ElMessage.success(t('common.operationSuccess'))
      load(1)
      checkVisible.value = false
    } else {
      ElMessage.error(apiMessage(res.data))
    }
  })
}

const reset = () => {
  status.value = ''
  content.value = ''
  load(1)
}

onMounted(() => load(1))
</script>

<style scoped></style>

<template>
  <div>
    <div class="search">
      <el-input v-model="name" :placeholder="$t('pages.examplan.searchPlaceholder')" style="width: 200px" />
      <el-button type="info" plain style="margin-left: 10px" @click="load(1)">{{ $t('common.search') }}</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">{{ $t('common.reset') }}</el-button>
    </div>
    <div v-if="user.role === 'ADMIN'" class="operation">
      <el-button type="primary" plain @click="handleAdd">{{ $t('common.add') }}</el-button>
      <el-button type="danger" plain @click="delBatch">{{ $t('common.batchDelete') }}</el-button>
    </div>

    <CrudTable
        :data="tableData"
        :columns="columns"
        :page-num="pageNum"
        :page-size="pageSize"
        :total="total"
        :loading="loading"
        :selectable="user.role === 'ADMIN'"
        :show-actions="user.role === 'ADMIN'"
        @selection-change="handleSelectionChange"
        @page-change="load"
    >
      <template #actions="{ row }">
        <el-button link type="primary" size="small" @click="handleEdit(row)">{{ $t('common.edit') }}</el-button>
        <el-button link type="danger" size="small" @click="del(row.id)">{{ $t('common.delete') }}</el-button>
      </template>
    </CrudTable>

    <el-dialog v-model="formVisible" :title="$t('pages.examplan.dialogTitle')" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" label-width="100px" style="padding-right: 50px" :model="form" :rules="rules">
        <el-form-item prop="name" :label="$t('pages.examplan.title')">
          <el-input v-model="form.name" autocomplete="off" />
        </el-form-item>
        <el-form-item prop="content" :label="$t('pages.examplan.content')">
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
defineOptions({ name: 'Examplan' })

import { ref, onMounted, computed } from 'vue'
import { useUser } from '@/components/useUser.ts'
import { useCrud } from '@/composables/useCrud'
import { t } from '@/i18n'
import CrudTable, { type CrudColumn } from '@/components/CrudTable.vue'

const { user } = useUser()
const name = ref('')

const {
  tableData, pageNum, pageSize, total, loading,
  formVisible, form, formRef, rules,
  load, handleAdd, handleEdit, save, del,
  handleSelectionChange, delBatch,
} = useCrud({
  url: '/examplan',
  rules: computed(() => ({
    name: [{ required: true, message: t('pages.examplan.ruleTitleRequired'), trigger: 'blur' }],
    content: [{ required: true, message: t('pages.examplan.ruleContentRequired'), trigger: 'blur' }],
  })),
  getParams: () => ({ name: name.value }),
})

const columns = computed<CrudColumn[]>(() => [
  { prop: 'id', label: t('pages.examplan.id'), width: 80, align: 'center', sortable: true },
  { prop: 'name', label: t('pages.examplan.title'), showOverflowTooltip: true },
  { prop: 'content', label: t('pages.examplan.content'), showOverflowTooltip: true },
  { prop: 'time', label: t('pages.examplan.time') },
])

const reset = () => {
  name.value = ''
  load(1)
}

onMounted(() => load(1))
</script>

<style scoped></style>

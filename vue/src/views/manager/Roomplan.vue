<template>
  <div>
    <div class="search">
      <el-input v-model="name" :placeholder="$t('pages.roomplan.searchPlaceholder')" style="width: 200px" />
      <el-select v-model="status" :placeholder="$t('pages.roomplan.statusPlaceholder')" style="width: 200px; margin-left: 5px">
        <el-option :label="$t('pages.roomplan.free')" value="空闲" /><el-option :label="$t('pages.roomplan.occupied')" value="占用" />
      </el-select>
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

    <el-dialog v-model="formVisible" :title="$t('pages.roomplan.dialogTitle')" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" label-width="100px" style="padding-right: 50px" :model="form" :rules="rules">
        <el-form-item prop="name" :label="$t('pages.roomplan.name')">
          <el-input v-model="form.name" autocomplete="off" />
        </el-form-item>
        <el-form-item prop="status" :label="$t('pages.roomplan.status')">
          <el-select v-model="form.status" :placeholder="$t('pages.roomplan.statusPlaceholder')" style="width: 100%">
            <el-option :label="$t('pages.roomplan.free')" value="空闲" /><el-option :label="$t('pages.roomplan.occupied')" value="占用" />
          </el-select>
        </el-form-item>
        <el-form-item prop="num" :label="$t('pages.roomplan.num')">
          <el-input v-model="form.num" autocomplete="off" />
        </el-form-item>
        <el-form-item prop="content" :label="$t('pages.roomplan.contentLabel')">
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
import { ref, onMounted, computed } from 'vue'
import { useUser } from '@/components/useUser.ts'
import { useCrud } from '@/composables/useCrud'
import { t } from '@/i18n'
import CrudTable, { type CrudColumn } from '@/components/CrudTable.vue'

const { user } = useUser()
const name = ref('')
const status = ref('')

const {
  tableData, pageNum, pageSize, total, loading,
  formVisible, form, formRef, rules,
  load, handleAdd, handleEdit, save, del,
  handleSelectionChange, delBatch,
} = useCrud({
  url: '/roomplan',
  rules: computed(() => ({
    name: [{ required: true, message: t('pages.roomplan.ruleNameRequired'), trigger: 'blur' }],
    status: [{ required: true, message: t('pages.roomplan.ruleStatusRequired'), trigger: 'blur' }],
    num: [{ required: true, message: t('pages.roomplan.ruleNumRequired'), trigger: 'blur' }],
    content: [{ required: true, message: t('pages.roomplan.ruleContentRequired'), trigger: 'blur' }],
  })),
  getParams: () => ({ name: name.value, status: status.value }),
})

const columns = computed<CrudColumn[]>(() => [
  { prop: 'id', label: t('pages.roomplan.id'), width: 80, align: 'center', sortable: true },
  { prop: 'name', label: t('pages.roomplan.name'), showOverflowTooltip: true },
  { prop: 'status', label: t('pages.roomplan.status'), showOverflowTooltip: true },
  { prop: 'content', label: t('pages.roomplan.description'), showOverflowTooltip: true },
  { prop: 'num', label: t('pages.roomplan.num') },
])

const reset = () => {
  name.value = ''
  status.value = ''
  load(1)
}

onMounted(() => load(1))
</script>

<style scoped></style>

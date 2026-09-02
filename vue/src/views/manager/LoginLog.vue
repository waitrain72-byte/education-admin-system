<template>
  <div>
    <div class="search">
      <el-input v-model="username" placeholder="按账号查询" style="width: 200px" />
      <el-select v-model="status" placeholder="按状态查询" clearable style="width: 160px; margin-left: 10px">
        <el-option :label="$t('pages.loginlog.statusSuccess')" value="成功" />
        <el-option :label="$t('pages.loginlog.statusFail')" value="失败" />
      </el-select>
      <el-button type="info" plain style="margin-left: 10px" @click="search">{{ $t('common.search') }}</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="onReset">{{ $t('common.reset') }}</el-button>
    </div>

    <div class="operation" v-permission="'ADMIN'">
      <el-button type="danger" plain @click="delBatch">{{ $t('common.batchDelete') }}</el-button>
    </div>

    <CrudTable
        :data="tableData"
        :columns="columns"
        :page-num="pageNum"
        :page-size="pageSize"
        :total="total"
        :loading="loading"
        :selectable="true"
        @selection-change="handleSelectionChange"
        @page-change="load"
    >
      <template #status="{ row }">
        <el-tag :type="row.status === '成功' ? 'success' : 'danger'">
          {{ row.status === '成功' ? $t('pages.loginlog.statusSuccess') : $t('pages.loginlog.statusFail') }}
        </el-tag>
      </template>
      <template #actions="{ row }">
        <el-button link size="small" type="danger" @click="del(row.id)">{{ $t('common.delete') }}</el-button>
      </template>
    </CrudTable>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'LoginLog' })

import { ref, computed, onMounted } from 'vue'
import { useCrud } from '@/composables/useCrud'
import CrudTable, { type CrudColumn } from '@/components/CrudTable.vue'
import { t } from '@/i18n'

const username = ref('')
const status = ref('')

const {
  tableData, pageNum, pageSize, total, loading,
  load, del, delBatch, handleSelectionChange,
} = useCrud({
  url: '/loginlog',
  getParams: () => ({ username: username.value, status: status.value }),
})

const columns = computed<CrudColumn[]>(() => [
  { prop: 'id', label: t('pages.loginlog.id'), width: 70, align: 'center' },
  { prop: 'username', label: t('pages.loginlog.username') },
  { prop: 'ip', label: 'IP', width: 140, align: 'center' },
  { prop: 'status', label: t('pages.loginlog.status'), width: 100, align: 'center' },
  { prop: 'msg', label: t('pages.loginlog.msg') },
  { prop: 'createTime', label: t('pages.operlog.time'), width: 170, align: 'center' },
])

const search = () => load(1)
const onReset = () => {
  username.value = ''
  status.value = ''
  load(1)
}

onMounted(() => load(1))
</script>

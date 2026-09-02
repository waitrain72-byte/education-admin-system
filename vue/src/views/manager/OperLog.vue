<template>
  <div>
    <div class="search">
      <el-input v-model="username" placeholder="按操作人查询" style="width: 200px" />
      <el-input v-model="module" placeholder="按操作模块查询" style="width: 200px; margin-left: 10px" />
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
      <template #actions="{ row }">
        <el-button link size="small" type="danger" @click="del(row.id)">{{ $t('common.delete') }}</el-button>
      </template>
    </CrudTable>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'OperLog' })

import { ref, computed } from 'vue'
import { onMounted } from 'vue'
import { useCrud } from '@/composables/useCrud'
import CrudTable, { type CrudColumn } from '@/components/CrudTable.vue'
import { t } from '@/i18n'

const username = ref('')
const module = ref('')

const {
  tableData, pageNum, pageSize, total, loading,
  load, del, delBatch, handleSelectionChange,
} = useCrud({
  url: '/operlog',
  getParams: () => ({ username: username.value, module: module.value }),
})

const columns = computed<CrudColumn[]>(() => [
  { prop: 'id', label: t('pages.operlog.id'), width: 70, align: 'center' },
  { prop: 'username', label: t('pages.operlog.username') },
  { prop: 'module', label: t('pages.operlog.module') },
  { prop: 'type', label: t('pages.operlog.type'), width: 90, align: 'center' },
  { prop: 'url', label: t('pages.operlog.url') },
  { prop: 'ip', label: 'IP', width: 130, align: 'center' },
  { prop: 'code', label: t('pages.operlog.code'), width: 80, align: 'center' },
  { prop: 'duration', label: t('pages.operlog.duration'), width: 100, align: 'center' },
  { prop: 'createTime', label: t('pages.operlog.time'), width: 170, align: 'center' },
])

const search = () => load(1)
const onReset = () => {
  username.value = ''
  module.value = ''
  load(1)
}

onMounted(() => load(1))
</script>

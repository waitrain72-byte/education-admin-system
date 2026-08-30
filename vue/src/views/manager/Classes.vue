<template>
  <div>
    <div class="search">
      <el-input v-model="name" :placeholder="$t('pages.classes.searchPlaceholder')" style="width: 200px" />
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

    <el-dialog v-model="formVisible" :title="$t('pages.classes.dialogTitle')" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" label-width="100px" style="padding-right: 50px" :model="form" :rules="rules">
        <el-form-item prop="name" :label="$t('pages.classes.name')">
          <el-input v-model="form.name" autocomplete="off" />
        </el-form-item>
        <el-form-item prop="content" :label="$t('pages.classes.content')">
          <el-input v-model="form.content" type="textarea" :rows="5" autocomplete="off" />
        </el-form-item>
        <el-form-item prop="specialityId" :label="$t('pages.classes.speciality')">
          <el-select v-model="form.specialityId" :placeholder="$t('pages.classes.specialityPlaceholder')" style="width: 100%">
            <el-option v-for="item in specialityData" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item prop="teacherId" :label="$t('pages.classes.teacher')">
          <el-select v-model="form.teacherId" :placeholder="$t('pages.classes.teacherPlaceholder')" style="width: 100%">
            <el-option v-for="item in teacherData" :key="item.id" :label="item.name" :value="item.id" />
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
defineOptions({ name: 'Classes' })

import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUser } from '@/components/useUser.ts'
import { useCrud } from '@/composables/useCrud'
import { apiMessage, t } from '@/i18n'
import CrudTable, { type CrudColumn } from '@/components/CrudTable.vue'

const { user } = useUser()
const name = ref('')
const specialityData = ref<any[]>([])
const teacherData = ref<any[]>([])

const {
  tableData, pageNum, pageSize, total, loading,
  formVisible, form, formRef, rules,
  load, handleAdd, handleEdit, save, del,
  handleSelectionChange, delBatch,
} = useCrud({
  url: '/classes',
  rules: computed(() => ({
    name: [{ required: true, message: t('pages.classes.ruleNameRequired'), trigger: 'blur' }],
  })),
  getParams: () => ({ name: name.value }),
})

const columns = computed<CrudColumn[]>(() => [
  { prop: 'id', label: t('pages.classes.id'), width: 80, align: 'center', sortable: true },
  { prop: 'name', label: t('pages.classes.name'), showOverflowTooltip: true },
  { prop: 'content', label: t('pages.classes.content'), showOverflowTooltip: true },
  { prop: 'specialityName', label: t('pages.classes.speciality'), showOverflowTooltip: true },
  { prop: 'teacherName', label: t('pages.classes.teacher'), showOverflowTooltip: true },
])

const loadSpeciality = () => {
  request.get('/speciality/selectAll').then((res: any) => {
    if (res.data.code === '200') {
      specialityData.value = res.data.data
    } else {
      ElMessage.error(apiMessage(res.data))
    }
  })
}

const loadTeacher = () => {
  request.get('/teacher/selectAll').then((res: any) => {
    if (res.data.code === '200') {
      teacherData.value = res.data.data
    } else {
      ElMessage.error(apiMessage(res.data))
    }
  })
}

const reset = () => {
  name.value = ''
  load(1)
}

onMounted(() => {
  load(1)
  loadSpeciality()
  loadTeacher()
})
</script>

<style scoped></style>

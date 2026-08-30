<template>
  <div>
    <div class="search">
      <el-input v-model="username" :placeholder="$t('pages.student.searchPlaceholder')" style="width: 200px" />
      <el-button type="info" plain style="margin-left: 10px" @click="load(1)">{{ $t('common.search') }}</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">{{ $t('common.reset') }}</el-button>
    </div>

    <div v-if="user.role !== 'STUDENT'" class="operation">
      <el-button type="primary" plain @click="handleAdd">{{ $t('common.add') }}</el-button>
      <el-button type="danger" plain @click="delBatch">{{ $t('common.batchDelete') }}</el-button>
      <el-button type="success" plain @click="exportExcel">{{ $t('pages.student.exportExcel') }}</el-button>
      <el-button type="warning" plain @click="downloadTemplate">{{ $t('pages.student.importTemplate') }}</el-button>
      <el-upload
          :action="baseUrl + '/student/import'"
          :headers="{ token: user.token }"
          accept=".xlsx"
          :show-file-list="false"
          :on-success="onImportSuccess"
          :on-error="() => ElMessage.error(t('pages.student.importFailed'))"
          style="display: inline-block; margin-left: 10px"
      >
        <el-button type="primary" plain>{{ $t('pages.student.importExcel') }}</el-button>
      </el-upload>
    </div>

    <CrudTable
        :data="tableData"
        :columns="columns"
        :page-num="pageNum"
        :page-size="pageSize"
        :total="total"
        :loading="loading"
        :selectable="user.role !== 'STUDENT'"
        :actions-width="200"
        @selection-change="handleSelectionChange"
        @page-change="load"
    >
      <template #avatar="{ row }">
        <div style="display: flex; align-items: center">
          <el-image v-if="row.avatar" style="width: 40px; height: 40px; border-radius: 50%" :src="row.avatar" />
        </div>
      </template>
      <template #actions="{ row }">
        <el-button v-permission="['ADMIN', 'TEACHER']" link size="small" type="primary" :icon="Edit" @click="handleEdit(row)">{{ $t('common.edit') }}</el-button>
        <el-button v-permission="'ADMIN'" link size="small" type="warning" :icon="Key" @click="resetPassword(row)">{{ $t('common.resetPassword') }}</el-button>
        <el-button v-permission="['ADMIN', 'TEACHER']" link size="small" type="danger" :icon="Delete" @click="del(row.id)">{{ $t('common.delete') }}</el-button>
      </template>
    </CrudTable>

    <el-dialog v-model="formVisible" :title="$t('pages.student.dialogTitle')" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="form" label-width="100px" style="padding-right: 50px" :rules="rules">
        <el-form-item :label="$t('pages.student.avatar')">
          <el-upload
class="avatar-uploader" :action="baseUrl + '/files/upload'"
                     :headers="{ token: user.token }" list-type="picture" :on-success="handleAvatarSuccess">
            <el-button type="primary">{{ $t('pages.student.uploadAvatar') }}</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item :label="$t('pages.student.username')" prop="username">
          <el-input v-model="form.username" :placeholder="$t('pages.student.username')" />
        </el-form-item>
        <el-form-item :label="$t('pages.student.name')" prop="name">
          <el-input v-model="form.name" :placeholder="$t('pages.student.name')" />
        </el-form-item>
        <el-form-item :label="$t('pages.student.college')" prop="collegeId">
          <el-select v-model="form.collegeId" :placeholder="$t('pages.student.collegePlaceholder')" style="width: 100%">
            <el-option v-for="item in collegeData" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('pages.student.speciality')" prop="specialityId">
          <el-select v-model="form.specialityId" :placeholder="$t('pages.student.specialityPlaceholder')" style="width: 100%">
            <el-option v-for="item in specialityData" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('pages.student.classes')" prop="classId">
          <el-select v-model="form.classId" :placeholder="$t('pages.student.classesPlaceholder')" style="width: 100%">
            <el-option v-for="item in classesData" :key="item.id" :label="item.name" :value="item.id" />
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
defineOptions({ name: 'Student' })

import { ref, onMounted, inject, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormRules } from 'element-plus'
import { Edit, Key, Delete } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { apiMessage, t } from '@/i18n'
import { useUser } from '@/components/useUser.ts'
import { useCrud } from '@/composables/useCrud'
import CrudTable, { type CrudColumn } from '@/components/CrudTable.vue'

const baseUrl = import.meta.env.VITE_BASE_URL || 'http://localhost:9091'
const { user, patchUser } = useUser()
const refreshUser = inject('refreshUser', () => {})
const username = ref('')
const collegeData = ref<any[]>([])
const specialityData = ref<any[]>([])
const classesData = ref<any[]>([])

const {
  tableData, pageNum, pageSize, total, loading,
  formVisible, form, formRef, rules,
  load, handleAdd, handleEdit, save, del, delBatch,
  handleSelectionChange,
} = useCrud({
  url: '/student',
  rules: computed<FormRules>(() => ({
    username: [{ required: true, message: t('pages.student.ruleUsernameRequired'), trigger: 'blur' }],
  })),
  getParams: () => ({ username: username.value }),
  afterSave: (formData) => {
    // 如果修改的是当前登录学生自己的信息，同步全局状态
    if (formData.id === user.value.id) {
      patchUser({
        avatar: formData.avatar,
        name: formData.name,
      })
      refreshUser()
    }
  },
})

const columns = computed<CrudColumn[]>(() => [
  { prop: 'id', label: t('pages.student.id'), width: 70, align: 'center', sortable: true },
  { prop: 'avatar', label: t('pages.student.avatar') },
  { prop: 'username', label: t('pages.student.account') },
  { prop: 'name', label: t('pages.student.name') },
  { prop: 'role', label: t('pages.student.role') },
  { prop: 'collegeName', label: t('pages.student.college') },
  { prop: 'specialityName', label: t('pages.student.speciality') },
  { prop: 'className', label: t('pages.student.classes') },
  { prop: 'score', label: t('pages.student.score') },
])

const loadCollege = () => {
  request.get('/college/selectAll').then((res: any) => {
    if (res.data.code === '200') {
      collegeData.value = res.data.data
    } else {
      ElMessage.error(apiMessage(res.data))
    }
  })
}

const loadSpeciality = () => {
  request.get('/speciality/selectAll').then((res: any) => {
    if (res.data.code === '200') {
      specialityData.value = res.data.data
    } else {
      ElMessage.error(apiMessage(res.data))
    }
  })
}

const loadClasses = () => {
  request.get('/classes/selectAll').then((res: any) => {
    if (res.data.code === '200') {
      classesData.value = res.data.data
    } else {
      ElMessage.error(apiMessage(res.data))
    }
  })
}

const resetPassword = (row: any) => {
  ElMessageBox.confirm(t('pages.student.resetConfirm', { username: row.username }), t('common.resetPassword'), { type: 'warning' }).then(() => {
    request.put('/student/resetPassword/' + row.id).then((res: any) => {
      if (res.data.code === '200') {
        ElMessage.success(t('pages.student.resetSuccess'))
      } else {
        ElMessage.error(apiMessage(res.data))
      }
    })
  }).catch(() => {})
}

const handleAvatarSuccess = (response: any) => {
  form.value.avatar = response.data
}

// ========== Excel 导入导出 ==========
const exportExcel = () => {
  request.get('/student/export', { responseType: 'blob' }).then((res: any) => {
    const url = URL.createObjectURL(res.data)
    const link = document.createElement('a')
    link.href = url
    link.download = '学生列表.xlsx'
    link.click()
    URL.revokeObjectURL(url)
  })
}

const downloadTemplate = () => {
  request.get('/student/importTemplate', { responseType: 'blob' }).then((res: any) => {
    const url = URL.createObjectURL(res.data)
    const link = document.createElement('a')
    link.href = url
    link.download = '学生导入模板.xlsx'
    link.click()
    URL.revokeObjectURL(url)
  })
}

const onImportSuccess = (response: any) => {
  if (response.code === '200') {
    ElMessage.success(t('pages.student.importResult', { result: response.data }))
    load(1)
  } else {
    ElMessage.error(apiMessage(response))
  }
}

const reset = () => {
  username.value = ''
  load(1)
}

onMounted(() => {
  load(1)
  loadCollege()
  loadSpeciality()
  loadClasses()
})
</script>

<style scoped></style>

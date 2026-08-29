<template>
  <div>
    <div class="search">
      <el-input v-model="username" :placeholder="$t('pages.teacher.searchPlaceholder')" style="width: 200px" />
      <el-button type="info" plain style="margin-left: 10px" @click="load(1)">{{ $t('common.search') }}</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">{{ $t('common.reset') }}</el-button>
    </div>

    <div v-if="user.role !== 'STUDENT'" class="operation">
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
        :selectable="user.role !== 'STUDENT'"
        :actions-width="200"
        @selection-change="handleSelectionChange"
        @page-change="load"
    >
      <template #avatar="{ row }">
        <div style="display: flex; align-items: center">
          <el-image
v-if="row.avatar" style="width: 40px; height: 40px; border-radius: 50%"
                    :src="row.avatar" :preview-src-list="[row.avatar]" />
        </div>
      </template>
      <template #actions="{ row }">
        <el-button v-permission="'ADMIN'" link size="small" type="primary" :icon="Edit" @click="handleEdit(row)">{{ $t('common.edit') }}</el-button>
        <el-button v-permission="'ADMIN'" link size="small" type="warning" :icon="Key" @click="resetPassword(row)">{{ $t('common.resetPassword') }}</el-button>
        <el-button v-permission="'ADMIN'" link size="small" type="danger" :icon="Delete" @click="del(row.id)">{{ $t('common.delete') }}</el-button>
      </template>
    </CrudTable>

    <el-dialog v-model="formVisible" :title="$t('pages.teacher.dialogTitle')" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="form" label-width="100px" style="padding-right: 50px" :rules="rules">
        <el-form-item :label="$t('pages.teacher.avatar')">
          <el-upload
class="avatar-uploader" :action="baseUrl + '/files/upload'"
                     :headers="{ token: user.token }" list-type="picture" :on-success="handleAvatarSuccess">
            <el-button type="primary">{{ $t('pages.teacher.uploadAvatar') }}</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item :label="$t('pages.teacher.username')" prop="username">
          <el-input v-model="form.username" :placeholder="$t('pages.teacher.username')" />
        </el-form-item>
        <el-form-item :label="$t('pages.teacher.name')" prop="name">
          <el-input v-model="form.name" :placeholder="$t('pages.teacher.name')" />
        </el-form-item>
        <el-form-item :label="$t('pages.teacher.phone')" prop="phone">
          <el-input v-model="form.phone" :placeholder="$t('pages.teacher.phone')" />
        </el-form-item>
        <el-form-item :label="$t('pages.teacher.email')" prop="email">
          <el-input v-model="form.email" :placeholder="$t('pages.teacher.email')" />
        </el-form-item>
        <el-form-item :label="$t('pages.teacher.title')" prop="title">
          <el-input v-model="form.title" :placeholder="$t('pages.teacher.title')" />
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

const {
  tableData, pageNum, pageSize, total, loading,
  formVisible, form, formRef, rules,
  load, handleAdd, handleEdit, save, del, delBatch,
  handleSelectionChange,
} = useCrud({
  url: '/teacher',
  rules: computed<FormRules>(() => ({
    username: [{ required: true, message: t('pages.teacher.ruleUsernameRequired'), trigger: 'blur' }],
  })),
  getParams: () => ({ username: username.value }),
  afterSave: (formData) => {
    // 如果修改的是当前登录用户自己的信息，同步全局状态
    if (formData.id === user.value.id) {
      patchUser({
        avatar: formData.avatar,
        name: formData.name,
        phone: formData.phone,
        email: formData.email,
      })
      refreshUser()
    }
  },
})

const columns = computed<CrudColumn[]>(() => [
  { prop: 'id', label: t('pages.teacher.id'), width: 70, align: 'center', sortable: true },
  { prop: 'avatar', label: t('pages.teacher.avatar') },
  { prop: 'username', label: t('pages.teacher.account') },
  { prop: 'name', label: t('pages.teacher.name') },
  { prop: 'phone', label: t('pages.teacher.phone') },
  { prop: 'email', label: t('pages.teacher.email') },
  { prop: 'role', label: t('pages.teacher.role') },
  { prop: 'title', label: t('pages.teacher.title') },
])

const resetPassword = (row: any) => {
  ElMessageBox.confirm(t('pages.teacher.resetConfirm', { username: row.username }), t('common.resetPassword'), { type: 'warning' }).then(() => {
    request.put('/teacher/resetPassword/' + row.id).then((res: any) => {
      if (res.data.code === '200') {
        ElMessage.success(t('pages.teacher.resetSuccess'))
      } else {
        ElMessage.error(apiMessage(res.data))
      }
    })
  }).catch(() => {})
}

const handleAvatarSuccess = (response: any) => {
  form.value.avatar = response.data
}

const reset = () => {
  username.value = ''
  load(1)
}

onMounted(() => load(1))
</script>

<style scoped></style>

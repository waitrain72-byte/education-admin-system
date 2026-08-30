<template>
  <div>
    <el-card style="width: 50%">
      <el-form ref="formRef" :model="user" :rules="rules" label-width="100px" style="padding-right: 50px">
        <el-form-item :label="$t('pages.password.originalPassword')" prop="password">
          <el-input v-model="user.password" show-password :placeholder="$t('pages.password.originalPassword')" />
        </el-form-item>
        <el-form-item :label="$t('pages.password.newPassword')" prop="newPassword">
          <el-input v-model="user.newPassword" show-password :placeholder="$t('pages.password.newPassword')" />
        </el-form-item>
        <el-form-item :label="$t('pages.password.confirmPassword')" prop="confirmPassword">
          <el-input v-model="user.confirmPassword" show-password :placeholder="$t('pages.password.confirmPlaceholder')" />
        </el-form-item>
        <div style="text-align: center; margin-bottom: 20px">
          <el-button type="primary" @click="update">{{ $t('pages.password.submit') }}</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Password' })

import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import request from '@/utils/request'
import { useUser } from '@/components/useUser.ts'
import { apiMessage, t } from '@/i18n'

const router = useRouter()
const formRef = ref<FormInstance>()
const { user: storeUser, clearUser } = useUser()
const user = reactive<Record<string, any>>({
  username: storeUser.value.username,
  role: storeUser.value.role,
  password: '',
  newPassword: '',
  confirmPassword: ''
})

const validatePassword = (_rule: any, value: string, callback: (e?: Error) => void) => {
  if (value === '') { callback(new Error(t('pages.password.ruleConfirmRequired'))) }
  else if (value !== user.newPassword) { callback(new Error(t('pages.password.ruleConfirmMismatch'))) }
  else { callback() }
}

const rules: FormRules = {
  password: [{ required: true, message: () => t('pages.password.ruleOriginalRequired'), trigger: 'blur' }],
  newPassword: [{ required: true, message: () => t('pages.password.ruleNewRequired'), trigger: 'blur' }],
  confirmPassword: [{ validator: validatePassword, required: true, trigger: 'blur' }],
}

const update = () => {
  formRef.value?.validate((valid) => {
    if (valid) {
      request.put('/updatePassword', user).then((res: any) => {
        if (res.data.code === '200') {
          clearUser()
          ElMessage.success(t('pages.password.success'))
          router.push('/login')
        } else { ElMessage.error(apiMessage(res.data)) }
      })
    }
  })
}
</script>

<style scoped>
:deep(.el-form-item__label) {
  font-weight: bold;
}
</style>

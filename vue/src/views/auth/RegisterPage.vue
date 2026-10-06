<template>
  <AuthShell>
    <h1 class="form-title">{{ $t('register.heading') }}</h1>
    <p class="form-sub">{{ $t('register.hint') }}</p>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="form" @submit.prevent="register">
      <el-form-item prop="username" :label="$t('login.account')">
        <el-input
          id="register-username"
          v-model="form.username"
          :prefix-icon="User"
          :placeholder="$t('register.usernamePlaceholder')"
          size="large"
          autocomplete="username"
        />
      </el-form-item>
      <el-form-item prop="password" :label="$t('login.password')">
        <el-input
          id="register-password"
          v-model="form.password"
          :prefix-icon="Lock"
          :placeholder="$t('register.passwordPlaceholder')"
          show-password
          size="large"
          autocomplete="new-password"
        />
      </el-form-item>
      <el-form-item prop="confirmPass" :label="$t('register.confirm')">
        <el-input
          id="register-confirm"
          v-model="form.confirmPass"
          :prefix-icon="Lock"
          :placeholder="$t('register.confirmPlaceholder')"
          show-password
          size="large"
          autocomplete="new-password"
        />
      </el-form-item>
      <el-button type="primary" size="large" class="form-submit" native-type="submit" :loading="submitting">
        {{ $t('register.submit') }}
      </el-button>
    </el-form>

    <div class="form-foot">
      {{ $t('register.hasAccount') }}
      <router-link to="/login">{{ $t('register.goLogin') }}</router-link>
    </div>
  </AuthShell>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { ElMessage } from '@/utils/element-plus'
import request from '@/utils/request'
import { t } from '@/i18n'
import AuthShell from './AuthShell.vue'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({ username: '', password: '', confirmPass: '' })

const validateConfirm = (_rule: any, value: string, callback: (error?: Error) => void) => {
  if (value === '') callback(new Error(t('register.ruleConfirmRequired')))
  else if (value !== form.password) callback(new Error(t('register.ruleConfirmMismatch')))
  else callback()
}

const rules: FormRules = {
  username: [
    { required: true, message: () => t('register.ruleUsernameRequired'), trigger: 'blur' },
    { min: 3, max: 20, message: () => t('register.ruleUsernameLength'), trigger: 'blur' },
  ],
  password: [
    { required: true, message: () => t('register.rulePasswordRequired'), trigger: 'blur' },
    { min: 6, max: 20, message: () => t('register.rulePasswordLength'), trigger: 'blur' },
  ],
  confirmPass: [{ validator: validateConfirm, trigger: 'blur' }],
}

/** 只开放学生自助注册；教师、管理员账号由教务后台创建 */
const register = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await request.post('/register', { username: form.username.trim(), password: form.password, role: 'STUDENT' })
    ElMessage.success(t('register.success'))
    router.push('/login')
  } catch {
    // 失败原因（用户名已存在等）的提示已由拦截器统一弹出
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.form-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--xm-text-primary);
}

.form-sub {
  margin: 4px 0 20px;
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.form :deep(.el-form-item__label) {
  padding-bottom: 4px;
  font-size: 13px;
  color: var(--xm-text-regular);
}

.form-submit {
  width: 100%;
  margin-top: 4px;
}

.form-foot {
  margin-top: 18px;
  text-align: center;
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.form-foot a {
  color: var(--xm-brand);
  font-weight: 600;
}
</style>

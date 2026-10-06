<template>
  <AuthShell>
    <h1 class="form-title">{{ $t('login.heading') }}</h1>
    <p class="form-sub">{{ $t('login.hint') }}</p>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="form" @submit.prevent="login">
      <el-form-item prop="username" :label="$t('login.account')">
        <el-input
          id="login-username"
          v-model="form.username"
          :prefix-icon="User"
          :placeholder="$t('login.usernamePlaceholder')"
          size="large"
          autocomplete="username"
        />
      </el-form-item>
      <el-form-item prop="password" :label="$t('login.password')">
        <el-input
          id="login-password"
          v-model="form.password"
          :prefix-icon="Lock"
          :placeholder="$t('login.passwordPlaceholder')"
          show-password
          size="large"
          autocomplete="current-password"
        />
      </el-form-item>
      <el-form-item prop="captcha" :label="$t('login.captcha')">
        <div class="captcha">
          <el-input id="login-captcha" v-model="form.captcha" :placeholder="$t('login.captchaPlaceholder')" size="large" />
          <button type="button" class="captcha__img" :title="$t('login.captchaRefresh')" @click="refreshCaptcha">
            <img v-if="captchaUrl" :src="captchaUrl" :alt="$t('login.captchaAlt')" />
          </button>
        </div>
      </el-form-item>

      <!-- 同一个账号名对应多个身份且密码相同（老数据可能出现）：后端返回 5012，这时才让用户选身份 -->
      <div v-if="needRole" class="role-pick" role="radiogroup" :aria-label="$t('login.pickRole')">
        <p class="role-pick__hint">{{ $t('login.pickRoleHint') }}</p>
        <div class="role-pick__options">
          <button
            v-for="r in roles"
            :key="r"
            type="button"
            role="radio"
            class="role-pick__option"
            :class="{ 'is-on': form.role === r }"
            :aria-checked="form.role === r"
            @click="form.role = r"
          >
            {{ $t('shell.roles.' + r) }}
          </button>
        </div>
      </div>

      <el-button type="primary" size="large" class="form-submit" native-type="submit" :loading="submitting">
        {{ $t('login.submit') }}
      </el-button>
    </el-form>

    <div class="form-foot">
      {{ $t('login.noAccount') }}
      <router-link to="/register">{{ $t('login.goRegister') }}</router-link>
    </div>
  </AuthShell>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { ElMessage } from '@/utils/element-plus'
import request, { ApiError } from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { usePermission } from '@/composables/usePermission'
import { pullThemeFromServer } from '@/composables/useTheme'
import { pullThemeColorFromServer } from '@/composables/useThemeColor'
import { pullLocaleFromServer } from '@/composables/useLocale'
import { t } from '@/i18n'
import AuthShell from './AuthShell.vue'

const router = useRouter()
const formRef = ref<FormInstance>()
const captchaUrl = ref('')
const submitting = ref(false)
const needRole = ref(false)
const roles = ['STUDENT', 'TEACHER', 'ADMIN'] as const
const { pullPermissions } = usePermission()

const form = reactive({
  username: '',
  password: '',
  captcha: '',
  /** 默认不传身份，由后端按账号判断；只有 5012 时才由用户补选 */
  role: '',
})

const rules: FormRules = {
  username: [{ required: true, message: () => t('register.ruleUsernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: () => t('register.rulePasswordRequired'), trigger: 'blur' }],
  captcha: [{ required: true, message: () => t('login.captchaPlaceholder'), trigger: 'blur' }],
}

const refreshCaptcha = async (): Promise<void> => {
  // 验证码一码一用（后端读取即失效）：换新图时旧输入必然错误，必须清空
  form.captcha = ''
  try {
    // 二进制响应没有 { code, msg, data } 包装，拦截器原样返回 Blob
    const blob = await request.get<Blob>('/captcha', { params: { t: Date.now() }, responseType: 'blob' })
    if (blob instanceof Blob) {
      // 先回收上一张验证码的 blob URL，否则每次刷新都会泄漏一个对象 URL
      if (captchaUrl.value) URL.revokeObjectURL(captchaUrl.value)
      captchaUrl.value = URL.createObjectURL(blob)
    }
  } catch {
    ElMessage.error(t('login.captchaFailed'))
  }
}

onBeforeUnmount(() => {
  if (captchaUrl.value) URL.revokeObjectURL(captchaUrl.value)
})

const login = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (needRole.value && !form.role) {
    ElMessage.warning(t('login.pickRole'))
    return
  }
  submitting.value = true
  try {
    const account = await request.post<Record<string, any>>('/login', {
      username: form.username.trim(),
      password: form.password,
      captcha: form.captcha,
      role: form.role || undefined,
    })
    // 登录态写入 Pinia store（内部负责持久化到 localStorage）
    useUserStore().updateUser(account)
    // 拉取当前用户权限码（按钮/接口级 RBAC），再从后端同步该账号保存的主题、主题色与语言
    pullPermissions()
    router.push('/')
    pullThemeFromServer()
    pullThemeColorFromServer()
    pullLocaleFromServer()
    ElMessage.success(t('login.welcomeBack', { name: account?.name || form.username }))
  } catch (e) {
    if (e instanceof ApiError && e.code === '5012') {
      needRole.value = true
    }
    // 失败原因的提示已由拦截器统一弹出；验证码一码一用，无论失败原因都要换一张
    refreshCaptcha()
  } finally {
    submitting.value = false
  }
}

onMounted(refreshCaptcha)
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

.captcha {
  display: flex;
  gap: 10px;
  width: 100%;
}

.captcha :deep(.el-input) {
  flex: 1;
}

.captcha__img {
  flex-shrink: 0;
  width: 120px;
  height: 40px;
  padding: 0;
  border: 1px solid var(--xm-border);
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
  cursor: pointer;
}

.captcha__img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.captcha__img:hover,
.captcha__img:focus-visible {
  border-color: var(--xm-brand);
}

.role-pick {
  margin: 4px 0 16px;
  padding: 12px;
  border-radius: 10px;
  background: var(--xm-warn-soft);
}

.role-pick__hint {
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--xm-warn);
}

.role-pick__options {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.role-pick__option {
  padding: 7px 0;
  border: 1px solid var(--xm-border);
  border-radius: 8px;
  background: var(--xm-bg-card);
  color: var(--xm-text-regular);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.role-pick__option.is-on {
  border-color: var(--xm-brand);
  color: var(--xm-brand);
  font-weight: 600;
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

<template>
  <div class="container">
    <!-- 右上角偏好区：语言 + 主题（登录前也能调整，保存到 localStorage） -->
    <div class="corner-actions">
      <div class="theme-toggle" :title="$t('layout.lang.switch')" @click="toggleLocale">
        {{ isZh ? 'EN' : '中' }}
      </div>
      <div class="theme-toggle" :title="$t(themeLabel)" @click="cycleTheme">
        <el-icon :size="20"><component :is="themeIcon" /></el-icon>
      </div>
    </div>

    <!-- 左侧图片区域 -->
    <div class="left-panel">
      <div class="left-content">
        <img src="@/assets/imgs/教务系统.png" alt="logo" class="logo" />
        <div class="system-title">{{ $t('login.systemName') }}</div>
        <div class="system-subtitle">{{ $t('login.systemSub') }}</div>
        <div class="decoration-text">{{ $t('login.slogan') }}</div>
      </div>
    </div>

    <!-- 右侧登录框 -->
    <div class="right-panel">
      <div class="login-box">
        <div class="login-title">{{ $t('login.title') }}</div>
        <div class="login-subtitle">{{ $t('login.subtitle') }}</div>
        <el-form ref="formRef" :model="form" :rules="rules" class="login-form">
          <el-form-item prop="username">
            <el-input
                v-model="form.username"
                :prefix-icon="User"
                :placeholder="$t('login.usernamePlaceholder')"
                size="large"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
                v-model="form.password"
                :prefix-icon="Lock"
                :placeholder="$t('login.passwordPlaceholder')"
                show-password
                size="large"
            />
          </el-form-item>
          <el-form-item prop="captcha">
            <div class="captcha-wrapper">
              <el-input
                  v-model="form.captcha"
                  :prefix-icon="Picture"
                  :placeholder="$t('login.captchaPlaceholder')"
                  size="large"
              />
              <img
                  :src="captchaUrl"
                  class="captcha-img"
                  :title="$t('login.captchaRefresh')"
                  alt="验证码"
                  @click="refreshCaptcha"
              />
            </div>
          </el-form-item>
          <el-form-item prop="role">
            <el-select v-model="form.role" :placeholder="$t('login.rolePlaceholder')" class="role-select" size="large">
              <el-option :label="$t('login.roleAdmin')" value="ADMIN" />
              <el-option :label="$t('login.roleTeacher')" value="TEACHER" />
              <el-option :label="$t('login.roleStudent')" value="STUDENT" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button class="login-button" size="large" @click="login">{{ $t('login.submit') }}</el-button>
          </el-form-item>
          <div class="register-link">
            <div></div>
            <div>{{ $t('login.noAccount') }} <router-link to="/register">{{ $t('login.goRegister') }}</router-link></div>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, Picture, Sunny, Moon, Monitor } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { useTheme, pullThemeFromServer } from '@/composables/useTheme'
import { currentLocale, setLocale, pullLocaleFromServer } from '@/composables/useLocale'
import { apiMessage, t } from '@/i18n'

interface LoginForm {
  username: string
  password: string
  captcha: string
  role: string
}

interface LoginResponse {
  code: string
  msg: string
  data: Record<string, unknown>
}

const router = useRouter()
const formRef = ref<FormInstance>()
const captchaUrl = ref<string>('')

// 主题切换：浅色 → 深色 → 跟随系统 循环（登录后管理端有下拉可选）
const mode = useTheme()
const themeIcon = computed(() => (mode.value === 'dark' ? Moon : mode.value === 'light' ? Sunny : Monitor))
const themeLabel = computed(() => `layout.theme.current.${mode.value}`)
const cycleTheme = () => {
  mode.value = mode.value === 'light' ? 'dark' : mode.value === 'dark' ? 'auto' : 'light'
}

// 语言切换：中 / EN
const isZh = computed(() => currentLocale() === 'zh-CN')
const toggleLocale = () => {
  setLocale(isZh.value ? 'en-US' : 'zh-CN')
}

const form = reactive<LoginForm>({
  username: '',
  password: '',
  captcha: '',
  role: '',
})

const rules: FormRules = {
  username: [{ required: true, message: () => t('register.ruleUsernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: () => t('register.rulePasswordRequired'), trigger: 'blur' }],
  captcha:  [{ required: true, message: () => t('login.captchaPlaceholder'), trigger: 'blur' }],
  role:     [{ required: true, message: () => t('login.rolePlaceholder'), trigger: 'change' }],
}

const refreshCaptcha = async (): Promise<void> => {
  try {
    const response = await request.get('/captcha', {
      params: { t: Date.now() },
      responseType: 'blob',
    })
    if (response.data instanceof Blob) {
      captchaUrl.value = URL.createObjectURL(response.data)
    }
  } catch {
    ElMessage.error(t('login.captchaFailed'))
  }
}

const login = (): void => {
  formRef.value?.validate((valid: boolean) => {
    if (valid) {
      request.post<LoginResponse>('/login', form)
          .then((res) => {
            if (res.data.code === '200') {
              // 登录态写入 Pinia store（内部负责持久化到 localStorage）
              useUserStore().updateUser(res.data.data as Record<string, any>)
              router.push('/')
              // 从后端拉取该用户保存的主题与语言偏好，覆盖本地默认，实现多端同步
              pullThemeFromServer()
              pullLocaleFromServer()
              ElMessage.success(t('common.operationSuccess'))
            } else {
              ElMessage.error(apiMessage(res.data))
              refreshCaptcha()
            }
          })
          .catch(() => {
            ElMessage.error(t('login.loginFailed'))
            refreshCaptcha()
          })
    }
  })
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<style scoped>
.container {
  height: 100vh;
  display: flex;
  overflow: hidden;
  position: relative;
}

/* 右上角偏好区（语言 + 主题） */
.corner-actions {
  position: absolute;
  top: 20px;
  right: 24px;
  z-index: 2;
  display: flex;
  gap: 10px;
}

/* 偏好按钮 */
.theme-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  height: 36px;
  padding: 0 8px;
  border-radius: 18px;
  font-size: 14px;
  font-weight: 600;
  color: var(--xm-text-regular);
  background: var(--xm-bg-card);
  box-shadow: var(--xm-shadow-card);
  cursor: pointer;
  transition: all 0.3s ease;
}

.theme-toggle:hover {
  color: var(--xm-brand);
  transform: scale(1.08);
}

/* ===== 左侧图片区域 ===== */
.left-panel {
  flex: 1;
  background-image: url('@/assets/imgs/管理会议.png');
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

/* 左侧遮罩，让文字更清晰 */
.left-panel::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
}

.left-content {
  position: relative;
  z-index: 1;
  text-align: center;
  color: white;
  padding: 40px;
}

.logo {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: white;
  padding: 10px;
  margin-bottom: 30px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.system-title {
  font-size: 42px;
  font-weight: bold;
  letter-spacing: 4px;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
  margin-bottom: 10px;
}

.system-subtitle {
  font-size: 18px;
  letter-spacing: 6px;
  opacity: 0.85;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  margin-bottom: 20px;
}

.decoration-text {
  font-size: 16px;
  letter-spacing: 4px;
  opacity: 0.7;
  font-weight: 300;
}

/* ===== 右侧登录区域 ===== */
.right-panel {
  width: 460px;
  min-width: 460px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--xm-bg-page);
  padding: 40px;
}

.login-box {
  width: 100%;
  max-width: 400px;
  padding: 50px 40px;
  background: var(--xm-bg-card);
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.08);
}

.login-title {
  text-align: center;
  font-size: 28px;
  font-weight: bold;
  color: var(--xm-text-primary);
  margin-bottom: 8px;
}

.login-subtitle {
  text-align: center;
  font-size: 14px;
  color: var(--xm-text-secondary);
  margin-bottom: 30px;
}

.login-form {
  display: flex;
  flex-direction: column;
  align-items: center;
}

:deep(.el-form-item) {
  width: 100%;
  margin-bottom: 20px;
}

:deep(.el-form-item__error) {
  color: #f56c6c;
  padding-top: 2px;
}

/* 输入框样式 */
:deep(.el-input__wrapper) {
  background: var(--xm-bg-input) !important;
  box-shadow: none !important;
  border-radius: 8px;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

:deep(.el-input__wrapper:hover) {
  background: var(--xm-bg-input-hover) !important;
  border-color: var(--xm-brand);
}

:deep(.el-input__wrapper.is-focus) {
  background: var(--xm-bg-card) !important;
  border-color: var(--xm-brand);
  box-shadow: 0 0 0 3px rgba(118, 132, 255, 0.12) !important;
}

:deep(.el-input__inner) {
  color: var(--xm-text-primary) !important;
}

:deep(.el-input__inner::placeholder) {
  color: var(--xm-text-secondary);
}

/* 前缀图标 */
:deep(.el-input__prefix-inner .el-icon) {
  color: var(--xm-brand);
  font-size: 18px;
}

/* 下拉选择框 */
.role-select {
  width: 100%;
}

.role-select :deep(.el-input__wrapper) {
  background: var(--xm-bg-input) !important;
  box-shadow: none !important;
  border-radius: 8px;
  border: 1px solid transparent;
}

.role-select :deep(.el-input__wrapper:hover) {
  background: var(--xm-bg-input-hover) !important;
  border-color: var(--xm-brand);
}

.role-select :deep(.el-input__wrapper.is-focus) {
  background: var(--xm-bg-card) !important;
  border-color: var(--xm-brand);
  box-shadow: 0 0 0 3px rgba(118, 132, 255, 0.12) !important;
}

.role-select :deep(.el-input__inner) {
  color: var(--xm-text-primary) !important;
}

.role-select :deep(.el-select__caret) {
  color: var(--xm-brand);
}

/* 验证码行 */
.captcha-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.captcha-wrapper :deep(.el-input) {
  flex: 1;
}

.captcha-img {
  height: 40px;
  border-radius: 8px;
  cursor: pointer;
  flex-shrink: 0;
  border: 1px solid var(--xm-border);
  transition: border-color 0.3s ease;
}

.captcha-img:hover {
  border-color: var(--xm-brand);
}

/* 登录按钮 */
.login-button {
  width: 100%;
  background: linear-gradient(135deg, #818cf8 0%, #6366f1 100%);
  border: none;
  color: white;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  padding: 14px 0;
  transition: all 0.3s ease;
  margin-top: 10px;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.4);
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
}

.login-button:active {
  transform: translateY(0);
}

/* 注册链接 */
.register-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  margin-top: 20px;
  color: var(--xm-text-regular);
  font-size: 14px;
}

.register-link a {
  color: var(--xm-brand);
  text-decoration: none;
  font-weight: 500;
  transition: color 0.3s ease;
}

.register-link a:hover {
  color: var(--xm-brand-strong);
  text-decoration: underline;
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .container {
    flex-direction: column;
  }

  .left-panel {
    flex: 0 0 200px;
    min-height: 200px;
  }

  .left-content .logo {
    width: 60px;
    height: 60px;
    margin-bottom: 10px;
  }

  .system-title {
    font-size: 24px;
  }

  .system-subtitle {
    font-size: 14px;
  }

  .decoration-text {
    font-size: 12px;
  }

  .right-panel {
    width: 100%;
    min-width: unset;
    flex: 1;
    padding: 20px;
  }

  .login-box {
    padding: 30px 24px;
  }
}
</style>

<template>
  <div class="container">
    <!-- 右上角偏好区：语言 + 主题 -->
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

    <!-- 右侧注册框 -->
    <div class="right-panel">
      <div class="register-box">
        <div class="register-title">{{ $t('register.title') }}</div>
        <div class="register-subtitle">{{ $t('register.subtitle') }}</div>
        <el-form ref="formRef" :model="form" :rules="rules" class="register-form">
          <el-form-item prop="username">
            <el-input
                v-model="form.username"
                :prefix-icon="User"
                :placeholder="$t('register.usernamePlaceholder')"
                size="large"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
                v-model="form.password"
                :prefix-icon="Lock"
                :placeholder="$t('register.passwordPlaceholder')"
                show-password
                size="large"
            />
          </el-form-item>
          <el-form-item prop="confirmPass">
            <el-input
                v-model="form.confirmPass"
                :prefix-icon="Lock"
                :placeholder="$t('register.confirmPlaceholder')"
                show-password
                size="large"
            />
          </el-form-item>
          <el-form-item>
            <el-button class="register-button" size="large" @click="register">{{ $t('register.submit') }}</el-button>
          </el-form-item>
          <div class="login-link">
            <div></div>
            <div>{{ $t('register.hasAccount') }} <router-link to="/login">{{ $t('register.goLogin') }}</router-link></div>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, Sunny, Moon, Monitor } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useTheme } from '@/composables/useTheme'
import { currentLocale, setLocale } from '@/composables/useLocale'
import { apiMessage, t } from '@/i18n'

interface RegisterForm {
  username: string
  password: string
  confirmPass: string
  role: string
}

const router = useRouter()
const formRef = ref<FormInstance>()

// 主题切换：浅色 → 深色 → 跟随系统 循环
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

const form = reactive<RegisterForm>({
  username: '',
  password: '',
  confirmPass: '',
  role: 'STUDENT',
})

// 确认密码校验
const validatePassword = (_rule: any, value: string, callback: (error?: Error) => void) => {
  if (value === '') {
    callback(new Error(t('register.ruleConfirmRequired')))
  } else if (value !== form.password) {
    callback(new Error(t('register.ruleConfirmMismatch')))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: [
    { required: true, message: () => t('register.ruleUsernameRequired'), trigger: 'blur' },
    { min: 3, max: 20, message: () => t('register.ruleUsernameLength'), trigger: 'blur' }
  ],
  password: [
    { required: true, message: () => t('register.rulePasswordRequired'), trigger: 'blur' },
    { min: 6, max: 20, message: () => t('register.rulePasswordLength'), trigger: 'blur' }
  ],
  confirmPass: [
    { validator: validatePassword, trigger: 'blur' }
  ]
}

const register = (): void => {
  formRef.value?.validate((valid: boolean) => {
    if (valid) {
      request.post('/register', {
        username: form.username,
        password: form.password,
        role: form.role
      }).then((res: any) => {
        if (res.data.code === '200') {
          ElMessage.success(t('register.success'))
          router.push('/login')
        } else {
          ElMessage.error(apiMessage(res.data))
        }
      }).catch(() => {
        ElMessage.error(t('register.failedRetry'))
      })
    }
  })
}
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

/* ===== 右侧注册区域 ===== */
.right-panel {
  width: 460px;
  min-width: 460px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--xm-bg-page);
  padding: 40px;
}

.register-box {
  width: 100%;
  max-width: 400px;
  padding: 50px 40px;
  background: var(--xm-bg-card);
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.08);
}

.register-title {
  text-align: center;
  font-size: 28px;
  font-weight: bold;
  color: var(--xm-text-primary);
  margin-bottom: 8px;
}

.register-subtitle {
  text-align: center;
  font-size: 14px;
  color: var(--xm-text-secondary);
  margin-bottom: 30px;
}

.register-form {
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

/* 注册按钮 */
.register-button {
  width: 100%;
  background: linear-gradient(135deg, #87cefa 0%, #7684ff 100%);
  border: none;
  color: white;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  padding: 14px 0;
  transition: all 0.3s ease;
  margin-top: 10px;
}

.register-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.4);
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
}

.register-button:active {
  transform: translateY(0);
}

/* 登录链接 */
.login-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  margin-top: 20px;
  color: var(--xm-text-regular);
  font-size: 14px;
}

.login-link a {
  color: var(--xm-brand);
  text-decoration: none;
  font-weight: 500;
  transition: color 0.3s ease;
}

.login-link a:hover {
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

  .register-box {
    padding: 30px 24px;
  }
}
</style>

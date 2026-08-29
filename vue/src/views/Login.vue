<template>
  <div class="container">
    <!-- 右上角主题切换：登录前也能调整，保存到 localStorage -->
    <div class="theme-toggle" :title="themeLabel" @click="cycleTheme">
      <el-icon :size="20"><component :is="themeIcon" /></el-icon>
    </div>

    <!-- 左侧图片区域 -->
    <div class="left-panel">
      <div class="left-content">
        <img src="@/assets/imgs/教务系统.png" alt="logo" class="logo" />
        <div class="system-title">教务管理系统</div>
        <div class="system-subtitle">Educational Management System</div>
        <div class="decoration-text">—— 智慧校园 · 高效管理 ——</div>
      </div>
    </div>

    <!-- 右侧登录框 -->
    <div class="right-panel">
      <div class="login-box">
        <div class="login-title">欢迎登录</div>
        <div class="login-subtitle">请输入您的账号信息</div>
        <el-form ref="formRef" :model="form" :rules="rules" class="login-form">
          <el-form-item prop="username">
            <el-input
                v-model="form.username"
                :prefix-icon="User"
                placeholder="请输入账号"
                size="large"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
                v-model="form.password"
                :prefix-icon="Lock"
                placeholder="请输入密码"
                show-password
                size="large"
            />
          </el-form-item>
          <el-form-item prop="captcha">
            <div class="captcha-wrapper">
              <el-input
                  v-model="form.captcha"
                  :prefix-icon="Picture"
                  placeholder="请输入验证码"
                  size="large"
              />
              <img
                  :src="captchaUrl"
                  class="captcha-img"
                  title="点击刷新"
                  alt="验证码"
                  @click="refreshCaptcha"
              />
            </div>
          </el-form-item>
          <el-form-item prop="role">
            <el-select v-model="form.role" placeholder="请选择角色" class="role-select" size="large">
              <el-option label="管理员" value="ADMIN" />
              <el-option label="教师" value="TEACHER" />
              <el-option label="学生" value="STUDENT" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button class="login-button" size="large" @click="login">登 录</el-button>
          </el-form-item>
          <div class="register-link">
            <div></div>
            <div>还没有账号？请 <router-link to="/register">注册</router-link></div>
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
const themeLabel = computed(() =>
    mode.value === 'dark' ? '当前深色模式，点击切换' : mode.value === 'light' ? '当前浅色模式，点击切换' : '当前跟随系统，点击切换'
)
const cycleTheme = () => {
  mode.value = mode.value === 'light' ? 'dark' : mode.value === 'dark' ? 'auto' : 'light'
}

const form = reactive<LoginForm>({
  username: '',
  password: '',
  captcha: '',
  role: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha:  [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  role:     [{ required: true, message: '请选择角色', trigger: 'change' }],
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
    ElMessage.error('获取验证码失败')
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
              // 从后端拉取该用户保存的主题偏好，覆盖本地默认，实现多端同步
              pullThemeFromServer()
              ElMessage.success('登录成功')
            } else {
              ElMessage.error(res.data.msg)
              refreshCaptcha()
            }
          })
          .catch(() => {
            ElMessage.error('登录失败')
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

/* 右上角主题切换按钮 */
.theme-toggle {
  position: absolute;
  top: 20px;
  right: 24px;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
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

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(118, 132, 255, 0.4);
  background: linear-gradient(135deg, #7bc4f5 0%, #6a78f0 100%);
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

<template>
  <div class="container">
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, Picture } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

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
  background: #f5f7fa;
  padding: 40px;
}

.login-box {
  width: 100%;
  max-width: 400px;
  padding: 50px 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.08);
}

.login-title {
  text-align: center;
  font-size: 28px;
  font-weight: bold;
  color: #1a2332;
  margin-bottom: 8px;
}

.login-subtitle {
  text-align: center;
  font-size: 14px;
  color: #909399;
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
  background: #f5f7fa !important;
  box-shadow: none !important;
  border-radius: 8px;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

:deep(.el-input__wrapper:hover) {
  background: #eef0f3 !important;
  border-color: #7684ff;
}

:deep(.el-input__wrapper.is-focus) {
  background: white !important;
  border-color: #7684ff;
  box-shadow: 0 0 0 3px rgba(118, 132, 255, 0.12) !important;
}

:deep(.el-input__inner) {
  color: #333 !important;
}

:deep(.el-input__inner::placeholder) {
  color: #aaa;
}

/* 前缀图标 */
:deep(.el-input__prefix-inner .el-icon) {
  color: #7684ff;
  font-size: 18px;
}

/* 下拉选择框 */
.role-select {
  width: 100%;
}

.role-select :deep(.el-input__wrapper) {
  background: #f5f7fa !important;
  box-shadow: none !important;
  border-radius: 8px;
  border: 1px solid transparent;
}

.role-select :deep(.el-input__wrapper:hover) {
  background: #eef0f3 !important;
  border-color: #7684ff;
}

.role-select :deep(.el-input__wrapper.is-focus) {
  background: white !important;
  border-color: #7684ff;
  box-shadow: 0 0 0 3px rgba(118, 132, 255, 0.12) !important;
}

.role-select :deep(.el-input__inner) {
  color: #333 !important;
}

.role-select :deep(.el-select__caret) {
  color: #7684ff;
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
  border: 1px solid #e8ecf1;
  transition: border-color 0.3s ease;
}

.captcha-img:hover {
  border-color: #7684ff;
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
  color: #606266;
  font-size: 14px;
}

.register-link a {
  color: #7684ff;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.3s ease;
}

.register-link a:hover {
  color: #4a5bcf;
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

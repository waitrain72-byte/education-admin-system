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

    <!-- 右侧注册框 -->
    <div class="right-panel">
      <div class="register-box">
        <div class="register-title">欢迎注册</div>
        <div class="register-subtitle">创建您的账号</div>
        <el-form ref="formRef" :model="form" :rules="rules" class="register-form">
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
          <el-form-item prop="confirmPass">
            <el-input
                v-model="form.confirmPass"
                :prefix-icon="Lock"
                placeholder="请确认密码"
                show-password
                size="large"
            />
          </el-form-item>
          <el-form-item>
            <el-button class="register-button" size="large" @click="register">注 册</el-button>
          </el-form-item>
          <div class="login-link">
            <div></div>
            <div>已有账号？请 <router-link to="/login">登录</router-link></div>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import request from '@/utils/request'

interface RegisterForm {
  username: string
  password: string
  confirmPass: string
  role: string
}

const router = useRouter()
const formRef = ref<FormInstance>()

const form = reactive<RegisterForm>({
  username: '',
  password: '',
  confirmPass: '',
  role: 'STUDENT',
})

// 确认密码校验
const validatePassword = (_rule: any, value: string, callback: (error?: Error) => void) => {
  if (value === '') {
    callback(new Error('请确认密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
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
          ElMessage.success('注册成功')
          router.push('/login')
        } else {
          ElMessage.error(res.data.msg || '注册失败')
        }
      }).catch(() => {
        ElMessage.error('注册失败，请稍后重试')
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
  background: #f5f7fa;
  padding: 40px;
}

.register-box {
  width: 100%;
  max-width: 400px;
  padding: 50px 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.08);
}

.register-title {
  text-align: center;
  font-size: 28px;
  font-weight: bold;
  color: #1a2332;
  margin-bottom: 8px;
}

.register-subtitle {
  text-align: center;
  font-size: 14px;
  color: #909399;
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
  box-shadow: 0 6px 20px rgba(118, 132, 255, 0.4);
  background: linear-gradient(135deg, #7bc4f5 0%, #6a78f0 100%);
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
  color: #606266;
  font-size: 14px;
}

.login-link a {
  color: #7684ff;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.3s ease;
}

.login-link a:hover {
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

  .register-box {
    padding: 30px 24px;
  }
}
</style>

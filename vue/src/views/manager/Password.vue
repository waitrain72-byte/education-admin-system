<template>
  <div>
    <el-card style="width: 50%">
      <el-form ref="formRef" :model="user" :rules="rules" label-width="100px" style="padding-right: 50px">
        <el-form-item label="原始密码" prop="password">
          <el-input v-model="user.password" show-password placeholder="原始密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="user.newPassword" show-password placeholder="新密码" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="user.confirmPassword" show-password placeholder="确认密码" />
        </el-form-item>
        <div style="text-align: center; margin-bottom: 20px">
          <el-button type="primary" @click="update">确认修改</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import request from '@/utils/request'
import { useUser } from '@/components/useUser.ts'

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
  if (value === '') { callback(new Error('请确认密码')) }
  else if (value !== user.newPassword) { callback(new Error('确认密码错误')) }
  else { callback() }
}

const rules: FormRules = {
  password: [{ required: true, message: '请输入原始密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmPassword: [{ validator: validatePassword, required: true, trigger: 'blur' }],
}

const update = () => {
  formRef.value?.validate((valid) => {
    if (valid) {
      request.put('/updatePassword', user).then((res: any) => {
        if (res.data.code === '200') {
          clearUser()
          ElMessage.success('修改密码成功')
          router.push('/login')
        } else { ElMessage.error(res.data.msg) }
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

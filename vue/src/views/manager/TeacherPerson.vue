<template>
  <div>
    <el-card style="width: 50%">
      <el-form :model="user" label-width="100px" style="padding-right: 50px">
        <div style="margin: 15px; text-align: center">
          <el-upload
            class="avatar-uploader"
            :action="'/api/files/upload'"
            :show-file-list="false"
            :headers="{ token: user.token }"
            :on-success="handleAvatarSuccess"
          >
            <img v-if="user.avatar" :src="user.avatar" class="avatar" />
            <div v-else class="avatar-placeholder">
              <el-icon :size="40" color="#8c939d"><Plus /></el-icon>
            </div>
          </el-upload>
        </div>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="user.username" placeholder="用户名" disabled />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="user.name" placeholder="姓名" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="user.phone" placeholder="电话" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="user.email" placeholder="邮箱" />
        </el-form-item>
        <el-form-item label="职称" prop="title">
          <el-input v-model="user.title" disabled />
        </el-form-item>
        <div style="text-align: center; margin-bottom: 20px">
          <el-button type="primary" @click="update">保 存</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'
const emit = defineEmits(['update:user'])

const user = reactive(JSON.parse(localStorage.getItem('xm-user') || '{}'))

const update = () => {
  request.put('/teacher/update', user).then((res: any) => {
    if (res.data.code === '200') {
      ElMessage.success('保存成功')
      localStorage.setItem('xm-user', JSON.stringify(user))
      emit('update:user')
    } else {
      ElMessage.error(res.data.msg)
    }
  })
}

const handleAvatarSuccess = (response: any) => {
  user.avatar = response.data
}
</script>

<style scoped>
.avatar-uploader {
  display: flex;
  justify-content: center;
}
:deep(.avatar-uploader .el-upload) {
  border: 1px dashed #d9d9d9;
  border-radius: 50%;
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.3s;
}
:deep(.avatar-uploader .el-upload:hover) {
  border-color: #409eff;
}
.avatar {
  width: 120px;
  height: 120px;
  display: block;
  border-radius: 50%;
  object-fit: cover;
}
.avatar-placeholder {
  width: 120px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}
</style>

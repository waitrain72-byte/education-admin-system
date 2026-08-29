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
        <el-form-item :label="$t('pages.person.usernameLabel')" prop="username">
          <el-input v-model="user.username" :placeholder="$t('pages.person.usernameLabel')" disabled />
        </el-form-item>
        <el-form-item :label="$t('pages.person.nameLabel')" prop="name">
          <el-input v-model="user.name" />
        </el-form-item>
        <el-form-item :label="$t('pages.person.phoneLabel')" prop="phone">
          <el-input v-model="user.phone" />
        </el-form-item>
        <el-form-item :label="$t('pages.person.emailLabel')" prop="email">
          <el-input v-model="user.email" />
        </el-form-item>
        <el-form-item :label="$t('pages.person.titleLabel')" prop="title">
          <el-input v-model="user.title" disabled />
        </el-form-item>
        <div style="text-align: center; margin-bottom: 20px">
          <el-button type="primary" @click="update">{{ $t('common.save') }}</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { t, apiMessage } from '@/i18n'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUser } from '@/components/useUser.ts'
const emit = defineEmits(['update:user'])

// 使用本地副本编辑，保存成功后才写回全局状态，避免表单输入污染登录状态
const { user: storeUser, updateUser } = useUser()
const user = reactive<Record<string, any>>({ ...storeUser.value })

const update = () => {
  request.put('/teacher/update', user).then((res: any) => {
    if (res.data.code === '200') {
      ElMessage.success(t('common.saveSuccess'))
      updateUser({ ...user })
      emit('update:user')
    } else {
      ElMessage.error(apiMessage(res.data))
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
  border: 1px dashed var(--xm-border-dashed);
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
  background: var(--xm-bg-hover);
}
</style>

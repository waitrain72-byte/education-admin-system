<template>
  <div>
    <div class="search">
      <el-input v-model="username" placeholder="请输入账号查询" style="width: 200px" />
      <el-button type="info" plain style="margin-left: 10px" @click="load(1)">查询</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">重置</el-button>
    </div>

    <div v-if="user.role !== 'STUDENT'" class="operation">
      <el-button type="primary" plain @click="handleAdd">新增</el-button>
      <el-button type="danger" plain @click="delBatch">批量删除</el-button>
    </div>

    <CrudTable
        :data="tableData"
        :columns="columns"
        :page-num="pageNum"
        :page-size="pageSize"
        :total="total"
        :loading="loading"
        :selectable="user.role !== 'STUDENT'"
        @selection-change="handleSelectionChange"
        @page-change="load"
    >
      <template #avatar="{ row }">
        <div style="display: flex; align-items: center">
          <el-image
v-if="row.avatar" style="width: 40px; height: 40px; border-radius: 50%"
                    :src="row.avatar" :preview-src-list="[row.avatar]" />
        </div>
      </template>
      <template #actions="{ row }">
        <el-button v-permission="'ADMIN'" size="small" type="primary" plain @click="handleEdit(row)">编辑</el-button>
        <el-button v-permission="'ADMIN'" size="small" type="warning" plain @click="resetPassword(row)">重置密码</el-button>
        <el-button v-permission="'ADMIN'" size="small" type="danger" plain @click="del(row.id)">删除</el-button>
      </template>
    </CrudTable>

    <el-dialog v-model="formVisible" title="教师信息" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="form" label-width="100px" style="padding-right: 50px" :rules="rules">
        <el-form-item label="头像">
          <el-upload
class="avatar-uploader" :action="baseUrl + '/files/upload'"
                     :headers="{ token: user.token }" list-type="picture" :on-success="handleAvatarSuccess">
            <el-button type="primary">上传头像</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="姓名" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" placeholder="电话" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="邮箱" />
        </el-form-item>
        <el-form-item label="职称" prop="title">
          <el-input v-model="form.title" placeholder="职称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取 消</el-button>
        <el-button type="primary" @click="save">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, inject } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useUser } from '@/components/useUser.ts'
import { useCrud } from '@/composables/useCrud'
import CrudTable, { type CrudColumn } from '@/components/CrudTable.vue'

const baseUrl = import.meta.env.VITE_BASE_URL || 'http://localhost:9091'
const { user, patchUser } = useUser()
const refreshUser = inject('refreshUser', () => {})
const username = ref('')

const {
  tableData, pageNum, pageSize, total, loading,
  formVisible, form, formRef, rules,
  load, handleAdd, handleEdit, save, del, delBatch,
  handleSelectionChange,
} = useCrud({
  url: '/teacher',
  rules: {
    username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  },
  getParams: () => ({ username: username.value }),
  afterSave: (formData) => {
    // 如果修改的是当前登录用户自己的信息，同步全局状态
    if (formData.id === user.value.id) {
      patchUser({
        avatar: formData.avatar,
        name: formData.name,
        phone: formData.phone,
        email: formData.email,
      })
      refreshUser()
    }
  },
})

const columns: CrudColumn[] = [
  { prop: 'id', label: '序号', width: 70, align: 'center', sortable: true },
  { prop: 'avatar', label: '头像' },
  { prop: 'username', label: '账号' },
  { prop: 'name', label: '姓名' },
  { prop: 'phone', label: '电话' },
  { prop: 'email', label: '邮箱' },
  { prop: 'role', label: '角色' },
  { prop: 'title', label: '职称' },
]

const resetPassword = (row: any) => {
  ElMessageBox.confirm(`确定将账号 ${row.username} 的密码重置为 123456 吗？`, '重置密码', { type: 'warning' }).then(() => {
    request.put('/teacher/resetPassword/' + row.id).then((res: any) => {
      if (res.data.code === '200') {
        ElMessage.success('密码已重置为 123456')
      } else {
        ElMessage.error(res.data.msg)
      }
    })
  }).catch(() => {})
}

const handleAvatarSuccess = (response: any) => {
  form.value.avatar = response.data
}

const reset = () => {
  username.value = ''
  load(1)
}

onMounted(() => load(1))
</script>

<style scoped></style>

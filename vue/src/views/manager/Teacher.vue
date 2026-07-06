<template>
  <div>
    <div class="search">
      <el-input placeholder="请输入账号查询" style="width: 200px" v-model="username" />
      <el-button type="info" plain style="margin-left: 10px" @click="load(1)">查询</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">重置</el-button>
    </div>

    <div class="operation" v-if="user.role !== 'STUDENT'">
      <el-button type="primary" plain @click="handleAdd">新增</el-button>
      <el-button type="danger" plain @click="delBatch">批量删除</el-button>
    </div>

    <div class="table">
      <el-table :data="tableData" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" v-if="user.role !== 'STUDENT'" />
        <el-table-column prop="id" label="序号" width="70" align="center" sortable />
        <el-table-column label="头像">
          <template #default="scope">
            <div style="display: flex; align-items: center">
              <el-image v-if="scope.row.avatar" style="width: 40px; height: 40px; border-radius: 50%"
                        :src="scope.row.avatar" :preview-src-list="[scope.row.avatar]" />
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="账号" />
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="phone" label="电话" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="role" label="角色" />
        <el-table-column prop="title" label="职称" />
        <el-table-column label="操作" align="center" width="260">
          <template #default="scope">
            <el-button size="small" type="primary" plain @click="handleEdit(scope.row)" :disabled="user.role !== 'ADMIN'">编辑</el-button>
            <el-button size="small" type="warning" plain @click="resetPassword(scope.row)" :disabled="user.role !== 'ADMIN'">重置密码</el-button>
            <el-button size="small" type="danger" plain @click="del(scope.row.id)" :disabled="user.role !== 'ADMIN'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination background @current-change="handleCurrentChange" :current-page="pageNum"
                       :page-sizes="[5, 10, 20]" :page-size="pageSize" layout="total, prev, pager, next" :total="total" />
      </div>
    </div>

    <el-dialog title="教师信息" v-model="fromVisible" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form :model="form" label-width="100px" style="padding-right: 50px" :rules="rules" ref="formRef">
        <el-form-item label="头像">
          <el-upload class="avatar-uploader" :action="baseUrl + '/files/upload'"
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
        <el-button @click="fromVisible = false">取 消</el-button>
        <el-button type="primary" @click="save">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, inject } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import request from '@/utils/request'
import { useUser } from '@/components/useUser.ts'

const baseUrl = import.meta.env.VITE_BASE_URL || 'http://localhost:9091'

// 使用全局用户状态
const { user, patchUser } = useUser()

// 从父组件获取刷新方法
const refreshUser = inject('refreshUser', () => {})

const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const username = ref('')
const fromVisible = ref(false)
const form = ref<Record<string, any>>({})
const formRef = ref<FormInstance>()
const ids = ref<number[]>([])

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }]
}

const handleAdd = () => {
  form.value = {}
  fromVisible.value = true
}

const handleEdit = (row: any) => {
  form.value = JSON.parse(JSON.stringify(row))
  fromVisible.value = true
}

const save = () => {
  formRef.value?.validate((valid) => {
    if (valid) {
      request({
        url: form.value.id ? '/teacher/update' : '/teacher/add',
        method: form.value.id ? 'PUT' : 'POST',
        data: form.value
      })
          .then((res: any) => {
            if (res.data.code === '200') {
              ElMessage.success('保存成功')

              // 🔥 如果修改的是当前登录用户自己的信息，更新全局状态
              if (form.value.id === user.value.id) {
                patchUser({
                  avatar: form.value.avatar,
                  name: form.value.name,
                  phone: form.value.phone,
                  email: form.value.email
                })
                // 通知父组件刷新
                refreshUser()
              }

              load(1)
              fromVisible.value = false
            } else {
              ElMessage.error(res.data.msg)
            }
          })
    }
  })
}

const del = (id: number) => {
  ElMessageBox.confirm('您确定删除吗？', '确认删除', { type: 'warning' }).then(() => {
    request.delete('/teacher/delete/' + id).then((res: any) => {
      if (res.data.code === '200') {
        ElMessage.success('操作成功')
        load(1)
      } else {
        ElMessage.error(res.data.msg)
      }
    })
  }).catch(() => {})
}

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

const handleSelectionChange = (rows: any[]) => {
  ids.value = rows.map(v => v.id)
}

const delBatch = () => {
  if (!ids.value.length) {
    ElMessage.warning('请选择数据')
    return
  }
  ElMessageBox.confirm('您确定批量删除这些数据吗？', '确认删除', { type: 'warning' }).then(() => {
    request.delete('/teacher/delete/batch', { data: ids.value }).then((res: any) => {
      if (res.data.code === '200') {
        ElMessage.success('操作成功')
        load(1)
      } else {
        ElMessage.error(res.data.msg)
      }
    })
  }).catch(() => {})
}

const load = (pNum?: number) => {
  if (pNum) pageNum.value = pNum
  request.get('/teacher/selectPage', {
    params: {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      username: username.value
    }
  })
      .then((res: any) => {
        tableData.value = res.data?.data?.list || []
        total.value = res.data?.data?.total || 0
      })
}

const reset = () => {
  username.value = ''
  load(1)
}

const handleCurrentChange = (pNum: number) => load(pNum)

const handleAvatarSuccess = (response: any) => {
  form.value.avatar = response.data
}

onMounted(() => load(1))
</script>

<style scoped></style>

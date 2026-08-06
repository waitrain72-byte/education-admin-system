<template>
  <div>
    <div class="search">
      <el-input v-model="content" placeholder="请输入请假说明" style="width: 200px" />
      <el-select v-model="status" placeholder="请选择状态" style="width: 200px; margin-left: 5px">
        <el-option label="待审核" value="待审核" />
        <el-option label="审核通过" value="审核通过" />
        <el-option label="审核不通过" value="审核不通过" />
      </el-select>
      <el-button type="info" plain style="margin-left: 10px" @click="load(1)">查询</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">重置</el-button>
    </div>

    <div v-if="user.role === 'STUDENT'" class="operation">
      <el-button type="primary" plain @click="handleAdd">请假申请</el-button>
    </div>

    <CrudTable
        :data="tableData"
        :columns="columns"
        :page-num="pageNum"
        :page-size="pageSize"
        :total="total"
        :loading="loading"
        @page-change="load"
    >
      <template #actions="{ row }">
        <el-button
v-if="user.role === 'STUDENT' && row.status !== '审核通过'" plain type="primary" size="small"
                   @click="handleEdit(row)">编辑</el-button>
        <el-button
v-if="user.role === 'ADMIN' && row.status === '待审核'" plain type="primary" size="small"
                   @click="handleCheck(row)">审核</el-button>
        <el-button
v-if="user.role === 'STUDENT' && row.status === '待审核'" plain type="danger" size="small"
                   @click="del(row.id)">撤销申请</el-button>
      </template>
    </CrudTable>

    <el-dialog v-model="formVisible" title="请假信息" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" label-width="100px" style="padding-right: 50px" :model="form" :rules="rules">
        <el-form-item prop="content" label="请假说明">
          <el-input v-model="form.content" type="textarea" :rows="4" autocomplete="off" />
        </el-form-item>
        <el-form-item prop="time" label="请假时间">
          <el-date-picker
v-model="form.time" style="width: 100%" value-format="YYYY-MM-DD"
                          type="date" placeholder="选择日期" />
        </el-form-item>
        <el-form-item prop="day" label="请假天数">
          <el-input v-model="form.day" autocomplete="off" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取 消</el-button>
        <el-button type="primary" @click="save">确 定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="checkVisible" title="请假审核" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form label-width="100px" style="padding-right: 50px" :model="form">
        <el-form-item prop="status" label="审核状态">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="待审核" value="待审核" />
            <el-option label="审核通过" value="审核通过" />
            <el-option label="审核不通过" value="审核不通过" />
          </el-select>
        </el-form-item>
        <el-form-item prop="descr" label="审核说明">
          <el-input v-model="form.descr" type="textarea" :rows="4" autocomplete="off" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="checkVisible = false">取 消</el-button>
        <el-button type="primary" @click="check">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUser } from '@/components/useUser.ts'
import { useCrud } from '@/composables/useCrud'
import CrudTable, { type CrudColumn } from '@/components/CrudTable.vue'

const { user } = useUser()
const content = ref('')
const status = ref('')
const checkVisible = ref(false)

const {
  tableData, pageNum, pageSize, total, loading,
  formVisible, form, formRef, rules,
  load, save, del,
} = useCrud({
  url: '/apply',
  deleteConfirmMessage: '您确定撤销申请吗？',
  rules: {
    time: [{ required: true, message: '请输入请假时间', trigger: 'blur' }],
    content: [{ required: true, message: '请输入请假说明', trigger: 'blur' }],
    day: [{ required: true, message: '请输入请假天数', trigger: 'blur' }],
  },
  getParams: () => ({ status: status.value, content: content.value }),
})

const columns: CrudColumn[] = [
  { prop: 'id', label: '序号', width: 80, align: 'center', sortable: true },
  { prop: 'studentName', label: '学生姓名', showOverflowTooltip: true },
  { prop: 'content', label: '请假说明', showOverflowTooltip: true },
  { prop: 'time', label: '请假时间' },
  { prop: 'day', label: '请假天数' },
  { prop: 'status', label: '审核状态' },
  { prop: 'descr', label: '审核说明' },
]

const handleAdd = () => {
  form.value = { studentId: user.value.id, status: '待审核' }
  formVisible.value = true
}

const handleEdit = (row: any) => {
  form.value = JSON.parse(JSON.stringify(row))
  form.value.status = '待审核'
  form.value.descr = ''
  formVisible.value = true
}

const handleCheck = (row: any) => {
  form.value = JSON.parse(JSON.stringify(row))
  checkVisible.value = true
}

const check = () => {
  request.put('/apply/update', form.value).then((res: any) => {
    if (res.data.code === '200') {
      ElMessage.success('操作成功')
      load(1)
      checkVisible.value = false
    } else {
      ElMessage.error(res.data.msg)
    }
  })
}

const reset = () => {
  status.value = ''
  content.value = ''
  load(1)
}

onMounted(() => load(1))
</script>

<style scoped></style>

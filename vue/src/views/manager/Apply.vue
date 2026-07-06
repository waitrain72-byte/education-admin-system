<template>
  <div>
    <div class="search">
      <el-input placeholder="请输入请假说明" style="width: 200px" v-model="content"></el-input>
      <el-select v-model="status" placeholder="请选择状态" style="width: 200px; margin-left: 5px">
        <el-option label="待审核" value="待审核" />
        <el-option label="审核通过" value="审核通过" />
        <el-option label="审核不通过" value="审核不通过" />
      </el-select>
      <el-button type="info" plain style="margin-left: 10px" @click="load(1)">查询</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">重置</el-button>
    </div>

    <div class="operation" v-if="user.role === 'STUDENT'">
      <el-button type="primary" plain @click="handleAdd">请假申请</el-button>
    </div>

    <div class="table">
      <el-table :data="tableData" stripe>
        <el-table-column prop="id" label="序号" width="80" align="center" sortable />
        <el-table-column prop="studentName" label="学生姓名" show-overflow-tooltip />
        <el-table-column prop="content" label="请假说明" show-overflow-tooltip />
        <el-table-column prop="time" label="请假时间" />
        <el-table-column prop="day" label="请假天数" />
        <el-table-column prop="status" label="审核状态" />
        <el-table-column prop="descr" label="审核说明" />
        <el-table-column label="操作" width="180" align="center">
          <template #default="scope">
            <el-button plain type="primary" @click="handleEdit(scope.row)" size="small"
              v-if="user.role === 'STUDENT' && scope.row.status !== '审核通过'">编辑</el-button>
            <el-button plain type="primary" @click="handleCheck(scope.row)" size="small"
              v-if="user.role === 'ADMIN' && scope.row.status === '待审核'">审核</el-button>
            <el-button plain type="danger" size="small" @click="del(scope.row.id)"
              v-if="user.role === 'STUDENT' && scope.row.status === '待审核'">撤销申请</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination background @current-change="handleCurrentChange" :current-page="pageNum"
          :page-sizes="[5, 10, 20]" :page-size="pageSize" layout="total, prev, pager, next" :total="total" />
      </div>
    </div>

    <el-dialog title="请假信息" v-model="fromVisible" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form label-width="100px" style="padding-right: 50px" :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="content" label="请假说明">
          <el-input type="textarea" :rows="4" v-model="form.content" autocomplete="off" />
        </el-form-item>
        <el-form-item prop="time" label="请假时间">
          <el-date-picker style="width: 100%" v-model="form.time" value-format="YYYY-MM-DD"
            type="date" placeholder="选择日期" />
        </el-form-item>
        <el-form-item prop="day" label="请假天数">
          <el-input v-model="form.day" autocomplete="off" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fromVisible = false">取 消</el-button>
        <el-button type="primary" @click="save">确 定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="请假审核" v-model="checkVisible" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form label-width="100px" style="padding-right: 50px" :model="form">
        <el-form-item prop="status" label="审核状态">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="待审核" value="待审核" />
            <el-option label="审核通过" value="审核通过" />
            <el-option label="审核不通过" value="审核不通过" />
          </el-select>
        </el-form-item>
        <el-form-item prop="descr" label="审核说明">
          <el-input type="textarea" :rows="4" v-model="form.descr" autocomplete="off" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import request from '@/utils/request'

const user = reactive(JSON.parse(localStorage.getItem('xm-user') || '{}'))
const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const status = ref(null)
const content = ref(null)
const fromVisible = ref(false)
const checkVisible = ref(false)
const form = ref<Record<string, any>>({})
const formRef = ref<FormInstance>()
const ids = ref<number[]>([])

const rules: FormRules = {
  time: [{ required: true, message: '请输入请假时间', trigger: 'blur' }],
  content: [{ required: true, message: '请输入请假说明', trigger: 'blur' }],
  day: [{ required: true, message: '请输入请假天数', trigger: 'blur' }],
}

const handleAdd = () => {
  form.value = { studentId: user.id, status: '待审核' }
  fromVisible.value = true
}
const handleEdit = (row: any) => {
  form.value = JSON.parse(JSON.stringify(row))
  form.value.status = '待审核'
  form.value.descr = ''
  fromVisible.value = true
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
    } else { ElMessage.error(res.data.msg) }
  })
}
const save = () => {
  formRef.value?.validate((valid) => {
    if (valid) {
      request({ url: form.value.id ? '/apply/update' : '/apply/add', method: form.value.id ? 'PUT' : 'POST', data: form.value })
        .then((res: any) => {
          if (res.data.code === '200') { ElMessage.success('保存成功'); load(1); fromVisible.value = false }
          else { ElMessage.error(res.data.msg) }
        })
    }
  })
}
const del = (id: number) => {
  ElMessageBox.confirm('您确定撤销申请吗？', '灵魂拷问', { type: 'warning' }).then(() => {
    request.delete('/apply/delete/' + id).then((res: any) => {
      if (res.data.code === '200') { ElMessage.success('操作成功'); load(1) }
      else { ElMessage.error(res.data.msg) }
    })
  }).catch(() => {})
}
const load = (pNum?: number) => {
  if (pNum) pageNum.value = pNum
  request.get('/apply/selectPage', { params: { pageNum: pageNum.value, pageSize: pageSize.value, status: status.value, content: content.value } })
    .then((res: any) => { tableData.value = res.data?.data?.list; total.value = res.data?.data?.total })
}
const reset = () => { status.value = null; content.value = null; load(1) }
const handleCurrentChange = (pNum: number) => load(pNum)

onMounted(() => load(1))
</script>

<style scoped></style>

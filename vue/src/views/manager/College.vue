<template>
  <div>
    <div class="search">
      <el-input placeholder="请输入学院名称" style="width: 200px" v-model="name" />
      <el-button type="info" plain style="margin-left: 10px" @click="load(1)">查询</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">重置</el-button>
    </div>
    <div class="operation" v-if="user.role === 'ADMIN'">
      <el-button type="primary" plain @click="handleAdd">新增</el-button>
      <el-button type="danger" plain @click="delBatch">批量删除</el-button>
    </div>
    <div class="table">
      <el-table :data="tableData" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" v-if="user.role === 'ADMIN'" />
        <el-table-column prop="id" label="序号" width="80" align="center" sortable />
        <el-table-column prop="name" label="学院名称" show-overflow-tooltip />
        <el-table-column prop="content" label="学院介绍" show-overflow-tooltip />
        <el-table-column label="操作" width="180" align="center" v-if="user.role === 'ADMIN'">
          <template #default="scope">
            <el-button plain type="primary" @click="handleEdit(scope.row)" size="small">编辑</el-button>
            <el-button plain type="danger" size="small" @click="del(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination background @current-change="handleCurrentChange" :current-page="pageNum"
          :page-sizes="[5, 10, 20]" :page-size="pageSize" layout="total, prev, pager, next" :total="total" />
      </div>
    </div>

    <el-dialog title="信息" v-model="fromVisible" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form label-width="100px" style="padding-right: 50px" :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="name" label="学院名称">
          <el-input v-model="form.name" autocomplete="off" />
        </el-form-item>
        <el-form-item prop="content" label="学院介绍">
          <el-input type="textarea" :rows="5" v-model="form.content" autocomplete="off" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import request from '@/utils/request'

const user = reactive(JSON.parse(localStorage.getItem('xm-user') || '{}'))
const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const name = ref(null)
const fromVisible = ref(false)
const form = ref<Record<string, any>>({})
const formRef = ref<FormInstance>()
const ids = ref<number[]>([])

const rules: FormRules = { name: [{ required: true, message: '请输入学院名称', trigger: 'blur' }] }

const handleAdd = () => { form.value = {}; fromVisible.value = true }
const handleEdit = (row: any) => { form.value = JSON.parse(JSON.stringify(row)); fromVisible.value = true }
const save = () => {
  formRef.value?.validate((valid) => {
    if (valid) {
      request({ url: form.value.id ? '/college/update' : '/college/add', method: form.value.id ? 'PUT' : 'POST', data: form.value })
        .then((res: any) => {
          if (res.data.code === '200') { ElMessage.success('保存成功'); load(1); fromVisible.value = false }
          else { ElMessage.error(res.data.msg) }
        })
    }
  })
}
const del = (id: number) => {
  ElMessageBox.confirm('您确定删除吗？', '确认删除', { type: 'warning' }).then(() => {
    request.delete('/college/delete/' + id).then((res: any) => {
      if (res.data.code === '200') { ElMessage.success('操作成功'); load(1) }
      else { ElMessage.error(res.data.msg) }
    })
  }).catch(() => {})
}
const handleSelectionChange = (rows: any[]) => { ids.value = rows.map(v => v.id) }
const delBatch = () => {
  if (!ids.value.length) { ElMessage.warning('请选择数据'); return }
  ElMessageBox.confirm('您确定批量删除这些数据吗？', '确认删除', { type: 'warning' }).then(() => {
    request.delete('/college/delete/batch', { data: ids.value }).then((res: any) => {
      if (res.data.code === '200') { ElMessage.success('操作成功'); load(1) }
      else { ElMessage.error(res.data.msg) }
    })
  }).catch(() => {})
}
const load = (pNum?: number) => {
  if (pNum) pageNum.value = pNum
  request.get('/college/selectPage', { params: { pageNum: pageNum.value, pageSize: pageSize.value, name: name.value } })
    .then((res: any) => { tableData.value = res.data?.data?.list; total.value = res.data?.data?.total })
}
const reset = () => { name.value = null; load(1) }
const handleCurrentChange = (pNum: number) => load(pNum)

onMounted(() => load(1))
</script>

<style scoped></style>

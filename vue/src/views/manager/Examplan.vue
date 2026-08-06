<template>
  <div>
    <div class="search">
      <el-input v-model="name" placeholder="请输入标题查询" style="width: 200px" />
      <el-button type="info" plain style="margin-left: 10px" @click="load(1)">查询</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">重置</el-button>
    </div>
    <div v-if="user.role === 'ADMIN'" class="operation">
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
        :selectable="user.role === 'ADMIN'"
        :show-actions="user.role === 'ADMIN'"
        @selection-change="handleSelectionChange"
        @page-change="load"
    >
      <template #actions="{ row }">
        <el-button plain type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
        <el-button plain type="danger" size="small" @click="del(row.id)">删除</el-button>
      </template>
    </CrudTable>

    <el-dialog v-model="formVisible" title="信息" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" label-width="100px" style="padding-right: 50px" :model="form" :rules="rules">
        <el-form-item prop="name" label="标题">
          <el-input v-model="form.name" autocomplete="off" />
        </el-form-item>
        <el-form-item prop="content" label="内容">
          <el-input v-model="form.content" type="textarea" :rows="5" autocomplete="off" />
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
import { ref, onMounted } from 'vue'
import { useUser } from '@/components/useUser.ts'
import { useCrud } from '@/composables/useCrud'
import CrudTable, { type CrudColumn } from '@/components/CrudTable.vue'

const { user } = useUser()
const name = ref('')

const {
  tableData, pageNum, pageSize, total, loading,
  formVisible, form, formRef, rules,
  load, handleAdd, handleEdit, save, del,
  handleSelectionChange, delBatch,
} = useCrud({
  url: '/examplan',
  rules: {
    name: [{ required: true, message: '请输入标题', trigger: 'blur' }],
    content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
  },
  getParams: () => ({ name: name.value }),
})

const columns: CrudColumn[] = [
  { prop: 'id', label: '序号', width: 80, align: 'center', sortable: true },
  { prop: 'name', label: '标题', showOverflowTooltip: true },
  { prop: 'content', label: '内容', showOverflowTooltip: true },
  { prop: 'time', label: '创建时间' },
]

const reset = () => {
  name.value = ''
  load(1)
}

onMounted(() => load(1))
</script>

<style scoped></style>

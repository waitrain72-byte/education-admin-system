<template>
  <div>
    <CrudTable
        :data="tableData"
        :columns="columns"
        :page-num="pageNum"
        :page-size="pageSize"
        :total="total"
        :loading="loading"
        :show-actions="user.role === 'STUDENT'"
        @page-change="load"
    >
      <template #actions="{ row }">
        <el-button link type="danger" size="small" :disabled="row.status !== '未开课'" @click="del(row.id)">取消选课</el-button>
        <el-button link type="primary" size="small" :disabled="row.status !== '已结课'" @click="initComment(row)">评教</el-button>
      </template>
    </CrudTable>

    <el-dialog v-model="formVisible" title="请填写评教信息" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" label-width="100px" style="padding-right: 50px" :model="form" :rules="rules">
        <el-form-item prop="content" label="评教内容">
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
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import request from '@/utils/request'
import { useUser } from '@/components/useUser.ts'
import { useCrud } from '@/composables/useCrud'
import CrudTable, { type CrudColumn } from '@/components/CrudTable.vue'

const { user } = useUser()

// 分页/删除复用通用 CRUD，取消选课的确认文案自定义
const { tableData, pageNum, pageSize, total, loading, load, del } = useCrud({
  url: '/choice',
  deleteConfirmMessage: '您确定取消选这门课吗？这个老师的课不好选哦！！',
})

const formVisible = ref(false)
const form = ref<Record<string, any>>({})
const formRef = ref<FormInstance>()

const rules: FormRules = {
  content: [{ required: true, message: '请输入评教内容', trigger: 'blur' }],
}

const columns: CrudColumn[] = [
  { prop: 'id', label: '序号', width: 80, align: 'center', sortable: true },
  { prop: 'name', label: '课程名称', showOverflowTooltip: true },
  { prop: 'type', label: '课程类型', showOverflowTooltip: true },
  { prop: 'teacherName', label: '授课教师', showOverflowTooltip: true },
  { prop: 'score', label: '学分', showOverflowTooltip: true },
  { prop: 'num', label: '上课人数', showOverflowTooltip: true },
  { prop: 'room', label: '上课教室', showOverflowTooltip: true },
  { prop: 'week', label: '周几', showOverflowTooltip: true },
  { prop: 'segment', label: '第几大节', showOverflowTooltip: true },
  { prop: 'status', label: '上课状态', showOverflowTooltip: true },
  { prop: 'studentName', label: '选课学生', showOverflowTooltip: true },
]

const initComment = (row: any) => {
  form.value = JSON.parse(JSON.stringify(row))
  formVisible.value = true
}

// 评教提交逻辑与普通 CRUD 不同（固定写 /comment/add），保持页面内实现
const save = () => {
  formRef.value?.validate((valid) => {
    if (!valid) return
    const data = {
      name: form.value.name,
      teacher: form.value.teacherName,
      student: user.value.name,
      content: form.value.content,
    }
    request.post('/comment/add', data).then((res: any) => {
      if (res.data.code === '200') {
        ElMessage.success('评教成功')
        formVisible.value = false
      } else {
        ElMessage.error(res.data.msg)
      }
    })
  })
}

onMounted(() => load(1))
</script>

<style scoped></style>

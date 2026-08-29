<template>
  <div>
    <div class="search">
      <el-input v-model="name" placeholder="请输入课程名称" style="width: 200px" />
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
        @selection-change="handleSelectionChange"
        @page-change="load"
    >
      <template #actions="{ row }">
        <el-button v-if="user.role !== 'STUDENT'" link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
        <el-button v-if="user.role === 'STUDENT'" link type="primary" size="small" :disabled="row.status !== '未开课'" @click="choiceCourse(row)">选课</el-button>
        <el-button v-if="user.role === 'ADMIN'" link type="danger" size="small" @click="del(row.id)">删除</el-button>
      </template>
    </CrudTable>

    <el-dialog v-model="formVisible" title="信息" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" label-width="100px" style="padding-right: 50px" :model="form" :rules="rules">
        <el-form-item prop="name" label="课程名称">
          <el-input v-model="form.name" autocomplete="off" :disabled="user.role !== 'ADMIN'" />
        </el-form-item>
        <el-form-item prop="type" label="课程类型">
          <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%" :disabled="user.role !== 'ADMIN'">
            <el-option label="必修" value="必修" /><el-option label="选修" value="选修" />
          </el-select>
        </el-form-item>
        <el-form-item prop="teacherId" label="授课教师">
          <el-select v-model="form.teacherId" placeholder="请选择教师" style="width: 100%" :disabled="user.role !== 'ADMIN'">
            <el-option v-for="item in teacherData" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item prop="score" label="学分">
          <el-input v-model="form.score" autocomplete="off" :disabled="user.role !== 'ADMIN'" />
        </el-form-item>
        <el-form-item prop="num" label="上课人数">
          <el-input v-model="form.num" autocomplete="off" :disabled="user.role !== 'ADMIN'" />
        </el-form-item>
        <el-form-item prop="room" label="上课教室">
          <el-input v-model="form.room" autocomplete="off" :disabled="user.role !== 'ADMIN'" />
        </el-form-item>
        <el-form-item prop="week" label="周几">
          <el-select v-model="form.week" placeholder="请选择" style="width: 100%" :disabled="user.role !== 'ADMIN'">
            <el-option v-for="d in ['星期一','星期二','星期三','星期四','星期五','星期六','星期日']" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item prop="segment" label="第几大节">
          <el-select v-model="form.segment" placeholder="请选择" style="width: 100%" :disabled="user.role !== 'ADMIN'">
            <el-option label="第一大节（08:30 ~ 10:10）" value="第一大节（08:30 ~ 10:10）" />
            <el-option label="第二大节（10:30 ~ 12:10）" value="第二大节（10:30 ~ 12:10）" />
            <el-option label="第三大节（14:00 ~ 15:40）" value="第三大节（14:00 ~ 15:40）" />
            <el-option label="第四大节（16:00 ~ 17:40）" value="第四大节（16:00 ~ 17:40）" />
            <el-option label="第五大节（19:00 ~ 20:40）" value="第五大节（19:00 ~ 20:40）" />
          </el-select>
        </el-form-item>
        <el-form-item prop="status" label="上课状态">
          <el-select v-model="form.status" placeholder="请选择" style="width: 100%">
            <el-option label="未开课" value="未开课" /><el-option label="已开课" value="已开课" /><el-option label="已结课" value="已结课" />
          </el-select>
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
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUser } from '@/components/useUser.ts'
import { useCrud } from '@/composables/useCrud'
import CrudTable, { type CrudColumn } from '@/components/CrudTable.vue'

const { user } = useUser()
const name = ref('')
const teacherData = ref<any[]>([])

const {
  tableData, pageNum, pageSize, total, loading,
  formVisible, form, formRef, rules,
  load, handleAdd, handleEdit, save, del, delBatch,
  handleSelectionChange,
} = useCrud({
  url: '/course',
  rules: {
    name: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  },
  getParams: () => ({ name: name.value }),
})

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
]

const choiceCourse = (row: any) => {
  request.post('/choice/add', { studentId: user.value.id, teacherId: row.teacherId, courseId: row.id }).then((res: any) => {
    if (res.data.code === '200') {
      ElMessage.success('选课成功')
    } else {
      ElMessage.error(res.data.msg)
    }
  })
}

const loadTeacher = () => {
  request.get('/teacher/selectAll').then((res: any) => {
    if (res.data.code === '200') {
      teacherData.value = res.data.data
    } else {
      ElMessage.error(res.data.msg)
    }
  })
}

const reset = () => {
  name.value = ''
  load(1)
}

onMounted(() => {
  load(1)
  loadTeacher()
})
</script>

<style scoped></style>

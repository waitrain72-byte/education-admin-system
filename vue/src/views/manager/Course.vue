<template>
  <div>
    <div class="search">
      <el-input v-model="name" :placeholder="$t('pages.course.searchPlaceholder')" style="width: 200px" />
      <el-button type="info" plain style="margin-left: 10px" @click="load(1)">{{ $t('common.search') }}</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">{{ $t('common.reset') }}</el-button>
    </div>
    <div v-if="user.role === 'ADMIN'" class="operation">
      <el-button type="primary" plain @click="handleAdd">{{ $t('common.add') }}</el-button>
      <el-button type="danger" plain @click="delBatch">{{ $t('common.batchDelete') }}</el-button>
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
        <el-button v-if="user.role !== 'STUDENT'" link type="primary" size="small" @click="handleEdit(row)">{{ $t('common.edit') }}</el-button>
        <el-button v-if="user.role === 'STUDENT'" link type="primary" size="small" :disabled="row.status !== '未开课'" @click="choiceCourse(row)">{{ $t('pages.course.choice') }}</el-button>
        <el-button v-if="user.role === 'ADMIN'" link type="danger" size="small" @click="del(row.id)">{{ $t('common.delete') }}</el-button>
      </template>
    </CrudTable>

    <el-dialog v-model="formVisible" :title="$t('pages.course.dialogTitle')" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" label-width="100px" style="padding-right: 50px" :model="form" :rules="rules">
        <el-form-item prop="name" :label="$t('pages.course.name')">
          <el-input v-model="form.name" autocomplete="off" :disabled="user.role !== 'ADMIN'" />
        </el-form-item>
        <el-form-item prop="type" :label="$t('pages.course.type')">
          <el-select v-model="form.type" :placeholder="$t('pages.course.typePlaceholder')" style="width: 100%" :disabled="user.role !== 'ADMIN'">
            <el-option :label="$t('pages.course.required')" value="必修" /><el-option :label="$t('pages.course.elective')" value="选修" />
          </el-select>
        </el-form-item>
        <el-form-item prop="teacherId" :label="$t('pages.course.teacher')">
          <el-select v-model="form.teacherId" :placeholder="$t('pages.course.teacherPlaceholder')" style="width: 100%" :disabled="user.role !== 'ADMIN'">
            <el-option v-for="item in teacherData" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item prop="score" :label="$t('pages.course.score')">
          <el-input v-model="form.score" autocomplete="off" :disabled="user.role !== 'ADMIN'" />
        </el-form-item>
        <el-form-item prop="num" :label="$t('pages.course.num')">
          <el-input v-model="form.num" autocomplete="off" :disabled="user.role !== 'ADMIN'" />
        </el-form-item>
        <el-form-item prop="room" :label="$t('pages.course.room')">
          <el-input v-model="form.room" autocomplete="off" :disabled="user.role !== 'ADMIN'" />
        </el-form-item>
        <el-form-item prop="week" :label="$t('pages.course.week')">
          <el-select v-model="form.week" :placeholder="$t('pages.course.selectPlaceholder')" style="width: 100%" :disabled="user.role !== 'ADMIN'">
            <el-option v-for="d in weekOptions" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item prop="segment" :label="$t('pages.course.segment')">
          <el-select v-model="form.segment" :placeholder="$t('pages.course.selectPlaceholder')" style="width: 100%" :disabled="user.role !== 'ADMIN'">
            <el-option v-for="s in segmentOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item prop="status" :label="$t('pages.course.status')">
          <el-select v-model="form.status" :placeholder="$t('pages.course.selectPlaceholder')" style="width: 100%">
            <el-option :label="$t('pages.course.notStarted')" value="未开课" /><el-option :label="$t('pages.course.started')" value="已开课" /><el-option :label="$t('pages.course.finished')" value="已结课" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="save">{{ $t('common.ok') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUser } from '@/components/useUser.ts'
import { useCrud } from '@/composables/useCrud'
import { apiMessage, t } from '@/i18n'
import CrudTable, { type CrudColumn } from '@/components/CrudTable.vue'

const { user } = useUser()
const name = ref('')
const teacherData = ref<any[]>([])

// 星期/大节下拉选项：label 走翻译，value 保持中文（数据库按中文存储）
const weekOptions = computed(() => [
  { value: '星期一', label: t('pages.course.monday') },
  { value: '星期二', label: t('pages.course.tuesday') },
  { value: '星期三', label: t('pages.course.wednesday') },
  { value: '星期四', label: t('pages.course.thursday') },
  { value: '星期五', label: t('pages.course.friday') },
  { value: '星期六', label: t('pages.course.saturday') },
  { value: '星期日', label: t('pages.course.sunday') },
])

const segmentOptions = computed(() => [
  { value: '第一大节（08:30 ~ 10:10）', label: t('pages.course.segment1') },
  { value: '第二大节（10:30 ~ 12:10）', label: t('pages.course.segment2') },
  { value: '第三大节（14:00 ~ 15:40）', label: t('pages.course.segment3') },
  { value: '第四大节（16:00 ~ 17:40）', label: t('pages.course.segment4') },
  { value: '第五大节（19:00 ~ 20:40）', label: t('pages.course.segment5') },
])

const {
  tableData, pageNum, pageSize, total, loading,
  formVisible, form, formRef, rules,
  load, handleAdd, handleEdit, save, del, delBatch,
  handleSelectionChange,
} = useCrud({
  url: '/course',
  rules: computed(() => ({
    name: [{ required: true, message: t('pages.course.ruleNameRequired'), trigger: 'blur' }],
  })),
  getParams: () => ({ name: name.value }),
})

const columns = computed<CrudColumn[]>(() => [
  { prop: 'id', label: t('pages.course.id'), width: 80, align: 'center', sortable: true },
  { prop: 'name', label: t('pages.course.name'), showOverflowTooltip: true },
  { prop: 'type', label: t('pages.course.type'), showOverflowTooltip: true },
  { prop: 'teacherName', label: t('pages.course.teacher'), showOverflowTooltip: true },
  { prop: 'score', label: t('pages.course.score'), showOverflowTooltip: true },
  { prop: 'num', label: t('pages.course.num'), showOverflowTooltip: true },
  { prop: 'room', label: t('pages.course.room'), showOverflowTooltip: true },
  { prop: 'week', label: t('pages.course.week'), showOverflowTooltip: true },
  { prop: 'segment', label: t('pages.course.segment'), showOverflowTooltip: true },
  { prop: 'status', label: t('pages.course.status'), showOverflowTooltip: true },
])

const choiceCourse = (row: any) => {
  request.post('/choice/add', { studentId: user.value.id, teacherId: row.teacherId, courseId: row.id }).then((res: any) => {
    if (res.data.code === '200') {
      ElMessage.success(t('pages.course.choiceSuccess'))
    } else {
      ElMessage.error(apiMessage(res.data))
    }
  })
}

const loadTeacher = () => {
  request.get('/teacher/selectAll').then((res: any) => {
    if (res.data.code === '200') {
      teacherData.value = res.data.data
    } else {
      ElMessage.error(apiMessage(res.data))
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

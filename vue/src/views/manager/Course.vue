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
          <el-select
            v-model="form.room"
            filterable
            :loading="freeRoomsLoading"
            :placeholder="$t('pages.course.roomPlaceholder')"
            style="width: 100%"
          >
            <el-option
              v-for="r in freeRooms"
              :key="r.code"
              :label="freeRoomLabel(r)"
              :value="r.code"
            />
          </el-select>
          <el-alert
            v-if="roomConflict"
            :title="roomConflict"
            type="warning"
            :closable="false"
            show-icon
            style="width: 100%; margin-top: 6px"
          />
        </el-form-item>
        <el-form-item prop="week" :label="$t('pages.course.week')">
          <el-select v-model="form.week" :placeholder="$t('pages.course.selectPlaceholder')" style="width: 100%">
            <el-option v-for="d in weekOptions" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item prop="segment" :label="$t('pages.course.segment')">
          <el-select v-model="form.segment" :placeholder="$t('pages.course.selectPlaceholder')" style="width: 100%">
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
defineOptions({ name: 'Course' })

import { ref, onMounted, computed, watch } from 'vue'
import { ElMessage } from '@/utils/element-plus'
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

// 教室空闲列表 + 自动分配 + 占用提示：星期/大节/人数/课程名（体育课优先场馆）/弹窗任一变化后刷新
// （只列「该时段空闲」的教室供选择；留空保存时后端也强校验兜底）
const freeRooms = ref<any[]>([])
const freeRoomsLoading = ref(false)
const roomConflict = ref('')
let roomTimer: ReturnType<typeof setTimeout> | undefined
const isPeCourse = computed(() => (form.value.name || '').includes('体育'))
// 选项描述：运动场馆用名称；教室优先用「使用说明」里的用途（多媒体教室/机房等），缺省回退名称
const roomDesc = (r: any) => {
  if (r.type === '运动场馆') return r.name
  return (
    String(r.content || '')
      .split('（')[0]
      .split('\n')[0]
      .trim() || r.name
  )
}
const freeRoomLabel = (r: any) => `${r.code} ${roomDesc(r)}（${r.num}人）`

const fetchFreeRooms = () => {
  if (!formVisible.value || !form.value.week || !form.value.segment) {
    freeRooms.value = []
    roomConflict.value = ''
    return
  }
  freeRoomsLoading.value = true
  const base = {
    week: form.value.week,
    segment: form.value.segment,
    num: form.value.num || undefined,
    excludeId: form.value.id || undefined,
  }
  // 体育类课程优先在运动场馆中找，没有合适场馆再回落到普通教室
  const req = isPeCourse.value
    ? request.get('/course/roomFree', { params: { ...base, typeFilter: '运动场馆' } }).then((r1: any) => {
        const venues = r1.data.data || []
        if (!venues.length) {
          return request.get('/course/roomFree', { params: base }).then((r2: any) => venues.concat(r2.data.data || []))
        }
        return venues
      })
    : request.get('/course/roomFree', { params: base }).then((r: any) => r.data.data || [])

  req
    .then((list: any[]) => {
      freeRooms.value = list
      // 系统自动分配：未手动选择教室时，自动填入容量最贴近的空闲教室/场地并提示
      if (user.value.role === 'ADMIN' && !form.value.room && list.length) {
        form.value.room = list[0].code
        ElMessage.success(t('pages.course.autoAssigned', { code: list[0].code, name: roomDesc(list[0]) }))
      }
      // 占用提示兜底：当前选中教室不在空闲列表（被他课占用）时给出提示
      if (form.value.room && !list.some((r) => r.code === form.value.room)) {
        request
          .get('/course/roomOccupied', {
            params: {
              room: form.value.room,
              week: form.value.week,
              segment: form.value.segment,
              excludeId: form.value.id || undefined,
            },
          })
          .then((res: any) => {
            const hit = res.data.data
            roomConflict.value = hit ? t('pages.course.roomOccupied', { name: hit.name, teacher: hit.teacherName || '-' }) : ''
          })
          .catch(() => {})
      } else {
        roomConflict.value = ''
      }
    })
    .catch(() => {
      // 查询失败时清掉过期的占用提示，避免残留误导
      roomConflict.value = ''
    })
    .finally(() => {
      freeRoomsLoading.value = false
    })
}

watch(
  () => [formVisible.value, form.value.name, form.value.week, form.value.segment, form.value.num],
  () => {
    if (roomTimer) clearTimeout(roomTimer)
    roomTimer = setTimeout(fetchFreeRooms, 300)
  },
)

const choiceCourse = (row: any) => {
  request.post('/choice/add', { studentId: user.value.id, teacherId: row.teacherId, courseId: row.id }).then((res: any) => {
    if (res.data.code === '200') {
      ElMessage.success(t('pages.course.choiceSuccess'))
    } else {
      ElMessage.error(apiMessage(res.data))
    }
  }).catch(() => {})
}

const loadTeacher = () => {
  request.get('/teacher/selectAll').then((res: any) => {
    if (res.data.code === '200') {
      teacherData.value = res.data.data
    } else {
      ElMessage.error(apiMessage(res.data))
    }
  }).catch(() => {})
}

const reset = () => {
  name.value = ''
  load(1)
}

onMounted(() => {
  load(1)
  // 教师下拉仅表单使用且对非管理员禁用：学生/教师无需加载，避免 teacher:view 403
  if (user.value.role === 'ADMIN') {
    loadTeacher()
  }
})
</script>

<style scoped></style>

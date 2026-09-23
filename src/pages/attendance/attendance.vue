<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 搜索区：按课程筛选 -->
    <view class="xm-card xm-row">
      <picker
        style="flex: 1"
        :range="courseSearchData"
        range-key="name"
        :value="searchCourseIndex"
        @change="onSearchCourseChange"
      >
        <view
          class="xm-input picker-display"
          :class="{ 'picker-placeholder': !courseId }"
        >
          {{ courseId ? searchCourseNameOf(courseId) : $t('pages.attendance.coursePlaceholder') }}
        </view>
      </picker>
      <button
        class="xm-btn xm-btn-primary"
        @click="search"
      >
        {{ $t('common.search') }}
      </button>
      <button
        class="xm-btn xm-btn-plain"
        @click="onReset"
      >
        {{ $t('common.reset') }}
      </button>
    </view>

    <!-- 操作区：教师可添加考勤 / 批量管理 -->
    <view
      class="xm-card xm-row"
      v-if="userStore.role === 'TEACHER'"
    >
      <button
        class="xm-btn xm-btn-primary"
        @click="onAdd"
      >
        {{ $t('pages.attendance.addAttendance') }}
      </button>
      <button
        class="xm-btn xm-btn-plain"
        @click="toggleManage"
      >
        {{ manageMode ? $t('common.done') : $t('common.manage') }}
      </button>
      <button
        v-if="manageMode"
        class="xm-btn xm-btn-danger"
        @click="delBatch"
      >
        {{ $t('common.batchDelete') }}
      </button>
    </view>

    <!-- 列表 -->
    <xm-empty
      v-if="!list.length && !loading"
      :action-text="$t('common.reload')"
      @action="load(true)"
    />

    <view
      v-for="row in list"
      :key="row.id"
      class="xm-card"
    >
      <view class="xm-between">
        <view class="xm-row">
          <checkbox
            v-if="manageMode"
            :checked="selectedIds.includes(row.id)"
            style="transform: scale(0.8)"
            @click.stop="toggleSelect(row.id)"
          />
          <view
            class="xm-value"
            style="font-weight: bold"
            >{{ row.courseName }}</view
          >
        </view>
        <view
          class="xm-tag"
          :class="statusTagClass(row.status)"
          >{{ statusLabelOf(row.status) }}</view
        >
      </view>
      <view
        class="xm-row"
        style="flex-wrap: wrap; margin-top: 12rpx"
      >
        <view class="xm-label field">{{ $t('pages.attendance.teacherName') }}: {{ row.teacherName }}</view>
        <view class="xm-label field">{{ $t('pages.attendance.studentName') }}: {{ row.studentName }}</view>
        <view class="xm-label field">{{ $t('pages.attendance.timeLabel') }}: {{ row.time }}</view>
        <view class="xm-label field">{{ $t('pages.attendance.id') }}: {{ row._index }}</view>
      </view>

      <view
        class="xm-actions"
        v-if="userStore.role === 'TEACHER' && !manageMode"
      >
        <button
          class="xm-btn xm-btn-plain"
          @click="onEdit(row)"
        >
          {{ $t('common.edit') }}
        </button>
        <button
          class="xm-btn xm-btn-danger"
          @click="del(row.id)"
        >
          {{ $t('common.delete') }}
        </button>
      </view>
    </view>

    <xm-list-footer
      :visible="!!list.length"
      :loading="loading"
      :finished="finished()"
      @load-more="loadNext"
    />

    <!-- 添加/编辑考勤表单（底部弹层，教师） -->
    <view
      v-if="formVisible"
      class="xm-mask"
      @click="closeForm"
    ></view>

    <view
      v-if="formVisible"
      class="xm-popup"
    >
      <view class="xm-popup-title">{{ $t('pages.attendance.dialogTitle') }}</view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.attendance.selectCourse') }}</view>
        <picker
          :range="courseData"
          range-key="name"
          :value="formCourseIndex"
          @change="onFormCourseChange"
        >
          <view
            class="xm-input picker-display"
            :class="{ 'picker-placeholder': !form.courseId }"
          >
            {{ form.courseId ? courseNameOf(form.courseId) : $t('pages.attendance.coursePlaceholder') }}
          </view>
        </picker>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.attendance.selectStudent') }}</view>
        <picker
          :range="studentData"
          range-key="studentName"
          :value="formStudentIndex"
          @change="onFormStudentChange"
        >
          <view
            class="xm-input picker-display"
            :class="{ 'picker-placeholder': studentId == null }"
          >
            {{ studentNameOf(studentId) || $t('pages.attendance.studentPlaceholder') }}
          </view>
        </picker>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.attendance.timeLabel') }}</view>
        <picker
          mode="date"
          :value="form.time"
          @change="onFormTimeChange"
        >
          <view
            class="xm-input picker-display"
            :class="{ 'picker-placeholder': !form.time }"
          >
            {{ form.time || $t('pages.attendance.datePlaceholder') }}
          </view>
        </picker>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.attendance.statusLabel') }}</view>
        <picker
          :range="statusLabels"
          :value="formStatusIndex"
          @change="onFormStatusChange"
        >
          <view
            class="xm-input picker-display"
            :class="{ 'picker-placeholder': !form.status }"
          >
            {{ form.status ? statusLabelOf(form.status) : $t('pages.attendance.statusPlaceholder') }}
          </view>
        </picker>
      </view>
      <view
        class="xm-row"
        style="margin-top: 16rpx"
      >
        <button
          class="xm-btn xm-btn-plain"
          style="flex: 1"
          @click="closeForm"
        >
          {{ $t('common.cancel') }}
        </button>
        <button
          class="xm-btn xm-btn-primary"
          style="flex: 1"
          @click="save"
        >
          {{ $t('common.ok') }}
        </button>
      </view>
    </view>
    <xm-loader />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { ensureLoggedIn } from '@/utils/authGuard'
import { useCrud } from '@/composables/useCrud'
import { getData } from '@/utils/request'
import { t } from '@/i18n'

const userStore = useUserStore()
const courseId = ref('')
const courseData = ref([])
const courseSearchData = ref([])
const studentData = ref([])
const studentId = ref(null)
const manageMode = ref(false)

const {
  list,
  loading,
  finished,
  form,
  formVisible,
  selectedIds,
  load,
  loadNext,
  search,
  closeForm,
  save,
  del,
  delBatch,
} = useCrud({
  url: '/attendance',
  getParams: () => ({ courseId: courseId.value }),
  validate: (f) => {
    if (!f.time) return t('pages.attendance.ruleTimeRequired')
    if (!f.status) return t('pages.attendance.ruleStatusRequired')
    return ''
  },
  beforeSave: (formData) => {
    formData.studentId = studentId.value
  },
})

const toggleManage = () => {
  manageMode.value = !manageMode.value
  if (!manageMode.value) selectedIds.value = []
}

const toggleSelect = (id) => {
  const idx = selectedIds.value.indexOf(id)
  if (idx >= 0) selectedIds.value.splice(idx, 1)
  else selectedIds.value.push(id)
}

// 考勤状态枚举值保持中文入库，仅翻译显示文案（与 Web 端一致）
const statusOptions = computed(() => [
  { label: t('pages.attendance.statusNormal'), value: '正常' },
  { label: t('pages.attendance.statusLate'), value: '迟到' },
  { label: t('pages.attendance.statusEarlyLeave'), value: '早退' },
  { label: t('pages.attendance.statusAbsent'), value: '缺勤' },
])
const statusLabels = computed(() => statusOptions.value.map((o) => o.label))
const statusLabelOf = (value) => {
  const opt = statusOptions.value.find((o) => o.value === value)
  return opt ? opt.label : value
}
const statusTagClass = (value) => {
  if (value === '正常') return 'xm-tag-success'
  if (value === '迟到') return 'xm-tag-warning'
  if (value === '早退') return 'xm-tag-warning'
  return 'xm-tag-danger'
}

const searchCourseIndex = computed(() => courseSearchData.value.findIndex((c) => c.id === courseId.value))
const formCourseIndex = computed(() => courseData.value.findIndex((c) => c.id === form.value.courseId))
const formStudentIndex = computed(() => studentData.value.findIndex((s) => s.studentId === studentId.value))
const formStatusIndex = computed(() => statusOptions.value.findIndex((o) => o.value === form.value.status))

const searchCourseNameOf = (id) => {
  const c = courseSearchData.value.find((item) => item.id === id)
  return c ? c.name : ''
}

const courseNameOf = (id) => {
  const c = courseData.value.find((item) => item.id === id)
  return c ? c.name : ''
}

const studentNameOf = (id) => {
  if (id == null) return ''
  const s = studentData.value.find((item) => item.studentId === id)
  return s ? s.studentName : ''
}

// 搜索用课程列表：学生按已选课程、管理员按全部课程、教师按所授课程（与 Web 端 loadCourseSearch 一致）
const loadCourseSearch = async () => {
  if (userStore.role === 'STUDENT') {
    // 学生端拿到的是选课记录，下拉要以课程 id 作为值
    const rows = (await getData('/choice/selectAll', { studentId: userStore.user.id })) || []
    rows.forEach((item) => {
      item.id = item.courseId
    })
    courseSearchData.value = rows
  } else {
    const params = userStore.role === 'ADMIN' ? {} : { teacherId: userStore.user.id }
    courseSearchData.value = (await getData('/course/selectAll', params)) || []
  }
}

// 教师所授课程列表（与 Web 端 loadCourseByTeacher 一致）
const loadCourseByTeacher = async () => {
  courseData.value = (await getData('/course/selectAll', { teacherId: userStore.user.id })) || []
}

/**
 * 按课程加载选课学生（与 Web 端 loadStudents 一致）。
 * openForm 为 true 时用于「编辑」：回显已选学生并打开表单；否则用于新增时切换课程，清空已选学生。
 */
const loadStudents = async (cId, openForm = false) => {
  const rows = await getData('/choice/selectAll', { courseId: cId })
  if (rows === null) return
  studentData.value = rows
  studentId.value = openForm ? form.value.studentId : null
  if (openForm) formVisible.value = true
}

// 选择课程后联动加载选课学生
const getStudent = (cId) => loadStudents(cId)

// 编辑时按课程加载学生并回显已选学生
const getStudentEdit = (cId) => loadStudents(cId, true)

const onSearchCourseChange = (e) => {
  const c = courseSearchData.value[e.detail.value]
  courseId.value = c ? c.id : ''
}

const onFormCourseChange = (e) => {
  const c = courseData.value[e.detail.value]
  if (c) {
    form.value.courseId = c.id
    getStudent(c.id)
  }
}

const onFormStudentChange = (e) => {
  const s = studentData.value[e.detail.value]
  studentId.value = s ? s.studentId : null
}

const onFormStatusChange = (e) => {
  const opt = statusOptions.value[e.detail.value]
  if (opt) form.value.status = opt.value
}

// 考勤日期：日期选择器（YYYY-MM-DD，后端仍按字符串存储）
const onFormTimeChange = (e) => {
  form.value.time = e.detail.value
}

const onAdd = () => {
  form.value = { teacherId: userStore.user.id }
  studentId.value = null
  formVisible.value = true
}

const onEdit = (row) => {
  form.value = JSON.parse(JSON.stringify(row))
  getStudentEdit(form.value.courseId)
}

const onReset = () => {
  courseId.value = ''
  search()
}

onShow(() => {
  uni.setNavigationBarTitle({ title: t('menu.attendance') })
  if (!ensureLoggedIn()) return
  load(true)
  loadCourseByTeacher()
  loadCourseSearch()
})

onReachBottom(() => loadNext())
</script>

<style lang="scss" scoped>
.field {
  width: 50%;
  margin-bottom: 8rpx;
}

.picker-display {
  display: flex;
  align-items: center;
}

.picker-placeholder {
  color: var(--xm-text-2);
}
</style>

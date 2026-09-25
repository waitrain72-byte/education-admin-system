<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 搜索区：按课程筛选 -->
    <view class="xm-card xm-row">
      <view class="search-course">
        <xm-picker
          v-model="query.courseId"
          :options="courseSearchData"
          label-key="name"
          value-key="id"
          :placeholder="$t('pages.attendance.coursePlaceholder')"
        />
      </view>
      <button
        class="xm-btn xm-btn-primary"
        @click="search"
      >
        {{ $t('common.search') }}
      </button>
      <button
        class="xm-btn xm-btn-plain"
        @click="resetQuery"
      >
        {{ $t('common.reset') }}
      </button>
    </view>

    <!-- 操作区：教师可添加考勤 / 批量管理 -->
    <xm-action-bar
      v-if="userStore.role === 'TEACHER'"
      :manage-mode="manageMode"
      :total="total"
      :selected-count="selectedIds.length"
      :add-text="$t('pages.attendance.addAttendance')"
      @add="onAdd"
      @toggle-manage="toggleManage"
      @del-batch="delBatch"
    />

    <!-- 列表 -->
    <xm-empty
      v-if="!list.length && !loading"
      :action-text="$t('common.reload')"
      @action="load(true)"
    />

    <!-- 列表：手机单列，平板 ≥720px 两列、≥1248px 三列（theme.scss .xm-list） -->
    <view class="xm-list">
      <view
        v-for="row in list"
        :key="row.id"
        class="xm-card"
      >
        <view class="xm-between">
          <view class="xm-row xm-card-head">
            <!-- 批量管理模式下显示勾选框 -->
            <checkbox
              v-if="manageMode"
              :checked="selectedIds.includes(row.id)"
              style="transform: scale(0.8)"
              @click.stop="toggleSelect(row.id)"
            />
            <text class="xm-card-name xm-ellipsis">{{ row.courseName }}</text>
          </view>
          <view
            class="xm-tag"
            :class="enumTag('attendanceStatus', row.status)"
            >{{ enumLabel('attendanceStatus', row.status) }}</view
          >
        </view>
        <view class="xm-meta">
          <view class="xm-meta-item">
            <xm-icon
              name="briefcase"
              :size="28"
            />
            <text class="xm-meta-text">{{ row.teacherName || '-' }}</text>
          </view>
          <view class="xm-meta-item">
            <xm-icon
              name="user"
              :size="28"
            />
            <text class="xm-meta-text">{{ row.studentName || '-' }}</text>
          </view>
          <view class="xm-meta-item">
            <xm-icon
              name="calendar"
              :size="28"
            />
            <text class="xm-meta-text">{{ row.time || '-' }}</text>
          </view>
          <view class="xm-meta-item xm-meta-end">
            <text class="xm-card-no">#{{ row._index }}</text>
          </view>
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
    </view>

    <xm-list-footer
      :visible="!!list.length"
      :loading="loading"
      :finished="finished()"
      @load-more="loadNext"
    />

    <!-- 添加/编辑考勤表单（底部弹层，教师） -->
    <xm-form-popup
      :visible="formVisible"
      :saving="saving"
      :title="$t(form.id ? 'common.editTitle' : 'common.addTitle', { name: $t('pages.attendance.entity') })"
      @close="closeForm"
      @save="save"
    >
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.attendance.selectCourse') }}</view>
        <xm-picker
          v-model="form.courseId"
          :options="courseData"
          label-key="name"
          value-key="id"
          :placeholder="$t('pages.attendance.coursePlaceholder')"
          @change="onFormCourseChange"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.attendance.selectStudent') }}</view>
        <xm-picker
          v-model="studentId"
          :options="studentData"
          label-key="studentName"
          value-key="studentId"
          :placeholder="$t('pages.attendance.studentPlaceholder')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.attendance.timeLabel') }}</view>
        <xm-picker
          v-model="form.time"
          mode="date"
          :placeholder="$t('pages.attendance.datePlaceholder')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.attendance.statusLabel') }}</view>
        <xm-picker
          v-model="form.status"
          :options="enumOptions('attendanceStatus')"
          :placeholder="$t('pages.attendance.statusPlaceholder')"
        />
      </view>
    </xm-form-popup>
    <xm-loader />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { useListPage } from '@/composables/useListPage'
import { attendanceApi, choiceApi, courseApi } from '@/api'
import { orNull, SILENT } from '@/utils/request'
import { t } from '@/i18n'
import { enumLabel, enumTag, enumOptions } from '@/utils/enums'

const userStore = useUserStore()
// 表单课程下拉（教师所授课程）/ 搜索课程下拉 / 所选课程的选课学生
const courseData = ref([])
const courseSearchData = ref([])
const studentData = ref([])
// 表单里选中的学生（保存前写入 form.studentId，与 Web 端一致）
const studentId = ref(null)

const {
  list,
  loading,
  finished,
  total,
  form,
  formVisible,
  saving,
  selectedIds,
  query,
  manageMode,
  load,
  loadNext,
  search,
  resetQuery,
  toggleManage,
  toggleSelect,
  handleAdd,
  closeForm,
  save,
  del,
  delBatch,
} = useListPage({
  api: attendanceApi,
  title: 'menu.attendance',
  query: { courseId: '' },
  loadExtras: () => {
    loadCourseByTeacher()
    loadCourseSearch()
  },
  validate: (f) => {
    if (!f.time) return t('pages.attendance.ruleTimeRequired')
    if (!f.status) return t('pages.attendance.ruleStatusRequired')
    return ''
  },
  beforeSave: (formData) => {
    formData.studentId = studentId.value
  },
})

// 搜索用课程列表：学生按已选课程、管理员按全部课程、教师按所授课程（与 Web 端 loadCourseSearch 一致）
const loadCourseSearch = async () => {
  if (userStore.role === 'STUDENT') {
    // 学生端拿到的是选课记录，下拉要以课程 id 作为值
    const rows = (await orNull(choiceApi.selectAll({ studentId: userStore.user.id }, SILENT))) || []
    rows.forEach((item) => {
      item.id = item.courseId
    })
    courseSearchData.value = rows
  } else {
    const params = userStore.role === 'ADMIN' ? {} : { teacherId: userStore.user.id }
    courseSearchData.value = (await orNull(courseApi.selectAll(params, SILENT))) || []
  }
}

// 教师所授课程列表（与 Web 端 loadCourseByTeacher 一致）
const loadCourseByTeacher = async () => {
  courseData.value = (await orNull(courseApi.selectAll({ teacherId: userStore.user.id }, SILENT))) || []
}

/**
 * 按课程加载选课学生（与 Web 端 loadStudents 一致）。
 * openForm 为 true 时用于「编辑」：回显已选学生并打开表单；否则用于新增时切换课程，清空已选学生。
 */
const loadStudents = async (cId, openForm = false) => {
  const rows = await orNull(choiceApi.selectAll({ courseId: cId }))
  if (rows === null) return
  studentData.value = rows
  studentId.value = openForm ? form.value.studentId : null
  if (openForm) formVisible.value = true
}

// 表单切换课程：联动加载该课程的选课学生
const onFormCourseChange = (course) => loadStudents(course.id)

const onAdd = () => {
  studentId.value = null
  handleAdd({ teacherId: userStore.user.id })
}

// 编辑：先按课程加载学生并回显已选学生，再打开表单
const onEdit = (row) => {
  form.value = JSON.parse(JSON.stringify(row))
  loadStudents(form.value.courseId, true)
}
</script>

<style lang="scss" scoped>
/* 搜索卡里的课程下拉：占满剩余宽度，课程名过长时省略而不是挤掉查询按钮 */
.search-course {
  flex: 1;
  min-width: 0;
}
</style>

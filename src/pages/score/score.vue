<template>
  <view
    class="xm-page"
    :class="themeClass"
  >
    <!-- 搜索区：按课程筛选 -->
    <view class="xm-card xm-row">
      <picker
        style="flex: 1"
        :range="courseData"
        range-key="name"
        :value="courseIndex"
        @change="onSearchCourseChange"
      >
        <view
          class="xm-input picker-display"
          :class="{ 'picker-placeholder': !courseId }"
        >
          {{ courseId ? courseNameOf(courseId) : $t('pages.score.coursePlaceholder') }}
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

    <!-- 操作区：教师可录入成绩 / 批量管理 -->
    <view
      class="xm-card xm-row"
      v-if="userStore.role === 'TEACHER'"
    >
      <button
        class="xm-btn xm-btn-primary"
        @click="onAdd"
      >
        {{ $t('common.add') }}
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
    <view
      v-if="!list.length && !loading"
      class="xm-empty"
      >{{ $t('common.empty') }}</view
    >

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
        <view class="xm-label">ID: {{ row.id }}</view>
      </view>
      <view
        class="xm-row"
        style="flex-wrap: wrap; margin-top: 12rpx"
      >
        <view class="xm-label field">{{ $t('pages.score.studentName') }}: {{ row.studentName }}</view>
        <view class="xm-label field">{{ $t('pages.score.teacherName') }}: {{ row.teacherName }}</view>
        <view class="xm-label field">{{ $t('pages.score.ordinaryScore') }}: {{ row.ordinaryScore }}</view>
        <view class="xm-label field">{{ $t('pages.score.examScore') }}: {{ row.examScore }}</view>
      </view>
      <view
        class="xm-between"
        style="margin-top: 8rpx"
      >
        <view
          class="xm-value"
          style="color: var(--xm-brand); font-weight: bold"
        >
          {{ $t('pages.score.totalScore') }}: {{ row.score }}
        </view>
      </view>

      <view
        class="xm-actions"
        v-if="userStore.role === 'TEACHER' && !manageMode"
      >
        <button
          class="xm-btn xm-btn-danger"
          @click="del(row.id)"
        >
          {{ $t('common.delete') }}
        </button>
      </view>
    </view>

    <view
      v-if="list.length"
      class="xm-empty"
      @click="loadNext"
    >
      {{ finished() ? $t('common.noMore') : $t('common.loadMore') }}
    </view>

    <!-- 成绩录入/编辑表单（底部弹层，仅教师） -->
    <view
      v-if="formVisible"
      class="xm-mask"
      @click="closeForm"
    ></view>

    <view
      v-if="formVisible"
      class="xm-popup"
    >
      <view class="xm-popup-title">{{ $t('pages.score.dialogTitle') }}</view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.score.selectCourse') }}</view>
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
            {{ form.courseId ? courseNameOf(form.courseId) : $t('pages.score.coursePlaceholder') }}
          </view>
        </picker>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.score.selectStudent') }}</view>
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
            {{ studentNameOf(studentId) || $t('pages.score.studentPlaceholder') }}
          </view>
        </picker>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.score.ordinaryScore') }}</view>
        <input
          class="xm-input"
          v-model="form.ordinaryScore"
          type="digit"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.score.examScore') }}</view>
        <input
          class="xm-input"
          v-model="form.examScore"
          type="digit"
        />
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
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { useCrud } from '@/composables/useCrud'
import { get, del as delRequest } from '@/utils/request'
import { t, apiMessage } from '@/i18n'

const userStore = useUserStore()
const courseId = ref('')
const courseData = ref([])
const studentData = ref([])
const studentId = ref(null)
const manageMode = ref(false)

const { list, loading, finished, form, formVisible, selectedIds, load, loadNext, search, closeForm, save, delBatch } =
  useCrud({
    url: '/score',
    getParams: () => ({ courseId: courseId.value }),
    validate: (f) => {
      if (!f.ordinaryScore && f.ordinaryScore !== 0) return t('pages.score.ruleOrdinaryRequired')
      if (!f.examScore && f.examScore !== 0) return t('pages.score.ruleExamRequired')
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

const courseIndex = computed(() => courseData.value.findIndex((c) => c.id === courseId.value))
const formCourseIndex = computed(() => courseData.value.findIndex((c) => c.id === form.value.courseId))
const formStudentIndex = computed(() => studentData.value.findIndex((s) => s.studentId === studentId.value))

const courseNameOf = (id) => {
  const c = courseData.value.find((item) => item.id === id)
  return c ? c.name : ''
}

const studentNameOf = (id) => {
  if (id == null) return ''
  const s = studentData.value.find((item) => item.studentId === id)
  return s ? s.studentName : ''
}

// 教师所授课程列表（与 Web 端 loadCourse 一致）
const loadCourse = () => {
  get('/course/selectAll', { teacherId: userStore.user.id }).then((res) => {
    if (res.data && res.data.code === '200') {
      courseData.value = res.data.data || []
    } else {
      uni.showToast({ title: apiMessage(res.data), icon: 'none' })
    }
  })
}

// 选择课程后联动加载选课学生（与 Web 端 getStudent 一致）
const getStudent = (cId) => {
  get('/choice/selectAll', { courseId: cId }).then((res) => {
    if (res.data && res.data.code === '200') {
      studentData.value = res.data.data || []
      studentId.value = null
    } else {
      uni.showToast({ title: apiMessage(res.data), icon: 'none' })
    }
  })
}

const onSearchCourseChange = (e) => {
  courseId.value = courseData.value[e.detail.value] ? courseData.value[e.detail.value].id : ''
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

const onAdd = () => {
  form.value = { teacherId: userStore.user.id }
  studentId.value = null
  formVisible.value = true
}

// 删除确认文案与 Web 端一致
const del = (id) => {
  uni.showModal({
    title: t('common.confirmDeleteTitle'),
    content: t('pages.score.deleteConfirm'),
    success: async (res) => {
      if (!res.confirm) return
      try {
        const r = await delRequest(`/score/delete/${id}`)
        if (r.data && r.data.code === '200') {
          uni.showToast({ title: t('common.operationSuccess'), icon: 'success' })
          load(true)
        } else {
          uni.showToast({ title: apiMessage(r.data), icon: 'none' })
        }
      } catch {
        // 请求层已统一提示
      }
    },
  })
}

const onReset = () => {
  courseId.value = ''
  search()
}

onShow(() => {
  uni.setNavigationBarTitle({ title: t('menu.score') })
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  load(true)
  loadCourse()
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

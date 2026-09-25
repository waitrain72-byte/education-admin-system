<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 搜索区 -->
    <xm-search-card
      v-model="query.name"
      :placeholder="$t('pages.course.searchPlaceholder')"
      @search="search"
      @reset="resetQuery"
    />

    <!-- 操作区：新增 / 批量管理（仅管理员） -->
    <xm-action-bar
      v-if="isAdmin"
      :manage-mode="manageMode"
      :total="total"
      :selected-count="selectedIds.length"
      @add="handleAdd()"
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
        v-for="item in list"
        :key="item.id"
        class="xm-card"
      >
        <!-- 标题行：课程名 + 状态；标签行：类型 / 学分 / 人数；信息栅格：教师 / 教室 / 上课时间 -->
        <view class="xm-between">
          <view class="xm-row xm-card-head">
            <!-- 批量管理模式下显示勾选框 -->
            <checkbox
              v-if="isAdmin && manageMode"
              :checked="selectedIds.includes(item.id)"
              style="transform: scale(0.8)"
              @click.stop="toggleSelect(item.id)"
            />
            <text class="xm-card-name xm-ellipsis">{{ item.name }}</text>
          </view>
          <text
            v-if="item.status"
            class="xm-tag"
            :class="enumTag('courseStatus', item.status)"
            >{{ enumLabel('courseStatus', item.status) }}</text
          >
        </view>
        <view class="xm-tags">
          <text
            v-if="item.type"
            class="xm-tag xm-tag-brand"
            >{{ enumLabel('courseType', item.type) }}</text
          >
          <text
            v-if="item.score != null && item.score !== ''"
            class="xm-tag"
            >{{ $t('pages.course.credit', { n: item.score }) }}</text
          >
          <text
            v-if="item.num"
            class="xm-tag"
            >{{ $t('pages.course.people', { n: item.num }) }}</text
          >
          <text class="xm-tags-end">#{{ item._index }}</text>
        </view>
        <view class="xm-meta">
          <view class="xm-meta-item">
            <xm-icon
              name="briefcase"
              :size="28"
            />
            <text class="xm-meta-text">{{ item.teacherName || '-' }}</text>
          </view>
          <view class="xm-meta-item">
            <xm-icon
              name="map-pin"
              :size="28"
            />
            <text class="xm-meta-text">{{ item.room || '-' }}</text>
          </view>
          <view class="xm-meta-item full">
            <xm-icon
              name="clock"
              :size="28"
            />
            <text class="xm-meta-text">{{ scheduleText(item.week, item.segment) || '-' }}</text>
          </view>
        </view>
        <view
          class="xm-actions"
          v-if="!manageMode"
        >
          <button
            v-if="userStore.role !== 'STUDENT'"
            class="xm-btn xm-btn-plain"
            @click="handleEdit(item)"
          >
            {{ $t('common.edit') }}
          </button>
          <button
            v-if="userStore.role === 'STUDENT'"
            class="xm-btn xm-btn-primary"
            :disabled="item.status !== '未开课'"
            @click="choiceCourse(item)"
          >
            {{ $t('pages.course.choice') }}
          </button>
          <button
            v-if="isAdmin"
            class="xm-btn xm-btn-danger"
            @click="del(item.id)"
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

    <!-- 新增/编辑表单（底部弹层，含教室 / 授课教师选择器） -->
    <course-form
      v-model:form="form"
      :visible="formVisible"
      :saving="saving"
      :is-admin="isAdmin"
      :teachers="teacherData"
      @close="closeForm"
      @save="save"
    />
    <xm-loader />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { useListPage } from '@/composables/useListPage'
import { courseApi, choiceApi, teacherApi } from '@/api'
import { orNull, SILENT } from '@/utils/request'
import { t } from '@/i18n'
import { enumLabel, enumTag, scheduleText } from '@/utils/enums'
import { assignRoomBeforeSave } from './room'
import CourseForm from './components/course-form.vue'

const userStore = useUserStore()
const teacherData = ref([])

const isAdmin = computed(() => userStore.role === 'ADMIN')

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
  handleEdit,
  closeForm,
  save,
  del,
  delBatch,
} = useListPage({
  api: courseApi,
  title: 'menu.course',
  query: { name: '' },
  // 教师下拉仅表单使用且对非管理员禁用：学生 / 教师无需加载，避免 teacher:view 403
  loadExtras: () => {
    if (userStore.role === 'ADMIN') loadTeacher()
  },
  validate: (f) => {
    if (!f.name) return t('pages.course.ruleNameRequired')
    // 已选教室则必须同时有上课时段，否则无法进行占用校验，会产生无排期课程
    if (f.room && (!f.week || !f.segment)) return t('pages.course.ruleRoomNeedTime')
    return ''
  },
  // 教室留空 = 系统自动分配：按容量最贴近人数取第一间空闲教室 / 场地并提示
  beforeSave: assignRoomBeforeSave,
})

// 学生选课（与 Web 端 choiceCourse 接口一致）
const choiceCourse = (row) => {
  choiceApi
    .add({ studentId: userStore.user.id, teacherId: row.teacherId, courseId: row.id })
    .then(() => uni.showToast({ title: t('pages.course.choiceSuccess'), icon: 'success' }))
    .catch(() => {
      // 选课失败（满员 / 时间冲突）的提示已由请求层统一弹出
    })
}

const loadTeacher = async () => {
  teacherData.value = (await orNull(teacherApi.selectAll(undefined, SILENT))) || []
}
</script>

<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 搜索区 -->
    <xm-search-card
      v-model="query.content"
      :placeholder="$t('pages.homework.contentPlaceholder')"
      @search="search"
      @reset="resetQuery"
    />

    <!-- 新增：右下角悬浮按钮（与各列表页一致，避让 Home 条，样式见 theme.scss .xm-fab） -->
    <view
      v-if="userStore.role === 'STUDENT'"
      class="xm-fab"
      :aria-label="$t('pages.homework.submitHomework')"
      aria-role="button"
      @click="onAdd"
    >
      <xm-icon
        name="plus"
        :size="48"
      />
    </view>

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
          <!-- 主标题：作业内容；右侧批改状态语义化（已批改绿/待批改橙） -->
          <view class="xm-value xm-ellipsis item-title">{{ row.content }}</view>
          <view
            class="xm-tag status-tag"
            :class="row.score ? 'xm-tag-success' : 'xm-tag-warning'"
            >{{ row.score ? $t('pages.homework.scoreUnit', { n: row.score }) : $t('pages.homework.pending') }}</view
          >
        </view>
        <view class="item-meta">
          <text>{{ row.courseName }} · </text>
          <text>{{ userStore.role === 'TEACHER' ? row.studentName : row.teacherName }}</text>
        </view>
        <view
          v-if="row.file"
          class="xm-between file-row"
        >
          <view class="xm-label file-name xm-ellipsis">{{ fileNameOf(row.file) }}</view>
          <button
            class="xm-btn xm-btn-plain file-btn"
            @click="openAttachment(row.file)"
          >
            {{ $t('pages.homework.download') }}
          </button>
        </view>
        <view
          v-if="row.score && row.descr"
          class="item-descr"
          >{{ row.descr }}</view
        >

        <!-- 操作：学生（未打分可编辑/可删除）、教师（打分），与 Web 端一致 -->
        <view
          class="xm-actions"
          v-if="userStore.role !== 'ADMIN'"
        >
          <button
            v-if="userStore.role === 'STUDENT' && !row.score"
            class="xm-btn xm-btn-plain"
            @click="onEdit(row)"
          >
            {{ $t('common.edit') }}
          </button>
          <button
            v-if="userStore.role === 'STUDENT'"
            class="xm-btn xm-btn-danger"
            @click="del(row.id)"
          >
            {{ $t('common.delete') }}
          </button>
          <button
            v-if="userStore.role === 'TEACHER'"
            class="xm-btn xm-btn-primary"
            @click="handleCheck(row)"
          >
            {{ $t('pages.homework.grade') }}
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

    <!-- 作业提交 / 编辑表单（学生，含附件上传） -->
    <homework-form
      v-model:form="form"
      v-model:uploading="uploading"
      :visible="formVisible"
      :saving="saving"
      :courses="courseData"
      @close="closeForm"
      @save="save"
    />

    <!-- 打分弹层（教师） -->
    <xm-form-popup
      :visible="checkVisible"
      :saving="checking"
      :title="$t('pages.homework.checkDialogTitle')"
      @close="checkVisible = false"
      @save="check"
    >
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.homework.scoreLabel') }}</view>
        <input
          class="xm-input"
          v-model="form.score"
          type="digit"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.homework.descrLabel') }}</view>
        <textarea
          class="xm-textarea"
          v-model="form.descr"
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
import { homeworkApi, choiceApi } from '@/api'
import { orNull, SILENT } from '@/utils/request'
import { t } from '@/i18n'
import { fileNameOf, openAttachment } from '@/utils/attachment'
import HomeworkForm from './components/homework-form.vue'

const userStore = useUserStore()
const checkVisible = ref(false)
// 审核/打分提交中：确定按钮转圈禁用，防止连点重复提交
const checking = ref(false)
// 附件上传中（由表单组件回传）：未传完时拦截保存
const uploading = ref(false)
const courseData = ref([])

// 删除确认文案与 Web 端一致；学生已选课程随列表一起加载（作业表单的课程下拉）
const {
  list,
  loading,
  finished,
  form,
  formVisible,
  saving,
  query,
  load,
  loadNext,
  search,
  resetQuery,
  handleAdd,
  handleEdit,
  save,
  closeForm,
  del,
} = useListPage({
  api: homeworkApi,
  title: 'menu.homework',
  query: { content: '' },
  deleteConfirm: 'pages.homework.deleteConfirm',
  loadExtras: () => loadCourse(),
  validate: (f) => {
    if (uploading.value) return t('pages.homework.uploadingWait')
    if (!f.courseId) return t('pages.homework.ruleCourseRequired')
    if (!f.content) return t('pages.homework.ruleContentRequired')
    return ''
  },
})

// 学生已选课程列表（与 Web 端 loadCourse 一致）
const loadCourse = async () => {
  courseData.value = (await orNull(choiceApi.selectAll({ studentId: userStore.user.id }, SILENT))) || []
}

const onAdd = () => handleAdd({ studentId: userStore.user.id })
// 学生编辑：重置回待审核并清空打分说明（与 Web 端 handleEdit 一致）
const onEdit = (row) => handleEdit(row, { status: '待审核', descr: '' })

const handleCheck = (row) => {
  form.value = JSON.parse(JSON.stringify(row))
  checkVisible.value = true
}

// 教师打分：与 Web 端 check 一致，直接走 /homework/update
const check = () => {
  if (checking.value) return
  checking.value = true
  homeworkApi
    .update(form.value)
    .then(() => {
      uni.showToast({ title: t('common.operationSuccess'), icon: 'success' })
      load(true)
      checkVisible.value = false
    })
    .catch(() => {
      // 提示已由请求层统一弹出
    })
    .finally(() => {
      checking.value = false
    })
}
</script>

<style lang="scss" scoped>
/* 主标题：作业内容单行省略，右侧状态标签不被挤压 */
.item-title {
  font-weight: bold;
  flex: 1;
  min-width: 0;
}

/* 状态标签：禁止收缩换行 */
.status-tag {
  flex-shrink: 0;
  white-space: nowrap;
}

/* 次要信息行：课程/师生弱化小字 */
.item-meta {
  font-size: 24rpx;
  color: var(--xm-text-2);
  margin-top: 12rpx;
}

/* 教师评语气泡 */
.item-descr {
  font-size: 26rpx;
  color: var(--xm-text);
  background: var(--xm-bg-input);
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
  margin-top: 12rpx;
}

.file-name {
  flex: 1;
  word-break: break-all;
  margin-right: 12rpx;
}

/* 列表里的附件行：文件名 + 小号下载按钮 */
.file-row {
  margin-top: 12rpx;
}

.file-btn {
  height: 56rpx;
  line-height: 56rpx;
  flex-shrink: 0;
}
</style>

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
          :options="courseData"
          label-key="name"
          value-key="id"
          :placeholder="$t('pages.score.coursePlaceholder')"
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

    <!-- 学生：成绩汇总（按课程学分加权，换算规则见 utils/gpa.js） -->
    <view
      v-if="isStudent && summary && summary.courseCount"
      class="xm-card summary-card"
    >
      <view class="summary-grid">
        <view class="summary-item">
          <text class="summary-value">{{ summary.gpa === null ? '-' : summary.gpa }}</text>
          <text class="summary-label">{{ $t('pages.score.gpa') }}</text>
        </view>
        <view class="summary-item">
          <text class="summary-value">{{ summary.weightedAvg === null ? '-' : summary.weightedAvg }}</text>
          <text class="summary-label">{{ $t('pages.score.weightedAvg') }}</text>
        </view>
        <view class="summary-item">
          <text class="summary-value">{{ summary.earnedCredits }}</text>
          <text class="summary-label">{{ $t('pages.score.earnedCredits') }}</text>
        </view>
        <view class="summary-item">
          <text
            class="summary-value"
            :class="{ 'score-fail': summary.failedCount > 0 }"
            >{{ summary.failedCount }}</text
          >
          <text class="summary-label">{{ $t('pages.score.failedCount') }}</text>
        </view>
      </view>
      <view class="summary-rule">{{ $t('pages.score.gpaRule') }}</view>
    </view>

    <!-- 操作区：教师可录入成绩 / 批量管理 -->
    <xm-action-bar
      v-if="userStore.role === 'TEACHER'"
      :manage-mode="manageMode"
      :total="total"
      :selected-count="selectedIds.length"
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
          <view class="xm-row item-head">
            <checkbox
              v-if="manageMode"
              :checked="selectedIds.includes(row.id)"
              style="transform: scale(0.8)"
              @click.stop="toggleSelect(row.id)"
            />
            <!-- 主标题：教师视角看学生、学生/管理员视角看课程 -->
            <view class="xm-value xm-ellipsis item-title">{{
              userStore.role === 'TEACHER' ? row.studentName : row.courseName
            }}</view>
          </view>
          <!-- 总分大字右置：不及格红色警示 -->
          <view
            class="score-total"
            :class="{ 'score-fail': Number(row.score) < 60 }"
            >{{ row.score }}</view
          >
        </view>
        <view class="item-meta">
          <text v-if="userStore.role === 'TEACHER'">{{ row.courseName }} · </text>
          <text v-else>{{ row.teacherName }} · </text>
          <text
            >{{ $t('pages.score.ordinaryScore') }} {{ row.ordinaryScore }} · {{ $t('pages.score.examScore') }}
            {{ row.examScore }}</text
          >
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
    </view>

    <xm-list-footer
      :visible="!!list.length"
      :loading="loading"
      :finished="finished()"
      @load-more="loadNext"
    />

    <!-- 成绩录入/编辑表单（底部弹层，仅教师） -->
    <xm-form-popup
      :visible="formVisible"
      :saving="saving"
      :title="$t('pages.score.entryTitle')"
      @close="closeForm"
      @save="save"
    >
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.score.selectCourse') }}</view>
        <xm-picker
          v-model="form.courseId"
          :options="courseData"
          label-key="name"
          value-key="id"
          :placeholder="$t('pages.score.coursePlaceholder')"
          @change="onFormCourseChange"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.score.selectStudent') }}</view>
        <xm-picker
          v-model="studentId"
          :options="studentData"
          label-key="studentName"
          value-key="studentId"
          :placeholder="$t('pages.score.studentPlaceholder')"
        />
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
    </xm-form-popup>
    <xm-loader />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { useListPage } from '@/composables/useListPage'
import { scoreApi, courseApi, choiceApi } from '@/api'
import { orNull, SILENT } from '@/utils/request'
import { t } from '@/i18n'
import { summarizeScores } from '@/utils/gpa'

const userStore = useUserStore()
const isStudent = computed(() => userStore.role === 'STUDENT')
// 课程下拉（搜索；教师的录入表单共用）/ 所选课程的选课学生
const courseData = ref([])
const studentData = ref([])
// 表单里选中的学生（保存前写入 form.studentId，与 Web 端一致）
const studentId = ref(null)

// 删除确认文案与 Web 端一致（删除后学生学分相应减少）
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
  api: scoreApi,
  title: 'menu.score',
  query: { courseId: '' },
  deleteConfirm: 'pages.score.deleteConfirm',
  loadExtras: () => loadCourse(),
  validate: (f) => {
    if (!f.ordinaryScore && f.ordinaryScore !== 0) return t('pages.score.ruleOrdinaryRequired')
    if (!f.examScore && f.examScore !== 0) return t('pages.score.ruleExamRequired')
    return ''
  },
  beforeSave: (formData) => {
    formData.studentId = studentId.value
  },
})

// 课程下拉：教师取本人所授课程（搜索与录入表单共用），学生取已选课程，管理员取全部课程
const loadCourse = async () => {
  if (isStudent.value) {
    const rows = (await orNull(choiceApi.selectAll({ studentId: userStore.user.id }, SILENT))) || []
    // 选课记录 → 课程选项（取值为课程 id）；选课记录联表带出的 score 是课程学分，供成绩汇总加权
    courseData.value = rows.map((r) => ({ id: r.courseId, name: r.name, credit: r.score }))
    loadSummary()
    return
  }
  const params = userStore.role === 'TEACHER' ? { teacherId: userStore.user.id } : undefined
  courseData.value = (await orNull(courseApi.selectAll(params, SILENT))) || []
}

// 学生成绩汇总：取本人全部成绩（后端按登录学生限定范围），不受列表分页与课程筛选影响
const summary = ref(null)
const loadSummary = async () => {
  const scores = await orNull(scoreApi.selectAll(undefined, SILENT))
  if (scores === null) return
  const credits = Object.fromEntries(courseData.value.map((c) => [c.id, c.credit]))
  summary.value = summarizeScores(scores, credits)
}

// 表单选择课程后联动加载选课学生（与 Web 端 getStudent 一致），并清空已选学生
const onFormCourseChange = async (course) => {
  const rows = await orNull(choiceApi.selectAll({ courseId: course.id }))
  if (rows === null) return
  studentData.value = rows
  studentId.value = null
}

const onAdd = () => {
  studentId.value = null
  handleAdd({ teacherId: userStore.user.id })
}
</script>

<style lang="scss" scoped>
/* 主标题行可收缩省略，右侧总分固定不被挤压 */
.item-head {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-weight: bold;
}

/* 总分大字右置；不及格红色警示 */
.score-total {
  font-size: 36rpx;
  font-weight: bold;
  color: var(--xm-brand);
  flex-shrink: 0;
}

.score-fail {
  color: var(--xm-danger);
}

/* 次要信息行：弱化小字 */
.item-meta {
  font-size: 24rpx;
  color: var(--xm-text-2);
  margin-top: 12rpx;
}

/* 学生成绩汇总卡：四项指标 + 换算规则说明 */
.summary-grid {
  display: flex;
  gap: 12rpx;
}

.summary-item {
  flex: 1;
  min-width: 0;
  text-align: center;
}

.summary-value {
  display: block;
  font-size: 40rpx;
  font-weight: bold;
  color: var(--xm-brand);
}

/* 有不及格时数字标红（需比 .summary-value 更具体才能覆盖品牌色） */
.summary-value.score-fail {
  color: var(--xm-danger);
}

.summary-label {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.summary-rule {
  margin-top: 16rpx;
  font-size: 22rpx;
  line-height: 1.5;
  color: var(--xm-text-2);
}

/* 搜索卡里的课程下拉：占满剩余宽度，课程名过长时省略而不是挤掉查询按钮 */
.search-course {
  flex: 1;
  min-width: 0;
}
</style>

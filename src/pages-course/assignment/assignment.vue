<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 作业详情（与 Web 端 views/course/AssignmentDetail.vue 一致）：
         学生看要求、提交 / 修改答案；老师看全班提交情况，逐个打分、写评语，批完自动跳到下一个 -->
    <view
      v-if="!data && loading"
      class="xm-card"
    >
      <view class="skeleton skeleton-title" />
      <view class="skeleton skeleton-line" />
      <view class="skeleton skeleton-line short" />
    </view>

    <template v-else-if="data">
      <view class="xm-card">
        <view class="title">{{ assignment.title }}</view>
        <view class="meta">
          <text
            class="xm-tag"
            :class="assignment.closed ? 'xm-tag-danger' : 'xm-tag-brand'"
            >{{
              assignment.closed
                ? $t('space.assignments.closed')
                : $t('space.assignments.due', { time: assignment.deadline })
            }}</text
          >
          <text
            v-if="assignment.closed"
            class="xm-num"
            >{{ assignment.deadline }}</text
          >
          <text>{{ $t('space.assignments.fullScoreOf', { n: assignment.fullScore }) }}</text>
        </view>
        <view
          v-if="canTeach"
          class="xm-actions"
        >
          <button
            class="xm-btn xm-btn-plain xm-btn-sm"
            @click="formOpen = true"
          >
            {{ $t('common.edit') }}
          </button>
          <button
            class="xm-btn xm-btn-danger xm-btn-sm"
            @click="remove"
          >
            {{ $t('common.delete') }}
          </button>
        </view>
      </view>

      <view class="xm-card">
        <view class="xm-section-head">
          <text class="xm-section-title">{{ $t('space.assignments.requirement') }}</text>
        </view>
        <view
          v-if="assignment.content"
          class="text"
          >{{ assignment.content }}</view
        >
        <view
          v-else
          class="muted"
          >{{ $t('space.assignments.noContent') }}</view
        >
        <view
          v-if="assignment.attachment"
          class="file"
        >
          <xm-file-chip
            :url="assignment.attachment"
            :name="assignment.attachmentName"
          />
        </view>
      </view>

      <!-- ========== 学生：我的提交 ========== -->
      <view
        v-if="!canTeach"
        class="xm-card"
      >
        <view class="xm-section-head">
          <text class="xm-section-title">{{ $t('space.assignments.mySubmission') }}</text>
          <text
            class="xm-tag"
            :class="statusMeta(data.myStatus).tag"
            >{{ $t('space.assignments.status.' + statusMeta(data.myStatus).key) }}</text
          >
        </view>

        <template v-if="mine && mine.status === '已批改'">
          <view class="score">
            <text class="score-value xm-num">{{ mine.score }}</text>
            <text class="score-full xm-num">/ {{ assignment.fullScore }}</text>
          </view>
          <view
            v-if="mine.descr"
            class="feedback"
          >
            <view class="feedback-label">{{ $t('space.assignments.teacherComment') }}</view>
            <view class="text">{{ mine.descr }}</view>
          </view>
          <view class="muted small">{{ $t('space.assignments.gradedHint') }}</view>
          <view class="answer">
            <view
              v-if="mine.content"
              class="text"
              >{{ mine.content }}</view
            >
            <xm-file-chip
              v-if="mine.file"
              :url="mine.file"
              :name="mine.fileName"
            />
          </view>
        </template>

        <template v-else-if="!assignment.closed">
          <textarea
            class="xm-textarea answer-input"
            v-model="answer"
            maxlength="5000"
            :placeholder="$t('space.assignments.answerPlaceholder')"
            :show-confirm-bar="false"
          />
          <view class="submit-row">
            <view class="submit-file">
              <xm-file-picker v-model="answerFile" />
            </view>
          </view>
          <button
            class="xm-btn xm-btn-primary xm-btn-block xm-btn-lg"
            :loading="submitting"
            :disabled="submitting || (!answer.trim() && !answerFile)"
            @click="submit"
          >
            {{ mine ? $t('space.assignments.resubmit') : $t('space.assignments.submit') }}
          </button>
          <view
            v-if="mine && mine.submitTime"
            class="muted small"
            >{{ $t('space.assignments.submitTime', { time: mine.submitTime }) }}</view
          >
        </template>

        <template v-else>
          <view class="muted">{{ $t('space.assignments.overdueHint') }}</view>
          <view
            v-if="mine"
            class="answer"
          >
            <view
              v-if="mine.content"
              class="text"
              >{{ mine.content }}</view
            >
            <xm-file-chip
              v-if="mine.file"
              :url="mine.file"
              :name="mine.fileName"
            />
            <view
              v-if="mine.submitTime"
              class="muted small"
              >{{ $t('space.assignments.submitTime', { time: mine.submitTime }) }}</view
            >
          </view>
        </template>
      </view>

      <!-- ========== 老师：提交情况与批改 ========== -->
      <view
        v-else
        class="xm-card"
      >
        <view class="xm-section-head">
          <text class="xm-section-title">{{ $t('space.assignments.roster') }}</text>
        </view>
        <xm-chips
          v-model="filter"
          :options="filterOptions"
        />
        <view
          v-if="!filtered.length"
          class="muted empty"
          >{{ $t('common.empty') }}</view
        >
        <view
          v-for="row in filtered"
          :key="row.studentId"
          class="row"
          :class="{ 'is-open': openId === row.studentId }"
        >
          <view
            class="row-head"
            @click="toggle(row)"
          >
            <xm-user-avatar
              :name="row.name"
              :avatar="row.avatar"
              :size="64"
            />
            <view class="row-who">
              <view class="row-name">{{ row.name }}</view>
              <view class="row-sub xm-num">{{ row.username }}</view>
            </view>
            <text
              v-if="row.submission && row.submission.status === '已批改'"
              class="row-score xm-num"
              >{{ row.submission.score }}</text
            >
            <text
              class="xm-tag"
              :class="statusMeta(row.status).tag"
              >{{ $t('space.assignments.status.' + statusMeta(row.status).key) }}</text
            >
          </view>

          <view
            v-if="openId === row.studentId && row.submission"
            class="row-body"
          >
            <view class="muted small">{{
              $t('space.assignments.submitTime', { time: row.submission.submitTime || '—' })
            }}</view>
            <view
              v-if="row.submission.content"
              class="text"
              >{{ row.submission.content }}</view
            >
            <view
              v-else
              class="muted"
              >{{ $t('space.assignments.noAnswer') }}</view
            >
            <view
              v-if="row.submission.file"
              class="file"
            >
              <xm-file-chip
                :url="row.submission.file"
                :name="row.submission.fileName"
              />
            </view>
            <view class="grade">
              <view class="grade-score">
                <text class="grade-label">{{ $t('space.assignments.score') }}</text>
                <input
                  class="grade-input xm-num"
                  :value="gradeScore"
                  type="digit"
                  maxlength="6"
                  @input="onScoreInput"
                />
                <text class="muted small xm-num">/ {{ assignment.fullScore }}</text>
              </view>
              <textarea
                class="xm-textarea grade-comment"
                v-model="gradeComment"
                maxlength="500"
                :placeholder="$t('space.assignments.commentPlaceholder')"
                :show-confirm-bar="false"
              />
              <button
                class="xm-btn xm-btn-primary xm-btn-block"
                :loading="grading"
                :disabled="grading || gradeScore === ''"
                @click="grade(row)"
              >
                {{
                  row.submission.status === '已批改' ? $t('space.assignments.regrade') : $t('space.assignments.grade')
                }}
              </button>
            </view>
          </view>
        </view>
      </view>
    </template>

    <assignment-form
      v-if="canTeach"
      :visible="formOpen"
      :course-id="courseId"
      :editing="assignment"
      @close="formOpen = false"
      @saved="onSaved"
    />
    <xm-loader />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { assignmentApi } from '@/api'
import { SILENT } from '@/utils/request'
import { ensureLoggedIn } from '@/utils/authGuard'
import { ASSIGNMENT_STATUSES } from '@/utils/courseSpace'
import { useCourseEvents } from '@/composables/useCourseEvents'
import { confirm } from '@/utils/confirm'
import { t } from '@/i18n'
import AssignmentForm from '../space/AssignmentForm.vue'

const FILTERS = ['all', 'ungraded', 'graded', 'missing']

const courseId = ref(0)
const assignmentId = ref(0)
const data = ref(null)
const loading = ref(false)

const assignment = computed(() => (data.value && data.value.assignment) || {})
const mine = computed(() => (data.value && data.value.mySubmission) || null)
/** 老师 / 管理员才拿得到全班名单 */
const canTeach = computed(() => !!(data.value && Array.isArray(data.value.roster)))

const statusMeta = (status) => ASSIGNMENT_STATUSES[status] || { key: 'pending', tag: '' }

// ---------- 学生提交（表单状态） ----------
const answer = ref('')
const answerFile = ref(null)
const submitting = ref(false)

const resetAnswer = () => {
  answer.value = (mine.value && mine.value.content) || ''
  answerFile.value = mine.value && mine.value.file ? { url: mine.value.file, name: mine.value.fileName || '' } : null
}

const load = async () => {
  if (!courseId.value || !assignmentId.value) return
  loading.value = true
  try {
    data.value = await assignmentApi.detail(courseId.value, assignmentId.value, SILENT)
    uni.setNavigationBarTitle({ title: assignment.value.title || t('space.tabs.assignments') })
    if (!canTeach.value) resetAnswer()
  } catch {
    data.value = null
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  submitting.value = true
  try {
    await assignmentApi.submit(courseId.value, assignmentId.value, {
      content: answer.value,
      file: (answerFile.value && answerFile.value.url) || null,
      fileName: (answerFile.value && answerFile.value.name) || null,
    })
    uni.showToast({ title: t('space.assignments.submitOk'), icon: 'success' })
    await load()
  } catch {
    // 截止、已批改等提示已由请求层统一弹出
  } finally {
    submitting.value = false
  }
}

// ---------- 老师批改 ----------
const filter = ref('all')
const openId = ref(null)
const gradeScore = ref('')
const gradeComment = ref('')
const grading = ref(false)

const roster = computed(() => (data.value && data.value.roster) || [])

const matches = (row, f) => {
  if (f === 'all') return true
  if (f === 'graded') return row.status === '已批改'
  if (f === 'ungraded') return row.status === '已提交'
  return !row.submission
}

const filterOptions = computed(() =>
  FILTERS.map((f) => ({
    value: f,
    label: t('space.assignments.filter.' + f),
    count: roster.value.filter((row) => matches(row, f)).length,
  })),
)

const filtered = computed(() => roster.value.filter((row) => matches(row, filter.value)))

const toggle = (row) => {
  if (openId.value === row.studentId || !row.submission) {
    openId.value = null
    return
  }
  openId.value = row.studentId
  const current = Number.parseFloat(row.submission.score)
  gradeScore.value = Number.isFinite(current) ? String(current) : ''
  gradeComment.value = row.submission.descr || ''
}

const onScoreInput = (e) => {
  gradeScore.value = String(e.detail.value || '')
}

const grade = async (row) => {
  const score = Number(gradeScore.value)
  if (gradeScore.value === '' || !Number.isFinite(score) || score < 0 || score > assignment.value.fullScore) {
    uni.showToast({ title: t('errors.5022'), icon: 'none' })
    return
  }
  grading.value = true
  try {
    const saved = await assignmentApi.grade(courseId.value, row.submission.id, {
      score: Math.round(score * 10) / 10,
      descr: gradeComment.value,
    })
    uni.showToast({ title: t('space.assignments.gradeSaved'), icon: 'none' })
    row.submission = saved
    row.status = '已批改'
    // 批完一个自动展开下一个待批改的，连续批改不用来回点
    const next = roster.value.find((r) => r.status === '已提交' && r.studentId !== row.studentId)
    if (next) toggle(next)
    else openId.value = null
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    grading.value = false
  }
}

// ---------- 编辑 / 删除 ----------
const formOpen = ref(false)

const onSaved = async () => {
  formOpen.value = false
  await load()
}

const remove = async () => {
  if (!(await confirm(t('space.assignments.deleteConfirm', { title: assignment.value.title })))) return
  try {
    await assignmentApi.remove(courseId.value, assignmentId.value)
    uni.showToast({ title: t('space.assignments.deleted'), icon: 'none' })
    setTimeout(() => uni.navigateBack(), 600)
  } catch {
    // 提示已由请求层统一弹出
  }
}

// 老师在别处改了作业、有人交了作业时，后端推课程事件，这里跟着刷新（正在批改时不打断）
useCourseEvents(
  () => courseId.value,
  ['assignments'],
  () => {
    if (openId.value === null && !formOpen.value) load()
  },
)

onLoad((query) => {
  courseId.value = Number(query.courseId) || 0
  assignmentId.value = Number(query.id) || 0
})

let shownOnce = false
onShow(() => {
  if (!ensureLoggedIn()) return
  if (shownOnce) return
  shownOnce = true
  load()
})

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
.title {
  font-size: 36rpx;
  font-weight: bold;
  line-height: 1.4;
  color: var(--xm-text);
}

.meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12rpx 20rpx;
  margin-top: 14rpx;
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.text {
  font-size: 28rpx;
  line-height: 1.8;
  color: var(--xm-text);
  white-space: pre-wrap;
  word-break: break-all;
}

.muted {
  font-size: 26rpx;
  color: var(--xm-text-2);
}

.muted.small {
  margin-top: 12rpx;
  font-size: 22rpx;
  color: var(--xm-text-3);
}

.empty {
  padding: 24rpx 0;
  text-align: center;
}

.file {
  margin-top: 16rpx;
}

.score {
  display: flex;
  align-items: baseline;
  gap: 8rpx;
}

.score-value {
  font-size: 80rpx;
  font-weight: bold;
  line-height: 1.1;
  color: var(--xm-brand);
}

.score-full {
  font-size: 28rpx;
  color: var(--xm-text-2);
}

.feedback {
  margin-top: 16rpx;
  padding: 18rpx 20rpx;
  border-radius: 14rpx;
  background: var(--xm-bg-sunken);
}

.feedback-label {
  margin-bottom: 6rpx;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.answer {
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid var(--xm-border);
}

.answer-input {
  min-height: 300rpx;
}

.submit-row {
  margin: 20rpx 0;
}

.submit-file {
  min-width: 0;
}

.row {
  border-top: 1rpx solid var(--xm-border);
}

.row:first-of-type {
  margin-top: 12rpx;
}

.row-head {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 18rpx 0;
}

.row-who {
  flex: 1;
  min-width: 0;
}

.row-name {
  font-size: 28rpx;
  color: var(--xm-text);
}

.row-sub {
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.row-score {
  font-size: 32rpx;
  font-weight: bold;
  color: var(--xm-brand);
}

.row.is-open .row-head {
  font-weight: 600;
}

.row-body {
  padding: 0 0 24rpx;
}

.grade {
  margin-top: 20rpx;
  padding: 20rpx;
  border-radius: 16rpx;
  background: var(--xm-bg-sunken);
}

.grade-score {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-bottom: 16rpx;
}

.grade-label {
  font-size: 26rpx;
  color: var(--xm-text);
}

.grade-input {
  width: 160rpx;
  height: 68rpx;
  padding: 0 16rpx;
  border: 1rpx solid var(--xm-border);
  border-radius: 12rpx;
  background: var(--xm-bg-card);
  font-size: 30rpx;
  color: var(--xm-text);
}

.grade-comment {
  min-height: 140rpx;
  margin-bottom: 16rpx;
  background: var(--xm-bg-card);
}
</style>

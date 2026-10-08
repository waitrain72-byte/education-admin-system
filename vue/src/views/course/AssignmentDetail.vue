<template>
  <div class="ad">
    <el-skeleton v-if="!data && loading" animated :rows="6" />

    <template v-else-if="data">
      <header class="ad-head">
        <div class="ad-head__meta">
          <span class="pill" :class="assignment.closed ? 'pill--bad' : 'pill--brand'">
            {{ assignment.closed ? $t('space.assignments.closed') : $t('space.assignments.due', { time: assignment.deadline }) }}
          </span>
          <span v-if="assignment.closed" class="num ad-head__deadline">{{ assignment.deadline }}</span>
          <span class="ad-head__full">{{ $t('space.assignments.fullScoreOf', { n: assignment.fullScore }) }}</span>
        </div>
        <div v-if="canTeach" class="ad-head__actions">
          <el-button size="small" @click="emit('edit', assignment)">{{ $t('common.edit') }}</el-button>
          <el-button size="small" type="danger" plain @click="emit('delete', assignment)">{{ $t('common.delete') }}</el-button>
        </div>
      </header>

      <section class="ad-section">
        <h3 class="ad-section__title">{{ $t('space.assignments.requirement') }}</h3>
        <p v-if="assignment.content" class="ad-text">{{ assignment.content }}</p>
        <p v-else class="ad-muted">{{ $t('space.assignments.noContent') }}</p>
        <FileChip v-if="assignment.attachment" class="ad-file" :url="assignment.attachment" :name="assignment.attachmentName" />
      </section>

      <!-- ========== 学生：我的提交 ========== -->
      <section v-if="!canTeach" class="ad-section">
        <h3 class="ad-section__title">
          {{ $t('space.assignments.mySubmission') }}
          <span class="pill" :class="statusMeta(data.myStatus).pill">{{ $t('space.assignments.status.' + statusMeta(data.myStatus).key) }}</span>
        </h3>

        <template v-if="mine && mine.status === '已批改'">
          <div class="ad-score">
            <span class="ad-score__value num">{{ mine.score }}</span>
            <span class="ad-score__full num">/ {{ assignment.fullScore }}</span>
          </div>
          <div v-if="mine.descr" class="ad-feedback">
            <div class="ad-feedback__label">{{ $t('space.assignments.teacherComment') }}</div>
            <p class="ad-text">{{ mine.descr }}</p>
          </div>
          <p class="ad-muted ad-muted--small">{{ $t('space.assignments.gradedHint') }}</p>
          <div class="ad-answer">
            <p v-if="mine.content" class="ad-text">{{ mine.content }}</p>
            <FileChip v-if="mine.file" :url="mine.file" :name="mine.fileName" />
          </div>
        </template>

        <template v-else-if="!assignment.closed">
          <el-input
            v-model="answer"
            type="textarea"
            :rows="7"
            maxlength="5000"
            show-word-limit
            :placeholder="$t('space.assignments.answerPlaceholder')"
          />
          <div class="ad-submit">
            <FileUploadButton v-model="answerFile" />
            <el-button type="primary" :loading="submitting" :disabled="!answer.trim() && !answerFile" @click="submit">
              {{ mine ? $t('space.assignments.resubmit') : $t('space.assignments.submit') }}
            </el-button>
          </div>
          <p v-if="mine?.submitTime" class="ad-muted ad-muted--small">
            {{ $t('space.assignments.submitTime', { time: mine.submitTime }) }}
          </p>
        </template>

        <template v-else>
          <p class="ad-muted">{{ $t('space.assignments.overdueHint') }}</p>
          <div v-if="mine" class="ad-answer">
            <p v-if="mine.content" class="ad-text">{{ mine.content }}</p>
            <FileChip v-if="mine.file" :url="mine.file" :name="mine.fileName" />
            <p v-if="mine.submitTime" class="ad-muted ad-muted--small">
              {{ $t('space.assignments.submitTime', { time: mine.submitTime }) }}
            </p>
          </div>
        </template>
      </section>

      <!-- ========== 老师：提交情况与批改 ========== -->
      <section v-else class="ad-section">
        <div class="ad-roster-head">
          <h3 class="ad-section__title">{{ $t('space.assignments.roster') }}</h3>
          <el-radio-group v-model="filter" size="small">
            <el-radio-button v-for="f in FILTERS" :key="f" :value="f">
              {{ $t('space.assignments.filter.' + f) }} {{ filterCounts[f] }}
            </el-radio-button>
          </el-radio-group>
        </div>

        <div v-if="!filtered.length" class="empty-note">{{ $t('common.empty') }}</div>
        <ul v-else class="ad-roster">
          <li v-for="row in filtered" :key="row.studentId" class="ad-row" :class="{ 'is-open': openId === row.studentId }">
            <button type="button" class="ad-row__head" :aria-expanded="openId === row.studentId" @click="toggle(row)">
              <UserAvatar :name="row.name" :avatar="row.avatar" :size="32" />
              <span class="ad-row__who">
                <span class="ad-row__name">{{ row.name }}</span>
                <span class="ad-row__sub num">{{ row.username }}</span>
              </span>
              <span v-if="row.submission?.status === '已批改'" class="ad-row__score num">{{ row.submission.score }}</span>
              <span class="pill" :class="statusMeta(row.status).pill">{{ $t('space.assignments.status.' + statusMeta(row.status).key) }}</span>
            </button>

            <div v-if="openId === row.studentId && row.submission" class="ad-row__body">
              <p class="ad-muted ad-muted--small">{{ $t('space.assignments.submitTime', { time: row.submission.submitTime || '—' }) }}</p>
              <p v-if="row.submission.content" class="ad-text">{{ row.submission.content }}</p>
              <p v-else class="ad-muted">{{ $t('space.assignments.noAnswer') }}</p>
              <FileChip v-if="row.submission.file" :url="row.submission.file" :name="row.submission.fileName" />
              <div class="ad-grade">
                <label class="ad-grade__score">
                  <span>{{ $t('space.assignments.score') }}</span>
                  <el-input-number
                    v-model="gradeScore"
                    :min="0"
                    :max="assignment.fullScore"
                    :precision="1"
                    :step="1"
                    controls-position="right"
                  />
                  <span class="ad-muted ad-muted--small num">/ {{ assignment.fullScore }}</span>
                </label>
                <el-input
                  v-model="gradeComment"
                  type="textarea"
                  :rows="2"
                  maxlength="500"
                  :placeholder="$t('space.assignments.commentPlaceholder')"
                />
                <el-button type="primary" :loading="grading" :disabled="gradeScore == null" @click="grade(row)">
                  {{ row.submission.status === '已批改' ? $t('space.assignments.regrade') : $t('space.assignments.grade') }}
                </el-button>
              </div>
            </div>
          </li>
        </ul>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import UserAvatar from '@/components/UserAvatar.vue'
import FileChip from '@/components/FileChip.vue'
import FileUploadButton, { type UploadedFile } from '@/components/FileUploadButton.vue'
import request from '@/utils/request'
import { ElMessage } from '@/utils/element-plus'
import { ASSIGNMENT_STATUSES } from '@/utils/courseSpace'
import { t } from '@/i18n'

const props = defineProps<{
  courseId: number
  assignmentId: number
  canTeach: boolean
}>()
const emit = defineEmits<{
  edit: [assignment: Record<string, any>]
  delete: [assignment: Record<string, any>]
  changed: []
}>()

const FILTERS = ['all', 'ungraded', 'graded', 'missing'] as const
type Filter = (typeof FILTERS)[number]

const data = ref<Record<string, any> | null>(null)
const loading = ref(false)
const assignment = computed<Record<string, any>>(() => data.value?.assignment || {})
const mine = computed<Record<string, any> | null>(() => data.value?.mySubmission || null)

const statusMeta = (status: string) => ASSIGNMENT_STATUSES[status] || { key: 'pending', pill: '' }

// ---------- 学生提交（表单状态） ----------
const answer = ref('')
const answerFile = ref<UploadedFile | null>(null)
const submitting = ref(false)

const resetAnswer = () => {
  answer.value = mine.value?.content || ''
  answerFile.value = mine.value?.file ? { url: mine.value.file, name: mine.value.fileName || '' } : null
}

const load = async () => {
  loading.value = true
  try {
    data.value = await request.get<Record<string, any>>(`/course/${props.courseId}/assignments/${props.assignmentId}`)
    resetAnswer()
  } catch {
    data.value = null
  } finally {
    loading.value = false
  }
}

watch(() => [props.courseId, props.assignmentId], load, { immediate: true })

defineExpose({ reload: load })

// ---------- 学生提交 ----------
const submit = async () => {
  submitting.value = true
  try {
    await request.post(`/course/${props.courseId}/assignments/${props.assignmentId}/submit`, {
      content: answer.value,
      file: answerFile.value?.url || null,
      fileName: answerFile.value?.name || null,
    })
    ElMessage.success(t('space.assignments.submitOk'))
    await load()
    emit('changed')
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    submitting.value = false
  }
}

// ---------- 老师批改 ----------
const filter = ref<Filter>('all')
const openId = ref<number | null>(null)
const gradeScore = ref<number | null>(null)
const gradeComment = ref('')
const grading = ref(false)

const roster = computed<Record<string, any>[]>(() => data.value?.roster || [])

const matches = (row: Record<string, any>, f: Filter) => {
  if (f === 'all') return true
  if (f === 'graded') return row.status === '已批改'
  if (f === 'ungraded') return row.status === '已提交'
  return !row.submission
}

const filterCounts = computed(() => {
  const counts = {} as Record<Filter, number>
  for (const f of FILTERS) counts[f] = roster.value.filter((row) => matches(row, f)).length
  return counts
})

const filtered = computed(() => roster.value.filter((row) => matches(row, filter.value)))

const toggle = (row: Record<string, any>) => {
  if (openId.value === row.studentId || !row.submission) {
    openId.value = null
    return
  }
  openId.value = row.studentId
  const current = Number.parseFloat(row.submission.score)
  gradeScore.value = Number.isFinite(current) ? current : null
  gradeComment.value = row.submission.descr || ''
}

const grade = async (row: Record<string, any>) => {
  if (gradeScore.value == null) return
  grading.value = true
  try {
    const saved = await request.put<Record<string, any>>(
      `/course/${props.courseId}/assignments/submissions/${row.submission.id}`,
      { score: gradeScore.value, descr: gradeComment.value },
    )
    ElMessage.success(t('space.assignments.gradeSaved'))
    row.submission = saved
    row.status = '已批改'
    // 批完一个自动展开下一个待批改的，连续批改不用来回点
    const next = roster.value.find((r) => r.status === '已提交' && r.studentId !== row.studentId)
    if (next) toggle(next)
    else openId.value = null
    emit('changed')
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    grading.value = false
  }
}
</script>

<style scoped>
.ad {
  display: grid;
  gap: 22px;
}

.ad-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.ad-head__meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.ad-head__actions {
  display: flex;
  gap: 8px;
}

.ad-section {
  display: grid;
  gap: 10px;
}

.ad-section__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  color: var(--xm-text-primary);
}

.ad-text {
  font-size: 14px;
  line-height: 1.75;
  color: var(--xm-text-regular);
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.ad-muted {
  font-size: 14px;
  color: var(--xm-text-secondary);
}

.ad-muted--small {
  font-size: 12px;
}

.ad-file {
  justify-self: start;
}

.ad-submit {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.ad-score {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.ad-score__value {
  font-size: 40px;
  font-weight: 700;
  color: var(--xm-brand);
  line-height: 1;
}

.ad-score__full {
  font-size: 16px;
  color: var(--xm-text-secondary);
}

.ad-feedback {
  padding: 12px 14px;
  border-left: 3px solid var(--xm-brand);
  border-radius: var(--xm-radius-sm);
  background: var(--xm-brand-soft);
}

.ad-feedback__label {
  margin-bottom: 4px;
  font-size: 12px;
  font-weight: 600;
  color: var(--xm-brand);
}

.ad-answer {
  display: grid;
  gap: 8px;
  justify-items: start;
  padding: 12px 14px;
  border: 1px dashed var(--xm-border);
  border-radius: var(--xm-radius);
}

/* ---------- 提交名单 ---------- */
.ad-roster-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.ad-roster {
  list-style: none;
  display: grid;
  gap: 6px;
}

.ad-row {
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius);
}

.ad-row.is-open {
  border-color: var(--xm-brand);
}

.ad-row__head {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 8px 12px;
  border: 0;
  background: none;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.ad-row__who {
  display: grid;
  flex: 1;
  min-width: 0;
}

.ad-row__name {
  font-size: 14px;
  color: var(--xm-text-primary);
}

.ad-row__sub {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.ad-row__score {
  font-size: 16px;
  font-weight: 700;
  color: var(--xm-brand);
}

.ad-row__body {
  display: grid;
  gap: 10px;
  justify-items: start;
  padding: 4px 12px 14px 54px;
}

.ad-grade {
  display: grid;
  gap: 8px;
  justify-items: start;
  width: 100%;
  padding-top: 10px;
  border-top: 1px solid var(--xm-border);
}

.ad-grade__score {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--xm-text-regular);
}

@media (max-width: 560px) {
  .ad-row__body {
    padding-left: 12px;
  }
}
</style>

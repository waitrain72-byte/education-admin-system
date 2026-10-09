<template>
  <!-- 课程空间 · 作业（与 Web 端 views/course/CourseAssignments.vue 一致）：
       作业列表（老师看提交 / 批改进度，学生看自己的状态与得分），点进去是作业详情页 -->
  <view>
    <button
      v-if="canTeach"
      class="xm-btn xm-btn-primary xm-btn-block create"
      @click="formOpen = true"
    >
      <xm-icon
        name="plus"
        :size="30"
      />
      {{ $t('space.assignments.create') }}
    </button>

    <view
      v-if="!data && loading"
      class="xm-card"
    >
      <view class="skeleton skeleton-title" />
      <view class="skeleton skeleton-line" />
    </view>

    <template v-else-if="data">
      <xm-empty
        v-if="!data.assignments.length"
        icon="clipboard"
        :text="canTeach ? $t('space.assignments.empty') : $t('space.assignments.emptyStudent')"
      />

      <view
        v-for="a in data.assignments"
        :key="a.id"
        class="xm-card item"
        @click="open(a.id)"
      >
        <view class="item-head">
          <text class="item-title">{{ a.title }}</text>
          <template v-if="!canTeach">
            <text
              v-if="a.myScore != null"
              class="item-score xm-num"
              >{{ a.myScore }}<text class="item-full">/{{ a.fullScore }}</text></text
            >
            <text
              class="xm-tag"
              :class="statusMeta(a.myStatus).tag"
              >{{ $t('space.assignments.status.' + statusMeta(a.myStatus).key) }}</text
            >
          </template>
        </view>
        <view class="item-meta">
          <text :class="{ 'is-closed': a.closed }">{{
            a.closed
              ? $t('space.assignments.closed') + ' · ' + a.deadline
              : $t('space.assignments.due', { time: a.deadline })
          }}</text>
          <text class="xm-num">{{ $t('space.assignments.fullScoreOf', { n: a.fullScore }) }}</text>
          <text v-if="a.attachment">{{ $t('space.assignments.attachment') }}</text>
        </view>
        <view
          v-if="canTeach"
          class="item-progress"
        >
          <text class="item-counts xm-num">
            {{ $t('space.assignments.submittedCount', { n: a.submitted, total: a.students }) }} ·
            {{ $t('space.assignments.gradedCount', { n: a.graded }) }}
          </text>
          <view class="item-bar">
            <view
              class="item-bar-graded"
              :style="'width:' + percent(a.graded, a.students) + '%'"
            />
            <view
              class="item-bar-submitted"
              :style="'width:' + percent(a.submitted - a.graded, a.students) + '%'"
            />
          </view>
        </view>
      </view>

      <!-- 改版前没挂在作业下的旧提交（老师批改用） -->
      <view
        v-if="canTeach && data.looseCount > 0"
        class="xm-card"
      >
        <view class="xm-section-head">
          <text class="xm-section-title">{{ $t('space.assignments.loose') }}</text>
          <text
            class="xm-link"
            @click="toggleLoose"
            >{{ $t('space.assignments.looseView') }}</text
          >
        </view>
        <view class="loose-hint">{{ $t('space.assignments.looseHint', { n: data.looseCount }) }}</view>
        <template v-if="looseOpen">
          <view
            v-for="h in loose"
            :key="h.id"
            class="loose"
          >
            <view class="loose-name">{{ h.studentName || '—' }}</view>
            <view
              v-if="h.content"
              class="loose-content"
              >{{ h.content }}</view
            >
            <xm-file-chip
              v-if="h.file"
              :url="h.file"
              :name="h.fileName || h.content"
            />
            <view class="loose-grade">
              <input
                class="xm-input loose-input"
                v-model="looseScores[h.id]"
                type="digit"
                :placeholder="$t('space.assignments.score')"
              />
              <button
                class="xm-btn xm-btn-soft xm-btn-sm"
                :disabled="looseScores[h.id] === '' || looseScores[h.id] == null"
                @click="gradeLoose(h)"
              >
                {{ h.score ? $t('space.assignments.regrade') : $t('space.assignments.grade') }}
              </button>
            </view>
          </view>
        </template>
      </view>
    </template>

    <assignment-form
      :visible="formOpen"
      :course-id="courseId"
      :editing="null"
      @close="formOpen = false"
      @saved="onCreated"
    />
  </view>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { assignmentApi } from '@/api'
import { SILENT } from '@/utils/request'
import { ASSIGNMENT_STATUSES } from '@/utils/courseSpace'
import { useCourseEvents } from '@/composables/useCourseEvents'
import { t } from '@/i18n'
import AssignmentForm from './AssignmentForm.vue'

const props = defineProps({
  overview: { type: Object, required: true },
  courseId: { type: Number, required: true },
  /** 进来就打开的作业（消息链接、首页待交作业带的 open=ID），只用一次 */
  openId: { type: Number, default: 0 },
})

const emit = defineEmits(['refresh'])

const relation = computed(() => props.overview.relation || 'visitor')
const canTeach = computed(() => relation.value === 'teacher' || relation.value === 'admin')

const data = ref(null)
const loading = ref(false)
const formOpen = ref(false)

const statusMeta = (status) => ASSIGNMENT_STATUSES[status] || { key: 'pending', tag: '' }
const percent = (n, total) => (total ? Math.max(0, Math.min(100, (n / total) * 100)) : 0)

const load = async () => {
  loading.value = true
  try {
    data.value = await assignmentApi.list(props.courseId, SILENT)
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    loading.value = false
  }
}

const open = (id) => uni.navigateTo({ url: `/pages-course/assignment/assignment?courseId=${props.courseId}&id=${id}` })

let openedFromQuery = false
watch(
  () => props.courseId,
  async () => {
    await load()
    const id = props.openId
    if (!openedFromQuery && id && data.value && data.value.assignments.some((a) => a.id === id)) {
      openedFromQuery = true
      open(id)
    }
  },
  { immediate: true },
)

useCourseEvents(() => props.courseId, ['assignments'], load)

const onCreated = async (saved) => {
  formOpen.value = false
  await load()
  emit('refresh')
  if (saved && saved.id) open(saved.id)
}

// ---------- 旧版自由提交 ----------
const looseOpen = ref(false)
const loose = ref([])
const looseScores = reactive({})

const loadLoose = async () => {
  try {
    loose.value = (await assignmentApi.loose(props.courseId, SILENT)) || []
    for (const h of loose.value) {
      const current = Number.parseFloat(h.score)
      looseScores[h.id] = Number.isFinite(current) ? String(current) : ''
    }
  } catch {
    loose.value = []
  }
}

const toggleLoose = async () => {
  looseOpen.value = !looseOpen.value
  if (looseOpen.value) await loadLoose()
}

const gradeLoose = async (h) => {
  const score = Number(looseScores[h.id])
  if (!Number.isFinite(score) || score < 0 || score > 100) {
    uni.showToast({ title: t('errors.5014'), icon: 'none' })
    return
  }
  try {
    const saved = await assignmentApi.grade(props.courseId, h.id, { score, descr: h.descr || '' })
    h.score = saved.score
    uni.showToast({ title: t('space.assignments.gradeSaved'), icon: 'none' })
    emit('refresh')
  } catch {
    // 提示已由请求层统一弹出
  }
}

defineExpose({ reload: load })
</script>

<style lang="scss" scoped>
.create {
  margin-bottom: 24rpx;
}

.item:active {
  opacity: 0.85;
}

.item-head {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.item-title {
  flex: 1;
  min-width: 0;
  font-size: 30rpx;
  font-weight: 600;
  color: var(--xm-text);
}

.item-score {
  font-size: 34rpx;
  font-weight: bold;
  color: var(--xm-brand);
}

.item-full {
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx 20rpx;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.is-closed {
  color: var(--xm-danger);
}

.item-progress {
  margin-top: 16rpx;
}

.item-counts {
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.item-bar {
  display: flex;
  height: 10rpx;
  margin-top: 8rpx;
  border-radius: 999rpx;
  background: var(--xm-bg-sunken);
  overflow: hidden;
}

.item-bar-graded {
  background: var(--xm-success);
}

.item-bar-submitted {
  background: var(--xm-info);
}

.loose-hint {
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.loose {
  padding: 20rpx 0;
  border-top: 1rpx solid var(--xm-border);
}

.loose-name {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--xm-text);
}

.loose-content {
  margin: 8rpx 0;
  font-size: 26rpx;
  color: var(--xm-text-2);
  word-break: break-all;
}

.loose-grade {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 12rpx;
}

.loose-input {
  flex: 1;
  height: 64rpx;
}
</style>

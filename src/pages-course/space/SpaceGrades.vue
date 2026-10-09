<template>
  <!-- 课程空间 · 成绩（与 Web 端 views/course/CourseGrades.vue 一致）：
       老师 / 管理员是成绩册（权重、统计、逐个录平时与期末、保存、发布 / 撤回）；学生看自己的成绩 -->
  <view>
    <view
      v-if="loading && !book && !mine"
      class="xm-card"
    >
      <view class="skeleton skeleton-title" />
      <view class="skeleton skeleton-line" />
      <view class="skeleton skeleton-line short" />
    </view>

    <!-- ==================== 老师 / 管理员：成绩册 ==================== -->
    <template v-else-if="canTeach && book">
      <view class="xm-card">
        <view class="xm-section-head">
          <text class="xm-section-title">{{ $t('space.grades.weights') }}</text>
          <text
            class="sum"
            :class="{ 'is-bad': !weightsOk }"
            >{{
              weightsOk ? $t('space.grades.sum', { n: weightSum }) : $t('space.grades.sumBad', { n: weightSum })
            }}</text
          >
        </view>
        <view class="weights">
          <view
            v-for="key in WEIGHT_KEYS"
            :key="key"
            class="weight"
          >
            <text class="weight-label">{{ $t('space.weights.' + key) }}</text>
            <view class="weight-field">
              <input
                class="weight-input xm-num"
                :value="weightText[key]"
                type="number"
                maxlength="3"
                @input="onWeightInput(key, $event)"
              />
              <text class="weight-pct">%</text>
            </view>
          </view>
        </view>
        <view class="rule">{{ $t('space.grades.rule') }}</view>
      </view>

      <view class="xm-card">
        <view class="stats">
          <view class="stat">
            <text class="stat-label">{{ $t('space.grades.stats.average') }}</text>
            <text class="stat-value xm-num">{{ stats.average == null ? '—' : stats.average }}</text>
          </view>
          <view class="stat">
            <text class="stat-label">{{ $t('space.grades.stats.passRate') }}</text>
            <text class="stat-value xm-num">{{ stats.passRate == null ? '—' : stats.passRate + '%' }}</text>
          </view>
          <view class="stat">
            <text class="stat-label">{{ $t('space.grades.stats.range') }}</text>
            <text class="stat-value xm-num">{{
              stats.highest == null ? '—' : stats.highest + ' / ' + stats.lowest
            }}</text>
          </view>
          <view class="stat">
            <text class="stat-label">{{ $t('space.grades.stats.progress') }}</text>
            <text class="stat-value xm-num">{{ stats.graded }} / {{ stats.students }}</text>
          </view>
        </view>
        <view class="dist">
          <view
            v-for="(count, i) in stats.distribution || []"
            :key="i"
            class="dist-col"
          >
            <view class="dist-wrap">
              <view
                class="dist-bar"
                :class="'dist-bar-' + i"
                :style="'height:' + barHeight(count) + '%'"
              />
            </view>
            <text class="dist-count xm-num">{{ count }}</text>
            <text class="dist-label">{{ $t('space.grades.buckets.' + BUCKETS[i]) }}</text>
          </view>
        </view>
      </view>

      <view class="toolbar">
        <xm-search
          v-model="keyword"
          :placeholder="$t('space.grades.search')"
        />
        <view
          v-if="dirtyCount || staleCount"
          class="dirty"
          >{{
            dirtyCount ? $t('space.grades.dirty', { n: dirtyCount }) : $t('space.grades.staleCount', { n: staleCount })
          }}</view
        >
        <view class="toolbar-actions">
          <button
            class="xm-btn xm-btn-plain xm-btn-sm"
            :disabled="(!dirtyCount && !staleCount) || !weightsOk || saving"
            :loading="saving"
            @click="save(false)"
          >
            {{ $t('space.grades.save') }}
          </button>
          <button
            v-if="stats.published > 0"
            class="xm-btn xm-btn-plain xm-btn-sm"
            :disabled="unpublishing"
            :loading="unpublishing"
            @click="unpublish"
          >
            {{ $t('space.grades.unpublish') }}
          </button>
          <button
            class="xm-btn xm-btn-primary xm-btn-sm"
            :disabled="!weightsOk || publishing"
            :loading="publishing"
            @click="publish"
          >
            {{ $t('space.grades.publish') }}
          </button>
        </view>
      </view>

      <xm-empty
        v-if="!rows.length"
        icon="users"
        :text="$t('space.grades.noMembers')"
      />
      <view
        v-for="row in visibleRows"
        :key="row.studentId"
        class="xm-card row"
      >
        <view class="row-head">
          <xm-user-avatar
            :name="row.name"
            :avatar="row.avatar"
            :size="64"
          />
          <view class="row-who">
            <view class="row-name">{{ row.name }}</view>
            <view class="row-sub xm-num">{{ row.username }}</view>
          </view>
          <view class="row-total">
            <text
              v-if="totalOf(row) != null"
              class="row-total-value xm-num"
              :class="{ 'is-fail': totalOf(row) < 60 }"
              >{{ totalOf(row) }}</text
            >
            <text
              v-else
              class="row-pending"
              >{{ $t('space.grades.pending') }}</text
            >
            <text
              class="xm-tag"
              :class="statusTag(row)"
              >{{ statusText(row) }}</text
            >
          </view>
        </view>
        <view class="fields">
          <view
            v-if="weights.attendance > 0"
            class="field"
          >
            <text class="field-label">{{ labelWithWeight('attendance') }}</text>
            <text class="field-auto xm-num">{{ row.attendanceScore == null ? '—' : row.attendanceScore }}</text>
          </view>
          <view
            v-if="weights.homework > 0"
            class="field"
          >
            <text class="field-label">{{ labelWithWeight('homework') }}</text>
            <text class="field-auto xm-num">{{ row.homeworkScore == null ? '—' : row.homeworkScore }}</text>
          </view>
          <view
            v-if="weights.ordinary > 0"
            class="field"
          >
            <text class="field-label">{{ labelWithWeight('ordinary') }}</text>
            <input
              class="field-input xm-num"
              :class="{ 'is-changed': changed(row, 'ordinary'), 'is-invalid': invalid(row.ordinaryText) }"
              :value="row.ordinaryText"
              type="digit"
              maxlength="5"
              @input="setText(row, 'ordinaryText', $event)"
            />
          </view>
          <view
            v-if="weights.exam > 0"
            class="field"
          >
            <text class="field-label">{{ labelWithWeight('exam') }}</text>
            <input
              class="field-input xm-num"
              :class="{ 'is-changed': changed(row, 'exam'), 'is-invalid': invalid(row.examText) }"
              :value="row.examText"
              type="digit"
              maxlength="5"
              @input="setText(row, 'examText', $event)"
            />
          </view>
        </view>
        <view
          v-if="weights.homework > 0 && row.homeworkScore == null"
          class="row-note"
          >{{ $t('space.grades.homeworkNone') }}</view
        >
        <view
          v-if="isStale(row)"
          class="row-note is-warn"
          >{{ $t('space.grades.stale', { n: row.publishedTotal }) }}</view
        >
      </view>
    </template>

    <!-- ==================== 学生：我的成绩 ==================== -->
    <view
      v-else-if="mine"
      class="xm-card mine"
    >
      <template v-if="mine.published">
        <view class="mine-label">{{ $t('space.grades.mine.total') }}</view>
        <view
          class="mine-total xm-num"
          :class="{ 'is-fail': !mine.passed }"
          >{{ mine.total }}</view
        >
        <view class="mine-tags">
          <text
            class="xm-tag"
            :class="mine.passed ? 'xm-tag-success' : 'xm-tag-danger'"
            >{{ mine.passed ? $t('space.grades.mine.passed') : $t('space.grades.mine.failed') }}</text
          >
          <text
            v-if="mine.passed && mine.credit"
            class="xm-tag xm-tag-brand"
            >{{ $t('space.grades.mine.creditEarned', { n: mine.credit }) }}</text
          >
          <text
            v-if="mine.gradePoint != null"
            class="xm-tag"
            >{{ $t('space.grades.mine.gradePoint', { n: mine.gradePoint }) }}</text
          >
        </view>
      </template>
      <template v-else>
        <view class="mine-waiting">{{ $t('space.grades.mine.notPublished') }}</view>
        <view
          v-if="visibleMyParts.length"
          class="mine-note"
          >{{ $t('space.grades.mine.livePoints') }}</view
        >
      </template>
      <view
        v-for="key in visibleMyParts"
        :key="key"
        class="part"
      >
        <text class="part-name">{{ $t('space.weights.' + key) }}</text>
        <text class="part-weight xm-num">{{ mine.weights[key] }}%</text>
        <text class="part-value xm-num">{{ myValue(key) == null ? '—' : myValue(key) }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, onUnmounted, reactive, ref, watch } from 'vue'
import { gradebookApi } from '@/api'
import { SILENT } from '@/utils/request'
import { matchesPerson, previewTotal, weightsValid } from '@/utils/courseSpace'
import { useCourseEvents } from '@/composables/useCourseEvents'
import { confirm } from '@/utils/confirm'
import { t } from '@/i18n'

const WEIGHT_KEYS = ['attendance', 'homework', 'ordinary', 'exam']
const BUCKETS = ['excellent', 'good', 'medium', 'pass', 'fail']

const props = defineProps({
  overview: { type: Object, required: true },
  courseId: { type: Number, required: true },
})

const emit = defineEmits(['refresh'])

const relation = computed(() => props.overview.relation || 'visitor')
const canTeach = computed(() => relation.value === 'teacher' || relation.value === 'admin')

const loading = ref(false)
const book = ref(null)
const mine = ref(null)

// ---------- 成绩册：rows 是可编辑的副本（平时 / 期末用文本框里的原样字符串），original 记录载入时的值 ----------
const rows = ref([])
const original = new Map()
const weights = reactive({ attendance: 0, homework: 0, ordinary: 30, exam: 70 })
const savedWeights = reactive({ attendance: 0, homework: 0, ordinary: 30, exam: 70 })
/** 权重输入框里的原样文字（清空时显示空，按 0 计） */
const weightText = reactive({ attendance: '0', homework: '0', ordinary: '30', exam: '70' })
const keyword = ref('')
const saving = ref(false)
const publishing = ref(false)
const unpublishing = ref(false)

const stats = computed(() => (book.value && book.value.stats) || { distribution: [0, 0, 0, 0, 0] })

const numText = (v) => (v == null ? '' : String(v))

/** 文本框 → 分数：空为 null；不是 0~100 的数时返回 NaN（标红、不让保存） */
const parseScore = (text) => {
  const s = String(text == null ? '' : text).trim()
  if (!s) return null
  const n = Number(s)
  if (!Number.isFinite(n) || n < 0 || n > 100) return NaN
  return Math.round(n * 10) / 10
}
const invalid = (text) => Number.isNaN(parseScore(text))

const load = async () => {
  loading.value = true
  try {
    if (canTeach.value) {
      const data = await gradebookApi.view(props.courseId, SILENT)
      book.value = data
      rows.value = (data.rows || []).map((r) => ({
        ...r,
        ordinaryText: numText(r.ordinaryScore),
        examText: numText(r.examScore),
      }))
      original.clear()
      for (const r of rows.value) original.set(r.studentId, { ordinary: r.ordinaryScore, exam: r.examScore })
      Object.assign(weights, data.weights)
      Object.assign(savedWeights, data.weights)
      for (const key of WEIGHT_KEYS) weightText[key] = numText(data.weights[key])
    } else {
      mine.value = await gradebookApi.mine(props.courseId, SILENT)
    }
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    loading.value = false
  }
}

watch([() => props.courseId, canTeach], load, { immediate: true })

const setText = (row, field, e) => {
  row[field] = String(e.detail.value || '')
}

const onWeightInput = (key, e) => {
  const text = String(e.detail.value || '').replace(/\D/g, '')
  weightText[key] = text
  weights[key] = text === '' ? 0 : Number(text)
}

const weightSum = computed(() => WEIGHT_KEYS.reduce((sum, key) => sum + (Number(weights[key]) || 0), 0))
const weightsOk = computed(() => weightsValid(weights))
const weightsChanged = computed(() => WEIGHT_KEYS.some((key) => weights[key] !== savedWeights[key]))

const valueOf = (row, field) => parseScore(field === 'ordinary' ? row.ordinaryText : row.examText)

const changed = (row, field) => {
  const before = (original.get(row.studentId) || {})[field]
  const now = valueOf(row, field)
  return (before == null ? null : before) !== now
}

const changedRows = computed(() => rows.value.filter((row) => changed(row, 'ordinary') || changed(row, 'exam')))
const dirtyCount = computed(() => changedRows.value.length + (weightsChanged.value ? 1 : 0))

const visibleRows = computed(() => rows.value.filter((row) => matchesPerson(row, keyword.value)))

const labelWithWeight = (key) => `${t('space.weights.' + key)} ${weights[key]}%`

/** 改分时即时预览总评（规则与后端一致，保存后以后端为准）；填错的分数不参与 */
const totalOf = (row) => {
  const ordinary = valueOf(row, 'ordinary')
  const exam = valueOf(row, 'exam')
  return previewTotal(weights, {
    attendance: row.attendanceScore,
    homework: row.homeworkScore,
    ordinary: Number.isNaN(ordinary) ? null : ordinary,
    exam: Number.isNaN(exam) ? null : exam,
  })
}

const isStale = (row) => row.status === '已发布' && row.publishedTotal !== totalOf(row)

/** 已发布、但按当前记录（考勤、作业变了）重算后对不上的行：没改分也允许保存，把新总评发给学生 */
const staleCount = computed(() => rows.value.filter((row) => isStale(row)).length)

const statusText = (row) => {
  if (row.status === '已发布') return t('space.grades.published')
  if (row.status === '草稿') return t('space.grades.draft')
  return t('space.grades.notEntered')
}
const statusTag = (row) => {
  if (row.status === '已发布') return 'xm-tag-success'
  if (row.status === '草稿') return 'xm-tag-warning'
  return ''
}

const barHeight = (count) => {
  const max = Math.max(1, ...(stats.value.distribution || [0]))
  return Math.round((count / max) * 100)
}

// 有没保存的修改时，离开页面（返回）先确认；微信小程序用 enableAlertBeforeUnload，其他端没有这个能力
watch(dirtyCount, (count) => {
  try {
    if (count > 0 && typeof uni.enableAlertBeforeUnload === 'function') {
      uni.enableAlertBeforeUnload({ message: t('space.grades.leaveConfirm'), fail: () => {} })
    } else if (count === 0 && typeof uni.disableAlertBeforeUnload === 'function') {
      uni.disableAlertBeforeUnload({ fail: () => {} })
    }
  } catch {
    // 平台不支持时忽略
  }
})
onUnmounted(() => {
  try {
    if (typeof uni.disableAlertBeforeUnload === 'function') uni.disableAlertBeforeUnload({ fail: () => {} })
  } catch {
    // 平台不支持时忽略
  }
})

// 学生端：老师发布 / 批改后后端会推消息，作业变动时也顺手刷新一下；老师有没保存的修改时不刷新，免得冲掉
useCourseEvents(
  () => props.courseId,
  ['assignments', 'attendance'],
  () => {
    if (!canTeach.value || !dirtyCount.value) load()
  },
)

/** 保存；返回是否成功（发布前先保存时用） */
const save = async (quiet) => {
  if (!weightsOk.value) return false
  if (changedRows.value.some((row) => invalid(row.ordinaryText) || invalid(row.examText))) {
    uni.showToast({ title: t('errors.5014'), icon: 'none' })
    return false
  }
  saving.value = true
  try {
    await gradebookApi.save(props.courseId, {
      weights: { ...weights },
      rows: changedRows.value.map((row) => ({
        studentId: row.studentId,
        ordinaryScore: valueOf(row, 'ordinary'),
        examScore: valueOf(row, 'exam'),
      })),
    })
    if (!quiet) uni.showToast({ title: t('space.grades.saved'), icon: 'success' })
    await load()
    emit('refresh')
    return true
  } catch {
    return false
  } finally {
    saving.value = false
  }
}

const publish = async () => {
  if (!(await confirm(t('space.grades.publishConfirm'), { title: t('space.grades.publish') }))) return
  if (dirtyCount.value && !(await save(true))) return
  publishing.value = true
  try {
    const result = await gradebookApi.publish(props.courseId)
    let message = t('space.grades.publishedMsg', { n: (result && result.count) || 0 })
    if (!result || !result.count) message = t('space.grades.nothingToPublish')
    else if (result.skipped) message = t('space.grades.publishedSkipped', { n: result.count, m: result.skipped })
    uni.showToast({ title: message, icon: 'none', duration: 2500 })
    await load()
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    publishing.value = false
  }
}

const unpublish = async () => {
  if (!(await confirm(t('space.grades.unpublishConfirm'), { title: t('space.grades.unpublish') }))) return
  unpublishing.value = true
  try {
    const result = await gradebookApi.unpublish(props.courseId)
    uni.showToast({ title: t('space.grades.unpublished', { n: (result && result.count) || 0 }), icon: 'none' })
    await load()
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    unpublishing.value = false
  }
}

// ---------- 学生 ----------
/** 发布了列出全部计分项；没发布只列按记录自动折算的考勤、作业（平时、期末要等老师发布） */
const visibleMyParts = computed(() =>
  WEIGHT_KEYS.filter(
    (key) =>
      (Number(mine.value && mine.value.weights && mine.value.weights[key]) || 0) > 0 &&
      (mine.value.published || key === 'attendance' || key === 'homework'),
  ),
)

const FIELD_OF = {
  attendance: 'attendanceScore',
  homework: 'homeworkScore',
  ordinary: 'ordinaryScore',
  exam: 'examScore',
}
const myValue = (key) => {
  const value = mine.value && mine.value[FIELD_OF[key]]
  return value == null ? null : value
}

/** 下拉刷新：老师有没保存的修改时不刷新，免得冲掉 */
const reload = () => {
  if (!canTeach.value || !dirtyCount.value) return load()
  return Promise.resolve()
}

defineExpose({ reload })
</script>

<style lang="scss" scoped>
.sum {
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.sum.is-bad {
  color: var(--xm-danger);
}

.weights {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx 24rpx;
}

.weight {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.weight-label {
  font-size: 26rpx;
  color: var(--xm-text);
}

.weight-field {
  display: flex;
  align-items: center;
  gap: 6rpx;
}

.weight-input {
  width: 110rpx;
  height: 64rpx;
  border: 1rpx solid var(--xm-border);
  border-radius: 12rpx;
  background: var(--xm-bg-input);
  font-size: 28rpx;
  text-align: center;
  color: var(--xm-text);
}

.weight-pct {
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.rule {
  margin-top: 18rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: var(--xm-text-3);
}

.stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.stat-label {
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.stat-value {
  font-size: 36rpx;
  font-weight: bold;
  color: var(--xm-text);
}

.dist {
  display: flex;
  gap: 16rpx;
  margin-top: 24rpx;
}

.dist-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
}

.dist-wrap {
  display: flex;
  align-items: flex-end;
  width: 100%;
  height: 140rpx;
  border-radius: 10rpx;
  background: var(--xm-bg-sunken);
  overflow: hidden;
}

.dist-bar {
  width: 100%;
  min-height: 4rpx;
  border-radius: 10rpx 10rpx 0 0;
  background: var(--xm-brand);
}

.dist-bar-3 {
  background: var(--xm-warning);
}

.dist-bar-4 {
  background: var(--xm-danger);
}

.dist-count {
  font-size: 24rpx;
  font-weight: 600;
  color: var(--xm-text);
}

.dist-label {
  font-size: 20rpx;
  color: var(--xm-text-2);
}

.toolbar {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
  margin-bottom: 20rpx;
}

.dirty {
  font-size: 24rpx;
  color: var(--xm-warning);
}

.toolbar-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12rpx;
}

.row-head {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.row-who {
  flex: 1;
  min-width: 0;
}

.row-name {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--xm-text);
}

.row-sub {
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.row-total {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6rpx;
}

.row-total-value {
  font-size: 40rpx;
  font-weight: bold;
  line-height: 1;
  color: var(--xm-text);
}

.row-total-value.is-fail {
  color: var(--xm-danger);
}

.row-pending {
  font-size: 24rpx;
  color: var(--xm-text-3);
}

.fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14rpx 20rpx;
  margin-top: 18rpx;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.field-label {
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.field-auto {
  height: 64rpx;
  line-height: 64rpx;
  font-size: 28rpx;
  color: var(--xm-text-2);
}

.field-input {
  height: 64rpx;
  padding: 0 16rpx;
  border: 1rpx solid var(--xm-border);
  border-radius: 12rpx;
  background: var(--xm-bg-input);
  font-size: 28rpx;
  color: var(--xm-text);
}

.field-input.is-changed {
  border-color: var(--xm-warning);
  background: var(--xm-warning-soft);
}

.field-input.is-invalid {
  border-color: var(--xm-danger);
  background: var(--xm-danger-soft);
}

.row-note {
  margin-top: 12rpx;
  font-size: 22rpx;
  color: var(--xm-text-3);
}

.row-note.is-warn {
  color: var(--xm-warning);
}

.mine {
  text-align: center;
}

.mine-label {
  font-size: 26rpx;
  color: var(--xm-text-2);
}

.mine-total {
  margin: 8rpx 0 16rpx;
  font-size: 96rpx;
  font-weight: bold;
  line-height: 1.1;
  color: var(--xm-brand);
}

.mine-total.is-fail {
  color: var(--xm-danger);
}

.mine-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.mine-waiting {
  padding: 16rpx 0;
  font-size: 28rpx;
  color: var(--xm-text-2);
}

.mine-note {
  margin-bottom: 12rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: var(--xm-text-3);
}

.part {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 18rpx 0;
  border-top: 1rpx solid var(--xm-border);
  text-align: left;
}

.part-name {
  flex: 1;
  font-size: 28rpx;
  color: var(--xm-text);
}

.part-weight {
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.part-value {
  min-width: 80rpx;
  font-size: 32rpx;
  font-weight: bold;
  text-align: right;
  color: var(--xm-text);
}
</style>

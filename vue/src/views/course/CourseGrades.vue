<template>
  <div class="gb">
    <div v-if="loading && !book && !mine" class="panel panel--pad">
      <el-skeleton animated :rows="6" />
    </div>

    <!-- ==================== 老师 / 管理员：成绩册 ==================== -->
    <template v-else-if="canTeach && book">
      <div class="gb-top">
        <section class="panel">
          <div class="panel__head">
            <span class="panel__title">{{ $t('space.grades.weights') }}</span>
            <span class="panel__hint" :class="{ 'is-bad': !weightsOk }">
              {{ weightsOk ? $t('space.grades.sum', { n: weightSum }) : $t('space.grades.sumBad', { n: weightSum }) }}
            </span>
          </div>
          <div class="panel__body">
            <div class="weights">
              <label v-for="key in WEIGHT_KEYS" :key="key" class="weights__item">
                <span class="weights__label">{{ $t('space.weights.' + key) }}</span>
                <el-input-number
                  v-model="weights[key]"
                  :min="0"
                  :max="100"
                  :step="5"
                  :precision="0"
                  :value-on-clear="0"
                  size="small"
                  controls-position="right"
                />
                <span class="weights__pct">%</span>
              </label>
            </div>
            <p class="gb-rule">{{ $t('space.grades.rule') }}</p>
          </div>
        </section>

        <section class="panel gb-stats">
          <div class="gb-stats__grid">
            <div>
              <div class="gb-stats__label">{{ $t('space.grades.stats.average') }}</div>
              <div class="gb-stats__value num">{{ stats.average ?? '—' }}</div>
            </div>
            <div>
              <div class="gb-stats__label">{{ $t('space.grades.stats.passRate') }}</div>
              <div class="gb-stats__value num">{{ stats.passRate != null ? stats.passRate + '%' : '—' }}</div>
            </div>
            <div>
              <div class="gb-stats__label">{{ $t('space.grades.stats.range') }}</div>
              <div class="gb-stats__value num">
                {{ stats.highest != null ? `${stats.highest} / ${stats.lowest}` : '—' }}
              </div>
            </div>
            <div>
              <div class="gb-stats__label">{{ $t('space.grades.stats.progress') }}</div>
              <div class="gb-stats__value num">{{ stats.graded }}<small> / {{ stats.students }}</small></div>
            </div>
          </div>
          <div class="dist" role="img" :aria-label="distributionLabel">
            <div v-for="(count, i) in stats.distribution" :key="i" class="dist__col">
              <div class="dist__bar-wrap">
                <div class="dist__bar" :class="'dist__bar--' + i" :style="{ height: barHeight(count) + '%' }"></div>
              </div>
              <div class="dist__count num">{{ count }}</div>
              <div class="dist__label">{{ $t('space.grades.buckets.' + BUCKETS[i]) }}</div>
            </div>
          </div>
        </section>
      </div>

      <section class="panel">
        <div class="gb-toolbar">
          <el-input
            v-model="keyword"
            :placeholder="$t('space.grades.search')"
            clearable
            size="default"
            class="gb-toolbar__search"
          />
          <span v-if="dirtyCount" class="gb-toolbar__dirty">{{ $t('space.grades.dirty', { n: dirtyCount }) }}</span>
          <span v-else-if="staleCount" class="gb-toolbar__dirty">{{ $t('space.grades.staleCount', { n: staleCount }) }}</span>
          <div class="gb-toolbar__actions">
            <el-button :disabled="(!dirtyCount && !staleCount) || !weightsOk" :loading="saving" @click="save()">
              {{ $t('space.grades.save') }}
            </el-button>
            <el-button v-if="stats.published > 0" plain :loading="unpublishing" @click="unpublish">
              {{ $t('space.grades.unpublish') }}
            </el-button>
            <el-button type="primary" :disabled="!weightsOk" :loading="publishing" @click="publish">
              {{ $t('space.grades.publish') }}
            </el-button>
          </div>
        </div>

        <div v-if="!rows.length" class="empty-note">{{ $t('space.grades.noMembers') }}</div>
        <el-table v-else :data="visibleRows" row-key="studentId" class="gb-table" size="default">
          <el-table-column :label="$t('space.grades.student')" min-width="170" fixed>
            <template #default="{ row }">
              <div class="gb-who">
                <UserAvatar :name="row.name" :avatar="row.avatar" :size="28" />
                <div class="gb-who__text">
                  <div class="gb-who__name">{{ row.name }}</div>
                  <div class="gb-who__sub num">{{ row.username }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column v-if="weights.attendance > 0" :label="labelWithWeight('attendance')" width="92" align="center">
            <template #default="{ row }">
              <span class="num gb-auto">{{ row.attendanceScore ?? '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column v-if="weights.homework > 0" :label="labelWithWeight('homework')" width="92" align="center">
            <template #default="{ row }">
              <span v-if="row.homeworkScore != null" class="num gb-auto">{{ row.homeworkScore }}</span>
              <el-tooltip v-else :content="$t('space.grades.homeworkNone')" placement="top">
                <span class="gb-auto gb-auto--none" tabindex="0">—</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column v-if="weights.ordinary > 0" :label="labelWithWeight('ordinary')" width="124" align="center">
            <template #default="{ row }">
              <el-input-number
                v-model="row.ordinaryScore"
                :min="0"
                :max="100"
                :precision="1"
                :controls="false"
                :value-on-clear="null"
                size="small"
                class="gb-input"
                :class="{ 'is-changed': changed(row, 'ordinaryScore') }"
                :aria-label="`${row.name} ${$t('space.weights.ordinary')}`"
              />
            </template>
          </el-table-column>
          <el-table-column v-if="weights.exam > 0" :label="labelWithWeight('exam')" width="124" align="center">
            <template #default="{ row }">
              <el-input-number
                v-model="row.examScore"
                :min="0"
                :max="100"
                :precision="1"
                :controls="false"
                :value-on-clear="null"
                size="small"
                class="gb-input"
                :class="{ 'is-changed': changed(row, 'examScore') }"
                :aria-label="`${row.name} ${$t('space.weights.exam')}`"
              />
            </template>
          </el-table-column>
          <el-table-column :label="$t('space.grades.total')" width="96" align="center">
            <template #default="{ row }">
              <span v-if="totalOf(row) != null" class="num gb-total" :class="{ 'is-fail': (totalOf(row) as number) < 60 }">
                {{ totalOf(row) }}
              </span>
              <span v-else class="gb-pending">{{ $t('space.grades.pending') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('space.grades.status')" min-width="120">
            <template #default="{ row }">
              <span class="pill" :class="statusPill(row)">{{ statusText(row) }}</span>
              <el-tooltip
                v-if="isStale(row)"
                :content="$t('space.grades.stale', { n: row.publishedTotal })"
                placement="top"
              >
                <span class="gb-stale" tabindex="0" aria-hidden="false">!</span>
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </template>

    <!-- ==================== 学生：我的成绩 ==================== -->
    <template v-else-if="mine">
      <section class="panel my-grade">
        <template v-if="mine.published">
          <div class="my-grade__label">{{ $t('space.grades.mine.total') }}</div>
          <div class="my-grade__total num" :class="{ 'is-fail': !mine.passed }">{{ mine.total }}</div>
          <div class="my-grade__tags">
            <span class="pill" :class="mine.passed ? 'pill--ok' : 'pill--bad'">
              {{ mine.passed ? $t('space.grades.mine.passed') : $t('space.grades.mine.failed') }}
            </span>
            <span v-if="mine.passed && mine.credit" class="pill pill--brand">{{ $t('space.grades.mine.creditEarned', { n: mine.credit }) }}</span>
            <span v-if="mine.gradePoint != null" class="pill">{{ $t('space.grades.mine.gradePoint', { n: mine.gradePoint }) }}</span>
          </div>
        </template>
        <template v-else>
          <div class="my-grade__waiting">{{ $t('space.grades.mine.notPublished') }}</div>
          <p v-if="visibleMyParts.length" class="my-grade__note">{{ $t('space.grades.mine.livePoints') }}</p>
        </template>

        <ul v-if="visibleMyParts.length" class="my-parts">
          <li v-for="key in visibleMyParts" :key="key" class="my-parts__item">
            <span class="my-parts__name">{{ $t('space.weights.' + key) }}</span>
            <span class="my-parts__weight num">{{ mine.weights[key] }}%</span>
            <span class="my-parts__value num">{{ myValue(key) ?? '—' }}</span>
          </li>
        </ul>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute } from 'vue-router'
import UserAvatar from '@/components/UserAvatar.vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from '@/utils/element-plus'
import { matchesPerson, previewTotal, weightsValid } from '@/utils/courseSpace'
import { useCourseEvents } from '@/composables/useCourseEvents'
import { t } from '@/i18n'

type WeightKey = 'attendance' | 'homework' | 'ordinary' | 'exam'

interface GradeRow {
  studentId: number
  username: string
  name: string
  avatar?: string
  attendanceScore: number | null
  homeworkScore: number | null
  ordinaryScore: number | null
  examScore: number | null
  total: number | null
  status: string | null
  publishedTotal: number | null
}

/** el-table 插槽里的行类型是宽松的 Record，辅助函数按它收参 */
type TableRow = Record<string, any>

const WEIGHT_KEYS: WeightKey[] = ['attendance', 'homework', 'ordinary', 'exam']
const BUCKETS = ['excellent', 'good', 'medium', 'pass', 'fail']

const props = defineProps<{ overview: Record<string, any> }>()
const emit = defineEmits<{ refresh: [] }>()

const route = useRoute()
const courseId = computed(() => Number(route.params.id))
const relation = computed<string>(() => props.overview.relation || 'visitor')
const canTeach = computed(() => relation.value === 'teacher' || relation.value === 'admin')

const loading = ref(false)
const book = ref<Record<string, any> | null>(null)
const mine = ref<Record<string, any> | null>(null)

// ---------- 成绩册状态：rows 是可编辑的副本，original 记录载入时的值，用来判断改动 ----------
const rows = ref<GradeRow[]>([])
const original = new Map<number, { ordinaryScore: number | null; examScore: number | null }>()
const weights = reactive<Record<WeightKey, number>>({ attendance: 0, homework: 0, ordinary: 30, exam: 70 })
const savedWeights = reactive<Record<WeightKey, number>>({ attendance: 0, homework: 0, ordinary: 30, exam: 70 })
const keyword = ref('')
const saving = ref(false)
const publishing = ref(false)
const unpublishing = ref(false)

const stats = computed<Record<string, any>>(() => book.value?.stats || { distribution: [0, 0, 0, 0, 0] })

const load = async () => {
  loading.value = true
  try {
    if (canTeach.value) {
      const data = await request.get<Record<string, any>>(`/course/${courseId.value}/gradebook`)
      book.value = data
      rows.value = (data.rows || []).map((r: GradeRow) => ({ ...r }))
      original.clear()
      for (const r of rows.value) original.set(r.studentId, { ordinaryScore: r.ordinaryScore, examScore: r.examScore })
      Object.assign(weights, data.weights)
      Object.assign(savedWeights, data.weights)
    } else {
      mine.value = await request.get<Record<string, any>>(`/course/${courseId.value}/gradebook/mine`)
    }
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    loading.value = false
  }
}

watch([courseId, canTeach], load, { immediate: true })

// 学生端：老师发布 / 批改后后端会推消息，作业变动时也顺手刷新一下
useCourseEvents(() => courseId.value, ['assignments', 'attendance'], () => {
  if (!canTeach.value || !dirtyCount.value) load()
})

const weightSum = computed(() => WEIGHT_KEYS.reduce((sum, key) => sum + (Number(weights[key]) || 0), 0))
const weightsOk = computed(() => weightsValid(weights))
const weightsChanged = computed(() => WEIGHT_KEYS.some((key) => weights[key] !== savedWeights[key]))

const changed = (row: TableRow, field: 'ordinaryScore' | 'examScore') =>
  (original.get(row.studentId)?.[field] ?? null) !== (row[field] ?? null)

const changedRows = computed(() => rows.value.filter((row) => changed(row, 'ordinaryScore') || changed(row, 'examScore')))
const dirtyCount = computed(() => changedRows.value.length + (weightsChanged.value ? 1 : 0))

const visibleRows = computed(() => rows.value.filter((row) => matchesPerson(row, keyword.value)))

const labelWithWeight = (key: WeightKey) => `${t('space.weights.' + key)} ${weights[key]}%`

/** 改分时即时预览总评（规则与后端一致，保存后以后端为准） */
const totalOf = (row: TableRow) =>
  previewTotal(weights, {
    attendance: row.attendanceScore,
    homework: row.homeworkScore,
    ordinary: row.ordinaryScore,
    exam: row.examScore,
  })

const isStale = (row: TableRow) => row.status === '已发布' && row.publishedTotal !== totalOf(row)

/** 已发布、但按当前记录（考勤、作业变了）重算后对不上的行：没改分也允许保存，把新总评发给学生 */
const staleCount = computed(() => rows.value.filter((row) => isStale(row)).length)

const statusText = (row: TableRow) => {
  if (row.status === '已发布') return t('space.grades.published')
  if (row.status === '草稿') return t('space.grades.draft')
  return t('space.grades.notEntered')
}

const statusPill = (row: TableRow) => {
  if (row.status === '已发布') return 'pill--ok'
  if (row.status === '草稿') return 'pill--warn'
  return ''
}

const barHeight = (count: number) => {
  const max = Math.max(1, ...((stats.value.distribution as number[]) || [0]))
  return Math.round((count / max) * 100)
}

const distributionLabel = computed(() =>
  BUCKETS.map((b, i) => `${t('space.grades.buckets.' + b)} ${(stats.value.distribution || [])[i] ?? 0}`).join('，'),
)

/** 保存；返回是否成功（发布前先保存时用） */
const save = async (quiet = false): Promise<boolean> => {
  if (!weightsOk.value) return false
  saving.value = true
  try {
    await request.put(`/course/${courseId.value}/gradebook`, {
      weights: { ...weights },
      rows: changedRows.value.map((row) => ({
        studentId: row.studentId,
        ordinaryScore: row.ordinaryScore,
        examScore: row.examScore,
      })),
    })
    if (!quiet) ElMessage.success(t('space.grades.saved'))
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
  try {
    await ElMessageBox.confirm(t('space.grades.publishConfirm'), t('space.grades.publish'), { type: 'warning' })
  } catch {
    return
  }
  if (dirtyCount.value && !(await save(true))) return
  publishing.value = true
  try {
    const result = await request.post<{ count: number; skipped: number }>(`/course/${courseId.value}/gradebook/publish`)
    if (!result?.count) ElMessage.info(t('space.grades.nothingToPublish'))
    else if (result.skipped) ElMessage.success(t('space.grades.publishedSkipped', { n: result.count, m: result.skipped }))
    else ElMessage.success(t('space.grades.publishedMsg', { n: result.count }))
    await load()
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    publishing.value = false
  }
}

const unpublish = async () => {
  try {
    await ElMessageBox.confirm(t('space.grades.unpublishConfirm'), t('space.grades.unpublish'), { type: 'warning' })
  } catch {
    return
  }
  unpublishing.value = true
  try {
    const result = await request.post<{ count: number }>(`/course/${courseId.value}/gradebook/unpublish`)
    ElMessage.success(t('space.grades.unpublished', { n: result?.count ?? 0 }))
    await load()
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    unpublishing.value = false
  }
}

onBeforeRouteLeave(async () => {
  if (!canTeach.value || !dirtyCount.value) return true
  try {
    await ElMessageBox.confirm(t('space.grades.leaveConfirm'), t('space.grades.save'), { type: 'warning' })
    return true
  } catch {
    return false
  }
})

// ---------- 学生 ----------
/** 发布了列出全部计分项；没发布只列按记录自动折算的考勤、作业（平时、期末要等老师发布） */
const visibleMyParts = computed(() =>
  WEIGHT_KEYS.filter(
    (key) =>
      (Number(mine.value?.weights?.[key]) || 0) > 0 &&
      (mine.value?.published || key === 'attendance' || key === 'homework'),
  ),
)

const myValue = (key: WeightKey) => {
  const field = { attendance: 'attendanceScore', homework: 'homeworkScore', ordinary: 'ordinaryScore', exam: 'examScore' }[key]
  return mine.value?.[field] ?? null
}
</script>

<style scoped>
.gb {
  display: grid;
  /* 列宽可以缩到 0：里面的宽表格自己横向滚动，不把整页撑出屏幕 */
  grid-template-columns: minmax(0, 1fr);
  gap: 20px;
}

.gb-top {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 20px;
}

.panel__hint.is-bad {
  color: var(--xm-bad);
  font-weight: 600;
}

/* ---------- 权重 ---------- */
.weights {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 20px;
}

.weights__item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.weights__label {
  width: 72px;
  font-size: 13px;
  color: var(--xm-text-regular);
}

.weights__pct {
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.gb-rule {
  margin-top: 14px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--xm-text-secondary);
}

/* ---------- 统计 ---------- */
.gb-stats {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 190px;
  gap: 16px;
  padding: 18px 20px;
}

.gb-stats__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px 16px;
  align-content: center;
}

.gb-stats__label {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.gb-stats__value {
  margin-top: 2px;
  font-size: 22px;
  font-weight: 700;
  color: var(--xm-text-primary);
}

.gb-stats__value small {
  font-size: 13px;
  font-weight: 500;
  color: var(--xm-text-secondary);
}

.dist {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 6px;
  align-items: end;
}

.dist__col {
  display: grid;
  justify-items: center;
  gap: 3px;
}

.dist__bar-wrap {
  display: flex;
  align-items: flex-end;
  width: 100%;
  height: 84px;
  border-radius: 4px;
  background: var(--xm-bg-sunken);
}

.dist__bar {
  width: 100%;
  min-height: 2px;
  border-radius: 4px;
  background: var(--xm-brand);
}

.dist__bar--3 {
  background: var(--xm-warn);
}

.dist__bar--4 {
  background: var(--xm-bad);
}

.dist__count {
  font-size: 12px;
  font-weight: 600;
  color: var(--xm-text-primary);
}

.dist__label {
  font-size: 11px;
  color: var(--xm-text-secondary);
  white-space: nowrap;
}

/* ---------- 工具条与表格 ---------- */
.gb-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 14px 20px;
  border-bottom: 1px solid var(--xm-border);
}

.gb-toolbar__search {
  width: 220px;
}

.gb-toolbar__dirty {
  font-size: 13px;
  color: var(--xm-warn);
}

.gb-toolbar__actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.gb-table {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: transparent;
}

.gb-who {
  display: flex;
  align-items: center;
  gap: 10px;
}

.gb-who__text {
  min-width: 0;
}

.gb-who__name {
  font-size: 14px;
  color: var(--xm-text-primary);
}

.gb-who__sub {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.gb-auto {
  color: var(--xm-text-regular);
}

.gb-auto--none {
  color: var(--xm-text-secondary);
  cursor: help;
}

.gb-input {
  width: 92px;
}

.gb-input.is-changed :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--xm-warn) inset;
  background: var(--xm-warn-soft);
}

.gb-total {
  font-size: 15px;
  font-weight: 700;
  color: var(--xm-text-primary);
}

.gb-total.is-fail {
  color: var(--xm-bad);
}

.gb-pending {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.gb-stale {
  display: inline-grid;
  place-items: center;
  width: 18px;
  height: 18px;
  margin-left: 6px;
  border-radius: 50%;
  background: var(--xm-warn-soft);
  color: var(--xm-warn);
  font-size: 12px;
  font-weight: 700;
  cursor: help;
}

/* ---------- 学生 ---------- */
.my-grade {
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 32px 24px 24px;
  text-align: center;
}

.my-grade__label {
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.my-grade__total {
  font-size: 64px;
  font-weight: 700;
  line-height: 1;
  color: var(--xm-brand);
}

.my-grade__total.is-fail {
  color: var(--xm-bad);
}

.my-grade__tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: center;
}

.my-grade__waiting {
  font-size: 18px;
  font-weight: 600;
  color: var(--xm-text-primary);
}

.my-grade__note {
  max-width: 420px;
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.my-parts {
  list-style: none;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 10px;
  width: 100%;
  max-width: 640px;
  margin-top: 12px;
}

.my-parts__item {
  display: grid;
  gap: 2px;
  padding: 12px;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius);
}

.my-parts__name {
  font-size: 13px;
  color: var(--xm-text-regular);
}

.my-parts__weight {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.my-parts__value {
  font-size: 22px;
  font-weight: 700;
  color: var(--xm-text-primary);
}

@media (max-width: 1080px) {
  .gb-top {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 560px) {
  .weights {
    grid-template-columns: minmax(0, 1fr);
  }

  .gb-stats {
    grid-template-columns: minmax(0, 1fr);
  }

  .gb-toolbar__search {
    width: 100%;
  }

  .gb-toolbar__actions {
    margin-left: 0;
  }
}
</style>

<template>
  <div class="as">
    <div v-if="canTeach" class="as-toolbar">
      <el-button type="primary" @click="openCreate">{{ $t('space.assignments.create') }}</el-button>
    </div>

    <div v-if="!data && loading" class="panel panel--pad">
      <el-skeleton animated :rows="4" />
    </div>

    <template v-else-if="data">
      <div v-if="!data.assignments.length" class="panel empty-block">
        <div class="empty-block__title">{{ canTeach ? $t('space.assignments.empty') : $t('space.assignments.emptyStudent') }}</div>
      </div>

      <ul v-else class="as-list">
        <li v-for="a in data.assignments" :key="a.id">
          <button type="button" class="as-item panel" @click="open(a.id)">
            <span class="as-item__main">
              <span class="as-item__title">{{ a.title }}</span>
              <span class="as-item__meta">
                <span :class="{ 'is-closed': a.closed }">
                  {{ a.closed ? $t('space.assignments.closed') + ' · ' + a.deadline : $t('space.assignments.due', { time: a.deadline }) }}
                </span>
                <span class="num">{{ $t('space.assignments.fullScoreOf', { n: a.fullScore }) }}</span>
                <span v-if="a.attachment">{{ $t('space.assignments.attachment') }}</span>
              </span>
            </span>

            <span v-if="canTeach" class="as-item__progress">
              <span class="as-item__counts num">
                {{ $t('space.assignments.submittedCount', { n: a.submitted, total: a.students }) }}
                · {{ $t('space.assignments.gradedCount', { n: a.graded }) }}
              </span>
              <span class="as-item__bar" aria-hidden="true">
                <span class="as-item__bar-graded" :style="{ width: percent(a.graded, a.students) + '%' }"></span>
                <span class="as-item__bar-submitted" :style="{ width: percent(a.submitted - a.graded, a.students) + '%' }"></span>
              </span>
            </span>
            <span v-else class="as-item__status">
              <span v-if="a.myScore != null" class="as-item__score num">{{ a.myScore }}<small>/{{ a.fullScore }}</small></span>
              <span class="pill" :class="statusMeta(a.myStatus).pill">{{ $t('space.assignments.status.' + statusMeta(a.myStatus).key) }}</span>
            </span>
          </button>
        </li>
      </ul>

      <section v-if="canTeach && data.looseCount > 0" class="panel loose">
        <div class="panel__head">
          <span class="panel__title">{{ $t('space.assignments.loose') }}</span>
          <el-button link type="primary" @click="toggleLoose">{{ $t('space.assignments.looseView') }}</el-button>
        </div>
        <div class="panel__body">
          <p class="loose__hint">{{ $t('space.assignments.looseHint', { n: data.looseCount }) }}</p>
          <ul v-if="looseOpen" class="loose__list">
            <li v-for="h in loose" :key="h.id" class="loose__item">
              <div class="loose__who">
                <span class="loose__name">{{ h.studentName || '—' }}</span>
                <span v-if="h.content" class="loose__content">{{ h.content }}</span>
                <FileChip v-if="h.file" :url="h.file" :name="h.fileName || h.content" />
              </div>
              <div class="loose__grade">
                <el-input-number v-model="looseScores[h.id]" :min="0" :max="100" :precision="1" size="small" controls-position="right" />
                <el-button size="small" type="primary" plain :disabled="looseScores[h.id] == null" @click="gradeLoose(h)">
                  {{ h.score ? $t('space.assignments.regrade') : $t('space.assignments.grade') }}
                </el-button>
              </div>
            </li>
          </ul>
        </div>
      </section>
    </template>

    <el-drawer v-model="drawerOpen" :title="drawerTitle" :size="drawerSize" destroy-on-close @closed="onDrawerClosed">
      <AssignmentDetail
        v-if="openAssignmentId"
        ref="detailRef"
        :course-id="courseId"
        :assignment-id="openAssignmentId"
        :can-teach="canTeach"
        @edit="openEdit"
        @delete="remove"
        @changed="onDetailChanged"
      />
    </el-drawer>

    <AssignmentForm v-model="formOpen" :course-id="courseId" :editing="editing" @saved="onSaved" />
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import FileChip from '@/components/FileChip.vue'
import AssignmentDetail from './AssignmentDetail.vue'
import AssignmentForm from './AssignmentForm.vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from '@/utils/element-plus'
import { ASSIGNMENT_STATUSES } from '@/utils/courseSpace'
import { useCourseEvents } from '@/composables/useCourseEvents'
import { t } from '@/i18n'

const props = defineProps<{ overview: Record<string, any> }>()
const emit = defineEmits<{ refresh: [] }>()

const route = useRoute()
const router = useRouter()
const courseId = computed(() => Number(route.params.id))
const relation = computed<string>(() => props.overview.relation || 'visitor')
const canTeach = computed(() => relation.value === 'teacher' || relation.value === 'admin')

const data = ref<Record<string, any> | null>(null)
const loading = ref(false)

const statusMeta = (status: string) => ASSIGNMENT_STATUSES[status] || { key: 'pending', pill: '' }
const percent = (n: number, total: number) => (total ? Math.max(0, Math.min(100, (n / total) * 100)) : 0)

const load = async () => {
  loading.value = true
  try {
    data.value = await request.get<Record<string, any>>(`/course/${courseId.value}/assignments`)
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    loading.value = false
  }
}

// ---------- 详情抽屉（?open=ID 可直接打开，消息里的链接就是这样） ----------
const drawerOpen = ref(false)
const openAssignmentId = ref<number | null>(null)
const detailRef = ref<InstanceType<typeof AssignmentDetail>>()

const drawerTitle = computed(() => {
  const a = data.value?.assignments?.find((x: Record<string, any>) => x.id === openAssignmentId.value)
  return a?.title || ''
})

/** 手机上抽屉铺满屏幕，桌面固定宽度 */
const drawerSize = ref('640px')

const open = (id: number) => {
  drawerSize.value = window.innerWidth < 720 ? '100%' : '640px'
  openAssignmentId.value = id
  drawerOpen.value = true
  if (Number(route.query.open) !== id) {
    router.replace({ query: { ...route.query, open: String(id) } })
  }
}

const onDrawerClosed = () => {
  openAssignmentId.value = null
  if (route.query.open) {
    const query = { ...route.query }
    delete query.open
    router.replace({ query })
  }
}

watch(
  courseId,
  async () => {
    await load()
    const id = Number(route.query.open)
    if (id && data.value?.assignments?.some((a: Record<string, any>) => a.id === id)) open(id)
  },
  { immediate: true },
)

const onDetailChanged = () => {
  load()
  emit('refresh')
}

useCourseEvents(() => courseId.value, ['assignments'], () => {
  load()
  detailRef.value?.reload()
})

// ---------- 布置 / 编辑 / 删除 ----------
const formOpen = ref(false)
const editing = ref<Record<string, any> | null>(null)

const openCreate = () => {
  editing.value = null
  formOpen.value = true
}

const openEdit = (assignment: Record<string, any>) => {
  editing.value = assignment
  formOpen.value = true
}

const onSaved = async (saved: Record<string, any>, created: boolean) => {
  await load()
  emit('refresh')
  if (created) open(saved.id)
  else detailRef.value?.reload()
}

const remove = async (assignment: Record<string, any>) => {
  try {
    await ElMessageBox.confirm(
      t('space.assignments.deleteConfirm', { title: assignment.title }),
      t('common.confirmDeleteTitle'),
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await request.delete(`/course/${courseId.value}/assignments/${assignment.id}`)
    ElMessage.success(t('space.assignments.deleted'))
    drawerOpen.value = false
    await load()
    emit('refresh')
  } catch {
    // 错误提示已由拦截器统一处理
  }
}

// ---------- 旧版自由提交 ----------
const looseOpen = ref(false)
const loose = ref<Record<string, any>[]>([])
const looseScores = reactive<Record<number, number | null>>({})

const loadLoose = async () => {
  try {
    loose.value = (await request.get<Record<string, any>[]>(`/course/${courseId.value}/assignments/loose`)) || []
    for (const h of loose.value) {
      const current = Number.parseFloat(h.score)
      looseScores[h.id] = Number.isFinite(current) ? current : null
    }
  } catch {
    loose.value = []
  }
}

const toggleLoose = async () => {
  looseOpen.value = !looseOpen.value
  if (looseOpen.value) await loadLoose()
}

const gradeLoose = async (h: Record<string, any>) => {
  try {
    const saved = await request.put<Record<string, any>>(`/course/${courseId.value}/assignments/submissions/${h.id}`, {
      score: looseScores[h.id],
      descr: h.descr || '',
    })
    h.score = saved.score
    ElMessage.success(t('space.assignments.gradeSaved'))
    emit('refresh')
  } catch {
    // 错误提示已由拦截器统一处理
  }
}
</script>

<style scoped>
.as {
  display: grid;
  /* 列宽可以缩到 0：里面的宽表格自己横向滚动，不把整页撑出屏幕 */
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
}

.as-toolbar {
  display: flex;
  justify-content: flex-end;
}

.empty-block {
  padding: 48px 20px;
  text-align: center;
}

.empty-block__title {
  font-size: 14px;
  color: var(--xm-text-secondary);
}

.as-list {
  list-style: none;
  display: grid;
  gap: 10px;
}

.as-item {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 16px 20px;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.as-item:hover {
  border-color: var(--xm-brand);
  box-shadow: var(--xm-shadow-card);
}

.as-item__main {
  display: grid;
  gap: 4px;
  flex: 1;
  min-width: 0;
}

.as-item__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--xm-text-primary);
  overflow-wrap: anywhere;
}

.as-item__meta {
  display: flex;
  gap: 4px 14px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.as-item__meta .is-closed {
  color: var(--xm-bad);
}

.as-item__progress {
  display: grid;
  gap: 6px;
  justify-items: end;
  width: 200px;
  flex-shrink: 0;
}

.as-item__counts {
  font-size: 12px;
  color: var(--xm-text-secondary);
  white-space: nowrap;
}

.as-item__bar {
  display: flex;
  width: 100%;
  height: 6px;
  border-radius: 999px;
  background: var(--xm-bg-sunken);
  overflow: hidden;
}

.as-item__bar-graded {
  background: var(--xm-ok);
}

.as-item__bar-submitted {
  background: var(--xm-info);
}

.as-item__status {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.as-item__score {
  font-size: 18px;
  font-weight: 700;
  color: var(--xm-brand);
}

.as-item__score small {
  font-size: 12px;
  font-weight: 500;
  color: var(--xm-text-secondary);
}

/* ---------- 旧版提交 ---------- */
.loose__hint {
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.loose__list {
  list-style: none;
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.loose__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 10px 12px;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius);
}

.loose__who {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  min-width: 0;
}

.loose__name {
  font-weight: 600;
  color: var(--xm-text-primary);
}

.loose__content {
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.loose__grade {
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 640px) {
  .as-item {
    flex-wrap: wrap;
  }

  .as-item__progress {
    width: 100%;
    justify-items: start;
  }
}
</style>

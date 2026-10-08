<template>
  <div class="ca">
    <div class="page-head">
      <div>
        <h1 class="page-title">{{ $t('admin.courses.title') }}</h1>
        <p class="page-sub">{{ $t('admin.courses.sub') }}</p>
      </div>
      <el-button type="primary" @click="openWizard(null)">{{ $t('admin.courses.create') }}</el-button>
    </div>

    <div class="ca-filters">
      <div class="chips" role="tablist">
        <button
          v-for="f in STATUS_FILTERS"
          :key="f.value"
          type="button"
          role="tab"
          class="chip"
          :class="{ 'is-on': status === f.value }"
          :aria-selected="status === f.value"
          @click="status = f.value"
        >
          {{ $t(f.label) }}<span class="chip__count num">{{ countOf(f.value) }}</span>
        </button>
      </div>
      <el-select v-model="teacherId" clearable filterable :placeholder="$t('admin.courses.allTeachers')" class="ca-filters__teacher">
        <el-option v-for="tc in teachers" :key="tc.id" :label="tc.name" :value="tc.id" />
      </el-select>
      <el-input v-model="keyword" clearable :placeholder="$t('admin.courses.search')" class="ca-filters__search" />
    </div>

    <section class="panel">
      <el-table v-loading="loading" :data="shown" row-key="id" class="ca-table" :empty-text="$t('admin.courses.empty')">
        <el-table-column :label="$t('admin.courses.name')" min-width="180">
          <template #default="{ row }">
            <div class="ca-name">
              <span class="ca-name__dot" :style="{ background: courseColor(row.name) }"></span>
              <div class="ca-name__main">
                <router-link :to="`/course/${row.id}`" class="ca-name__link">{{ row.name }}</router-link>
                <span class="ca-sub">{{ row.type || '—' }} · {{ $t('courses.credits', { n: row.score ?? 0 }) }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="teacherName" :label="$t('admin.courses.teacher')" min-width="90" />
        <el-table-column :label="$t('admin.courses.timeRoom')" min-width="140">
          <template #default="{ row }">
            <template v-if="row.week">
              <div>{{ row.week }} {{ segmentShortName(row.segment) }}</div>
              <div class="ca-sub num">{{ row.room || '—' }}</div>
            </template>
            <span v-else class="pill pill--warn">{{ $t('courses.unscheduled') }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('admin.courses.enrolled')" min-width="130">
          <template #default="{ row }">
            <div class="ca-seats">
              <span class="ca-seats__bar"><span :style="{ width: fill(row) + '%' }" :class="{ 'is-full': fill(row) >= 100 }"></span></span>
              <span class="num">{{ counts[row.id] || 0 }}<template v-if="row.num"> / {{ row.num }}</template></span>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="$t('admin.courses.status')" width="120">
          <template #default="{ row }">
            <el-select :model-value="row.status" size="small" class="ca-status" :aria-label="$t('admin.courses.status')" @change="(v: string) => changeStatus(row, v)">
              <el-option :label="$t('courses.status.notStarted')" value="未开课" />
              <el-option :label="$t('courses.filter.active')" value="已开课" />
              <el-option :label="$t('courses.status.finished')" value="已结课" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column :label="$t('admin.courses.actions')" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openWizard(row)">{{ $t('admin.courses.edit') }}</el-button>
            <el-button link type="danger" @click="remove(row)">{{ $t('admin.courses.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <CourseWizard v-model="wizardOpen" :course="editing" :teachers="teachers" @saved="load" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CourseWizard from './CourseWizard.vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from '@/utils/element-plus'
import { courseColor } from '@/utils/courseColor'
import { segmentShortName } from '@/utils/schedule'
import { t } from '@/i18n'

defineOptions({ name: 'CourseAdmin' })

const UNSCHEDULED = 'unscheduled'

const STATUS_FILTERS = [
  { value: '', label: 'courses.filter.all' },
  { value: '已开课', label: 'courses.filter.active' },
  { value: '未开课', label: 'courses.filter.upcoming' },
  { value: '已结课', label: 'courses.filter.finished' },
  { value: UNSCHEDULED, label: 'courses.unscheduled' },
]

const route = useRoute()
const router = useRouter()

const courses = ref<Record<string, any>[]>([])
const counts = ref<Record<number, number>>({})
const teachers = ref<Record<string, any>[]>([])
const loading = ref(false)
const status = ref(route.query.filter === UNSCHEDULED ? UNSCHEDULED : '')
const teacherId = ref<number | undefined>()
const keyword = ref('')

// 从首页「待排课」带着 ?filter=unscheduled 进来，换了筛选就把参数去掉，免得刷新又回到待排课
watch(status, (value) => {
  if (route.query.filter && value !== UNSCHEDULED) {
    router.replace({ query: { ...route.query, filter: undefined } })
  }
})

/** 还没定时间或教室、又没结课的课（首页「待排课」里列的就是这些） */
const isUnscheduled = (c: Record<string, any>) => c.status !== '已结课' && (!c.week || !c.segment || !c.room)

const matchesStatus = (c: Record<string, any>, value: string) =>
  !value || (value === UNSCHEDULED ? isUnscheduled(c) : c.status === value)

const countOf = (value: string) => courses.value.filter((c) => matchesStatus(c, value)).length

const shown = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  return courses.value.filter((c) => {
    if (!matchesStatus(c, status.value)) return false
    if (teacherId.value && c.teacherId !== teacherId.value) return false
    if (!k) return true
    return [c.name, c.teacherName, c.room].some((v) => String(v || '').toLowerCase().includes(k))
  })
})

const fill = (row: Record<string, any>) => {
  const cap = Number(row.num) || 0
  return cap ? Math.min(100, Math.round(((counts.value[row.id] || 0) / cap) * 100)) : 0
}

const load = async () => {
  loading.value = true
  try {
    const [list, cards, teacherList] = await Promise.all([
      request.get<Record<string, any>[]>('/course/selectAll'),
      request.get<Record<string, any>[]>('/course/mine'),
      request.get<Record<string, any>[]>('/teacher/selectAll'),
    ])
    courses.value = list || []
    counts.value = Object.fromEntries((cards || []).map((c) => [c.id, Number(c.studentCount) || 0]))
    teachers.value = teacherList || []
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    loading.value = false
  }
}

// ---------- 开课 / 编辑 ----------
const wizardOpen = ref(false)
const editing = ref<Record<string, any> | null>(null)

const openWizard = (course: Record<string, any> | null) => {
  editing.value = course
  wizardOpen.value = true
}

// 首页「开课」带 ?create=1，「待排课」里点某门课带 ?edit=课程 id：进来直接打开向导，用完把参数去掉免得刷新又弹
onMounted(async () => {
  await load()
  const editId = Number(route.query.edit)
  if (route.query.create) {
    openWizard(null)
  } else if (editId) {
    const course = courses.value.find((c) => c.id === editId)
    if (course) openWizard(course)
  }
  if (route.query.create || route.query.edit) {
    router.replace({ query: { ...route.query, create: undefined, edit: undefined } })
  }
})

/** 状态的显示文字（库里存的是中文原值） */
const STATUS_LABEL: Record<string, string> = {
  未开课: 'courses.status.notStarted',
  已开课: 'courses.filter.active',
  已结课: 'courses.status.finished',
}

const changeStatus = async (row: Record<string, any>, value: string) => {
  try {
    await request.put('/course/update', { id: row.id, status: value })
    row.status = value
    ElMessage.success(t('admin.courses.statusChanged', { name: row.name, status: STATUS_LABEL[value] ? t(STATUS_LABEL[value]) : value }))
  } catch {
    // 错误提示已由拦截器统一处理
  }
}

const remove = async (row: Record<string, any>) => {
  try {
    await ElMessageBox.confirm(t('admin.courses.deleteConfirm', { name: row.name }), t('common.confirmDeleteTitle'), { type: 'warning' })
  } catch {
    return
  }
  try {
    await request.delete(`/course/delete/${row.id}`)
    ElMessage.success(t('admin.courses.deleted'))
    await load()
  } catch {
    // 已有学生选课等提示已由拦截器统一处理
  }
}
</script>

<style scoped>
.ca {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
}

.ca .page-head {
  margin-bottom: 0;
}

.ca-filters {
  display: flex;
  align-items: center;
  gap: 10px 14px;
  flex-wrap: wrap;
}

.ca-filters__teacher {
  width: 150px;
}

.ca-filters__search {
  width: 220px;
  margin-left: auto;
}

/* 表格底色要不透明：「操作」列固定在右侧，横向滚动时其他列从它下面滑过 */
.ca-table {
  --el-table-bg-color: var(--xm-bg-card);
  --el-table-tr-bg-color: var(--xm-bg-card);
  --el-table-header-bg-color: var(--xm-bg-card);
}

.ca-name {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.ca-name__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.ca-name__main {
  display: grid;
  min-width: 0;
}

.ca-name__link {
  min-width: 0;
  overflow: hidden;
  color: var(--xm-text-primary);
  font-weight: 600;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.ca-sub {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.ca-name__link:hover {
  color: var(--xm-brand);
}

.ca-seats {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ca-seats__bar {
  width: 64px;
  height: 6px;
  border-radius: 999px;
  background: var(--xm-bg-sunken);
  overflow: hidden;
}

.ca-seats__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--xm-brand);
}

.ca-seats__bar span.is-full {
  background: var(--xm-bad);
}

.ca-status {
  width: 96px;
}

@media (max-width: 640px) {
  .ca-filters__search,
  .ca-filters__teacher {
    width: 100%;
    margin-left: 0;
  }
}
</style>

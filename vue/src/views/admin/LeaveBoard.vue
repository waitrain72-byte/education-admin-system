<template>
  <div class="lb">
    <div class="page-head">
      <div>
        <h1 class="page-title">{{ $t('admin.leaves.title') }}</h1>
        <p class="page-sub">{{ $t('admin.leaves.sub') }}</p>
      </div>
      <el-input v-model="keyword" clearable :placeholder="$t('admin.leaves.search')" class="lb-search" />
    </div>

    <!-- 窄屏一次只看一列，用这一排切换 -->
    <div class="chips lb-switch" role="tablist">
      <button
        v-for="col in columns"
        :key="col.key"
        type="button"
        role="tab"
        class="chip"
        :class="{ 'is-on': mobileColumn === col.key }"
        :aria-selected="mobileColumn === col.key"
        @click="mobileColumn = col.key"
      >
        {{ $t(`admin.leaves.columns.${col.key}`) }}<span class="chip__count num">{{ col.items.length }}</span>
      </button>
    </div>

    <div v-loading="loading" class="lb-board">
      <section
        v-for="col in columns"
        :key="col.key"
        class="lb-col"
        :class="[`lb-col--${col.key}`, { 'is-current': mobileColumn === col.key }]"
      >
        <header class="lb-col__head">
          <span class="lb-col__dot"></span>
          <span class="lb-col__title">{{ $t(`admin.leaves.columns.${col.key}`) }}</span>
          <span class="lb-col__count num">{{ col.items.length }}</span>
        </header>
        <ul v-if="col.items.length" class="lb-list">
          <li v-for="a in col.items" :key="a.id">
            <button type="button" class="lb-card" @click="openReview(a)">
              <span class="lb-card__who">
                <UserAvatar :name="a.studentName" :size="28" />
                <span class="lb-card__name">{{ a.studentName || '—' }}</span>
                <span class="lb-card__days num">{{ $t('admin.leaves.days', { n: a.day || 1 }) }}</span>
              </span>
              <span class="lb-card__range num">{{ rangeText(a) }}</span>
              <span class="lb-card__reason">{{ a.content }}</span>
              <span v-if="a.descr && col.key !== 'pending'" class="lb-card__note">{{ $t('admin.leaves.reviewed', { text: a.descr }) }}</span>
            </button>
          </li>
        </ul>
        <div v-else class="empty-note">{{ $t('admin.leaves.empty') }}</div>
      </section>
    </div>

    <el-drawer v-model="drawerOpen" :title="$t('admin.leaves.review')" :size="drawerSize" destroy-on-close>
      <div v-if="current" class="lr">
        <div class="lr__who">
          <UserAvatar :name="current.studentName" :size="40" />
          <div>
            <div class="lr__name">{{ current.studentName || '—' }}</div>
            <span class="pill" :class="STATUS_PILL[statusKey(current.status)]">{{ $t(`admin.leaves.columns.${statusKey(current.status)}`) }}</span>
          </div>
        </div>

        <dl class="lr__facts">
          <div>
            <dt>{{ $t('admin.leaves.range') }}</dt>
            <dd class="num">{{ rangeText(current) }} · {{ $t('admin.leaves.days', { n: current.day || 1 }) }}</dd>
          </div>
          <div>
            <dt>{{ $t('admin.leaves.reason') }}</dt>
            <dd class="lr__reason">{{ current.content || '—' }}</dd>
          </div>
        </dl>

        <div class="lr__affected">
          <div class="lr__label">{{ $t('admin.leaves.affected', { n: affected.length }) }}</div>
          <el-skeleton v-if="affectedLoading" animated :rows="2" />
          <p v-else-if="!affected.length" class="lr__empty">{{ $t('admin.leaves.affectedEmpty') }}</p>
          <ul v-else class="lr__list">
            <li v-for="c in affected" :key="c.date + c.courseId" class="lr__item">
              <span class="lr__bar" :style="{ background: courseColor(c.courseName) }"></span>
              <span class="lr__when num">{{ fmt(c.date) }} {{ c.start }}</span>
              <span class="lr__course">{{ c.courseName }}</span>
              <span class="lr__teacher">{{ c.teacherName }}</span>
            </li>
          </ul>
        </div>

        <el-form label-position="top">
          <el-form-item :label="$t('admin.leaves.note')">
            <el-input v-model="note" type="textarea" :rows="3" maxlength="200" show-word-limit :placeholder="$t('admin.leaves.notePlaceholder')" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button type="danger" plain :disabled="current?.status === REJECTED" :loading="saving === REJECTED" @click="decide(REJECTED)">
          {{ $t('admin.leaves.reject') }}
        </el-button>
        <el-button type="primary" :disabled="current?.status === APPROVED" :loading="saving === APPROVED" @click="decide(APPROVED)">
          {{ $t('admin.leaves.approve') }}
        </el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from '@/utils/element-plus'
import UserAvatar from '@/components/UserAvatar.vue'
import { courseColor } from '@/utils/courseColor'
import { addDays, formatDay } from '@/utils/schedule'
import { currentLocale } from '@/composables/useLocale'
import { t } from '@/i18n'

defineOptions({ name: 'LeaveBoard' })

// 待审核以外的两种状态；其余（含历史上的空状态）都算待审核
const APPROVED = '审核通过'
const REJECTED = '审核不通过'

type ColumnKey = 'pending' | 'approved' | 'rejected'
const STATUS_PILL: Record<ColumnKey, string> = { pending: 'pill--warn', approved: 'pill--ok', rejected: 'pill--bad' }

const statusKey = (status: string | null | undefined): ColumnKey =>
  status === APPROVED ? 'approved' : status === REJECTED ? 'rejected' : 'pending'

const applies = ref<Record<string, any>[]>([])
const loading = ref(false)
const keyword = ref('')
const mobileColumn = ref<ColumnKey>('pending')

const columns = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  const shown = k ? applies.value.filter((a) => String(a.studentName || '').toLowerCase().includes(k)) : applies.value
  // 待审核按请假日期先后（最早的最急），已审核的按日期倒序（最近的在上面）
  const byDate = (desc: boolean) => (a: Record<string, any>, b: Record<string, any>) =>
    (desc ? -1 : 1) * String(a.time || '').localeCompare(String(b.time || ''))
  return (['pending', 'approved', 'rejected'] as ColumnKey[]).map((key) => ({
    key,
    items: shown.filter((a) => statusKey(a.status) === key).sort(byDate(key !== 'pending')),
  }))
})

const fmt = (iso: string) => formatDay(iso, currentLocale(), { month: 'short', day: 'numeric', weekday: 'short' })

const rangeText = (a: Record<string, any>) => {
  if (!a.time) return '—'
  const days = Math.max(1, Number(a.day) || 1)
  return days === 1 ? fmt(a.time) : `${fmt(a.time)} – ${fmt(addDays(a.time, days - 1))}`
}

const load = async () => {
  loading.value = true
  try {
    applies.value = (await request.get<Record<string, any>[]>('/apply/selectAll')) || []
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    loading.value = false
  }
}

// 首页「待审批的请假」里点某一条带 ?open=申请 id：进来直接打开它的审核抽屉
const route = useRoute()
const router = useRouter()

onMounted(async () => {
  await load()
  const openId = Number(route.query.open)
  if (!openId) return
  const apply = applies.value.find((a) => a.id === openId)
  if (apply) {
    mobileColumn.value = statusKey(apply.status)
    openReview(apply)
  }
  router.replace({ query: { ...route.query, open: undefined } })
})

// ---------- 审核抽屉 ----------
const drawerOpen = ref(false)
const drawerSize = ref('480px')
const current = ref<Record<string, any> | null>(null)
const note = ref('')
const affected = ref<Record<string, any>[]>([])
const affectedLoading = ref(false)
const saving = ref('')

const openReview = async (apply: Record<string, any>) => {
  current.value = apply
  note.value = apply.descr || ''
  affected.value = []
  drawerSize.value = window.innerWidth < 560 ? '100%' : '480px'
  drawerOpen.value = true
  affectedLoading.value = true
  try {
    affected.value = (await request.get<Record<string, any>[]>(`/apply/${apply.id}/affected`)) || []
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    affectedLoading.value = false
  }
}

const decide = async (status: string) => {
  const apply = current.value
  if (!apply) return
  saving.value = status
  try {
    await request.put('/apply/update', { id: apply.id, status, descr: note.value.trim() })
    apply.status = status
    apply.descr = note.value.trim()
    ElMessage.success(status === APPROVED ? t('admin.leaves.approvedMsg') : t('admin.leaves.rejectedMsg'))
    drawerOpen.value = false
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    saving.value = ''
  }
}
</script>

<style scoped>
.lb {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
}

.lb .page-head {
  margin-bottom: 0;
}

.lb-search {
  width: 220px;
}

.lb-switch {
  display: none;
}

.lb-board {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  align-items: start;
  min-height: 160px;
}

.lb-col {
  display: grid;
  gap: 10px;
  padding: 12px;
  border-radius: var(--xm-radius-lg);
  background: var(--xm-bg-sunken);
}

.lb-col__head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 4px;
}

.lb-col__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--xm-warn);
}

.lb-col--approved .lb-col__dot {
  background: var(--xm-ok);
}

.lb-col--rejected .lb-col__dot {
  background: var(--xm-bad);
}

.lb-col__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--xm-text-primary);
}

.lb-col__count {
  margin-left: auto;
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.lb-list {
  display: grid;
  gap: 8px;
  list-style: none;
}

.lb-card {
  display: grid;
  gap: 6px;
  width: 100%;
  padding: 12px;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius);
  background: var(--xm-bg-card);
  color: var(--xm-text-regular);
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.lb-card:hover {
  border-color: var(--xm-brand);
  box-shadow: var(--xm-shadow-card);
}

.lb-card__who {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.lb-card__name {
  min-width: 0;
  overflow: hidden;
  font-weight: 600;
  color: var(--xm-text-primary);
  white-space: nowrap;
  text-overflow: ellipsis;
}

.lb-card__days {
  margin-left: auto;
  flex-shrink: 0;
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.lb-card__range {
  font-size: 13px;
  color: var(--xm-text-primary);
}

.lb-card__reason,
.lb-card__note {
  display: -webkit-box;
  overflow: hidden;
  font-size: 13px;
  line-height: 1.5;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.lb-card__note {
  color: var(--xm-text-secondary);
}

/* ---------- 审核抽屉 ---------- */
.lr {
  display: grid;
  gap: 18px;
}

.lr__who {
  display: flex;
  align-items: center;
  gap: 12px;
}

.lr__name {
  margin-bottom: 4px;
  font-size: 16px;
  font-weight: 600;
  color: var(--xm-text-primary);
}

.lr__facts {
  display: grid;
  gap: 12px;
}

.lr__facts dt,
.lr__label {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.lr__facts dd {
  margin-top: 2px;
  font-size: 14px;
  color: var(--xm-text-primary);
}

.lr__reason {
  white-space: pre-wrap;
  word-break: break-word;
}

.lr__affected {
  display: grid;
  gap: 8px;
  padding: 12px 14px;
  border-radius: var(--xm-radius);
  background: var(--xm-bg-sunken);
}

.lr__empty {
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.lr__list {
  display: grid;
  gap: 6px;
  list-style: none;
  max-height: 240px;
  overflow-y: auto;
}

.lr__item {
  display: grid;
  grid-template-columns: 4px auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.lr__bar {
  width: 4px;
  height: 18px;
  border-radius: 2px;
}

.lr__when {
  color: var(--xm-text-secondary);
  white-space: nowrap;
}

.lr__course {
  overflow: hidden;
  color: var(--xm-text-primary);
  white-space: nowrap;
  text-overflow: ellipsis;
}

.lr__teacher {
  color: var(--xm-text-secondary);
}

@media (max-width: 760px) {
  .lb-search {
    width: 100%;
  }

  .lb-switch {
    display: flex;
  }

  .lb-board {
    grid-template-columns: minmax(0, 1fr);
  }

  .lb-col:not(.is-current) {
    display: none;
  }

  .lb-col__head {
    display: none;
  }
}
</style>

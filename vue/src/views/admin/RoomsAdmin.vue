<template>
  <div class="ra">
    <div class="page-head">
      <div>
        <h1 class="page-title">{{ $t('admin.rooms.title') }}</h1>
        <p class="page-sub">{{ $t('admin.rooms.sub') }}</p>
      </div>
      <el-button type="primary" @click="openForm(null)">{{ $t('admin.rooms.create') }}</el-button>
    </div>

    <div class="ra-filters">
      <div class="chips" role="tablist">
        <button
          v-for="f in TYPE_FILTERS"
          :key="f"
          type="button"
          role="tab"
          class="chip"
          :class="{ 'is-on': typeFilter === f }"
          :aria-selected="typeFilter === f"
          @click="typeFilter = f"
        >
          {{ f ? typeLabel(f) : $t('admin.rooms.all') }}<span class="chip__count num">{{ countOf(f) }}</span>
        </button>
      </div>
      <el-input v-model="keyword" clearable :placeholder="$t('admin.rooms.search')" class="ra-filters__search" />
    </div>

    <div class="ra-body">
      <section v-loading="loading" class="ra-list" :aria-label="$t('admin.rooms.title')">
        <button
          v-for="r in shown"
          :key="r.id"
          type="button"
          class="ra-room"
          :class="{ 'is-on': selectedId === r.id }"
          :aria-pressed="selectedId === r.id"
          @click="selectedId = r.id"
        >
          <span class="ra-room__top">
            <span class="ra-room__code num">{{ r.code }}</span>
            <span class="pill" :class="TYPE_PILL[r.type] || 'pill--info'">{{ typeLabel(r.type) }}</span>
          </span>
          <span class="ra-room__name">{{ r.name }}</span>
          <span class="ra-room__meta">
            <span class="num">{{ $t('admin.rooms.seatsUnit', { n: r.num ?? 0 }) }}</span>
            <span class="num">{{ $t('admin.rooms.used', { n: bookedOf(r.code).length }) }}</span>
          </span>
          <span class="ra-room__bar"><span :style="{ width: usage(r.code) + '%' }"></span></span>
        </button>
        <div v-if="!loading && !shown.length" class="empty-note">{{ $t('admin.rooms.empty') }}</div>
      </section>

      <section class="panel ra-detail">
        <template v-if="selected">
          <div class="ra-detail__head">
            <div>
              <div class="ra-detail__title">
                <span class="num">{{ selected.code }}</span>
                <span class="ra-detail__name">{{ selected.name }}</span>
              </div>
              <div class="ra-detail__sub">
                {{ typeLabel(selected.type) }} · {{ $t('admin.rooms.seatsUnit', { n: selected.num ?? 0 }) }} · {{ statusLabel(selected.status) }}
              </div>
            </div>
            <div class="ra-detail__actions">
              <el-button size="small" @click="openForm(selected)">{{ $t('admin.rooms.edit') }}</el-button>
              <el-button size="small" type="danger" plain @click="remove(selected)">{{ $t('admin.rooms.delete') }}</el-button>
            </div>
          </div>
          <p v-if="selected.content" class="ra-detail__content">{{ selected.content }}</p>

          <div class="ra-week-title">{{ $t('admin.rooms.occupancy') }}</div>
          <div class="ra-week-scroll">
            <div class="ra-week" role="grid" :aria-label="$t('admin.rooms.occupancy')">
              <div class="ra-week__corner"></div>
              <div v-for="(w, wi) in WEEKDAYS" :key="w" class="ra-week__day" role="columnheader">{{ weekdayLabel(wi) }}</div>
              <template v-for="s in SEGMENTS" :key="s">
                <div class="ra-week__seg" role="rowheader">{{ segmentShortName(s) }}</div>
                <div v-for="w in WEEKDAYS" :key="w + s" class="ra-week__cell" role="gridcell">
                  <router-link
                    v-for="c in slot(selected.code, w, s)"
                    :key="c.id"
                    :to="`/course/${c.id}`"
                    class="ra-block"
                    :class="{ 'is-pending': c.status !== '已开课', 'is-over': (c.num || 0) > (selected.num || 0) }"
                    :style="{ '--cc': courseColor(c.name) }"
                    :title="`${c.name} · ${c.teacherName || '—'} · ${$t('admin.rooms.capacityOf', { n: c.num ?? 0 })}`"
                  >
                    <span class="ra-block__name">{{ c.name }}</span>
                    <span class="ra-block__meta">{{ c.teacherName || '—' }}</span>
                  </router-link>
                </div>
              </template>
            </div>
          </div>
          <p class="ra-legend">{{ $t('admin.rooms.legend') }}</p>
        </template>
        <div v-else class="empty-note ra-detail__hint">{{ $t('admin.rooms.selectHint') }}</div>
      </section>
    </div>

    <el-dialog v-model="formOpen" :title="editing ? $t('admin.rooms.edit') : $t('admin.rooms.create')" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="ra-form-row">
          <el-form-item :label="$t('admin.rooms.code')" prop="code" class="ra-form-row__item">
            <el-input v-model="form.code" maxlength="20" />
          </el-form-item>
          <el-form-item :label="$t('admin.rooms.seats')" class="ra-form-row__item">
            <el-input-number v-model="form.num" :min="1" :max="1000" :precision="0" controls-position="right" style="width: 100%" />
          </el-form-item>
        </div>
        <p v-if="editing && form.code.trim() !== editing.code && bookedOf(editing.code).length" class="ra-form-note">
          {{ $t('admin.rooms.renameNote', { n: bookedOf(editing.code).length }) }}
        </p>
        <el-form-item :label="$t('admin.rooms.name')" prop="name">
          <el-input v-model="form.name" maxlength="30" />
        </el-form-item>
        <div class="ra-form-row">
          <el-form-item :label="$t('admin.rooms.type')" class="ra-form-row__item">
            <el-select v-model="form.type" style="width: 100%">
              <el-option v-for="ty in TYPES" :key="ty" :label="typeLabel(ty)" :value="ty" />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('admin.rooms.status')" class="ra-form-row__item">
            <el-select v-model="form.status" style="width: 100%">
              <el-option v-for="st in STATUSES" :key="st" :label="statusLabel(st)" :value="st" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item :label="$t('admin.rooms.content')">
          <el-input v-model="form.content" maxlength="100" :placeholder="$t('admin.rooms.contentPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formOpen = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from '@/utils/element-plus'
import { courseColor } from '@/utils/courseColor'
import { SEGMENTS, WEEKDAYS, addDays, formatDay, segmentShortName } from '@/utils/schedule'
import { currentLocale } from '@/composables/useLocale'
import { t } from '@/i18n'

defineOptions({ name: 'RoomsAdmin' })

/** 数据库里按中文存的类型与状态；显示时翻译 */
const TYPES = ['授课教室', '运动场馆', '固定占用']
const STATUSES = ['空闲', '占用']
const TYPE_KEY: Record<string, string> = { 授课教室: 'classroom', 运动场馆: 'venue', 固定占用: 'reserved' }
const STATUS_KEY: Record<string, string> = { 空闲: 'free', 占用: 'busy' }
const TYPE_PILL: Record<string, string> = { 授课教室: 'pill--brand', 运动场馆: 'pill--ok', 固定占用: 'pill--warn' }
const TYPE_FILTERS = ['', ...TYPES]
/** 一周可排的时段总数（7 天 × 5 个大节），算占用率用 */
const SLOT_TOTAL = WEEKDAYS.length * SEGMENTS.length

const typeLabel = (type: string) => (TYPE_KEY[type] ? t(`admin.rooms.types.${TYPE_KEY[type]}`) : type || '—')
const statusLabel = (status: string) => (STATUS_KEY[status] ? t(`admin.rooms.statuses.${STATUS_KEY[status]}`) : status || '—')
/** 表头用本地化的星期简称：2024-01-01 是星期一 */
const weekdayLabel = (i: number) => formatDay(addDays('2024-01-01', i), currentLocale(), { weekday: 'short' })

const rooms = ref<Record<string, any>[]>([])
const courses = ref<Record<string, any>[]>([])
const loading = ref(false)
const typeFilter = ref('')
const keyword = ref('')
const selectedId = ref<number | null>(null)

const countOf = (type: string) => (type ? rooms.value.filter((r) => r.type === type).length : rooms.value.length)

const shown = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  return rooms.value
    .filter((r) => !typeFilter.value || r.type === typeFilter.value)
    .filter((r) => !k || [r.code, r.name, r.content].some((v) => String(v || '').toLowerCase().includes(k)))
    .sort((a, b) => String(a.code || '').localeCompare(String(b.code || ''), undefined, { numeric: true }))
})

const selected = computed(() => rooms.value.find((r) => r.id === selectedId.value) || null)

// 筛选后选中的教室不在列表里了，就换成列表第一间
watch(shown, (list) => {
  if (!list.some((r) => r.id === selectedId.value)) selectedId.value = list[0]?.id ?? null
})

/** 每间教室排着的课（不含已结课：已结课不再占教室，和后端的占用判断一致） */
const byRoom = computed(() => {
  const map = new Map<string, Record<string, any>[]>()
  for (const c of courses.value) {
    if (!c.room || !c.week || !c.segment || c.status === '已结课') continue
    if (!map.has(c.room)) map.set(c.room, [])
    map.get(c.room)!.push(c)
  }
  return map
})

const bookedOf = (code: string) => byRoom.value.get(code) || []
const usage = (code: string) => Math.min(100, Math.round((bookedOf(code).length / SLOT_TOTAL) * 100))
const slot = (code: string, week: string, segment: string) => bookedOf(code).filter((c) => c.week === week && c.segment === segment)

const load = async () => {
  loading.value = true
  try {
    const [roomList, courseList] = await Promise.all([
      request.get<Record<string, any>[]>('/roomplan/selectAll'),
      request.get<Record<string, any>[]>('/course/selectAll'),
    ])
    rooms.value = roomList || []
    courses.value = courseList || []
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    loading.value = false
  }
}

onMounted(load)

// ---------- 新建 / 编辑 ----------
const formOpen = ref(false)
const formRef = ref<FormInstance>()
const editing = ref<Record<string, any> | null>(null)
const saving = ref(false)
const form = reactive({ code: '', name: '', type: '授课教室', status: '空闲', num: 60, content: '' })

const rules = computed<FormRules>(() => ({
  code: [{ required: true, whitespace: true, message: t('admin.rooms.codeRequired'), trigger: 'blur' }],
  name: [{ required: true, whitespace: true, message: t('admin.rooms.nameRequired'), trigger: 'blur' }],
}))

const openForm = (room: Record<string, any> | null) => {
  editing.value = room
  Object.assign(form, {
    code: room?.code || '',
    name: room?.name || '',
    type: room?.type || '授课教室',
    status: room?.status || '空闲',
    num: room?.num ?? 60,
    content: room?.content || '',
  })
  formOpen.value = true
}

const save = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  const body = { code: form.code.trim(), name: form.name.trim(), type: form.type, status: form.status, num: form.num, content: form.content.trim() }
  saving.value = true
  try {
    if (editing.value) {
      await request.put('/roomplan/update', { ...body, id: editing.value.id })
    } else {
      await request.post('/roomplan/add', body)
    }
    ElMessage.success(t('admin.rooms.saved'))
    formOpen.value = false
    // 改了编号时课程上的教室也跟着改了，课程一起重新拉
    await load()
    if (!editing.value) selectedId.value = rooms.value.find((r) => r.code === body.code)?.id ?? selectedId.value
  } catch {
    // 编号重复等提示已由拦截器统一处理
  } finally {
    saving.value = false
  }
}

const remove = async (room: Record<string, any>) => {
  try {
    await ElMessageBox.confirm(t('admin.rooms.deleteConfirm', { name: `${room.code} ${room.name}` }), t('common.confirmDeleteTitle'), { type: 'warning' })
  } catch {
    return
  }
  try {
    await request.delete(`/roomplan/delete/${room.id}`)
    ElMessage.success(t('admin.rooms.deleted'))
    await load()
  } catch {
    // 还有课排在这里的提示已由拦截器统一处理
  }
}
</script>

<style scoped>
.ra {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
}

.ra .page-head {
  margin-bottom: 0;
}

.ra-filters {
  display: flex;
  align-items: center;
  gap: 10px 14px;
  flex-wrap: wrap;
}

.ra-filters__search {
  width: 220px;
  margin-left: auto;
}

.ra-body {
  display: grid;
  grid-template-columns: minmax(200px, 260px) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.ra-list {
  display: grid;
  gap: 8px;
  min-height: 120px;
}

.ra-room {
  display: grid;
  gap: 4px;
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius);
  background: var(--xm-bg-card);
  color: var(--xm-text-regular);
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.ra-room:hover {
  border-color: var(--xm-brand);
}

.ra-room.is-on {
  border-color: var(--xm-brand);
  box-shadow: 0 0 0 1px var(--xm-brand) inset;
}

.ra-room__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.ra-room__code {
  font-size: 17px;
  font-weight: 700;
  color: var(--xm-text-primary);
}

.ra-room__name {
  overflow: hidden;
  font-size: 13px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.ra-room__meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.ra-room__bar {
  height: 4px;
  margin-top: 2px;
  border-radius: 999px;
  background: var(--xm-bg-sunken);
  overflow: hidden;
}

.ra-room__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--xm-brand);
}

.ra-detail {
  display: grid;
  gap: 14px;
  padding: 20px;
  min-width: 0;
}

.ra-detail__hint {
  padding: 40px 0;
}

.ra-detail__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.ra-detail__title {
  display: flex;
  align-items: baseline;
  gap: 10px;
  font-size: 22px;
  font-weight: 700;
  color: var(--xm-text-primary);
}

.ra-detail__name {
  font-size: 15px;
  font-weight: 500;
  color: var(--xm-text-regular);
}

.ra-detail__sub,
.ra-detail__content,
.ra-legend {
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.ra-week-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--xm-text-primary);
}

.ra-week-scroll {
  overflow-x: auto;
}

/* 7 天都放得下才不用横向滚动；更窄时（手机）再滚 */
.ra-week {
  display: grid;
  grid-template-columns: 52px repeat(7, minmax(52px, 1fr));
  gap: 4px;
  min-width: 460px;
}

.ra-week__day,
.ra-week__seg {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.ra-week__day {
  padding: 4px 0;
  text-align: center;
}

.ra-week__seg {
  display: flex;
  align-items: center;
}

.ra-week__cell {
  display: grid;
  gap: 4px;
  min-height: 52px;
  padding: 3px;
  border-radius: var(--xm-radius-sm);
  background: var(--xm-bg-sunken);
}

.ra-block {
  display: grid;
  gap: 1px;
  min-width: 0;
  padding: 4px 6px;
  border-left: 3px solid var(--cc);
  border-radius: var(--xm-radius-sm);
  background: color-mix(in srgb, var(--cc) 14%, var(--xm-bg-card));
  color: var(--xm-text-primary);
}

.ra-block.is-pending {
  border-left-style: dashed;
  background: var(--xm-bg-card);
}

.ra-block.is-over {
  box-shadow: 0 0 0 1px var(--xm-bad) inset;
}

/* 格子窄，课名折成两行再截断 */
.ra-block__name {
  display: -webkit-box;
  overflow: hidden;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.35;
  overflow-wrap: anywhere;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.ra-block__meta {
  overflow: hidden;
  font-size: 11px;
  color: var(--xm-text-secondary);
  white-space: nowrap;
  text-overflow: ellipsis;
}

.ra-form-row {
  display: flex;
  gap: 16px;
}

.ra-form-row__item {
  flex: 1;
  min-width: 0;
}

.ra-form-note {
  margin: -8px 0 12px;
  font-size: 12px;
  color: var(--xm-warn);
}

@media (max-width: 900px) {
  .ra-body {
    grid-template-columns: minmax(0, 1fr);
  }

  .ra-list {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  }
}

@media (max-width: 640px) {
  .ra-filters__search {
    width: 100%;
    margin-left: 0;
  }
}
</style>

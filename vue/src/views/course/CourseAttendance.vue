<template>
  <div class="att">
    <div v-if="!panel && loading" class="panel panel--pad">
      <el-skeleton animated :rows="5" />
    </div>

    <!-- ==================== 老师 / 管理员 ==================== -->
    <template v-else-if="panel && canTeach">
      <div class="att-top">
        <section class="panel session" :class="{ 'is-live': active }">
          <template v-if="active">
            <div class="session__label">{{ $t('space.attendance.codeLabel') }}</div>
            <div class="session__code num" :aria-label="active.code.split('').join(' ')">
              <span v-for="(digit, i) in active.code.split('')" :key="i" class="session__digit">{{ digit }}</span>
            </div>
            <p class="session__hint">{{ $t('space.attendance.codeHint') }}</p>
            <div class="session__stats">
              <div>
                <div class="session__stat-label">{{ $t('space.attendance.remaining') }}</div>
                <div class="session__stat num" :class="{ 'is-urgent': remaining <= 30 }">
                  {{ remaining > 0 ? formatCountdown(remaining) : $t('space.attendance.expired') }}
                </div>
              </div>
              <div>
                <div class="session__stat-label">{{ $t('space.attendance.signedCount') }}</div>
                <div class="session__stat num">{{ active.signed }} <small>/ {{ active.total }}</small></div>
              </div>
            </div>
            <div class="session__progress" role="progressbar" :aria-valuenow="active.signed" :aria-valuemax="active.total">
              <div class="session__progress-bar" :style="{ width: signedPercent + '%' }"></div>
            </div>
            <el-button class="session__end" :loading="ending" @click="endSession">{{ $t('space.attendance.end') }}</el-button>
          </template>

          <template v-else>
            <div class="session__idle-title">{{ $t('space.attendance.start') }}</div>
            <p class="session__hint">{{ $t('space.attendance.startHint') }}</p>
            <div class="session__duration">
              <span class="session__duration-label">{{ $t('space.attendance.duration') }}</span>
              <el-radio-group v-model="minutes" size="small">
                <el-radio-button v-for="m in DURATIONS" :key="m" :value="m">
                  {{ $t('space.attendance.minutes', { n: m }) }}
                </el-radio-button>
              </el-radio-group>
            </div>
            <el-button type="primary" size="large" :loading="starting" @click="startSession">
              {{ $t('space.attendance.start') }}
            </el-button>
          </template>
        </section>

        <section class="panel history">
          <div class="panel__head">
            <span class="panel__title">{{ $t('space.attendance.history') }}</span>
          </div>
          <div class="panel__body">
            <div v-if="!panel.history.length" class="empty-note">{{ $t('space.attendance.historyEmpty') }}</div>
            <ul v-else class="history__list">
              <li v-for="day in panel.history" :key="day.date">
                <button
                  type="button"
                  class="history__item"
                  :class="{ 'is-current': day.date === date }"
                  @click="pickDate(day.date)"
                >
                  <span class="history__date num">{{ day.date }}</span>
                  <span class="history__bar" aria-hidden="true">
                    <span
                      v-for="s in ATTENDANCE_STATUSES"
                      :key="s.value"
                      :class="'history__seg history__seg--' + s.key"
                      :style="{ flexGrow: day[s.value] || 0 }"
                    ></span>
                  </span>
                  <span class="history__counts">
                    <template v-for="s in ATTENDANCE_STATUSES" :key="s.value">
                      <span v-if="day[s.value]">{{ $t('space.attendance.statuses.' + s.key) }} {{ day[s.value] }}</span>
                    </template>
                  </span>
                </button>
              </li>
            </ul>
          </div>
        </section>
      </div>

      <section class="panel">
        <div class="panel__head roster-head">
          <span class="panel__title">{{ $t('space.attendance.roster') }}</span>
          <div class="roster-head__tools">
            <span class="panel__hint">{{ $t('space.attendance.markHint') }}</span>
            <el-date-picker
              v-model="date"
              type="date"
              value-format="YYYY-MM-DD"
              :clearable="false"
              size="small"
              :aria-label="$t('space.attendance.date')"
              style="width: 150px"
              @change="loadPanel"
            />
          </div>
        </div>
        <div class="panel__body">
          <div class="roster-summary">
            <span v-for="s in ATTENDANCE_STATUSES" :key="s.value" class="pill" :class="s.pill">
              {{ $t('space.attendance.statuses.' + s.key) }} <b class="num">{{ rosterCounts[s.value] || 0 }}</b>
            </span>
            <span class="pill">{{ $t('space.attendance.noRecord') }} <b class="num">{{ rosterCounts.none || 0 }}</b></span>
          </div>
          <div v-if="!panel.roster.length" class="empty-note">{{ $t('space.members.empty') }}</div>
          <ul v-else class="roster">
            <li v-for="row in panel.roster" :key="row.studentId" class="roster__item">
              <UserAvatar :name="row.name" :avatar="row.avatar" :size="36" />
              <div class="roster__who">
                <div class="roster__name">{{ row.name }}</div>
                <div class="roster__sub num">{{ row.username }}</div>
              </div>
              <el-dropdown trigger="click" @command="(status: string) => mark(row, status)">
                <button
                  type="button"
                  class="pill roster__status"
                  :class="attendanceMeta(row.status)?.pill"
                  :title="row.bySession ? $t('space.attendance.bySession') : undefined"
                >
                  {{ row.status ? $t('space.attendance.statuses.' + attendanceMeta(row.status)?.key) : $t('space.attendance.noRecord') }}
                  <span aria-hidden="true">▾</span>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-for="s in ATTENDANCE_STATUSES" :key="s.value" :command="s.value" :disabled="row.status === s.value">
                      {{ $t('space.attendance.statuses.' + s.key) }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </li>
          </ul>
        </div>
      </section>
    </template>

    <!-- ==================== 学生 ==================== -->
    <template v-else-if="panel">
      <section class="panel checkin" :class="{ 'is-live': active && !active.signed }">
        <template v-if="active && active.signed">
          <div class="checkin__done" aria-hidden="true">✓</div>
          <div class="checkin__title">{{ $t('space.attendance.student.done') }}</div>
        </template>
        <template v-else-if="active && active.locked">
          <div class="checkin__title">{{ $t('space.attendance.student.title') }}</div>
          <p class="checkin__warn">{{ $t('space.attendance.student.locked') }}</p>
        </template>
        <template v-else-if="active">
          <div class="checkin__title">{{ $t('space.attendance.student.open') }}</div>
          <p class="checkin__sub">
            {{ $t('space.attendance.student.closesAt', { time: active.expireTime.slice(11, 16) }) }}
            · <span class="num">{{ formatCountdown(remaining) }}</span>
          </p>
          <form class="checkin__form" @submit.prevent="checkin">
            <el-input
              v-model="code"
              class="checkin__input"
              maxlength="4"
              inputmode="numeric"
              autocomplete="one-time-code"
              :placeholder="'0000'"
              :aria-label="$t('space.attendance.student.enterCode')"
              @input="code = code.replace(/\D/g, '')"
            />
            <el-button type="primary" size="large" native-type="submit" :loading="checking" :disabled="code.length !== 4">
              {{ $t('space.attendance.student.submit') }}
            </el-button>
          </form>
          <p class="checkin__sub">{{ $t('space.attendance.student.enterCode') }}</p>
        </template>
        <template v-else>
          <div class="checkin__title checkin__title--idle">{{ $t('space.attendance.student.idle') }}</div>
          <p class="checkin__sub">{{ $t('space.attendance.student.idleHint') }}</p>
        </template>
      </section>

      <section class="panel">
        <div class="panel__head">
          <span class="panel__title">{{ $t('space.attendance.student.myRecords') }}</span>
          <span v-if="myRate != null" class="panel__hint">
            {{ $t('space.attendance.student.rate') }} <b class="num">{{ myRate }}%</b>
          </span>
        </div>
        <div class="panel__body">
          <div class="roster-summary">
            <span v-for="s in ATTENDANCE_STATUSES" :key="s.value" class="pill" :class="s.pill">
              {{ $t('space.attendance.statuses.' + s.key) }} <b class="num">{{ panel.counts[s.value] || 0 }}</b>
            </span>
          </div>
          <div v-if="!panel.records.length" class="empty-note">{{ $t('space.attendance.student.empty') }}</div>
          <ul v-else class="records">
            <li v-for="r in panel.records" :key="r.id" class="records__item">
              <span class="num">{{ r.time }}</span>
              <span class="pill" :class="attendanceMeta(r.status)?.pill">
                {{ attendanceMeta(r.status) ? $t('space.attendance.statuses.' + attendanceMeta(r.status)?.key) : r.status }}
              </span>
            </li>
          </ul>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import UserAvatar from '@/components/UserAvatar.vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from '@/utils/element-plus'
import { ATTENDANCE_STATUSES, attendanceMeta, attendanceRateOf, formatCountdown, wallMs } from '@/utils/courseSpace'
import { useCourseEvents } from '@/composables/useCourseEvents'
import { t } from '@/i18n'

const props = defineProps<{ overview: Record<string, any> }>()
const emit = defineEmits<{ refresh: [] }>()

const DURATIONS = [2, 5, 10, 15]

const route = useRoute()
const courseId = computed(() => Number(route.params.id))
const relation = computed<string>(() => props.overview.relation || 'visitor')
const canTeach = computed(() => relation.value === 'teacher' || relation.value === 'admin')

const panel = ref<Record<string, any> | null>(null)
const loading = ref(false)
const date = ref('')
const minutes = ref(5)
const starting = ref(false)
const ending = ref(false)
const code = ref('')
const checking = ref(false)

const active = computed<Record<string, any> | null>(() => panel.value?.active || null)

// ---------- 倒计时：按「服务器时间 + 本地流逝」推算，与浏览器时区无关 ----------
const fetchedAt = ref(0)
const serverAtFetch = ref(NaN)
const tickNow = ref(Date.now())
let ticker: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  ticker = setInterval(() => {
    tickNow.value = Date.now()
  }, 1000)
})
onBeforeUnmount(() => {
  if (ticker) clearInterval(ticker)
})

const remaining = computed(() => {
  if (!active.value || Number.isNaN(serverAtFetch.value)) return 0
  const nowServer = serverAtFetch.value + (tickNow.value - fetchedAt.value)
  return Math.max(0, Math.round((wallMs(active.value.expireTime) - nowServer) / 1000))
})

const signedPercent = computed(() => {
  const total = Number(active.value?.total) || 0
  return total ? Math.min(100, Math.round((Number(active.value?.signed) / total) * 100)) : 0
})

const loadPanel = async () => {
  loading.value = true
  try {
    const data = await request.get<Record<string, any>>(`/course/${courseId.value}/attendance`, {
      params: date.value ? { date: date.value } : undefined,
    })
    panel.value = data
    fetchedAt.value = Date.now()
    tickNow.value = fetchedAt.value
    serverAtFetch.value = wallMs(data.now)
    if (data.date) date.value = data.date
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    loading.value = false
  }
}

watch(courseId, () => {
  date.value = ''
  loadPanel()
}, { immediate: true })

useCourseEvents(() => courseId.value, ['attendance'], loadPanel)

/** 倒计时归零：老师端直接收尾（后端幂等），学生端刷新看结果 */
let expiring = false
watch(remaining, async (value, old) => {
  if (!active.value || value > 0 || old === 0 || expiring) return
  expiring = true
  try {
    if (canTeach.value) {
      await request.post(`/course/${courseId.value}/attendance/sessions/${active.value.id}/finish`)
      emit('refresh')
    }
  } catch {
    // 定时任务也会收尾，这里失败不提示
  } finally {
    expiring = false
    await loadPanel()
  }
})

// ---------- 老师操作 ----------
const startSession = async () => {
  starting.value = true
  try {
    await request.post(`/course/${courseId.value}/attendance/sessions`, { minutes: minutes.value })
    date.value = ''
    await loadPanel()
    emit('refresh')
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    starting.value = false
  }
}

const endSession = async () => {
  if (!active.value) return
  try {
    await ElMessageBox.confirm(t('space.attendance.endConfirm'), t('space.attendance.end'), { type: 'warning' })
  } catch {
    return
  }
  ending.value = true
  try {
    const result = await request.post<{ marked: number }>(
      `/course/${courseId.value}/attendance/sessions/${active.value.id}/finish`,
    )
    ElMessage.success(t('space.attendance.ended', { n: result?.marked ?? 0 }))
    await loadPanel()
    emit('refresh')
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    ending.value = false
  }
}

const pickDate = (day: string) => {
  date.value = day
  loadPanel()
}

const rosterCounts = computed(() => {
  const counts: Record<string, number> = {}
  for (const row of panel.value?.roster || []) {
    const key = row.status || 'none'
    counts[key] = (counts[key] || 0) + 1
  }
  return counts
})

const mark = async (row: Record<string, any>, status: string) => {
  try {
    await request.put(`/course/${courseId.value}/attendance/records`, {
      studentId: row.studentId,
      time: date.value,
      status,
    })
    row.status = status
    row.bySession = false
    const meta = attendanceMeta(status)
    ElMessage.success(t('space.attendance.marked', { status: meta ? t('space.attendance.statuses.' + meta.key) : status }))
    loadPanel()
  } catch {
    // 错误提示已由拦截器统一处理
  }
}

// ---------- 学生签到 ----------
const myRate = computed(() => attendanceRateOf(panel.value?.counts))

const checkin = async () => {
  if (code.value.length !== 4 || checking.value) return
  checking.value = true
  try {
    await request.post(`/course/${courseId.value}/attendance/checkin`, { code: code.value })
    ElMessage.success(t('space.attendance.student.success'))
    code.value = ''
    await loadPanel()
    emit('refresh')
  } catch {
    // 签到码错误等提示已由拦截器统一处理；锁定状态要刷新才看得到
    code.value = ''
    await loadPanel()
  } finally {
    checking.value = false
  }
}
</script>

<style scoped>
.att {
  display: grid;
  /* 列宽可以缩到 0：里面的宽表格自己横向滚动，不把整页撑出屏幕 */
  grid-template-columns: minmax(0, 1fr);
  gap: 20px;
}

.att-top {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 20px;
  align-items: stretch;
}

/* ---------- 签到场次 ---------- */
.session {
  display: grid;
  align-content: start;
  justify-items: start;
  gap: 12px;
  padding: 22px 24px;
}

.session.is-live {
  border-color: var(--xm-brand);
  box-shadow: 0 0 0 3px var(--xm-brand-soft);
}

.session__label,
.session__stat-label,
.session__duration-label {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.session__code {
  display: flex;
  gap: 10px;
}

.session__digit {
  display: grid;
  place-items: center;
  width: 64px;
  height: 78px;
  border-radius: 12px;
  background: var(--xm-brand-soft);
  color: var(--xm-brand);
  font-size: 46px;
  font-weight: 700;
  line-height: 1;
}

.session__hint {
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.session__stats {
  display: flex;
  gap: 36px;
  margin-top: 4px;
}

.session__stat {
  margin-top: 2px;
  font-size: 24px;
  font-weight: 700;
  color: var(--xm-text-primary);
}

.session__stat small {
  font-size: 14px;
  font-weight: 500;
  color: var(--xm-text-secondary);
}

.session__stat.is-urgent {
  color: var(--xm-bad);
}

.session__progress {
  width: 100%;
  height: 6px;
  border-radius: 999px;
  background: var(--xm-bg-sunken);
  overflow: hidden;
}

.session__progress-bar {
  height: 100%;
  border-radius: inherit;
  background: var(--xm-brand);
  transition: width 0.3s ease;
}

.session__end {
  margin-top: 4px;
}

.session__idle-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--xm-text-primary);
}

.session__duration {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin: 4px 0;
}

/* ---------- 历次考勤 ---------- */
.history {
  min-width: 0;
}

.history__list {
  list-style: none;
  display: grid;
  gap: 2px;
  max-height: 300px;
  overflow-y: auto;
}

.history__item {
  display: grid;
  grid-template-columns: 96px minmax(60px, 1fr);
  grid-template-areas:
    'date bar'
    'counts counts';
  gap: 4px 12px;
  align-items: center;
  width: 100%;
  padding: 8px 10px;
  border: 0;
  border-radius: var(--xm-radius);
  background: none;
  color: var(--xm-text-primary);
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.history__item:hover {
  background: var(--xm-bg-hover);
}

.history__item.is-current {
  background: var(--xm-brand-soft);
}

.history__date {
  grid-area: date;
  font-size: 13px;
}

.history__bar {
  grid-area: bar;
  display: flex;
  gap: 2px;
  height: 8px;
  border-radius: 999px;
  overflow: hidden;
}

.history__seg--normal {
  background: var(--xm-ok);
}

.history__seg--late,
.history__seg--early {
  background: var(--xm-warn);
}

.history__seg--absent {
  background: var(--xm-bad);
}

.history__seg--leave {
  background: var(--xm-info);
}

.history__counts {
  grid-area: counts;
  display: flex;
  flex-wrap: wrap;
  gap: 2px 10px;
  font-size: 12px;
  color: var(--xm-text-secondary);
}

/* ---------- 名单 ---------- */
.roster-head {
  flex-wrap: wrap;
  align-items: center;
}

.roster-head__tools {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.roster-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 14px;
}

.roster-summary b {
  font-weight: 700;
}

.roster {
  list-style: none;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 8px;
}

.roster__item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius);
}

.roster__who {
  flex: 1;
  min-width: 0;
}

.roster__name {
  font-size: 14px;
  color: var(--xm-text-primary);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.roster__sub {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.roster__status {
  border: 0;
  cursor: pointer;
  font-family: inherit;
}

/* ---------- 学生签到 ---------- */
.checkin {
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 32px 24px;
  text-align: center;
}

.checkin.is-live {
  border-color: var(--xm-brand);
  box-shadow: 0 0 0 3px var(--xm-brand-soft);
}

.checkin__title {
  font-size: 20px;
  font-weight: 700;
  color: var(--xm-text-primary);
}

.checkin__title--idle {
  font-size: 16px;
  color: var(--xm-text-regular);
}

.checkin__sub {
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.checkin__warn {
  font-size: 14px;
  color: var(--xm-bad);
}

.checkin__form {
  display: flex;
  gap: 10px;
  margin-top: 6px;
}

.checkin__input {
  width: 168px;
}

.checkin__input :deep(.el-input__wrapper) {
  height: 48px;
}

.checkin__input :deep(.el-input__inner) {
  font-family: var(--xm-font-num);
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 0.5em;
  text-align: center;
  text-indent: 0.5em;
}

.checkin__done {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--xm-ok-soft);
  color: var(--xm-ok);
  font-size: 28px;
  font-weight: 700;
}

.records {
  list-style: none;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 8px;
}

.records__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius);
  font-size: 13px;
  color: var(--xm-text-primary);
}

@media (max-width: 860px) {
  .att-top {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 480px) {
  .session__digit {
    width: 52px;
    height: 64px;
    font-size: 36px;
  }

  .session__stats {
    gap: 24px;
  }
}
</style>

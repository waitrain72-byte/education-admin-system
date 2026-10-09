<template>
  <!-- 课程空间 · 签到（与 Web 端 views/course/CourseAttendance.vue 一致）：
       老师发起 4 位签到码签到、看实时人数、按日期改考勤；学生输入签到码、看自己的考勤 -->
  <view>
    <view
      v-if="!panel && loading"
      class="xm-card"
    >
      <view class="skeleton skeleton-title" />
      <view class="skeleton skeleton-line" />
      <view class="skeleton skeleton-line short" />
    </view>

    <!-- ==================== 老师 / 管理员 ==================== -->
    <template v-else-if="panel && canTeach">
      <view
        class="xm-card session"
        :class="{ 'is-live': active }"
      >
        <template v-if="active">
          <view class="session-label">{{ $t('space.attendance.codeLabel') }}</view>
          <view class="session-code">
            <text
              v-for="(digit, i) in active.code.split('')"
              :key="i"
              class="session-digit xm-num"
              >{{ digit }}</text
            >
          </view>
          <view class="session-hint">{{ $t('space.attendance.codeHint') }}</view>
          <view class="session-stats">
            <view class="session-stat">
              <text class="session-stat-label">{{ $t('space.attendance.remaining') }}</text>
              <text
                class="session-stat-value xm-num"
                :class="{ 'is-urgent': remaining <= 30 }"
                >{{ remaining > 0 ? formatCountdown(remaining) : $t('space.attendance.expired') }}</text
              >
            </view>
            <view class="session-stat">
              <text class="session-stat-label">{{ $t('space.attendance.signedCount') }}</text>
              <text class="session-stat-value xm-num">{{ active.signed }} / {{ active.total }}</text>
            </view>
          </view>
          <view class="session-progress">
            <view
              class="session-progress-bar"
              :style="'width:' + signedPercent + '%'"
            />
          </view>
          <button
            class="xm-btn xm-btn-plain xm-btn-block"
            :loading="ending"
            :disabled="ending"
            @click="endSession"
          >
            {{ $t('space.attendance.end') }}
          </button>
        </template>
        <template v-else>
          <view class="session-title">{{ $t('space.attendance.start') }}</view>
          <view class="session-hint">{{ $t('space.attendance.startHint') }}</view>
          <view class="session-duration">
            <text class="session-duration-label">{{ $t('space.attendance.duration') }}</text>
            <view class="xm-seg">
              <view
                v-for="m in DURATIONS"
                :key="m"
                class="xm-seg-item"
                :class="{ on: minutes === m }"
                @click="minutes = m"
                >{{ $t('space.attendance.minutes', { n: m }) }}</view
              >
            </view>
          </view>
          <button
            class="xm-btn xm-btn-primary xm-btn-block xm-btn-lg"
            :loading="starting"
            :disabled="starting"
            @click="startSession"
          >
            {{ $t('space.attendance.start') }}
          </button>
        </template>
      </view>

      <view class="xm-card">
        <view class="xm-section-head">
          <text class="xm-section-title">{{ $t('space.attendance.roster') }}</text>
          <picker
            mode="date"
            :value="date"
            @change="onDatePicked"
          >
            <view class="date-pick xm-num">
              {{ date }}
              <xm-icon
                name="chevron-down"
                :size="26"
              />
            </view>
          </picker>
        </view>
        <view class="summary">
          <text
            v-for="s in ATTENDANCE_STATUSES"
            :key="s.value"
            class="xm-tag"
            :class="s.tag"
            >{{ $t('space.attendance.statuses.' + s.key) }} {{ rosterCounts[s.value] || 0 }}</text
          >
          <text class="xm-tag">{{ $t('space.attendance.noRecord') }} {{ rosterCounts.none || 0 }}</text>
        </view>
        <view class="hint">{{ $t('space.attendance.markHint') }}</view>
        <view
          v-if="!panel.roster.length"
          class="muted"
          >{{ $t('space.members.empty') }}</view
        >
        <view
          v-for="row in panel.roster"
          :key="row.studentId"
          class="roster"
        >
          <xm-user-avatar
            :name="row.name"
            :avatar="row.avatar"
            :size="68"
          />
          <view class="roster-who">
            <view class="roster-name">{{ row.name }}</view>
            <view class="roster-sub xm-num">
              {{ row.username }}
              <text
                v-if="row.onLeave && row.status !== '请假'"
                class="roster-leave"
                >{{ $t('leave.onLeave') }}</text
              >
            </view>
          </view>
          <view
            class="xm-tag roster-status"
            :class="statusTag(row.status)"
            @click="chooseStatus(row)"
          >
            {{ statusText(row.status) }} ▾
          </view>
        </view>
      </view>

      <view class="xm-card">
        <view class="xm-section-head">
          <text class="xm-section-title">{{ $t('space.attendance.history') }}</text>
        </view>
        <view
          v-if="!panel.history.length"
          class="muted"
          >{{ $t('space.attendance.historyEmpty') }}</view
        >
        <view
          v-for="day in panel.history"
          :key="day.date"
          class="history"
          :class="{ 'is-current': day.date === date }"
          @click="pickDate(day.date)"
        >
          <text class="history-date xm-num">{{ day.date }}</text>
          <view class="history-bar">
            <view
              v-for="s in ATTENDANCE_STATUSES"
              :key="s.value"
              class="history-seg"
              :class="'history-seg-' + s.key"
              :style="'flex-grow:' + (day[s.value] || 0)"
            />
          </view>
          <view class="history-counts">
            <template
              v-for="s in ATTENDANCE_STATUSES"
              :key="s.value"
            >
              <text v-if="day[s.value]">{{ $t('space.attendance.statuses.' + s.key) }} {{ day[s.value] }}</text>
            </template>
          </view>
        </view>
      </view>
    </template>

    <!-- ==================== 学生 ==================== -->
    <template v-else-if="panel">
      <view
        class="xm-card checkin"
        :class="{ 'is-live': active && !active.signed }"
      >
        <template v-if="active && active.signed">
          <view class="checkin-done">✓</view>
          <view class="checkin-title">{{ $t('space.attendance.student.done') }}</view>
        </template>
        <template v-else-if="active && active.locked">
          <view class="checkin-title">{{ $t('space.attendance.student.title') }}</view>
          <view class="checkin-warn">{{ $t('space.attendance.student.locked') }}</view>
        </template>
        <template v-else-if="active">
          <view class="checkin-title">{{ $t('space.attendance.student.open') }}</view>
          <view class="checkin-sub">
            {{ $t('space.attendance.student.closesAt', { time: (active.expireTime || '').slice(11, 16) }) }} ·
            <text class="xm-num">{{ formatCountdown(remaining) }}</text>
          </view>
          <input
            class="checkin-input xm-num"
            v-model="code"
            type="number"
            maxlength="4"
            placeholder="0000"
            confirm-type="done"
            @input="onCodeInput"
            @confirm="checkin"
          />
          <button
            class="xm-btn xm-btn-primary xm-btn-block xm-btn-lg"
            :loading="checking"
            :disabled="code.length !== 4 || checking"
            @click="checkin"
          >
            {{ $t('space.attendance.student.submit') }}
          </button>
          <view class="checkin-sub">{{ $t('space.attendance.student.enterCode') }}</view>
        </template>
        <template v-else>
          <view class="checkin-title is-idle">{{ $t('space.attendance.student.idle') }}</view>
          <view class="checkin-sub">{{ $t('space.attendance.student.idleHint') }}</view>
        </template>
      </view>

      <view class="xm-card">
        <view class="xm-section-head">
          <text class="xm-section-title">{{ $t('space.attendance.student.myRecords') }}</text>
          <text
            v-if="myRate != null"
            class="xm-label"
            >{{ $t('space.attendance.student.rate') }} <text class="rate xm-num">{{ myRate }}%</text></text
          >
        </view>
        <view class="summary">
          <text
            v-for="s in ATTENDANCE_STATUSES"
            :key="s.value"
            class="xm-tag"
            :class="s.tag"
            >{{ $t('space.attendance.statuses.' + s.key) }} {{ panel.counts[s.value] || 0 }}</text
          >
        </view>
        <view
          v-if="!panel.records.length"
          class="muted"
          >{{ $t('space.attendance.student.empty') }}</view
        >
        <view
          v-for="r in panel.records"
          :key="r.id"
          class="record"
        >
          <text class="xm-num">{{ r.time }}</text>
          <text
            class="xm-tag"
            :class="statusTag(r.status)"
            >{{ statusText(r.status) }}</text
          >
        </view>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, onUnmounted, ref, watch } from 'vue'
import { checkinApi } from '@/api'
import { SILENT } from '@/utils/request'
import { ATTENDANCE_STATUSES, attendanceMeta, attendanceRateOf, formatCountdown, wallMs } from '@/utils/courseSpace'
import { useCourseEvents } from '@/composables/useCourseEvents'
import { confirm } from '@/utils/confirm'
import { t } from '@/i18n'

const props = defineProps({
  overview: { type: Object, required: true },
  courseId: { type: Number, required: true },
})

const emit = defineEmits(['refresh'])

const DURATIONS = [2, 5, 10, 15]

const relation = computed(() => props.overview.relation || 'visitor')
const canTeach = computed(() => relation.value === 'teacher' || relation.value === 'admin')

const panel = ref(null)
const loading = ref(false)
const date = ref('')
const minutes = ref(5)
const starting = ref(false)
const ending = ref(false)
const code = ref('')
const checking = ref(false)

const active = computed(() => (panel.value && panel.value.active) || null)

// ---------- 倒计时：按「服务器时间 + 本地流逝」推算，与手机时区无关 ----------
const fetchedAt = ref(0)
const serverAtFetch = ref(NaN)
const tickNow = ref(Date.now())
const ticker = setInterval(() => {
  tickNow.value = Date.now()
}, 1000)
onUnmounted(() => clearInterval(ticker))

const remaining = computed(() => {
  if (!active.value || Number.isNaN(serverAtFetch.value)) return 0
  const nowServer = serverAtFetch.value + (tickNow.value - fetchedAt.value)
  return Math.max(0, Math.round((wallMs(active.value.expireTime) - nowServer) / 1000))
})

const signedPercent = computed(() => {
  const total = Number(active.value && active.value.total) || 0
  return total ? Math.min(100, Math.round((Number(active.value.signed) / total) * 100)) : 0
})

const loadPanel = async () => {
  loading.value = true
  try {
    const data = await checkinApi.view(props.courseId, date.value ? { date: date.value } : undefined, SILENT)
    panel.value = data
    fetchedAt.value = Date.now()
    tickNow.value = fetchedAt.value
    serverAtFetch.value = wallMs(data.now)
    if (data.date) date.value = data.date
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    loading.value = false
  }
}

watch(
  () => props.courseId,
  () => {
    date.value = ''
    loadPanel()
  },
  { immediate: true },
)

useCourseEvents(() => props.courseId, ['attendance'], loadPanel)

/** 倒计时归零：老师端直接收尾（后端幂等），学生端刷新看结果 */
let expiring = false
watch(remaining, async (value, old) => {
  if (!active.value || value > 0 || old === 0 || expiring) return
  expiring = true
  try {
    if (canTeach.value) {
      await checkinApi.finish(props.courseId, active.value.id, SILENT)
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
    await checkinApi.start(props.courseId, { minutes: minutes.value })
    date.value = ''
    await loadPanel()
    emit('refresh')
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    starting.value = false
  }
}

const endSession = async () => {
  if (!active.value) return
  if (!(await confirm(t('space.attendance.endConfirm'), { title: t('space.attendance.end') }))) return
  ending.value = true
  try {
    const result = await checkinApi.finish(props.courseId, active.value.id)
    uni.showToast({ title: t('space.attendance.ended', { n: (result && result.marked) || 0 }), icon: 'none' })
    await loadPanel()
    emit('refresh')
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    ending.value = false
  }
}

const pickDate = (day) => {
  date.value = day
  loadPanel()
}
const onDatePicked = (e) => pickDate(e.detail.value)

const rosterCounts = computed(() => {
  const counts = {}
  for (const row of (panel.value && panel.value.roster) || []) {
    const key = row.status || 'none'
    counts[key] = (counts[key] || 0) + 1
  }
  return counts
})

const statusText = (status) => {
  if (!status) return t('space.attendance.noRecord')
  const meta = attendanceMeta(status)
  return meta ? t('space.attendance.statuses.' + meta.key) : status
}
const statusTag = (status) => {
  const meta = attendanceMeta(status)
  return meta ? meta.tag : ''
}

/** 点状态改考勤：底部菜单列出五种状态 */
const chooseStatus = (row) => {
  uni.showActionSheet({
    itemList: ATTENDANCE_STATUSES.map((s) => t('space.attendance.statuses.' + s.key)),
    success: (res) => {
      const status = ATTENDANCE_STATUSES[res.tapIndex].value
      if (status !== row.status) mark(row, status)
    },
    fail: () => {},
  })
}

const mark = async (row, status) => {
  try {
    await checkinApi.saveRecords(props.courseId, { studentId: row.studentId, time: date.value, status })
    row.status = status
    row.bySession = false
    uni.showToast({ title: t('space.attendance.marked', { status: statusText(status) }), icon: 'none' })
    loadPanel()
  } catch {
    // 提示已由请求层统一弹出
  }
}

// ---------- 学生签到 ----------
const myRate = computed(() => attendanceRateOf(panel.value && panel.value.counts))

const onCodeInput = (e) => {
  code.value = String(e.detail.value || '')
    .replace(/\D/g, '')
    .slice(0, 4)
}

const checkin = async () => {
  if (code.value.length !== 4 || checking.value) return
  checking.value = true
  try {
    await checkinApi.checkin(props.courseId, code.value)
    uni.showToast({ title: t('space.attendance.student.success'), icon: 'success' })
    code.value = ''
    await loadPanel()
    emit('refresh')
  } catch {
    // 签到码错误等提示已由请求层统一弹出；锁定状态要刷新才看得到
    code.value = ''
    await loadPanel()
  } finally {
    checking.value = false
  }
}

defineExpose({ reload: loadPanel })
</script>

<style lang="scss" scoped>
.session {
  text-align: center;
}

.session.is-live {
  border-color: var(--xm-brand);
}

.session-label,
.session-title {
  font-size: 30rpx;
  font-weight: bold;
  color: var(--xm-text);
}

.session-code {
  display: flex;
  justify-content: center;
  gap: 16rpx;
  margin: 24rpx 0 12rpx;
}

.session-digit {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 104rpx;
  height: 128rpx;
  border-radius: 20rpx;
  background: var(--xm-brand-soft);
  color: var(--xm-brand);
  font-size: 72rpx;
  font-weight: bold;
}

.session-hint {
  margin: 8rpx 0 24rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: var(--xm-text-2);
}

.session-stats {
  display: flex;
  justify-content: space-around;
  margin-bottom: 20rpx;
}

.session-stat {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.session-stat-label {
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.session-stat-value {
  font-size: 40rpx;
  font-weight: bold;
  color: var(--xm-text);
}

.session-stat-value.is-urgent {
  color: var(--xm-danger);
}

.session-progress {
  height: 12rpx;
  margin-bottom: 28rpx;
  border-radius: 999rpx;
  background: var(--xm-bg-sunken);
  overflow: hidden;
}

.session-progress-bar {
  height: 100%;
  border-radius: 999rpx;
  background: var(--xm-brand);
  transition: width 0.4s ease;
}

.session-duration {
  margin-bottom: 28rpx;
  text-align: left;
}

.session-duration-label {
  display: block;
  margin-bottom: 12rpx;
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.date-pick {
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 8rpx 18rpx;
  border: 1rpx solid var(--xm-border);
  border-radius: 12rpx;
  font-size: 24rpx;
  color: var(--xm-text);
}

.summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
}

.hint {
  margin: 14rpx 0 6rpx;
  font-size: 22rpx;
  color: var(--xm-text-3);
}

.muted {
  padding: 12rpx 0;
  font-size: 26rpx;
  color: var(--xm-text-2);
}

.roster {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 16rpx 0;
}

.roster + .roster {
  border-top: 1rpx solid var(--xm-border);
}

.roster-who {
  flex: 1;
  min-width: 0;
}

.roster-name {
  font-size: 28rpx;
  color: var(--xm-text);
}

.roster-sub {
  margin-top: 2rpx;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.roster-leave {
  margin-left: 10rpx;
  color: var(--xm-info);
}

.roster-status {
  flex-shrink: 0;
  padding: 8rpx 20rpx;
  font-size: 24rpx;
}

.history {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  padding: 16rpx 12rpx;
  margin: 0 -12rpx;
  border-radius: 14rpx;
}

.history.is-current {
  background: var(--xm-brand-soft);
}

.history-date {
  font-size: 26rpx;
  font-weight: 600;
  color: var(--xm-text);
}

.history-bar {
  display: flex;
  height: 12rpx;
  border-radius: 999rpx;
  overflow: hidden;
  background: var(--xm-bg-sunken);
}

.history-seg {
  flex-basis: 0;
}

.history-seg-normal {
  background: var(--xm-success);
}

.history-seg-late,
.history-seg-early {
  background: var(--xm-warning);
}

.history-seg-absent {
  background: var(--xm-danger);
}

.history-seg-leave {
  background: var(--xm-info);
}

.history-counts {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.checkin {
  text-align: center;
}

.checkin.is-live {
  border-color: var(--xm-danger);
}

.checkin-done {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 112rpx;
  height: 112rpx;
  margin: 8rpx auto 16rpx;
  border-radius: 50%;
  background: var(--xm-success-soft);
  color: var(--xm-success);
  font-size: 60rpx;
  font-weight: bold;
}

.checkin-title {
  font-size: 32rpx;
  font-weight: bold;
  color: var(--xm-text);
}

.checkin-title.is-idle {
  color: var(--xm-text-2);
}

.checkin-sub {
  margin: 12rpx 0;
  font-size: 24rpx;
  line-height: 1.6;
  color: var(--xm-text-2);
}

.checkin-warn {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: var(--xm-danger);
}

.checkin-input {
  height: 120rpx;
  margin: 20rpx 0;
  border: 2rpx solid var(--xm-border);
  border-radius: 20rpx;
  background: var(--xm-bg-input);
  font-size: 64rpx;
  font-weight: bold;
  letter-spacing: 32rpx;
  text-align: center;
  color: var(--xm-text);
}

.rate {
  font-weight: bold;
  color: var(--xm-text);
}

.record {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14rpx 0;
  font-size: 26rpx;
  color: var(--xm-text);
}

.record + .record {
  border-top: 1rpx solid var(--xm-border);
}
</style>

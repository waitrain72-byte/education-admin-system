<template>
  <view
    class="xm-page xm-page-narrow"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 开课 / 编辑课程（与 Web 端 views/admin/CourseWizard.vue 一致）：
         基本信息 → 时间与教室（只列出这个时段空着、坐得下的教室）→ 确认 -->
    <view class="steps">
      <view
        v-for="(key, i) in STEPS"
        :key="key"
        class="step"
        :class="{ on: step === i, done: step > i }"
      >
        <text class="step-no xm-num">{{ step > i ? '✓' : i + 1 }}</text>
        <text class="step-label">{{ $t('admin.wizard.steps.' + key) }}</text>
      </view>
    </view>

    <!-- 第一步：基本信息 -->
    <view
      v-show="step === 0"
      class="xm-card"
    >
      <view class="xm-form-item">
        <view class="xm-form-label required">{{ $t('admin.wizard.name') }}</view>
        <input
          class="xm-input"
          v-model="form.name"
          maxlength="50"
          :placeholder="$t('admin.wizard.namePlaceholder')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('admin.wizard.type') }}</view>
        <view class="xm-seg">
          <view
            v-for="type in TYPES"
            :key="type"
            class="xm-seg-item"
            :class="{ on: form.type === type }"
            @click="form.type = type"
            >{{ courseTypeLabel(type) }}</view
          >
        </view>
      </view>
      <view class="row2">
        <view class="xm-form-item">
          <view class="xm-form-label">{{ $t('admin.wizard.credit') }}</view>
          <input
            class="xm-input"
            v-model="form.score"
            type="number"
            maxlength="2"
          />
        </view>
        <view class="xm-form-item">
          <view class="xm-form-label">{{ $t('admin.wizard.capacity') }}</view>
          <input
            class="xm-input"
            v-model="form.num"
            type="number"
            maxlength="3"
          />
        </view>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label required">{{ $t('admin.wizard.teacher') }}</view>
        <picker
          mode="selector"
          :range="teacherRange"
          :value="teacherIndex"
          @change="onTeacherPicked"
        >
          <view
            class="xm-select"
            :class="{ placeholder: teacherIndex < 0 }"
          >
            <text>{{ teacherIndex < 0 ? $t('admin.wizard.teacherPlaceholder') : teacherRange[teacherIndex] }}</text>
            <xm-icon
              name="chevron-down"
              :size="28"
            />
          </view>
        </picker>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('admin.wizard.intro') }}</view>
        <textarea
          class="xm-textarea"
          v-model="form.intro"
          maxlength="500"
          :placeholder="$t('admin.wizard.introPlaceholder')"
          :show-confirm-bar="false"
        />
      </view>
    </view>

    <!-- 第二步：时间与教室 -->
    <view
      v-show="step === 1"
      class="xm-card"
    >
      <view
        class="check"
        @click="toggleUnscheduled"
      >
        <view
          class="check-box"
          :class="{ on: unscheduled }"
        >
          <xm-icon
            v-if="unscheduled"
            name="check"
            :size="24"
          />
        </view>
        <text>{{ $t('admin.wizard.noSchedule') }}</text>
      </view>
      <template v-if="!unscheduled">
        <view class="row2">
          <view class="xm-form-item">
            <view class="xm-form-label">{{ $t('admin.wizard.week') }}</view>
            <picker
              mode="selector"
              :range="weekRange"
              :value="weekIndexOf"
              @change="onWeekPicked"
            >
              <view
                class="xm-select"
                :class="{ placeholder: !form.week }"
              >
                <text>{{ form.week ? weekLabel(form.week, true) : '—' }}</text>
                <xm-icon
                  name="chevron-down"
                  :size="28"
                />
              </view>
            </picker>
          </view>
          <view class="xm-form-item">
            <view class="xm-form-label">{{ $t('admin.wizard.segment') }}</view>
            <picker
              mode="selector"
              :range="segmentRange"
              :value="segmentIndexOf"
              @change="onSegmentPicked"
            >
              <view
                class="xm-select"
                :class="{ placeholder: !form.segment }"
              >
                <text class="xm-ellipsis">{{ form.segment ? segmentLabel(form.segment) : '—' }}</text>
                <xm-icon
                  name="chevron-down"
                  :size="28"
                />
              </view>
            </picker>
          </view>
        </view>
        <view class="xm-form-label">{{ $t('admin.wizard.room') }}</view>
        <view class="hint">{{ $t('admin.wizard.roomHint') }}</view>
        <view
          v-if="roomsLoading"
          class="skeleton skeleton-line"
        />
        <view
          v-else-if="form.week && form.segment && !rooms.length"
          class="warn"
          >{{ $t('admin.wizard.noRoom') }}</view
        >
        <view
          v-else
          class="rooms"
        >
          <view
            v-for="r in rooms"
            :key="r.code"
            class="room"
            :class="{ on: form.room === r.code }"
            @click="form.room = r.code"
          >
            <text class="room-code xm-num">{{ r.code }}</text>
            <text class="room-name">{{ r.type === '运动场馆' ? r.name : r.content || r.name }}</text>
            <text class="room-seats xm-num">{{ $t('admin.wizard.seats', { n: r.num }) }}</text>
          </view>
        </view>
      </template>
    </view>

    <!-- 第三步：确认 -->
    <view
      v-show="step === 2"
      class="xm-card"
    >
      <view class="xm-kv">
        <text class="xm-kv-key">{{ $t('admin.wizard.name') }}</text>
        <text class="xm-kv-value">{{ form.name }}</text>
      </view>
      <view class="xm-kv">
        <text class="xm-kv-key">{{ $t('admin.wizard.type') }}</text>
        <text class="xm-kv-value">{{ courseTypeLabel(form.type) }}</text>
      </view>
      <view class="xm-kv">
        <text class="xm-kv-key">{{ $t('admin.wizard.credit') }}</text>
        <text class="xm-kv-value xm-num">{{ form.score }}</text>
      </view>
      <view class="xm-kv">
        <text class="xm-kv-key">{{ $t('admin.wizard.capacity') }}</text>
        <text class="xm-kv-value xm-num">{{ form.num }}</text>
      </view>
      <view class="xm-kv">
        <text class="xm-kv-key">{{ $t('admin.wizard.teacher') }}</text>
        <text class="xm-kv-value">{{ teacherName }}</text>
      </view>
      <view class="xm-kv">
        <text class="xm-kv-key">{{ $t('admin.courses.time') }}</text>
        <text class="xm-kv-value">{{
          unscheduled || !form.week
            ? $t('courses.unscheduled')
            : weekLabel(form.week, true) + ' ' + segmentLabel(form.segment)
        }}</text>
      </view>
      <view class="xm-kv">
        <text class="xm-kv-key">{{ $t('admin.courses.room') }}</text>
        <text class="xm-kv-value">{{ (!unscheduled && form.room) || '—' }}</text>
      </view>
      <view class="xm-form-item status">
        <view class="xm-form-label">{{ $t('admin.wizard.status') }}</view>
        <view class="xm-seg">
          <view
            v-for="s in STATUSES"
            :key="s"
            class="xm-seg-item"
            :class="{ on: form.status === s }"
            @click="form.status = s"
            >{{ courseStatusLabel(s) }}</view
          >
        </view>
      </view>
    </view>

    <view class="footer">
      <button
        class="xm-btn xm-btn-plain"
        style="flex: 1"
        @click="back"
      >
        {{ step > 0 ? $t('admin.wizard.prev') : $t('common.cancel') }}
      </button>
      <button
        v-if="step < 2"
        class="xm-btn xm-btn-primary"
        style="flex: 1"
        @click="next"
      >
        {{ $t('admin.wizard.next') }}
      </button>
      <button
        v-else
        class="xm-btn xm-btn-primary"
        style="flex: 1"
        :loading="saving"
        :disabled="saving"
        @click="save"
      >
        {{ editingId ? $t('admin.wizard.save') : $t('admin.wizard.create') }}
      </button>
    </view>
    <xm-loader />
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { courseApi, teacherApi } from '@/api'
import { SILENT } from '@/utils/request'
import { ensureRole } from '@/utils/authGuard'
import { SEGMENTS, WEEKDAYS, parseSegmentTime } from '@/utils/schedule'
import { courseStatusLabel, courseTypeLabel, segmentLabel, weekLabel } from '@/utils/courseText'
import { t } from '@/i18n'

const STEPS = ['basic', 'schedule', 'confirm']
const TYPES = ['必修', '选修']
const STATUSES = ['未开课', '已开课', '已结课']

const editingId = ref(0)
const step = ref(0)
const saving = ref(false)
const unscheduled = ref(false)
const teachers = ref([])
const rooms = ref([])
const roomsLoading = ref(false)

const form = reactive({
  name: '',
  type: '必修',
  score: '2',
  num: '60',
  teacherId: null,
  intro: '',
  week: '',
  segment: '',
  room: '',
  status: '未开课',
})

const teacherRange = computed(() => teachers.value.map((x) => (x.title ? `${x.name}（${x.title}）` : x.name)))
const teacherIndex = computed(() => teachers.value.findIndex((x) => x.id === form.teacherId))
const teacherName = computed(() => (teachers.value.find((x) => x.id === form.teacherId) || {}).name || '—')
const onTeacherPicked = (e) => {
  form.teacherId = teachers.value[Number(e.detail.value)].id
}

const weekRange = computed(() => WEEKDAYS.map((w) => weekLabel(w, true)))
const weekIndexOf = computed(() => Math.max(0, WEEKDAYS.indexOf(form.week)))
const segmentRange = computed(() =>
  SEGMENTS.map((s) => {
    const [start, end] = parseSegmentTime(s)
    return `${segmentLabel(s)} ${start}–${end}`
  }),
)
const segmentIndexOf = computed(() => Math.max(0, SEGMENTS.indexOf(form.segment)))

const onWeekPicked = (e) => {
  form.week = WEEKDAYS[Number(e.detail.value)]
  loadRooms()
}
const onSegmentPicked = (e) => {
  form.segment = SEGMENTS[Number(e.detail.value)]
  loadRooms()
}

const toggleUnscheduled = () => {
  unscheduled.value = !unscheduled.value
  if (!unscheduled.value) loadRooms()
}

const intOf = (text, min, max, fallback) => {
  const n = Math.round(Number(text))
  return Number.isFinite(n) && n >= min && n <= max ? n : fallback
}

/** 这个时段空着、坐得下的教室；体育课优先找运动场馆 */
const loadRooms = async () => {
  if (!form.week || !form.segment) {
    rooms.value = []
    return
  }
  roomsLoading.value = true
  const base = {
    week: form.week,
    segment: form.segment,
    num: intOf(form.num, 1, 500, 0) || undefined,
    excludeId: editingId.value || undefined,
  }
  try {
    let list = []
    if (form.name.includes('体育')) list = (await courseApi.roomFree({ ...base, typeFilter: '运动场馆' }, SILENT)) || []
    if (!list.length) list = (await courseApi.roomFree(base, SILENT)) || []
    rooms.value = list
    // 原来的教室还空着就保留，否则默认选最贴近人数的那间
    if (!list.some((r) => r.code === form.room)) form.room = (list[0] && list[0].code) || ''
  } catch {
    rooms.value = []
  } finally {
    roomsLoading.value = false
  }
}

const next = () => {
  if (step.value === 0) {
    if (!form.name.trim()) {
      uni.showToast({ title: t('admin.wizard.nameRequired'), icon: 'none' })
      return
    }
    if (!form.teacherId) {
      uni.showToast({ title: t('admin.wizard.teacherRequired'), icon: 'none' })
      return
    }
    step.value = 1
    if (!unscheduled.value) loadRooms()
    return
  }
  if (step.value === 1 && !unscheduled.value && (!form.week || !form.segment || !form.room)) {
    uni.showToast({ title: t('admin.wizard.roomRequired'), icon: 'none' })
    return
  }
  step.value = 2
}

const back = () => {
  if (step.value > 0) step.value -= 1
  else uni.navigateBack()
}

const save = async () => {
  const body = {
    name: form.name.trim(),
    type: form.type,
    score: intOf(form.score, 1, 10, 2),
    num: intOf(form.num, 1, 500, 60),
    teacherId: form.teacherId,
    intro: form.intro,
    status: form.status,
    week: unscheduled.value ? '' : form.week,
    segment: unscheduled.value ? '' : form.segment,
    room: unscheduled.value ? '' : form.room,
  }
  saving.value = true
  try {
    if (editingId.value) {
      await courseApi.update({ ...body, id: editingId.value })
      uni.showToast({ title: t('admin.wizard.saved'), icon: 'success' })
    } else {
      await courseApi.add(body)
      uni.showToast({ title: t('admin.wizard.created', { name: body.name }), icon: 'none' })
    }
    setTimeout(() => uni.navigateBack(), 700)
  } catch {
    // 教室被占用等提示已由请求层统一弹出
    saving.value = false
  }
}

const load = async () => {
  try {
    teachers.value = (await teacherApi.selectAll(undefined, SILENT)) || []
    if (!editingId.value) return
    const c = await courseApi.selectById(editingId.value)
    if (!c) return
    Object.assign(form, {
      name: c.name || '',
      type: c.type || '必修',
      score: String(c.score == null ? 2 : c.score),
      num: String(c.num == null ? 60 : c.num),
      teacherId: c.teacherId == null ? null : c.teacherId,
      intro: c.intro || '',
      week: c.week || '',
      segment: c.segment || '',
      room: c.room || '',
      status: c.status || '未开课',
    })
    unscheduled.value = !c.week
  } catch {
    // 提示已由请求层统一弹出
  }
}

onLoad((query) => {
  editingId.value = Number(query.id) || 0
})

let shownOnce = false
onShow(() => {
  if (!ensureRole(['ADMIN'])) return
  uni.setNavigationBarTitle({ title: editingId.value ? t('admin.wizard.editTitle') : t('admin.wizard.createTitle') })
  if (shownOnce) return
  shownOnce = true
  load()
})
</script>

<style lang="scss" scoped>
.steps {
  display: flex;
  gap: 12rpx;
  margin-bottom: 24rpx;
}

.step {
  flex: 1 1 0;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 14rpx 12rpx;
  border-radius: 16rpx;
  background: var(--xm-bg-card);
  color: var(--xm-text-2);
  font-size: 24rpx;
  box-sizing: border-box;
}

.step-label {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.step.on {
  background: var(--xm-brand-soft);
  color: var(--xm-brand);
  font-weight: 600;
}

.step-no {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  border: 2rpx solid currentColor;
  font-size: 22rpx;
  flex-shrink: 0;
}

.step.done {
  color: var(--xm-success);
}

.row2 {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20rpx;
}

.check {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-bottom: 24rpx;
  font-size: 26rpx;
  color: var(--xm-text);
}

.check-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36rpx;
  height: 36rpx;
  border: 2rpx solid var(--xm-border);
  border-radius: 8rpx;
  color: var(--xm-on-brand);
  flex-shrink: 0;
}

.check-box.on {
  border-color: var(--xm-brand);
  background: var(--xm-brand);
}

.hint {
  margin: -4rpx 0 14rpx;
  font-size: 22rpx;
  color: var(--xm-text-3);
}

.warn {
  padding: 16rpx 0;
  font-size: 26rpx;
  color: var(--xm-danger);
}

.rooms {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14rpx;
}

.room {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
  padding: 16rpx 18rpx;
  border: 2rpx solid var(--xm-border);
  border-radius: 16rpx;
  background: var(--xm-bg-card);
}

.room.on {
  border-color: var(--xm-brand);
  background: var(--xm-brand-soft);
}

.room-code {
  font-size: 28rpx;
  font-weight: bold;
  color: var(--xm-text);
}

.room-name {
  font-size: 22rpx;
  color: var(--xm-text-2);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.room-seats {
  font-size: 22rpx;
  color: var(--xm-brand);
}

.status {
  margin-top: 24rpx;
}

.footer {
  display: flex;
  gap: 20rpx;
  padding-bottom: 24rpx;
}
</style>

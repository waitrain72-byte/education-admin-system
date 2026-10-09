<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 教室（与 Web 端 views/admin/RoomsAdmin.vue 一致）：教室与运动场馆，点一间看它每周的占用 -->
    <view class="sub">{{ $t('admin.rooms.sub') }}</view>
    <view class="filters">
      <xm-search
        v-model="keyword"
        :placeholder="$t('admin.rooms.search')"
      />
      <xm-chips
        v-model="typeFilter"
        :options="typeOptions"
      />
    </view>

    <view
      v-if="loading && !rooms.length"
      class="xm-card"
    >
      <view class="skeleton skeleton-line" />
      <view class="skeleton skeleton-line short" />
    </view>
    <xm-empty
      v-else-if="!shown.length"
      icon="building"
      :text="$t('admin.rooms.empty')"
    />
    <view
      v-else
      class="xm-list"
    >
      <view
        v-for="r in shown"
        :key="r.id"
        class="xm-card room"
        @click="selected = r"
      >
        <view class="room-top">
          <text class="room-code xm-num">{{ r.code }}</text>
          <text
            class="xm-tag"
            :class="TYPE_TAG[r.type] || 'xm-tag-info'"
            >{{ typeLabel(r.type) }}</text
          >
        </view>
        <view class="room-name">{{ r.name }}</view>
        <view class="room-meta">
          <text class="xm-num">{{ $t('admin.rooms.seatsUnit', { n: r.num == null ? 0 : r.num }) }}</text>
          <text class="xm-num">{{ $t('admin.rooms.used', { n: bookedOf(r.code).length }) }}</text>
        </view>
        <view class="room-bar">
          <view
            class="room-bar-fill"
            :style="'width:' + usage(r.code) + '%'"
          />
        </view>
      </view>
    </view>

    <view
      class="xm-fab"
      @click="openForm(null)"
    >
      <xm-icon
        name="plus"
        :size="48"
      />
    </view>

    <!-- 教室详情：每周占用 -->
    <view v-if="selected">
      <view
        class="xm-mask"
        @click="selected = null"
        @touchmove.stop.prevent="noop"
      />
      <view class="xm-popup detail">
        <view class="detail-title">
          <text class="xm-num">{{ selected.code }}</text>
          <text class="detail-name">{{ selected.name }}</text>
        </view>
        <view class="detail-sub">
          {{ typeLabel(selected.type) }} ·
          {{ $t('admin.rooms.seatsUnit', { n: selected.num == null ? 0 : selected.num }) }} ·
          {{ statusLabel(selected.status) }}
        </view>
        <view
          v-if="selected.content"
          class="detail-content"
          >{{ selected.content }}</view
        >
        <view class="xm-form-label week-title">{{ $t('admin.rooms.occupancy') }}</view>
        <view class="week">
          <view class="week-row">
            <view class="week-seg" />
            <view
              v-for="w in WEEKDAYS"
              :key="w"
              class="week-day"
              >{{ weekLabel(w) }}</view
            >
          </view>
          <view
            v-for="(s, si) in SEGMENTS"
            :key="s"
            class="week-row"
          >
            <view class="week-seg xm-num">{{ si + 1 }}</view>
            <view
              v-for="w in WEEKDAYS"
              :key="w + s"
              class="week-cell"
            >
              <view
                v-for="c in slot(selected.code, w, s)"
                :key="c.id"
                class="week-block"
                :class="{ 'is-pending': c.status !== '已开课', 'is-over': (c.num || 0) > (selected.num || 0) }"
                :style="'--cc:' + courseColor(c.name)"
                @click="openCourse(c)"
                >{{ c.name }}</view
              >
            </view>
          </view>
        </view>
        <view class="legend">{{ $t('admin.rooms.legend') }}</view>
        <view class="xm-actions">
          <button
            class="xm-btn xm-btn-danger"
            style="flex: 1"
            @click="remove(selected)"
          >
            {{ $t('admin.rooms.delete') }}
          </button>
          <button
            class="xm-btn xm-btn-primary"
            style="flex: 1"
            @click="openForm(selected)"
          >
            {{ $t('admin.rooms.edit') }}
          </button>
        </view>
      </view>
    </view>

    <!-- 新建 / 编辑 -->
    <xm-form-popup
      :visible="formOpen"
      :title="editing ? $t('admin.rooms.edit') : $t('admin.rooms.create')"
      :saving="saving"
      :confirm-text="$t('common.save')"
      @close="formOpen = false"
      @save="save"
    >
      <view class="row2">
        <view class="xm-form-item">
          <view class="xm-form-label required">{{ $t('admin.rooms.code') }}</view>
          <input
            class="xm-input"
            v-model="form.code"
            maxlength="20"
          />
        </view>
        <view class="xm-form-item">
          <view class="xm-form-label">{{ $t('admin.rooms.seats') }}</view>
          <input
            class="xm-input"
            v-model="form.num"
            type="number"
            maxlength="4"
          />
        </view>
      </view>
      <view
        v-if="renameCount"
        class="rename"
        >{{ $t('admin.rooms.renameNote', { n: renameCount }) }}</view
      >
      <view class="xm-form-item">
        <view class="xm-form-label required">{{ $t('admin.rooms.name') }}</view>
        <input
          class="xm-input"
          v-model="form.name"
          maxlength="50"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('admin.rooms.type') }}</view>
        <view class="xm-seg">
          <view
            v-for="type in TYPES"
            :key="type"
            class="xm-seg-item"
            :class="{ on: form.type === type }"
            @click="form.type = type"
            >{{ typeLabel(type) }}</view
          >
        </view>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('admin.rooms.status') }}</view>
        <view class="xm-seg">
          <view
            v-for="s in STATUSES"
            :key="s"
            class="xm-seg-item"
            :class="{ on: form.status === s }"
            @click="form.status = s"
            >{{ statusLabel(s) }}</view
          >
        </view>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('admin.rooms.content') }}</view>
        <textarea
          class="xm-textarea"
          v-model="form.content"
          maxlength="200"
          :placeholder="$t('admin.rooms.contentPlaceholder')"
          :show-confirm-bar="false"
        />
      </view>
    </xm-form-popup>
    <xm-loader />
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { courseApi, roomplanApi } from '@/api'
import { SILENT } from '@/utils/request'
import { ensureRole } from '@/utils/authGuard'
import { courseColor } from '@/utils/courseColor'
import { SEGMENTS, WEEKDAYS } from '@/utils/schedule'
import { weekLabel } from '@/utils/courseText'
import { confirm } from '@/utils/confirm'
import { t } from '@/i18n'

/** 数据库里按中文存的类型与状态；显示时翻译 */
const TYPES = ['授课教室', '运动场馆', '固定占用']
const STATUSES = ['空闲', '占用']
const TYPE_KEY = { 授课教室: 'classroom', 运动场馆: 'venue', 固定占用: 'reserved' }
const STATUS_KEY = { 空闲: 'free', 占用: 'busy' }
const TYPE_TAG = { 授课教室: 'xm-tag-brand', 运动场馆: 'xm-tag-success', 固定占用: 'xm-tag-warning' }
/** 一周可排的时段总数（7 天 × 5 个大节），算占用率用 */
const SLOT_TOTAL = WEEKDAYS.length * SEGMENTS.length

const typeLabel = (type) => (TYPE_KEY[type] ? t(`admin.rooms.types.${TYPE_KEY[type]}`) : type || '—')
const statusLabel = (status) => (STATUS_KEY[status] ? t(`admin.rooms.statuses.${STATUS_KEY[status]}`) : status || '—')

const rooms = ref([])
const courses = ref([])
const loading = ref(false)
const typeFilter = ref('')
const keyword = ref('')
const selected = ref(null)
const noop = () => {}

const typeOptions = computed(() =>
  ['', ...TYPES].map((type) => ({
    value: type,
    label: type ? typeLabel(type) : t('admin.rooms.all'),
    count: type ? rooms.value.filter((r) => r.type === type).length : rooms.value.length,
  })),
)

const shown = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  return rooms.value
    .filter((r) => !typeFilter.value || r.type === typeFilter.value)
    .filter(
      (r) =>
        !k ||
        [r.code, r.name, r.content].some((v) =>
          String(v || '')
            .toLowerCase()
            .includes(k),
        ),
    )
    .sort((a, b) => String(a.code || '').localeCompare(String(b.code || ''), undefined, { numeric: true }))
})

/** 每间教室排着的课（不含已结课：已结课不再占教室，和后端的占用判断一致） */
const byRoom = computed(() => {
  const map = {}
  for (const c of courses.value) {
    if (!c.room || !c.week || !c.segment || c.status === '已结课') continue
    if (!map[c.room]) map[c.room] = []
    map[c.room].push(c)
  }
  return map
})

const bookedOf = (code) => byRoom.value[code] || []
const usage = (code) => Math.min(100, Math.round((bookedOf(code).length / SLOT_TOTAL) * 100))
const slot = (code, week, segment) => bookedOf(code).filter((c) => c.week === week && c.segment === segment)

const load = async () => {
  loading.value = true
  try {
    const [roomList, courseList] = await Promise.all([
      roomplanApi.selectAll(undefined, SILENT),
      courseApi.selectAll(undefined, SILENT),
    ])
    rooms.value = roomList || []
    courses.value = courseList || []
    if (selected.value) selected.value = rooms.value.find((r) => r.id === selected.value.id) || null
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    loading.value = false
  }
}

const openCourse = (c) => uni.navigateTo({ url: `/pages-course/space/space?id=${c.id}` })

// ---------- 新建 / 编辑 ----------
const formOpen = ref(false)
const editing = ref(null)
const saving = ref(false)
const form = reactive({ code: '', name: '', type: '授课教室', status: '空闲', num: '60', content: '' })

/** 改了编号时，提示排在这间教室的课会一起改用新编号 */
const renameCount = computed(() => {
  if (!editing.value || form.code.trim() === editing.value.code) return 0
  return bookedOf(editing.value.code).length
})

const openForm = (room) => {
  editing.value = room
  Object.assign(form, {
    code: (room && room.code) || '',
    name: (room && room.name) || '',
    type: (room && room.type) || '授课教室',
    status: (room && room.status) || '空闲',
    num: String(room && room.num != null ? room.num : 60),
    content: (room && room.content) || '',
  })
  selected.value = null
  formOpen.value = true
}

const save = async () => {
  if (!form.code.trim()) {
    uni.showToast({ title: t('admin.rooms.codeRequired'), icon: 'none' })
    return
  }
  if (!form.name.trim()) {
    uni.showToast({ title: t('admin.rooms.nameRequired'), icon: 'none' })
    return
  }
  const num = Math.round(Number(form.num))
  const body = {
    code: form.code.trim(),
    name: form.name.trim(),
    type: form.type,
    status: form.status,
    num: Number.isFinite(num) && num > 0 ? num : 0,
    content: form.content.trim(),
  }
  saving.value = true
  try {
    if (editing.value) await roomplanApi.update({ ...body, id: editing.value.id })
    else await roomplanApi.add(body)
    uni.showToast({ title: t('admin.rooms.saved'), icon: 'success' })
    formOpen.value = false
    // 改了编号时课程上的教室也跟着改了，课程一起重新拉
    await load()
  } catch {
    // 编号重复等提示已由请求层统一弹出
  } finally {
    saving.value = false
  }
}

const remove = async (room) => {
  if (!(await confirm(t('admin.rooms.deleteConfirm', { name: `${room.code} ${room.name}` })))) return
  try {
    await roomplanApi.delete(room.id)
    uni.showToast({ title: t('admin.rooms.deleted'), icon: 'none' })
    selected.value = null
    await load()
  } catch {
    // 还有课排在这里的提示已由请求层统一弹出
  }
}

onShow(() => {
  if (!ensureRole(['ADMIN'])) return
  uni.setNavigationBarTitle({ title: t('admin.rooms.title') })
  load()
})

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
.sub {
  margin-bottom: 20rpx;
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.filters {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.room-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.room-code {
  font-size: 34rpx;
  font-weight: bold;
  color: var(--xm-text);
}

.room-name {
  margin-top: 6rpx;
  font-size: 26rpx;
  color: var(--xm-text-2);
}

.room-meta {
  display: flex;
  gap: 24rpx;
  margin-top: 10rpx;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.room-bar {
  height: 8rpx;
  margin-top: 12rpx;
  border-radius: 999rpx;
  background: var(--xm-bg-sunken);
  overflow: hidden;
}

.room-bar-fill {
  height: 100%;
  background: var(--xm-brand);
}

.detail-title {
  display: flex;
  align-items: baseline;
  gap: 16rpx;
  font-size: 36rpx;
  font-weight: bold;
  color: var(--xm-text);
}

.detail-name {
  font-size: 28rpx;
  font-weight: normal;
  color: var(--xm-text-2);
}

.detail-sub {
  margin-top: 6rpx;
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.detail-content {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: var(--xm-text);
}

.week-title {
  margin-top: 24rpx;
}

.week {
  border: 1rpx solid var(--xm-border);
  border-radius: 16rpx;
  overflow: hidden;
}

.week-row {
  display: flex;
}

.week-row + .week-row {
  border-top: 1rpx solid var(--xm-border);
}

.week-seg {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44rpx;
  flex-shrink: 0;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.week-day {
  flex: 1;
  padding: 10rpx 0;
  border-left: 1rpx solid var(--xm-border);
  font-size: 20rpx;
  text-align: center;
  color: var(--xm-text-2);
}

.week-cell {
  flex: 1;
  min-width: 0;
  min-height: 84rpx;
  padding: 4rpx;
  border-left: 1rpx solid var(--xm-border);
  box-sizing: border-box;
}

.week-block {
  padding: 6rpx;
  border-radius: 8rpx;
  background: var(--cc);
  color: #ffffff;
  font-size: 18rpx;
  line-height: 1.3;
  word-break: break-all;
}

.week-block.is-pending {
  border: 2rpx dashed var(--cc);
  background: transparent;
  color: var(--cc);
}

.week-block.is-over {
  box-shadow: 0 0 0 3rpx var(--xm-danger);
}

.legend {
  margin: 12rpx 0 20rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: var(--xm-text-3);
}

.row2 {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20rpx;
}

.rename {
  margin: -8rpx 0 20rpx;
  padding: 14rpx 18rpx;
  border-radius: 12rpx;
  background: var(--xm-warning-soft);
  font-size: 22rpx;
  color: var(--xm-warning);
}
</style>

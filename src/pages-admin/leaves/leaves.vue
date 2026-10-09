<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 请假审批（与 Web 端 views/admin/LeaveBoard.vue 一致）：待审核 / 已批准 / 未批准三栏，
         手机上一次看一栏；点一条看受影响的课，写审核意见后批准或驳回 -->
    <view class="sub">{{ $t('admin.leaves.sub') }}</view>
    <view class="filters">
      <xm-search
        v-model="keyword"
        :placeholder="$t('admin.leaves.search')"
      />
      <xm-chips
        v-model="column"
        :options="columnOptions"
      />
    </view>

    <view
      v-if="loading && !applies.length"
      class="xm-card"
    >
      <view class="skeleton skeleton-line" />
      <view class="skeleton skeleton-line short" />
    </view>
    <xm-empty
      v-else-if="!currentItems.length"
      icon="clipboard"
      :text="$t('admin.leaves.empty')"
    />
    <view
      v-else
      class="xm-list"
    >
      <view
        v-for="a in currentItems"
        :key="a.id"
        class="xm-card leave"
        @click="openReview(a)"
      >
        <view class="leave-who">
          <xm-user-avatar
            :name="a.studentName"
            :size="56"
          />
          <text class="leave-name">{{ a.studentName || '—' }}</text>
          <text class="leave-days xm-num">{{ $t('admin.leaves.days', { n: a.day || 1 }) }}</text>
        </view>
        <view class="leave-range xm-num">{{ rangeText(a) }}</view>
        <view class="leave-reason">{{ a.content }}</view>
        <view
          v-if="a.descr && column !== 'pending'"
          class="leave-note"
          >{{ $t('admin.leaves.reviewed', { text: a.descr }) }}</view
        >
      </view>
    </view>

    <!-- 审核 -->
    <view v-if="current">
      <view
        class="xm-mask"
        @click="current = null"
        @touchmove.stop.prevent="noop"
      />
      <view class="xm-popup review">
        <view class="xm-popup-title">{{ $t('admin.leaves.review') }}</view>
        <view class="review-who">
          <xm-user-avatar
            :name="current.studentName"
            :size="80"
          />
          <view>
            <view class="review-name">{{ current.studentName || '—' }}</view>
            <text
              class="xm-tag"
              :class="STATUS_TAG[statusKey(current.status)]"
              >{{ $t('admin.leaves.columns.' + statusKey(current.status)) }}</text
            >
          </view>
        </view>
        <view class="xm-kv">
          <text class="xm-kv-key">{{ $t('admin.leaves.range') }}</text>
          <text class="xm-kv-value xm-num"
            >{{ rangeText(current) }} · {{ $t('admin.leaves.days', { n: current.day || 1 }) }}</text
          >
        </view>
        <view class="xm-kv">
          <text class="xm-kv-key">{{ $t('admin.leaves.reason') }}</text>
          <text class="xm-kv-value">{{ current.content || '—' }}</text>
        </view>

        <view class="affected">
          <view class="xm-form-label">{{ $t('admin.leaves.affected', { n: affected.length }) }}</view>
          <view
            v-if="affectedLoading"
            class="skeleton skeleton-line"
          />
          <view
            v-else-if="!affected.length"
            class="muted"
            >{{ $t('admin.leaves.affectedEmpty') }}</view
          >
          <view
            v-for="c in affected"
            :key="c.date + '-' + c.courseId + '-' + c.start"
            class="affected-row"
          >
            <view
              class="affected-bar"
              :style="'background:' + courseColor(c.courseName)"
            />
            <text class="affected-when xm-num">{{ fmt(c.date) }} {{ c.start }}</text>
            <text class="affected-course">{{ c.courseName }}</text>
            <text class="affected-teacher">{{ c.teacherName }}</text>
          </view>
        </view>

        <view class="xm-form-item">
          <view class="xm-form-label">{{ $t('admin.leaves.note') }}</view>
          <textarea
            class="xm-textarea"
            v-model="note"
            maxlength="200"
            :placeholder="$t('admin.leaves.notePlaceholder')"
            :show-confirm-bar="false"
          />
        </view>
        <view class="xm-actions">
          <button
            class="xm-btn xm-btn-danger"
            style="flex: 1"
            :disabled="current.status === REJECTED || !!saving"
            :loading="saving === REJECTED"
            @click="decide(REJECTED)"
          >
            {{ $t('admin.leaves.reject') }}
          </button>
          <button
            class="xm-btn xm-btn-primary"
            style="flex: 1"
            :disabled="current.status === APPROVED || !!saving"
            :loading="saving === APPROVED"
            @click="decide(APPROVED)"
          >
            {{ $t('admin.leaves.approve') }}
          </button>
        </view>
      </view>
    </view>
    <xm-loader />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { applyApi } from '@/api'
import { SILENT } from '@/utils/request'
import { ensureRole } from '@/utils/authGuard'
import { courseColor } from '@/utils/courseColor'
import { addDays, formatDay } from '@/utils/schedule'
import { isZh, t } from '@/i18n'

// 待审核以外的两种状态；其余（含历史上的空状态）都算待审核
const APPROVED = '审核通过'
const REJECTED = '审核不通过'
const STATUS_TAG = { pending: 'xm-tag-warning', approved: 'xm-tag-success', rejected: 'xm-tag-danger' }
const statusKey = (status) => (status === APPROVED ? 'approved' : status === REJECTED ? 'rejected' : 'pending')

const applies = ref([])
const loading = ref(false)
const keyword = ref('')
const column = ref('pending')
const noop = () => {}

const columns = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  const shown = k
    ? applies.value.filter((a) =>
        String(a.studentName || '')
          .toLowerCase()
          .includes(k),
      )
    : applies.value
  // 待审核按请假日期先后（最早的最急），已审核的按日期倒序（最近的在上面）
  const byDate = (desc) => (a, b) => (desc ? -1 : 1) * String(a.time || '').localeCompare(String(b.time || ''))
  return ['pending', 'approved', 'rejected'].map((key) => ({
    key,
    items: shown.filter((a) => statusKey(a.status) === key).sort(byDate(key !== 'pending')),
  }))
})

const columnOptions = computed(() =>
  columns.value.map((col) => ({
    value: col.key,
    label: t('admin.leaves.columns.' + col.key),
    count: col.items.length,
  })),
)
const currentItems = computed(() => (columns.value.find((col) => col.key === column.value) || {}).items || [])

const fmt = (iso) => formatDay(iso, isZh(), true)
const rangeText = (a) => {
  if (!a.time) return '—'
  const days = Math.max(1, Number(a.day) || 1)
  return days === 1 ? fmt(a.time) : `${fmt(a.time)} – ${fmt(addDays(a.time, days - 1))}`
}

const load = async () => {
  loading.value = true
  try {
    applies.value = (await applyApi.selectAll(undefined, SILENT)) || []
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    loading.value = false
  }
}

// ---------- 审核 ----------
const current = ref(null)
const note = ref('')
const affected = ref([])
const affectedLoading = ref(false)
const saving = ref('')

const openReview = async (apply) => {
  current.value = apply
  note.value = apply.descr || ''
  affected.value = []
  affectedLoading.value = true
  try {
    affected.value = (await applyApi.affected(apply.id, SILENT)) || []
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    affectedLoading.value = false
  }
}

const decide = async (status) => {
  const apply = current.value
  if (!apply) return
  saving.value = status
  try {
    await applyApi.update({ id: apply.id, status, descr: note.value.trim() })
    apply.status = status
    apply.descr = note.value.trim()
    uni.showToast({
      title: status === APPROVED ? t('admin.leaves.approvedMsg') : t('admin.leaves.rejectedMsg'),
      icon: 'none',
    })
    current.value = null
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    saving.value = ''
  }
}

/** 首页「待审批的请假」里点某一条带 open=申请 id：进来直接打开它的审核 */
let openId = 0
onLoad((query) => {
  openId = Number(query.open) || 0
})

onShow(async () => {
  if (!ensureRole(['ADMIN'])) return
  uni.setNavigationBarTitle({ title: t('admin.leaves.title') })
  await load()
  if (!openId) return
  const apply = applies.value.find((a) => a.id === openId)
  openId = 0
  if (apply) {
    column.value = statusKey(apply.status)
    openReview(apply)
  }
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
  line-height: 1.5;
  color: var(--xm-text-2);
}

.filters {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.leave-who {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.leave-name {
  flex: 1;
  font-size: 28rpx;
  font-weight: 600;
  color: var(--xm-text);
}

.leave-days {
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.leave-range {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: var(--xm-text);
}

.leave-reason {
  margin-top: 6rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: var(--xm-text-2);
}

.leave-note {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: var(--xm-text-3);
}

.review-who {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-bottom: 12rpx;
}

.review-name {
  margin-bottom: 6rpx;
  font-size: 30rpx;
  font-weight: 600;
  color: var(--xm-text);
}

.affected {
  margin: 20rpx 0;
}

.muted {
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.affected-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 8rpx 0;
  font-size: 24rpx;
}

.affected-bar {
  width: 8rpx;
  height: 32rpx;
  border-radius: 4rpx;
  flex-shrink: 0;
}

.affected-when {
  flex-shrink: 0;
  color: var(--xm-text-2);
}

.affected-course {
  flex: 1;
  min-width: 0;
  color: var(--xm-text);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.affected-teacher {
  flex-shrink: 0;
  color: var(--xm-text-2);
}
</style>

<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 选课广场（与 Web 端 views/square/CourseSquarePage.vue 一致）：本学期开设的课程，学生在这里选课、退选，
         按关键字、类型、星期、状态筛选；学生还能看到「猜你想选」 -->
    <view class="head">
      <text class="head-sub">{{ canEnroll ? $t('square.sub.student') : $t('square.sub.other') }}</text>
      <text class="head-count xm-num">{{ $t('square.count', { n: courses.length }) }}</text>
    </view>

    <view class="filters">
      <xm-search
        v-model="keyword"
        :placeholder="$t('square.search')"
      />
      <xm-chips
        v-model="type"
        :options="typeOptions"
        @change="load"
      />
      <view class="filter-row">
        <picker
          mode="selector"
          :range="weekRange"
          :value="weekIndex"
          @change="onWeekPicked"
        >
          <view
            class="filter-pill"
            :class="{ on: week }"
          >
            {{ week ? weekLabel(week, true) : $t('square.allWeekdays') }}
            <xm-icon
              name="chevron-down"
              :size="24"
            />
          </view>
        </picker>
        <picker
          mode="selector"
          :range="statusRange"
          :value="statusIndex"
          @change="onStatusPicked"
        >
          <view
            class="filter-pill"
            :class="{ on: status }"
          >
            {{ status ? statusRange[statusIndex] : $t('square.allStatus') }}
            <xm-icon
              name="chevron-down"
              :size="24"
            />
          </view>
        </picker>
        <view
          class="filter-pill"
          :class="{ on: available }"
          @click="toggle('available')"
          >{{ $t('square.onlyAvailable') }}</view
        >
        <view
          class="filter-pill"
          :class="{ on: includeEnded }"
          @click="toggle('includeEnded')"
          >{{ $t('square.includeEnded') }}</view
        >
      </view>
    </view>

    <view
      v-if="recommendations.length"
      class="rec"
    >
      <view class="rec-head">
        <text class="rec-title">{{ $t('square.recommend') }}</text>
        <text class="rec-hint">{{ $t('square.recommendHint') }}</text>
      </view>
      <scroll-view
        scroll-x
        class="rec-strip"
        :show-scrollbar="false"
      >
        <view
          v-for="c in recommendations"
          :key="'rec' + c.id"
          class="rec-item"
        >
          <square-card
            :course="c"
            :can-enroll="canEnroll"
            :busy="busyId === c.id"
            @enroll="enroll"
            @drop="drop"
          />
        </view>
      </scroll-view>
    </view>

    <view
      v-if="loading && !courses.length"
      class="xm-card"
    >
      <view class="skeleton skeleton-title" />
      <view class="skeleton skeleton-line" />
      <view class="skeleton skeleton-line short" />
    </view>
    <view
      v-else-if="courses.length"
      class="xm-list"
    >
      <view
        v-for="c in courses"
        :key="c.id"
        class="list-item"
      >
        <square-card
          :course="c"
          :can-enroll="canEnroll"
          :busy="busyId === c.id"
          @enroll="enroll"
          @drop="drop"
        />
      </view>
    </view>
    <xm-empty
      v-else
      icon="compass"
      :text="$t('square.empty')"
    />
    <xm-loader />
  </view>
</template>

<script setup>
import { computed, onUnmounted, ref, watch } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { courseApi } from '@/api'
import { SILENT } from '@/utils/request'
import { ensureLoggedIn } from '@/utils/authGuard'
import { WEEKDAYS } from '@/utils/schedule'
import { courseTypeLabel, weekLabel } from '@/utils/courseText'
import { confirm } from '@/utils/confirm'
import { t } from '@/i18n'
import SquareCard from './SquareCard.vue'

const courses = ref([])
const recommendations = ref([])
const types = ref([])
const canEnroll = ref(false)
const loading = ref(false)
const busyId = ref(null)

const keyword = ref('')
const type = ref('')
const week = ref('')
const status = ref('')
const available = ref(false)
const includeEnded = ref(false)

const typeOptions = computed(() => [
  { value: '', label: t('square.allTypes') },
  ...types.value.map((value) => ({ value, label: courseTypeLabel(value) })),
])

// 星期、状态用原生选择器：第一项是「全部」
const weekRange = computed(() => [t('square.allWeekdays'), ...WEEKDAYS.map((w) => weekLabel(w, true))])
const weekIndex = computed(() => (week.value ? WEEKDAYS.indexOf(week.value) + 1 : 0))
const STATUS_VALUES = ['', '已开课', '未开课']
const statusRange = computed(() => [t('square.allStatus'), t('courses.filter.active'), t('courses.filter.upcoming')])
const statusIndex = computed(() => Math.max(0, STATUS_VALUES.indexOf(status.value)))

const onWeekPicked = (e) => {
  const i = Number(e.detail.value)
  week.value = i > 0 ? WEEKDAYS[i - 1] : ''
  load()
}
const onStatusPicked = (e) => {
  status.value = STATUS_VALUES[Number(e.detail.value)] || ''
  load()
}
const toggle = (name) => {
  if (name === 'available') available.value = !available.value
  else includeEnded.value = !includeEnded.value
  load()
}

let seq = 0
const load = async () => {
  const mine = ++seq
  loading.value = true
  try {
    const data = await courseApi.square(
      {
        keyword: keyword.value.trim() || undefined,
        type: type.value || undefined,
        week: week.value || undefined,
        status: status.value || undefined,
        available: available.value || undefined,
        includeEnded: includeEnded.value || undefined,
      },
      SILENT,
    )
    // 连续输入时只认最后一次请求的结果
    if (mine !== seq) return
    courses.value = data.courses || []
    recommendations.value = data.recommendations || []
    if (data.types && data.types.length) types.value = data.types
    canEnroll.value = !!data.canEnroll
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    if (mine === seq) loading.value = false
  }
}

// 输入关键字 300ms 后再查，连续输入只发最后一次
let timer = null
watch(keyword, () => {
  if (timer) clearTimeout(timer)
  timer = setTimeout(load, 300)
})
onUnmounted(() => {
  if (timer) clearTimeout(timer)
})

const enroll = async (course) => {
  busyId.value = course.id
  try {
    await courseApi.enroll(course.id)
    uni.showToast({ title: t('square.enrolledMsg', { name: course.name }), icon: 'none' })
    await load()
  } catch {
    // 满员、冲突等提示已由请求层统一弹出
  } finally {
    busyId.value = null
  }
}

const drop = async (course) => {
  if (!(await confirm(t('square.dropConfirm', { name: course.name }), { title: t('square.drop') }))) return
  busyId.value = course.id
  try {
    await courseApi.drop(course.id)
    uni.showToast({ title: t('square.droppedMsg', { name: course.name }), icon: 'none' })
    await load()
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    busyId.value = null
  }
}

onShow(() => {
  if (!ensureLoggedIn()) return
  uni.setNavigationBarTitle({ title: t('square.title') })
  // 每次显示都刷新：从课程概览里选了 / 退了课回来，名额和状态要跟着变
  load()
})

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.head-sub {
  flex: 1;
  font-size: 24rpx;
  line-height: 1.5;
  color: var(--xm-text-2);
}

.head-count {
  flex-shrink: 0;
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.filters {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.filter-pill {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  height: 56rpx;
  padding: 0 20rpx;
  border: 1rpx solid var(--xm-border);
  border-radius: 999rpx;
  background: var(--xm-bg-card);
  font-size: 24rpx;
  color: var(--xm-text-2);
  box-sizing: border-box;
}

.filter-pill.on {
  border-color: var(--xm-brand);
  background: var(--xm-brand-soft);
  color: var(--xm-brand);
}

.rec {
  margin-bottom: 24rpx;
}

.rec-head {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 4rpx 16rpx;
  margin-bottom: 14rpx;
}

.rec-title {
  font-size: 30rpx;
  font-weight: bold;
  color: var(--xm-text);
}

.rec-hint {
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.rec-strip {
  white-space: nowrap;
  margin: 0 -24rpx;
  padding: 0 24rpx;
  width: calc(100% + 48rpx);
  box-sizing: border-box;
}

.rec-item {
  display: inline-block;
  width: 560rpx;
  margin-right: 20rpx;
  vertical-align: top;
  white-space: normal;
}

.list-item {
  margin-bottom: 24rpx;
}

@media (min-width: 720px) {
  .list-item {
    margin-bottom: 0;
  }
}
</style>

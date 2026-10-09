<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 课程管理（与 Web 端 views/admin/CourseAdmin.vue 一致）：全校课程，开课、排课、改状态、删除 -->
    <view class="sub">{{ $t('admin.courses.sub') }}</view>

    <view class="filters">
      <xm-search
        v-model="keyword"
        :placeholder="$t('admin.courses.search')"
      />
      <xm-chips
        v-model="status"
        :options="statusOptions"
      />
      <picker
        mode="selector"
        :range="teacherRange"
        :value="teacherIndex"
        @change="onTeacherPicked"
      >
        <view
          class="filter-pill"
          :class="{ on: teacherId }"
        >
          {{ teacherIndex > 0 ? teacherRange[teacherIndex] : $t('admin.courses.allTeachers') }}
          <xm-icon
            name="chevron-down"
            :size="24"
          />
        </view>
      </picker>
    </view>

    <view
      v-if="loading && !courses.length"
      class="xm-card"
    >
      <view class="skeleton skeleton-title" />
      <view class="skeleton skeleton-line" />
    </view>
    <xm-empty
      v-else-if="!shown.length"
      icon="book"
      :text="$t('admin.courses.empty')"
    />
    <view
      v-else
      class="xm-list"
    >
      <view
        v-for="c in shown"
        :key="c.id"
        class="xm-card course"
      >
        <view
          class="course-head"
          @click="openSpace(c)"
        >
          <view
            class="course-dot"
            :style="'background:' + courseColor(c.name)"
          />
          <view class="course-main">
            <view class="course-name">{{ c.name }}</view>
            <view class="course-sub">
              {{ courseTypeLabel(c.type) }} · {{ $t('courses.credits', { n: c.score == null ? 0 : c.score }) }} ·
              {{ c.teacherName || '—' }}
            </view>
          </view>
          <view
            class="xm-tag course-status"
            :class="courseStatusTag(c.status)"
            @click.stop="chooseStatus(c)"
            >{{ courseStatusLabel(c.status) || '—' }} ▾</view
          >
        </view>

        <view class="course-time">
          <template v-if="c.week">
            <xm-icon
              name="clock"
              :size="26"
            />
            <text>{{ courseWhen(c) }}</text>
          </template>
          <text
            v-else
            class="xm-tag xm-tag-warning"
            >{{ $t('courses.unscheduled') }}</text
          >
        </view>

        <view class="seats">
          <view class="seats-bar">
            <view
              class="seats-fill"
              :class="{ 'is-full': fill(c) >= 100 }"
              :style="'width:' + fill(c) + '%'"
            />
          </view>
          <text class="seats-text xm-num"
            >{{ counts[c.id] || 0 }}<template v-if="c.num"> / {{ c.num }}</template></text
          >
        </view>

        <view class="xm-actions">
          <button
            class="xm-btn xm-btn-plain xm-btn-sm"
            @click="openForm(c.id)"
          >
            {{ $t('admin.courses.edit') }}
          </button>
          <button
            class="xm-btn xm-btn-danger xm-btn-sm"
            @click="remove(c)"
          >
            {{ $t('admin.courses.delete') }}
          </button>
        </view>
      </view>
    </view>

    <view
      class="xm-fab"
      @click="openForm(0)"
    >
      <xm-icon
        name="plus"
        :size="48"
      />
    </view>
    <xm-loader />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { courseApi, teacherApi } from '@/api'
import { SILENT } from '@/utils/request'
import { ensureRole } from '@/utils/authGuard'
import { courseColor } from '@/utils/courseColor'
import { courseStatusLabel, courseStatusTag, courseTypeLabel, courseWhen } from '@/utils/courseText'
import { confirm } from '@/utils/confirm'
import { t } from '@/i18n'

const UNSCHEDULED = 'unscheduled'
const STATUSES = ['未开课', '已开课', '已结课']

const courses = ref([])
const counts = ref({})
const teachers = ref([])
const loading = ref(false)
const status = ref('')
const teacherId = ref(0)
const keyword = ref('')

/** 还没定时间或教室、又没结课的课（首页「待排课」里列的就是这些） */
const isUnscheduled = (c) => c.status !== '已结课' && (!c.week || !c.segment || !c.room)
const matchesStatus = (c, value) => !value || (value === UNSCHEDULED ? isUnscheduled(c) : c.status === value)

const statusOptions = computed(() =>
  [
    { value: '', label: t('courses.filter.all') },
    { value: '已开课', label: t('courses.filter.active') },
    { value: '未开课', label: t('courses.filter.upcoming') },
    { value: '已结课', label: t('courses.filter.finished') },
    { value: UNSCHEDULED, label: t('courses.unscheduled') },
  ].map((o) => ({ ...o, count: courses.value.filter((c) => matchesStatus(c, o.value)).length })),
)

const teacherRange = computed(() => [t('admin.courses.allTeachers'), ...teachers.value.map((x) => x.name)])
const teacherIndex = computed(() => {
  const i = teachers.value.findIndex((x) => x.id === teacherId.value)
  return i < 0 ? 0 : i + 1
})
const onTeacherPicked = (e) => {
  const i = Number(e.detail.value)
  teacherId.value = i > 0 ? teachers.value[i - 1].id : 0
}

const shown = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  return courses.value.filter((c) => {
    if (!matchesStatus(c, status.value)) return false
    if (teacherId.value && c.teacherId !== teacherId.value) return false
    if (!k) return true
    return [c.name, c.teacherName, c.room].some((v) =>
      String(v || '')
        .toLowerCase()
        .includes(k),
    )
  })
})

const fill = (c) => {
  const cap = Number(c.num) || 0
  return cap ? Math.min(100, Math.round(((counts.value[c.id] || 0) / cap) * 100)) : 0
}

const load = async () => {
  loading.value = true
  try {
    const [list, cards, teacherList] = await Promise.all([
      courseApi.selectAll(undefined, SILENT),
      courseApi.mine(SILENT),
      teacherApi.selectAll(undefined, SILENT),
    ])
    courses.value = list || []
    counts.value = Object.fromEntries((cards || []).map((c) => [c.id, Number(c.studentCount) || 0]))
    teachers.value = teacherList || []
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    loading.value = false
  }
}

const openForm = (id) => uni.navigateTo({ url: '/pages-admin/course-form/course-form' + (id ? `?id=${id}` : '') })
const openSpace = (c) => uni.navigateTo({ url: `/pages-course/space/space?id=${c.id}` })

/** 改状态：底部菜单三选一 */
const chooseStatus = (c) => {
  uni.showActionSheet({
    itemList: STATUSES.map((s) => courseStatusLabel(s)),
    success: async (res) => {
      const value = STATUSES[res.tapIndex]
      if (value === c.status) return
      try {
        await courseApi.update({ id: c.id, status: value })
        c.status = value
        uni.showToast({
          title: t('admin.courses.statusChanged', { name: c.name, status: courseStatusLabel(value) }),
          icon: 'none',
        })
      } catch {
        // 提示已由请求层统一弹出
      }
    },
    fail: () => {},
  })
}

const remove = async (c) => {
  if (!(await confirm(t('admin.courses.deleteConfirm', { name: c.name })))) return
  try {
    await courseApi.delete(c.id)
    uni.showToast({ title: t('admin.courses.deleted'), icon: 'none' })
    await load()
  } catch {
    // 已有学生选课等提示已由请求层统一弹出
  }
}

onLoad((query) => {
  // 首页「待排课」带 filter=unscheduled 进来
  if (query.filter === UNSCHEDULED) status.value = UNSCHEDULED
})

onShow(() => {
  if (!ensureRole(['ADMIN'])) return
  uni.setNavigationBarTitle({ title: t('admin.courses.title') })
  // 每次显示都刷新：从开课表单回来要看到新课
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
  align-items: flex-start;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.filters > * {
  width: 100%;
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

.course-head {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.course-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  flex-shrink: 0;
}

.course-main {
  flex: 1;
  min-width: 0;
}

.course-name {
  font-size: 30rpx;
  font-weight: 600;
  color: var(--xm-text);
}

.course-sub {
  margin-top: 4rpx;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.course-status {
  flex-shrink: 0;
  padding: 6rpx 18rpx;
}

.course-time {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 16rpx;
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.seats {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 14rpx;
}

.seats-bar {
  flex: 1;
  height: 10rpx;
  border-radius: 999rpx;
  background: var(--xm-bg-sunken);
  overflow: hidden;
}

.seats-fill {
  height: 100%;
  background: var(--xm-brand);
}

.seats-fill.is-full {
  background: var(--xm-danger);
}

.seats-text {
  flex-shrink: 0;
  font-size: 22rpx;
  color: var(--xm-text-2);
}
</style>

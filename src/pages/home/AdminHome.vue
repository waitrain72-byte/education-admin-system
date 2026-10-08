<template>
  <!-- 管理员首页（与 Web 端 views/home/AdminHome.vue 同一组内容）：问候 + 快捷操作、全校指标、待审批请假、
       待排课的课程、近期考试、最新通知 -->
  <view>
    <home-hero
      :eyebrow="semesterLine"
      :title="$t('workbench.hello', { name: user.name || user.username, greeting })"
    >
      <template #aside>
        <view class="quick">
          <view
            v-for="action in actions"
            :key="action.label"
            class="quick-item"
            @click="action.go()"
          >
            <view class="quick-icon">
              <xm-icon
                :name="action.icon"
                :size="40"
              />
            </view>
            <text class="quick-label">{{ $t(action.label) }}</text>
          </view>
        </view>
      </template>
    </home-hero>

    <view class="kpis">
      <view
        v-for="kpi in kpis"
        :key="kpi.key"
        class="kpi"
        :class="{ 'is-alert': kpi.alert }"
      >
        <text class="kpi-label">{{ $t(kpi.label) }}</text>
        <text class="kpi-value xm-num">{{ kpi.value }}</text>
        <text
          v-if="kpi.sub"
          class="kpi-sub"
          >{{ kpi.sub }}</text
        >
      </view>
    </view>

    <view class="xm-card">
      <view class="xm-section-head">
        <text class="xm-section-title">{{ $t('workbench.admin.pendingLeaves') }}</text>
        <text
          class="xm-link"
          @click="go('/pages-admin/leaves/leaves')"
          >{{ $t('workbench.admin.goReview') }} ›</text
        >
      </view>
      <view v-if="(data.pendingApplies || []).length">
        <view
          v-for="a in data.pendingApplies"
          :key="a.id"
          class="row"
          @click="go('/pages-admin/leaves/leaves?open=' + a.id)"
        >
          <xm-user-avatar
            :name="a.studentName"
            :size="64"
          />
          <view class="row-main">
            <view class="row-title">
              <text class="xm-ellipsis">{{ a.studentName || '—' }}</text>
              <text class="row-meta xm-num">{{ a.time }} · {{ $t('workbench.admin.days', { n: a.day }) }}</text>
            </view>
            <view class="row-text">{{ a.content }}</view>
          </view>
        </view>
      </view>
      <view
        v-else
        class="empty-note"
        >{{ $t('workbench.admin.noPendingLeaves') }}</view
      >
    </view>

    <view class="xm-card">
      <view class="xm-section-head">
        <text class="xm-section-title">{{ $t('workbench.admin.unscheduled') }}</text>
        <text
          class="xm-link"
          @click="go('/pages-admin/courses/courses?filter=unscheduled')"
          >{{ $t('workbench.admin.goSchedule') }} ›</text
        >
      </view>
      <view v-if="(data.unscheduledCourses || []).length">
        <!-- 点一门课直接打开它的排课表单 -->
        <view
          v-for="c in data.unscheduledCourses"
          :key="c.id"
          class="row"
          @click="go('/pages-admin/course-form/course-form?id=' + c.id)"
        >
          <view
            class="row-swatch"
            :style="'background:' + courseColor(c.name)"
          />
          <view class="row-main">
            <view class="row-title">
              <text class="xm-ellipsis">{{ c.name }}</text>
            </view>
            <view class="row-text">{{ c.teacherName || '—' }} · {{ courseStatusLabel(c.status) || '—' }}</view>
          </view>
        </view>
      </view>
      <view
        v-else
        class="empty-note"
        >{{ $t('workbench.admin.allScheduled') }}</view
      >
    </view>

    <exams-card
      :exams="data.exams || []"
      more-url="/pages-admin/exams/exams"
    />

    <view class="xm-card">
      <view class="xm-section-head">
        <text class="xm-section-title">{{ $t('workbench.admin.notices') }}</text>
        <text
          class="xm-link"
          @click="openLink('/messages?tab=notices')"
          >{{ $t('home.viewAll') }} ›</text
        >
      </view>
      <view v-if="(data.notices || []).length">
        <view
          v-for="n in data.notices"
          :key="n.id"
          class="notice"
          @click="openLink('/messages?tab=notices&notice=' + n.id)"
        >
          <text class="notice-title">{{ n.title }}</text>
          <text class="notice-time xm-num">{{ n.time }}</text>
        </view>
      </view>
      <view
        v-else
        class="empty-note"
        >{{ $t('common.empty') }}</view
      >
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { courseColor } from '@/utils/courseColor'
import { courseStatusLabel } from '@/utils/courseText'
import { openLink } from '@/utils/link'
import { t } from '@/i18n'
import { useHomeText } from './useHomeText'
import HomeHero from './HomeHero.vue'
import ExamsCard from './ExamsCard.vue'

const props = defineProps({
  data: { type: Object, required: true },
})

const user = computed(() => useUserStore().user)
const { greeting, semesterLine } = useHomeText(() => props.data.semester)

const go = (url) => uni.navigateTo({ url })

/** 快捷操作（Web 端还有「数据大屏」，小程序屏幕放不下，不提供） */
const actions = [
  {
    label: 'workbench.admin.actions.openCourse',
    icon: 'book-open',
    go: () => go('/pages-admin/course-form/course-form'),
  },
  {
    label: 'workbench.admin.actions.notice',
    icon: 'megaphone',
    go: () => openLink('/messages?tab=notices&compose=1'),
  },
  { label: 'workbench.admin.actions.semester', icon: 'calendar', go: () => go('/pages-admin/semester/semester') },
]

const kpis = computed(() => {
  const k = props.data.kpis || {}
  return [
    { key: 'students', label: 'workbench.admin.kpi.students', value: k.studentCount ?? 0 },
    { key: 'teachers', label: 'workbench.admin.kpi.teachers', value: k.teacherCount ?? 0 },
    {
      key: 'courses',
      label: 'workbench.admin.kpi.activeCourses',
      value: k.activeCourseCount ?? 0,
      sub: t('workbench.admin.kpi.ofCourses', { n: k.courseCount ?? 0 }),
    },
    { key: 'choices', label: 'workbench.admin.kpi.choices', value: k.choiceCount ?? 0 },
    {
      key: 'leaves',
      label: 'workbench.admin.kpi.pendingLeaves',
      value: k.pendingApply ?? 0,
      alert: (k.pendingApply ?? 0) > 0,
    },
    { key: 'homework', label: 'workbench.admin.kpi.ungraded', value: k.ungradedHomework ?? 0 },
    { key: 'logins', label: 'workbench.admin.kpi.loginToday', value: k.loginToday ?? 0 },
  ]
})
</script>

<style lang="scss" scoped>
.quick {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
  padding: 20rpx 8rpx;
  border: 1rpx solid var(--xm-border);
  border-radius: 20rpx;
  background: var(--xm-bg-card);
}

.quick-item:active {
  border-color: var(--xm-brand);
}

.quick-icon {
  display: flex;
  color: var(--xm-brand);
}

.quick-label {
  font-size: 24rpx;
  color: var(--xm-text);
  text-align: center;
}

.kpis {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.kpi {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
  padding: 22rpx 24rpx;
  border: 1rpx solid var(--xm-card-border);
  border-radius: 20rpx;
  background: var(--xm-bg-card);
  box-shadow: var(--xm-shadow);
}

.kpi.is-alert {
  border-color: var(--xm-warning);
  background: var(--xm-warning-soft);
}

.kpi-label {
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.kpi-value {
  font-size: 44rpx;
  font-weight: bold;
  line-height: 1.2;
  color: var(--xm-text);
}

.kpi.is-alert .kpi-value {
  color: var(--xm-warning);
}

.kpi-sub {
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 16rpx 0;
}

.row + .row {
  border-top: 1rpx solid var(--xm-border);
}

.row-swatch {
  width: 16rpx;
  height: 64rpx;
  border-radius: 8rpx;
  flex-shrink: 0;
}

.row-main {
  flex: 1;
  min-width: 0;
}

.row-title {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: var(--xm-text);
}

.row-meta {
  flex-shrink: 0;
  font-size: 22rpx;
  font-weight: normal;
  color: var(--xm-text-2);
}

.row-text {
  margin-top: 4rpx;
  font-size: 24rpx;
  color: var(--xm-text-2);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.notice {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 18rpx 0;
}

.notice + .notice {
  border-top: 1rpx solid var(--xm-border);
}

.notice-title {
  flex: 1;
  min-width: 0;
  font-size: 26rpx;
  color: var(--xm-text);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.notice-time {
  flex-shrink: 0;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.empty-note {
  padding: 16rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: var(--xm-text-2);
}
</style>

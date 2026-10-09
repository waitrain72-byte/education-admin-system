<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 成绩单（与 Web 端 views/grades/TranscriptPage.vue 一致）：所有课的已发布成绩、学分、绩点汇总；
         老师发布之后才出现，点一门课进它的成绩页 -->
    <view class="sub">{{ $t('transcript.sub') }}</view>

    <view
      v-if="loading && !data"
      class="xm-card"
    >
      <view class="skeleton skeleton-title" />
      <view class="skeleton skeleton-line" />
      <view class="skeleton skeleton-line short" />
    </view>

    <template v-else-if="data">
      <view class="kpis">
        <view class="kpi">
          <text class="kpi-label">{{ $t('transcript.credits') }}</text>
          <text class="kpi-value xm-num">{{ summary.credits }}</text>
          <template v-if="summary.requiredCredits">
            <text class="kpi-sub">{{ $t('transcript.ofRequired', { n: summary.requiredCredits }) }}</text>
            <view class="kpi-bar">
              <view
                class="kpi-bar-fill"
                :style="'width:' + creditFill + '%'"
              />
            </view>
          </template>
        </view>
        <view class="kpi">
          <text class="kpi-label">{{ $t('transcript.gpa') }}</text>
          <text class="kpi-value xm-num">{{ summary.gpa == null ? '—' : summary.gpa }}</text>
        </view>
        <view class="kpi">
          <text class="kpi-label">{{ $t('transcript.average') }}</text>
          <text class="kpi-value xm-num">{{ summary.average == null ? '—' : summary.average }}</text>
        </view>
        <view class="kpi">
          <text class="kpi-label">{{ $t('transcript.results') }}</text>
          <view class="kpi-counts">
            <text class="xm-tag xm-tag-success">{{ $t('transcript.passed', { n: summary.passed || 0 }) }}</text>
            <text
              v-if="summary.failed"
              class="xm-tag xm-tag-danger"
              >{{ $t('transcript.failed', { n: summary.failed }) }}</text
            >
            <text
              v-if="summary.pending"
              class="xm-tag"
              >{{ $t('transcript.pending', { n: summary.pending }) }}</text
            >
          </view>
        </view>
      </view>

      <xm-empty
        v-if="!rows.length"
        icon="award"
        :text="$t('transcript.empty')"
      />
      <view
        v-for="row in rows"
        :key="row.courseId"
        class="xm-card course"
        @click="openCourse(row)"
      >
        <view class="course-head">
          <view
            class="course-dot"
            :style="'background:' + courseColor(row.courseName)"
          />
          <view class="course-main">
            <view class="course-name">{{ row.courseName }}</view>
            <view class="course-sub">
              {{ courseTypeLabel(row.type) }} · {{ $t('courses.credits', { n: row.credit == null ? 0 : row.credit }) }}
              <template v-if="row.teacherName"> · {{ row.teacherName }}</template>
            </view>
          </view>
          <view class="course-result">
            <text
              v-if="row.published"
              class="course-total xm-num"
              :class="{ 'is-fail': !row.passed }"
              >{{ fmt(row.total) }}</text
            >
            <text
              v-if="!row.published"
              class="xm-tag"
              >{{ $t('transcript.notOut') }}</text
            >
            <text
              v-else-if="row.passed"
              class="xm-tag xm-tag-success"
              >{{ $t('transcript.pass') }}</text
            >
            <text
              v-else
              class="xm-tag xm-tag-danger"
              >{{ $t('transcript.fail') }}</text
            >
          </view>
        </view>
        <view
          v-if="row.published"
          class="parts"
        >
          <view
            v-for="col in PARTS"
            :key="col.prop"
            class="part"
          >
            <text class="part-label">{{ $t(col.label) }}</text>
            <text class="part-value xm-num">{{ fmt(row[col.prop]) }}</text>
          </view>
          <view class="part">
            <text class="part-label">{{ $t('transcript.gradePoint') }}</text>
            <text class="part-value xm-num">{{ Number(row.gradePoint || 0).toFixed(1) }}</text>
          </view>
        </view>
      </view>

      <view class="hint">{{ $t('transcript.gpaHint') }}</view>
    </template>
    <xm-loader />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { scoreApi } from '@/api'
import { SILENT } from '@/utils/request'
import { ensureLoggedIn } from '@/utils/authGuard'
import { courseColor } from '@/utils/courseColor'
import { courseTypeLabel } from '@/utils/courseText'
import { t } from '@/i18n'

/** 四个分项：考勤、作业由记录折算，平时、期末由老师录入（权重在成绩册里设） */
const PARTS = [
  { prop: 'attendanceScore', label: 'transcript.attendance' },
  { prop: 'homeworkScore', label: 'transcript.homework' },
  { prop: 'ordinaryScore', label: 'transcript.ordinary' },
  { prop: 'examScore', label: 'transcript.exam' },
]

const data = ref(null)
const loading = ref(false)

const summary = computed(() => (data.value && data.value.summary) || {})
const rows = computed(() => (data.value && data.value.rows) || [])
const creditFill = computed(() => {
  const required = Number(summary.value.requiredCredits) || 0
  return required ? Math.min(100, Math.round(((Number(summary.value.credits) || 0) / required) * 100)) : 0
})

/** 分数保留到一位小数，整数不带 .0；没有这一项（权重为 0 或没录）显示 — */
const fmt = (value) => (value == null ? '—' : String(Math.round(value * 10) / 10))

const load = async () => {
  loading.value = true
  try {
    data.value = await scoreApi.transcript(SILENT)
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    loading.value = false
  }
}

const openCourse = (row) => uni.navigateTo({ url: `/pages-course/space/space?id=${row.courseId}&tab=grades` })

onShow(() => {
  if (!ensureLoggedIn()) return
  uni.setNavigationBarTitle({ title: t('transcript.title') })
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
  line-height: 1.5;
  color: var(--xm-text-2);
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

.kpi-label {
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.kpi-value {
  font-size: 44rpx;
  font-weight: bold;
  line-height: 1.2;
  color: var(--xm-text);
}

.kpi-sub {
  font-size: 20rpx;
  color: var(--xm-text-3);
}

.kpi-bar {
  height: 8rpx;
  margin-top: 6rpx;
  border-radius: 999rpx;
  background: var(--xm-bg-sunken);
  overflow: hidden;
}

.kpi-bar-fill {
  height: 100%;
  background: var(--xm-brand);
}

.kpi-counts {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
  margin-top: 6rpx;
}

.course:active {
  opacity: 0.85;
}

.course-head {
  display: flex;
  align-items: center;
  gap: 18rpx;
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

.course-result {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6rpx;
  flex-shrink: 0;
}

.course-total {
  font-size: 44rpx;
  font-weight: bold;
  line-height: 1;
  color: var(--xm-brand);
}

.course-total.is-fail {
  color: var(--xm-danger);
}

.parts {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8rpx;
  margin-top: 18rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid var(--xm-border);
}

.part {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
}

.part-label {
  font-size: 20rpx;
  color: var(--xm-text-2);
}

.part-value {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--xm-text);
}

.hint {
  padding: 8rpx 8rpx 24rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: var(--xm-text-3);
}
</style>

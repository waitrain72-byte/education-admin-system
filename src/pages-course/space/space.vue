<template>
  <view
    class="xm-page space"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 课程空间（与 Web 端 views/course/CourseSpaceLayout.vue 一致）：课程色页头 + 七个分区。
         没选这门课的学生（访客）只能看概览；各分区第一次打开时才加载，之后切换不重新加载（成绩册没保存的修改也不会丢） -->
    <view
      v-if="!overview && loading"
      class="xm-card"
    >
      <view class="skeleton skeleton-title" />
      <view class="skeleton skeleton-line" />
      <view class="skeleton skeleton-line short" />
    </view>
    <xm-empty
      v-else-if="!overview"
      icon="book"
      :text="$t('space.notFound')"
      :action-text="$t('space.backToCourses')"
      @action="backToCourses"
    />

    <template v-else>
      <view
        class="head"
        :style="'background-color:' + color"
      >
        <view class="head-tags">
          <text class="head-tag">{{ courseTypeLabel(course.type) }}</text>
          <text class="head-tag xm-num">{{
            $t('courses.credits', { n: course.credit == null ? '—' : course.credit })
          }}</text>
          <text class="head-tag">{{ courseStatusLabel(course.status) || '—' }}</text>
        </view>
        <view class="head-name">{{ course.name }}</view>
        <view class="head-meta">
          <text>{{ course.teacherName || '—' }}</text>
          <text>{{ courseWhen(course, false) }}</text>
          <text v-if="course.room">{{ course.room }}</text>
          <text class="xm-num">{{ $t('workbench.students', { n: overview.studentCount }) }}</text>
        </view>
      </view>

      <scroll-view
        scroll-x
        class="tabs"
        :scroll-into-view="'tab-' + tab"
        :show-scrollbar="false"
      >
        <view
          v-for="key in tabs"
          :id="'tab-' + key"
          :key="key"
          class="tab"
          :class="{ on: tab === key }"
          @click="setTab(key)"
        >
          {{ $t('space.tabs.' + key) }}
          <view
            v-if="key === 'attendance' && overview.signing"
            class="tab-live"
          />
        </view>
      </scroll-view>

      <view v-show="tab === 'overview'">
        <space-overview
          v-if="visited.overview"
          ref="overviewRef"
          :overview="overview"
          :course-id="courseId"
          :focus-post="focusPost"
          @refresh="refresh"
          @go="setTab"
        />
      </view>
      <view v-show="tab === 'attendance'">
        <space-attendance
          v-if="visited.attendance"
          ref="attendanceRef"
          :overview="overview"
          :course-id="courseId"
          @refresh="refresh"
        />
      </view>
      <view v-show="tab === 'assignments'">
        <space-assignments
          v-if="visited.assignments"
          ref="assignmentsRef"
          :overview="overview"
          :course-id="courseId"
          :open-id="openAssignment"
          @refresh="refresh"
        />
      </view>
      <view v-show="tab === 'grades'">
        <space-grades
          v-if="visited.grades"
          ref="gradesRef"
          :overview="overview"
          :course-id="courseId"
          @refresh="refresh"
        />
      </view>
      <view v-show="tab === 'evaluation'">
        <space-evaluation
          v-if="visited.evaluation"
          ref="evaluationRef"
          :overview="overview"
          :course-id="courseId"
        />
      </view>
      <view v-show="tab === 'resources'">
        <space-resources
          v-if="visited.resources"
          ref="resourcesRef"
          :overview="overview"
          :course-id="courseId"
        />
      </view>
      <view v-show="tab === 'members'">
        <space-members
          v-if="visited.members"
          ref="membersRef"
          :overview="overview"
          :course-id="courseId"
        />
      </view>
    </template>
    <xm-loader />
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { courseApi } from '@/api'
import { SILENT } from '@/utils/request'
import { ensureLoggedIn } from '@/utils/authGuard'
import { COURSE_SECTIONS } from '@/utils/link'
import { courseColor } from '@/utils/courseColor'
import { courseStatusLabel, courseTypeLabel, courseWhen } from '@/utils/courseText'
import { useCourseEvents } from '@/composables/useCourseEvents'
import { t } from '@/i18n'
import SpaceOverview from './SpaceOverview.vue'
import SpaceAttendance from './SpaceAttendance.vue'
import SpaceAssignments from './SpaceAssignments.vue'
import SpaceGrades from './SpaceGrades.vue'
import SpaceEvaluation from './SpaceEvaluation.vue'
import SpaceResources from './SpaceResources.vue'
import SpaceMembers from './SpaceMembers.vue'

const courseId = ref(0)
const overview = ref(null)
const loading = ref(false)
const tab = ref('overview')
/** 进来时要打开的分区（tab=xxx）：等概览加载完、知道是不是课程成员后再切过去，访客不挂载成员才能看的分区 */
let requestedTab = ''
/** 从消息、首页带进来的定位参数：公告 post=ID、作业 open=ID */
const focusPost = ref(0)
const openAssignment = ref(0)
/** 打开过的分区（v-if 只在第一次打开时挂载，之后用 v-show 切换） */
const visited = reactive({ overview: true })

const overviewRef = ref(null)
const attendanceRef = ref(null)
const assignmentsRef = ref(null)
const gradesRef = ref(null)
const evaluationRef = ref(null)
const resourcesRef = ref(null)
const membersRef = ref(null)
const SECTION_REFS = {
  overview: overviewRef,
  attendance: attendanceRef,
  assignments: assignmentsRef,
  grades: gradesRef,
  evaluation: evaluationRef,
  resources: resourcesRef,
  members: membersRef,
}

const course = computed(() => (overview.value && overview.value.course) || {})
const color = computed(() => courseColor(course.value.name))
const relation = computed(() => (overview.value && overview.value.relation) || 'visitor')

/** 访客（没选这门课的学生）只能看概览，其余分区只对课程成员开放 */
const tabs = computed(() => COURSE_SECTIONS.filter((key) => key === 'overview' || relation.value !== 'visitor'))

const setTab = (key) => {
  const next = tabs.value.includes(key) ? key : 'overview'
  visited[next] = true
  tab.value = next
}

const load = async () => {
  if (!courseId.value) return
  loading.value = true
  try {
    overview.value = await courseApi.overview(courseId.value, SILENT)
    uni.setNavigationBarTitle({ title: course.value.name || t('nav.courses') })
    // 没选这门课的人直接打开签到、作业等链接时，退回概览（这些分区的接口对访客一律 403）
    setTab(requestedTab || tab.value)
    requestedTab = ''
  } catch {
    overview.value = null
  } finally {
    loading.value = false
  }
}

/** 静默刷新（签到开始、布置作业、发公告、选课退课后）：失败时保留旧数据，不闪成「没有找到这门课」 */
const refresh = async () => {
  if (!courseId.value) return
  try {
    overview.value = await courseApi.overview(courseId.value, SILENT)
    setTab(tab.value)
  } catch {
    // 保留旧数据
  }
}

useCourseEvents(() => courseId.value, ['attendance', 'assignments', 'posts'], refresh)

const backToCourses = () => uni.switchTab({ url: '/pages/courses/courses' })

onLoad((query) => {
  courseId.value = Number(query.id) || 0
  focusPost.value = Number(query.post) || 0
  openAssignment.value = Number(query.open) || 0
  if (query.tab && COURSE_SECTIONS.includes(query.tab)) requestedTab = query.tab
})

let shownOnce = false
onShow(() => {
  if (!ensureLoggedIn()) return
  if (!shownOnce) {
    shownOnce = true
    load()
    return
  }
  // 从作业详情等页面回来：刷新概览（待办、人数）和当前分区
  refresh()
  const section = SECTION_REFS[tab.value].value
  if (section && section.reload) section.reload()
})

onPullDownRefresh(async () => {
  const section = SECTION_REFS[tab.value].value
  await Promise.all([refresh(), section && section.reload ? section.reload() : null])
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
.head {
  margin: -24rpx -24rpx 0;
  padding: 28rpx 32rpx 32rpx;
  color: #ffffff;
  background-image:
    radial-gradient(circle at 92% 0%, rgba(255, 255, 255, 0.16) 0 140rpx, transparent 142rpx),
    repeating-linear-gradient(90deg, rgba(255, 255, 255, 0.06) 0 1rpx, transparent 1rpx 60rpx);
}

.head-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
}

.head-tag {
  padding: 2rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.2);
  font-size: 22rpx;
}

.head-name {
  margin-top: 20rpx;
  font-size: 40rpx;
  font-weight: bold;
  line-height: 1.3;
}

.head-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4rpx 24rpx;
  margin-top: 10rpx;
  font-size: 24rpx;
  opacity: 0.92;
}

/* 分区标签：吸顶，横向滑动；H5 的导航栏是页面里的固定元素，用 --window-top 让开 */
.tabs {
  position: sticky;
  top: var(--window-top, 0px);
  z-index: 20;
  margin: 0 -24rpx 24rpx;
  padding: 0 12rpx;
  width: calc(100% + 48rpx);
  box-sizing: border-box;
  white-space: nowrap;
  background: var(--xm-bg-card);
  border-bottom: 1rpx solid var(--xm-border);
}

.tab {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  padding: 24rpx 20rpx 20rpx;
  font-size: 28rpx;
  color: var(--xm-text-2);
  border-bottom: 4rpx solid transparent;
}

.tab.on {
  color: var(--xm-brand);
  font-weight: bold;
  border-bottom-color: var(--xm-brand);
}

.tab-live {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: var(--xm-danger);
}

@media (min-width: 720px) {
  .head {
    margin: -24rpx -24px 0;
    padding-left: 24px;
    padding-right: 24px;
  }

  .tabs {
    margin-left: -24px;
    margin-right: -24px;
    width: calc(100% + 48px);
  }
}
</style>

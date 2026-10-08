<template>
  <div class="space">
    <div v-if="!overview && loading" class="panel panel--pad">
      <el-skeleton animated :rows="4" />
    </div>
    <el-result v-else-if="!overview" icon="warning" :title="$t('space.notFound')">
      <template #extra>
        <el-button type="primary" @click="router.push('/courses')">{{ $t('space.backToCourses') }}</el-button>
      </template>
    </el-result>

    <template v-else>
      <header class="space-head" :style="{ '--cc': color }">
        <div class="space-head__band">
          <router-link to="/courses" class="space-head__back">‹ {{ $t('nav.courses') }}</router-link>
          <div class="space-head__tags">
            <span class="space-head__tag">{{ course.type || '—' }}</span>
            <span class="space-head__tag num">{{ $t('courses.credits', { n: course.credit ?? '—' }) }}</span>
            <span class="space-head__tag">{{ course.status || '—' }}</span>
          </div>
          <h1 class="space-head__name">{{ course.name }}</h1>
          <p class="space-head__meta">
            <span>{{ course.teacherName || '—' }}</span>
            <span v-if="course.week">{{ course.week }} {{ segmentShortName(course.segment) }}<template v-if="course.start">（{{ course.start }}–{{ course.end }}）</template></span>
            <span v-if="course.room">{{ course.room }}</span>
            <span class="num">{{ $t('workbench.students', { n: overview.studentCount }) }}</span>
          </p>
        </div>
        <nav class="space-tabs" :aria-label="$t('space.tabsLabel')">
          <router-link
            v-for="tab in tabs"
            :key="tab.key"
            :to="`/course/${courseId}/${tab.key}`"
            class="space-tabs__item"
            active-class="is-active"
          >
            {{ $t('space.tabs.' + tab.key) }}
            <span
              v-if="tab.key === 'attendance' && overview.signing"
              class="space-tabs__live"
              :title="$t('space.overview.signingTeacher')"
            ></span>
          </router-link>
        </nav>
      </header>

      <router-view v-if="relation !== 'visitor' || route.name === 'CourseOverview'" v-slot="{ Component }">
        <component :is="Component" :overview="overview" @refresh="refresh" />
      </router-view>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, provide, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { courseColor } from '@/utils/courseColor'
import { segmentShortName } from '@/utils/schedule'
import { useCourseEvents } from '@/composables/useCourseEvents'

const route = useRoute()
const router = useRouter()

const overview = ref<Record<string, any> | null>(null)
const loading = ref(false)

const courseId = computed(() => Number(route.params.id))
const course = computed(() => overview.value?.course || {})
const color = computed(() => courseColor(course.value.name))
const relation = computed<string>(() => overview.value?.relation || 'visitor')

/** 课程内的分页：访客（没选这门课的学生）只能看概览，其余页面只对课程成员开放 */
const TABS = ['overview', 'attendance', 'assignments', 'grades', 'evaluation', 'resources', 'members']
const tabs = computed(() =>
  TABS.filter((key) => key === 'overview' || relation.value !== 'visitor').map((key) => ({ key })),
)

const load = async () => {
  if (!courseId.value) return
  loading.value = true
  try {
    overview.value = await request.get<Record<string, any>>(`/course/${courseId.value}/overview`)
  } catch {
    overview.value = null
  } finally {
    loading.value = false
  }
}

/** 静默刷新（签到开始、布置作业、发公告时后端推事件）：失败时保留旧数据，不闪成「没有找到这门课」 */
const refresh = async () => {
  if (!courseId.value) return
  try {
    overview.value = await request.get<Record<string, any>>(`/course/${courseId.value}/overview`)
  } catch {
    // 保留旧数据
  }
}

// 子页面通过 inject 拿到课程概览与刷新方法，不必各自再请求一遍
provide('courseSpace', { overview, relation, reload: refresh })

useCourseEvents(() => courseId.value, ['attendance', 'assignments', 'posts'], refresh)

// 没选这门课的人直接打开签到、作业等链接时，退回概览（这些页面的接口对访客一律 403）
watch([relation, () => route.name], ([rel, name]) => {
  if (overview.value && rel === 'visitor' && name !== 'CourseOverview') {
    router.replace(`/course/${courseId.value}/overview`)
  }
})

watch(courseId, load, { immediate: true })
</script>

<style scoped>
.space {
  display: grid;
  /* 列宽可以缩到 0：里面的宽表格自己横向滚动，不把整页撑出屏幕 */
  grid-template-columns: minmax(0, 1fr);
  gap: 20px;
}

.space-head {
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius-lg);
  background: var(--xm-bg-card);
  overflow: hidden;
}

/* 课程色页头：与课程卡片封面同一套「格线 + 大圆」的处理 */
.space-head__band {
  display: grid;
  gap: 6px;
  padding: 18px 24px 22px;
  color: #fff;
  background:
    radial-gradient(circle at 94% 0%, rgba(255, 255, 255, 0.16) 0 120px, transparent 121px),
    repeating-linear-gradient(90deg, rgba(255, 255, 255, 0.06) 0 1px, transparent 1px 32px),
    var(--cc);
}

.space-head__back {
  justify-self: start;
  margin-bottom: 6px;
  color: rgba(255, 255, 255, 0.86);
  font-size: 13px;
}

.space-head__back:hover {
  color: #fff;
}

.space-head__tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.space-head__tag {
  padding: 1px 9px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.2);
  font-size: 12px;
}

.space-head__name {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.25;
  text-wrap: balance;
}

.space-head__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 18px;
  font-size: 14px;
  opacity: 0.92;
}

.space-tabs {
  display: flex;
  gap: 4px;
  padding: 0 16px;
  overflow-x: auto;
  /* 窄屏放不下时横向滑动；滚动条不显示（导航条下面压一道粗滚动条很难看） */
  scrollbar-width: none;
}

.space-tabs::-webkit-scrollbar {
  display: none;
}

.space-tabs__item {
  position: relative;
  padding: 12px 12px;
  color: var(--xm-text-secondary);
  font-size: 14px;
  white-space: nowrap;
}

.space-tabs__item:hover {
  color: var(--xm-text-primary);
}

.space-tabs__item.is-active {
  color: var(--xm-text-primary);
  font-weight: 600;
}

.space-tabs__live {
  display: inline-block;
  width: 7px;
  height: 7px;
  margin-left: 4px;
  border-radius: 50%;
  background: var(--xm-bad);
  vertical-align: 2px;
  animation: live-pulse 1.6s ease-in-out infinite;
}

@keyframes live-pulse {
  50% {
    opacity: 0.35;
  }
}

@media (prefers-reduced-motion: reduce) {
  .space-tabs__live {
    animation: none;
  }
}

.space-tabs__item.is-active::after {
  content: '';
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 0;
  height: 3px;
  border-radius: 3px 3px 0 0;
  background: var(--cc);
}

@media (max-width: 640px) {
  .space-head__band {
    padding: 16px 16px 18px;
  }

  .space-head__name {
    font-size: 22px;
  }

  /* 手机上七个分页尽量一屏放下 */
  .space-tabs {
    gap: 2px;
    padding: 0 6px;
  }

  .space-tabs__item {
    padding: 12px 8px;
  }

  .space-tabs__item.is-active::after {
    left: 8px;
    right: 8px;
  }
}
</style>

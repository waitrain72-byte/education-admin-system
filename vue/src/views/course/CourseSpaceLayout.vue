<template>
  <div class="space">
    <!-- 骨架：课程色页头（标签、课名、教师与时间）+ 分页栏 + 下面一块内容 -->
    <SkeletonRegion v-if="skeleton" class="space-sk">
      <header class="space-head">
        <div class="space-head__band space-sk__band">
          <SkBlock class="space-sk__bg" tone="cover" w="100%" h="100%" />
          <SkBlock class="space-sk__line space-sk__back" tone="on-cover" :w="52" :h="13" />
          <div class="space-head__tags">
            <SkBlock v-for="w in [38, 46, 50]" :key="w" tone="on-cover" shape="pill" :w="w" :h="20" />
          </div>
          <SkBlock class="space-sk__name" tone="on-cover" :w="220" :h="26" />
          <div class="space-head__meta">
            <SkBlock v-for="w in [48, 150, 40, 64]" :key="w" class="space-sk__line" tone="on-cover" :w="w" />
          </div>
        </div>
        <div class="space-tabs">
          <span v-for="i in 7" :key="i" class="space-tabs__item"><SkBlock :w="i === 1 ? 32 : 28" class="space-sk__line" /></span>
        </div>
      </header>
      <section class="panel panel--pad">
        <SkeletonList :rows="4" />
      </section>
    </SkeletonRegion>
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
import { useSkeleton } from '@/composables/useSkeleton'
import SkBlock from '@/components/skeleton/SkBlock.vue'
import SkeletonList from '@/components/skeleton/SkeletonList.vue'
import SkeletonRegion from '@/components/skeleton/SkeletonRegion.vue'

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

// 放在首次加载之后创建：这时 loading 已经为真，第一次渲染就是骨架。换课时页面上还有上一门课，不再出骨架
const skeleton = useSkeleton(() => !overview.value && loading.value)
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

/* ---------- 骨架：页头的课程色换成占位底色，内容位置与真实页头一致 ---------- */
.space-sk {
  display: grid;
  gap: 20px;
}

.space-sk__band {
  position: relative;
  background: none;
}

.space-sk__band > :not(.space-sk__bg) {
  position: relative;
}

.space-sk__bg {
  position: absolute;
  inset: 0;
  border-radius: 0;
}

/* 占位块高度取字号，上下补回行高差，换成内容时页头不跳 */
.space-sk__line {
  margin: 3px 0;
}

.space-sk__back {
  margin-bottom: 9px;
}

.space-sk__name {
  margin: 4px 0;
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

<template>
  <router-link :to="to" class="course-card" :class="{ 'is-finished': finished }" :style="{ '--cc': color }">
    <div class="course-card__cover">
      <div class="course-card__tags">
        <span class="course-card__tag">{{ course.type || '—' }}</span>
        <span v-if="statusLabel" class="course-card__tag course-card__tag--status">{{ statusLabel }}</span>
      </div>
      <div class="course-card__name">{{ course.name }}</div>
      <div class="course-card__time">
        <template v-if="course.week">{{ course.week }} {{ segmentShortName(course.segment) }}</template>
        <template v-else>{{ $t('courses.unscheduled') }}</template>
        <template v-if="course.room"> · {{ course.room }}</template>
      </div>
    </div>
    <div class="course-card__foot">
      <slot name="foot">
        <span>{{ course.teacherName || '—' }}</span>
        <span v-if="course.credit != null" class="num">{{ $t('courses.credits', { n: course.credit }) }}</span>
      </slot>
    </div>
  </router-link>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { courseColor } from '@/utils/courseColor'
import { segmentShortName } from '@/utils/schedule'
import { t } from '@/i18n'

export interface CourseCardData {
  id: number
  name: string
  type?: string
  credit?: number | null
  teacherName?: string
  room?: string
  week?: string
  segment?: string
  status?: string
}

const props = defineProps<{
  course: CourseCardData
  to: string
}>()

const color = computed(() => courseColor(props.course.name))
const finished = computed(() => props.course.status === '已结课')

/** 正在上的课不标状态（默认就是它），只标「未开课」「已结课」 */
const statusLabel = computed(() => {
  if (props.course.status === '未开课') return t('courses.status.notStarted')
  if (props.course.status === '已结课') return t('courses.status.finished')
  return ''
})
</script>

<style scoped>
.course-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius-lg);
  background: var(--xm-bg-card);
  overflow: hidden;
  color: var(--xm-text-primary);
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease;
}

.course-card:hover,
.course-card:focus-visible {
  transform: translateY(-2px);
  box-shadow: var(--xm-shadow-card-lg);
}

.course-card__cover {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 112px;
  padding: 14px 16px 16px;
  color: #fff;
  /* 右上角的大圆和底部的细格线：课表网格的意象 */
  background:
    radial-gradient(circle at 92% 8%, rgba(255, 255, 255, 0.18) 0 44px, transparent 45px),
    repeating-linear-gradient(90deg, rgba(255, 255, 255, 0.06) 0 1px, transparent 1px 28px),
    var(--cc);
}

.course-card.is-finished .course-card__cover {
  filter: saturate(0.45);
}

.course-card__tags {
  display: flex;
  gap: 6px;
  margin-bottom: auto;
}

.course-card__tag {
  padding: 1px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.2);
  font-size: 12px;
}

.course-card__tag--status {
  background: rgba(0, 0, 0, 0.22);
}

.course-card__name {
  margin-top: 14px;
  font-size: 17px;
  font-weight: 700;
  line-height: 1.3;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.course-card__time {
  font-size: 13px;
  opacity: 0.9;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.course-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 16px;
  font-size: 13px;
  color: var(--xm-text-secondary);
}

@media (prefers-reduced-motion: reduce) {
  .course-card {
    transition: none;
  }

  .course-card:hover {
    transform: none;
  }
}
</style>

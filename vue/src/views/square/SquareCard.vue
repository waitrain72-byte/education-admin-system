<template>
  <article class="sq-card" :class="{ 'is-ended': ended }" :style="{ '--cc': color }">
    <router-link :to="`/course/${course.id}`" class="sq-card__cover">
      <span class="sq-card__tags">
        <span class="sq-card__tag">{{ course.type || '—' }}</span>
        <span class="sq-card__tag num">{{ $t('courses.credits', { n: course.credit ?? '—' }) }}</span>
        <span v-if="course.status !== '已开课'" class="sq-card__tag">{{ course.status || '—' }}</span>
      </span>
      <span class="sq-card__name">{{ course.name }}</span>
      <span class="sq-card__time">
        <template v-if="course.week">{{ course.week }} {{ segmentShortName(course.segment) }}</template>
        <template v-else>{{ $t('courses.unscheduled') }}</template>
        <template v-if="course.room"> · {{ course.room }}</template>
      </span>
    </router-link>

    <div class="sq-card__body">
      <div class="sq-card__teacher">{{ course.teacherName || '—' }}</div>
      <p v-if="course.intro" class="sq-card__intro">{{ course.intro }}</p>
      <p v-if="course.reason" class="sq-card__reason">{{ course.reason }}</p>

      <div class="sq-card__seats">
        <span class="sq-card__bar" aria-hidden="true">
          <span :style="{ width: fillPercent + '%' }" :class="{ 'is-full': full }"></span>
        </span>
        <span class="sq-card__seat-text num">
          <template v-if="course.seatsLeft == null">{{ $t('square.noLimit') }}</template>
          <template v-else-if="full">{{ $t('square.full') }}</template>
          <template v-else>{{ $t('square.seatsLeft', { n: course.seatsLeft }) }}</template>
        </span>
      </div>

      <div class="sq-card__foot">
        <span v-if="course.enrolled" class="pill pill--ok">{{ $t('square.enrolled') }}</span>
        <span v-else-if="course.conflict" class="pill pill--warn sq-card__conflict" :title="$t('square.conflict', { name: course.conflict })">
          {{ $t('square.conflict', { name: course.conflict }) }}
        </span>
        <span class="sq-card__spacer"></span>
        <template v-if="canEnroll">
          <el-button v-if="course.enrolled && course.canDrop" size="small" plain :loading="busy" @click="emit('drop', course)">
            {{ $t('square.drop') }}
          </el-button>
          <el-button
            v-else-if="!course.enrolled && !ended"
            size="small"
            type="primary"
            :disabled="full || !!course.conflict"
            :loading="busy"
            @click="emit('enroll', course)"
          >
            {{ $t('square.enroll') }}
          </el-button>
        </template>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { courseColor } from '@/utils/courseColor'
import { segmentShortName } from '@/utils/schedule'

/**
 * 课程广场的卡片：上半截是课程色封面（点进课程概览），下半截是教师、简介、名额和选课按钮。
 * 按钮不放在链接里面（链接里套按钮既不合法，也会点一下就跳走）。
 */
const props = defineProps<{
  course: Record<string, any>
  canEnroll: boolean
  busy?: boolean
}>()
const emit = defineEmits<{
  enroll: [course: Record<string, any>]
  drop: [course: Record<string, any>]
}>()

const color = computed(() => courseColor(props.course.name))
const ended = computed(() => props.course.status === '已结课')
const full = computed(() => props.course.seatsLeft != null && props.course.seatsLeft <= 0)
const fillPercent = computed(() => {
  const cap = Number(props.course.capacity) || 0
  if (!cap) return 0
  return Math.min(100, Math.round((Number(props.course.studentCount) / cap) * 100))
})
</script>

<style scoped>
.sq-card {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius-lg);
  background: var(--xm-bg-card);
  overflow: hidden;
  transition:
    box-shadow 0.15s ease,
    transform 0.15s ease;
}

.sq-card:hover {
  box-shadow: var(--xm-shadow-card);
}

.sq-card.is-ended {
  opacity: 0.75;
}

.sq-card__cover {
  display: grid;
  gap: 4px;
  min-height: 112px;
  padding: 14px 16px;
  color: #fff;
  background:
    radial-gradient(circle at 100% 0%, rgba(255, 255, 255, 0.16) 0 64px, transparent 65px),
    repeating-linear-gradient(90deg, rgba(255, 255, 255, 0.06) 0 1px, transparent 1px 24px),
    var(--cc);
}

.sq-card__tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.sq-card__tag {
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.2);
  font-size: 12px;
  line-height: 20px;
}

.sq-card__name {
  font-size: 17px;
  font-weight: 700;
  line-height: 1.3;
}

.sq-card__time {
  font-size: 12px;
  opacity: 0.92;
}

.sq-card__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  padding: 12px 16px 14px;
}

.sq-card__teacher {
  font-size: 13px;
  color: var(--xm-text-regular);
}

.sq-card__intro {
  display: -webkit-box;
  font-size: 12px;
  line-height: 1.6;
  color: var(--xm-text-secondary);
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.sq-card__reason {
  font-size: 12px;
  color: var(--xm-brand);
}

.sq-card__seats {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: auto;
}

.sq-card__bar {
  flex: 1;
  height: 5px;
  border-radius: 999px;
  background: var(--xm-bg-sunken);
  overflow: hidden;
}

.sq-card__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--xm-brand);
}

.sq-card__bar span.is-full {
  background: var(--xm-bad);
}

.sq-card__seat-text {
  font-size: 12px;
  color: var(--xm-text-secondary);
  white-space: nowrap;
}

.sq-card__foot {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 28px;
}

.sq-card__conflict {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sq-card__spacer {
  flex: 1;
}
</style>

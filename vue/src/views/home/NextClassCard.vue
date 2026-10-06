<template>
  <div class="next" :style="next ? { '--cc': courseColor(next.name) } : undefined">
    <template v-if="next">
      <div class="next__label">
        <span class="next__dot" :class="{ 'is-live': state === 'ongoing' }"></span>
        {{ state === 'ongoing' ? $t('workbench.next.ongoing') : $t('workbench.next.upcoming', { time: next.start }) }}
      </div>
      <router-link :to="`/course/${next.id}`" class="next__name">{{ next.name }}</router-link>
      <div class="next__meta">{{ [next.room, showTeacher ? next.teacherName : null].filter(Boolean).join(' · ') }}</div>
      <div class="next__foot num">
        <template v-if="state === 'ongoing'">{{ $t('workbench.next.endsAt', { time: next.end }) }}</template>
        <template v-else-if="countdown">{{ countdown }}</template>
      </div>
      <slot name="action" :course="next" :state="state" />
    </template>
    <template v-else>
      <div class="next__label">{{ $t('workbench.next.title') }}</div>
      <div class="next__none">{{ hasClassesToday ? $t('workbench.next.allDone') : $t('workbench.next.noneToday') }}</div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { classState, minutesUntil, nextClass } from '@/utils/schedule'
import { courseColor } from '@/utils/courseColor'
import { useServerClock } from '@/composables/useServerClock'
import { t } from '@/i18n'
import type { TodayCourse } from './TodayClasses.vue'

const props = withDefaults(defineProps<{ courses: TodayCourse[]; showTeacher?: boolean }>(), { showTeacher: true })

const clock = useServerClock()
const next = computed(() => nextClass(props.courses, clock.value.hhmm))
const state = computed(() => (next.value ? classState(next.value.segment, clock.value.hhmm) : ''))
const hasClassesToday = computed(() => props.courses.length > 0)

/** 距离上课：一小时以内显示分钟，再远显示「X 小时 Y 分」 */
const countdown = computed(() => {
  if (!next.value) return ''
  const minutes = minutesUntil(next.value.segment, clock.value.hhmm)
  if (minutes === null) return ''
  if (minutes < 60) return t('workbench.next.inMinutes', { n: minutes })
  return t('workbench.next.inHours', { h: Math.floor(minutes / 60), m: minutes % 60 })
})
</script>

<style scoped>
.next {
  position: relative;
  display: grid;
  gap: 4px;
  width: 300px;
  max-width: 100%;
  padding: 16px 18px 16px 22px;
  border-radius: var(--xm-radius-lg);
  background: var(--xm-bg-card);
  border: 1px solid var(--xm-border);
  box-shadow: var(--xm-shadow-card-lg);
  overflow: hidden;
}

/* 左侧一条课程色竖条，与课程卡片、课表里同一门课的颜色一致 */
.next::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 6px;
  background: var(--cc, var(--xm-border));
}

.next__label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.next__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--xm-info);
}

.next__dot.is-live {
  background: var(--xm-ok);
  animation: pulse 1.6s ease-in-out infinite;
}

@keyframes pulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(29, 128, 72, 0.45);
  }
  50% {
    box-shadow: 0 0 0 5px rgba(29, 128, 72, 0);
  }
}

.next__name {
  font-size: 18px;
  font-weight: 700;
  color: var(--xm-text-primary);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.next__name:hover {
  color: var(--xm-brand);
}

.next__meta {
  font-size: 13px;
  color: var(--xm-text-regular);
}

.next__foot {
  margin-top: 4px;
  font-size: 13px;
  font-weight: 600;
  color: var(--xm-brand);
}

.next__none {
  margin-top: 6px;
  font-size: 15px;
  color: var(--xm-text-regular);
}

@media (prefers-reduced-motion: reduce) {
  .next__dot.is-live {
    animation: none;
  }
}

@media (max-width: 640px) {
  .next {
    width: 100%;
  }
}
</style>

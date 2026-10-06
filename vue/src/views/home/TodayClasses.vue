<template>
  <ol v-if="courses.length" class="today">
    <li v-for="c in sorted" :key="c.id" class="today__row" :class="'is-' + (stateOf(c) || 'unknown')">
      <div class="today__time num">
        <b>{{ c.start || '—' }}</b>
        <span>{{ c.end }}</span>
      </div>
      <span class="today__bar" :style="{ background: courseColor(c.name) }"></span>
      <div class="today__info">
        <router-link :to="`/course/${c.id}`" class="today__name">{{ c.name }}</router-link>
        <div class="today__meta">
          {{ [c.room, showTeacher ? c.teacherName : null].filter(Boolean).join(' · ') || '—' }}
        </div>
      </div>
      <span class="pill" :class="pillClass(c)">{{ stateLabel(c) }}</span>
      <slot name="action" :course="c" :state="stateOf(c)" />
    </li>
  </ol>
  <div v-else class="empty-note">{{ $t('workbench.noClassToday') }}</div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { classState, minutesUntil, segmentIndex, type ClassState } from '@/utils/schedule'
import { courseColor } from '@/utils/courseColor'
import { useServerClock } from '@/composables/useServerClock'
import { t } from '@/i18n'

export interface TodayCourse {
  id: number
  name: string
  room?: string
  teacherName?: string
  segment?: string
  start?: string
  end?: string
}

const props = withDefaults(defineProps<{ courses: TodayCourse[]; showTeacher?: boolean }>(), { showTeacher: true })

const clock = useServerClock()
const sorted = computed(() => [...props.courses].sort((a, b) => segmentIndex(a.segment) - segmentIndex(b.segment)))

const stateOf = (c: TodayCourse): ClassState => classState(c.segment, clock.value.hhmm)

const stateLabel = (c: TodayCourse) => {
  const state = stateOf(c)
  if (state === 'ongoing') return t('workbench.state.ongoing')
  if (state === 'done') return t('workbench.state.done')
  if (state === 'upcoming') {
    const minutes = minutesUntil(c.segment, clock.value.hhmm)
    return minutes !== null && minutes <= 60 ? t('workbench.state.soon', { n: minutes }) : t('workbench.state.upcoming')
  }
  return '—'
}

const pillClass = (c: TodayCourse) => {
  const state = stateOf(c)
  if (state === 'ongoing') return 'pill--brand'
  if (state === 'upcoming') return 'pill--info'
  return ''
}
</script>

<style scoped>
.today {
  list-style: none;
  display: grid;
  gap: 6px;
}

.today__row {
  display: grid;
  grid-template-columns: 54px 4px minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: var(--xm-radius);
}

.today__row.is-ongoing {
  background: var(--xm-brand-soft);
}

.today__row.is-done {
  opacity: 0.6;
}

.today__time {
  display: grid;
  line-height: 1.25;
}

.today__time b {
  font-size: 16px;
  color: var(--xm-text-primary);
}

.today__time span {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.today__bar {
  align-self: stretch;
  width: 4px;
  border-radius: 2px;
}

.today__name {
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 15px;
  font-weight: 600;
  color: var(--xm-text-primary);
}

.today__name:hover {
  color: var(--xm-brand);
}

.today__meta {
  margin-top: 2px;
  font-size: 13px;
  color: var(--xm-text-secondary);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
</style>

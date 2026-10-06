<template>
  <section class="panel">
    <div class="panel__head">
      <span class="panel__title">{{ $t('workbench.exams') }}</span>
      <router-link v-if="moreTo" :to="moreTo" class="panel__more">{{ $t('home.viewAll') }} ›</router-link>
    </div>
    <div class="panel__body">
      <ul v-if="exams.length" class="exams">
        <li v-for="e in exams" :key="e.id" class="exams__row">
          <div class="exams__date num">
            <b>{{ day(e.examTime) }}</b>
            <span>{{ month(e.examTime) }}</span>
          </div>
          <div class="exams__info">
            <div class="exams__name">{{ e.name }}</div>
            <div class="exams__meta num">{{ e.examTime }}</div>
          </div>
          <ExamCountdownTag :exam-time="e.examTime" />
        </li>
      </ul>
      <div v-else class="empty-note">{{ $t('workbench.noExams') }}</div>
    </div>
  </section>
</template>

<script setup lang="ts">
import ExamCountdownTag from '@/components/ExamCountdownTag.vue'

withDefaults(defineProps<{ exams: Array<Record<string, any>>; moreTo?: string }>(), { moreTo: '' })

/** 考试时间「2026-11-02 09:00」→ 日 02、月 11 月（日历撕页的样子） */
const day = (time: string) => (time || '').slice(8, 10) || '—'
const month = (time: string) => {
  const m = Number((time || '').slice(5, 7))
  return m ? `${m}月` : ''
}
</script>

<style scoped>
.exams {
  list-style: none;
  display: grid;
  gap: 10px;
}

.exams__row {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
}

.exams__date {
  display: grid;
  justify-items: center;
  padding: 6px 0;
  border-radius: 10px;
  background: var(--xm-bg-sunken);
  line-height: 1.15;
}

.exams__date b {
  font-size: 18px;
  color: var(--xm-text-primary);
}

.exams__date span {
  font-size: 11px;
  color: var(--xm-text-secondary);
}

.exams__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--xm-text-primary);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.exams__meta {
  font-size: 12px;
  color: var(--xm-text-secondary);
}
</style>

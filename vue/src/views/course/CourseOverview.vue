<template>
  <div class="ov">
    <section class="panel">
      <div class="panel__head">
        <span class="panel__title">{{ $t('space.overview.info') }}</span>
      </div>
      <div class="panel__body">
        <dl class="facts">
          <div>
            <dt>{{ $t('space.overview.time') }}</dt>
            <dd>
              <template v-if="course.week">{{ course.week }} {{ segmentShortName(course.segment) }}</template>
              <template v-else>{{ $t('courses.unscheduled') }}</template>
              <span v-if="course.start" class="num facts__sub">{{ course.start }}–{{ course.end }}</span>
            </dd>
          </div>
          <div>
            <dt>{{ $t('space.overview.room') }}</dt>
            <dd>{{ course.room || '—' }}</dd>
          </div>
          <div>
            <dt>{{ $t('space.overview.type') }}</dt>
            <dd>{{ course.type || '—' }}</dd>
          </div>
          <div>
            <dt>{{ $t('space.overview.credit') }}</dt>
            <dd class="num">{{ course.credit ?? '—' }}</dd>
          </div>
          <div>
            <dt>{{ $t('space.overview.enrolled') }}</dt>
            <dd class="num">
              {{ overview.studentCount }}<template v-if="course.capacity"> / {{ course.capacity }}</template>
            </dd>
          </div>
          <div>
            <dt>{{ $t('space.overview.status') }}</dt>
            <dd>{{ course.status || '—' }}</dd>
          </div>
        </dl>
        <div v-if="course.capacity" class="capacity" :aria-label="$t('space.overview.enrolled')">
          <div class="capacity__bar" :style="{ width: capacityPercent + '%' }"></div>
        </div>
      </div>
    </section>

    <aside class="ov__side">
      <section class="panel">
        <div class="panel__head">
          <span class="panel__title">{{ $t('space.overview.teacher') }}</span>
        </div>
        <div class="panel__body teacher">
          <template v-if="overview.teacher">
            <UserAvatar :name="overview.teacher.name" :avatar="overview.teacher.avatar" :size="52" />
            <div class="teacher__info">
              <div class="teacher__name">{{ overview.teacher.name }}</div>
              <div class="teacher__title">{{ overview.teacher.title || '—' }}</div>
              <div v-if="overview.teacher.email" class="teacher__mail">{{ overview.teacher.email }}</div>
            </div>
          </template>
          <div v-else class="empty-note">—</div>
        </div>
      </section>

      <section class="panel panel--pad relation">
        <span class="pill" :class="relationPill">{{ $t('space.relation.' + overview.relation) }}</span>
        <p class="relation__text">{{ $t('space.relationHint.' + overview.relation) }}</p>
      </section>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { segmentShortName } from '@/utils/schedule'

const props = defineProps<{ overview: Record<string, any> }>()

const course = computed(() => props.overview.course || {})
const capacityPercent = computed(() => {
  const cap = Number(course.value.capacity) || 0
  if (!cap) return 0
  return Math.min(100, Math.round((Number(props.overview.studentCount) / cap) * 100))
})

const relationPill = computed(() => (props.overview.relation === 'visitor' ? '' : 'pill--brand'))
</script>

<style scoped>
.ov {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  align-items: start;
}

.ov__side {
  display: grid;
  gap: 20px;
}

.facts {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px 24px;
}

.facts dt {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.facts dd {
  margin-top: 4px;
  font-size: 15px;
  color: var(--xm-text-primary);
}

.facts__sub {
  margin-left: 6px;
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.capacity {
  height: 6px;
  margin-top: 18px;
  border-radius: 999px;
  background: var(--xm-bg-sunken);
  overflow: hidden;
}

.capacity__bar {
  height: 100%;
  border-radius: inherit;
  background: var(--xm-brand);
}

.teacher {
  display: flex;
  align-items: center;
  gap: 14px;
}

.teacher__info {
  min-width: 0;
}

.teacher__name {
  font-size: 16px;
  font-weight: 600;
  color: var(--xm-text-primary);
}

.teacher__title,
.teacher__mail {
  font-size: 13px;
  color: var(--xm-text-secondary);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.relation {
  display: grid;
  gap: 8px;
  justify-items: start;
}

.relation__text {
  font-size: 13px;
  color: var(--xm-text-secondary);
}

@media (max-width: 960px) {
  .ov {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>

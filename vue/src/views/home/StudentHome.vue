<template>
  <div class="home">
    <HomeHero :eyebrow="semesterLine" :title="$t('workbench.hello', { name: user.name, greeting })">
      <span>
        {{ $t('workbench.stats.credits') }}
        <b class="num">{{ data.stats.credits }}</b><template v-if="data.stats.requiredCredits"> / <span class="num">{{ data.stats.requiredCredits }}</span></template>
      </span>
      <span>{{ $t('workbench.stats.gpa') }} <b class="num">{{ data.stats.gpa ?? '—' }}</b></span>
      <span>
        {{ $t('workbench.stats.attendance') }}
        <b class="num">{{ data.stats.attendanceRate == null ? '—' : data.stats.attendanceRate + '%' }}</b>
      </span>
      <template #aside>
        <NextClassCard :courses="data.todayCourses" />
      </template>
    </HomeHero>

    <div v-if="data.warning" class="warning-bar" role="status">
      <span class="pill" :class="data.warning.level === '高风险' ? 'pill--bad' : 'pill--warn'">{{ data.warning.level }}</span>
      <span class="warning-bar__text">
        {{ $t('workbench.warning', { index: data.warning.riskIndex }) }}{{ data.warning.suggestion }}
      </span>
    </div>

    <div class="home-grid">
      <div class="home-grid__main">
        <section class="panel">
          <div class="panel__head">
            <span class="panel__title">{{ $t('workbench.todayTitle') }}</span>
            <span class="panel__hint">{{ data.semester.weekday }}</span>
          </div>
          <div class="panel__body">
            <TodayClasses :courses="data.todayCourses" />
          </div>
        </section>

        <section class="panel">
          <div class="panel__head">
            <span class="panel__title">{{ $t('workbench.myCourses') }}</span>
            <router-link to="/courses" class="panel__more">{{ $t('workbench.allCourses') }} ›</router-link>
          </div>
          <div class="panel__body">
            <div v-if="activeCourses.length" class="course-grid">
              <CourseCard v-for="c in activeCourses.slice(0, 6)" :key="c.id" :course="c" :to="`/course/${c.id}`" />
            </div>
            <div v-else class="empty-note">
              {{ $t('workbench.noCourses') }}
              <router-link to="/square" class="panel__more">{{ $t('workbench.goSelect') }} ›</router-link>
            </div>
          </div>
        </section>
      </div>

      <aside class="home-grid__side">
        <section class="panel">
          <div class="panel__head">
            <span class="panel__title">{{ $t('workbench.progress') }}</span>
            <router-link to="/grades" class="panel__more">{{ $t('transcript.entry') }} ›</router-link>
          </div>
          <div class="panel__body progress">
            <ProgressRing
              :value="data.stats.credits"
              :max="data.stats.requiredCredits"
              :size="116"
              :aria-label="$t('workbench.stats.credits')"
            >
              <b class="num progress__value">{{ data.stats.credits }}</b>
              <span class="progress__unit">
                {{ data.stats.requiredCredits ? $t('workbench.ofCredits', { n: data.stats.requiredCredits }) : $t('workbench.creditsUnit') }}
              </span>
            </ProgressRing>
            <dl class="progress__list">
              <div>
                <dt>{{ $t('workbench.stats.gpa') }}</dt>
                <dd class="num">{{ data.stats.gpa ?? '—' }}</dd>
              </div>
              <div>
                <dt>{{ $t('workbench.stats.attendance') }}</dt>
                <dd class="num">{{ data.stats.attendanceRate == null ? '—' : data.stats.attendanceRate + '%' }}</dd>
              </div>
              <div>
                <dt>{{ $t('workbench.stats.ongoing') }}</dt>
                <dd class="num">{{ data.stats.ongoingCourses }}</dd>
              </div>
            </dl>
          </div>
        </section>

        <section class="panel">
          <div class="panel__head">
            <span class="panel__title">{{ $t('workbench.dueTitle') }}</span>
          </div>
          <div class="panel__body">
            <ul v-if="data.todos?.length" class="due">
              <li v-for="todo in data.todos" :key="todo.id">
                <router-link :to="`/course/${todo.courseId}/assignments?open=${todo.id}`" class="due__item">
                  <span class="due__bar" :style="{ background: courseColor(todo.courseName) }"></span>
                  <span class="due__text">
                    <span class="due__title">{{ todo.title }}</span>
                    <span class="due__meta">{{ todo.courseName }} · <span class="num">{{ $t('workbench.dueAt', { time: todo.deadline.slice(5) }) }}</span></span>
                  </span>
                </router-link>
              </li>
            </ul>
            <div v-else class="empty-note">{{ $t('workbench.noDue') }}</div>
          </div>
        </section>

        <ExamsPanel :exams="data.exams" />
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUser } from '@/components/useUser'
import CourseCard from '@/components/CourseCard.vue'
import ProgressRing from '@/components/ProgressRing.vue'
import HomeHero from './HomeHero.vue'
import NextClassCard from './NextClassCard.vue'
import TodayClasses from './TodayClasses.vue'
import ExamsPanel from './ExamsPanel.vue'
import { useHomeText } from './useHomeText'
import { courseColor } from '@/utils/courseColor'

const props = defineProps<{ data: Record<string, any> }>()

const { user } = useUser()
const { greeting, semesterLine } = useHomeText(() => props.data.semester)

/** 首页只放还在上的课（未开课、已开课），已结课的去「课程」页看 */
const activeCourses = computed(() => (props.data.courses || []).filter((c: any) => c.status !== '已结课'))
</script>

<style scoped>
.progress {
  display: flex;
  align-items: center;
  gap: 20px;
}

.progress__value {
  font-size: 28px;
  line-height: 1;
  color: var(--xm-text-primary);
}

.progress__unit {
  margin-top: 4px;
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.progress__list {
  display: grid;
  gap: 10px;
  flex: 1;
}

.progress__list div {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 8px;
}

.progress__list dt {
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.progress__list dd {
  font-size: 18px;
  font-weight: 600;
  color: var(--xm-text-primary);
}

.warning-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius);
  background: var(--xm-bg-card);
  font-size: 14px;
  color: var(--xm-text-regular);
}

.warning-bar__text {
  min-width: 0;
}

.due {
  list-style: none;
  display: grid;
  gap: 4px;
}

.due__item {
  display: flex;
  align-items: stretch;
  gap: 10px;
  padding: 8px 10px;
  margin: 0 -10px;
  border-radius: var(--xm-radius);
}

.due__item:hover {
  background: var(--xm-bg-hover);
}

.due__bar {
  width: 4px;
  border-radius: 2px;
  flex-shrink: 0;
}

.due__text {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.due__title {
  font-size: 14px;
  color: var(--xm-text-primary);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.due__meta {
  font-size: 12px;
  color: var(--xm-text-secondary);
}
</style>

<template>
  <div class="home">
    <HomeHero :eyebrow="semesterLine" :title="$t('workbench.hello', { name: user.name, greeting })">
      <template #aside>
        <div class="quick">
          <router-link v-for="action in actions" :key="action.to" :to="action.to" class="quick__item">
            <el-icon class="quick__icon"><component :is="action.icon" /></el-icon>
            <span>{{ $t(action.label) }}</span>
          </router-link>
        </div>
      </template>
    </HomeHero>

    <div class="kpis">
      <div v-for="kpi in kpis" :key="kpi.key" class="kpi" :class="{ 'kpi--alert': kpi.alert }">
        <span class="kpi__label">{{ $t(kpi.label) }}</span>
        <b class="kpi__value num">{{ kpi.value }}</b>
        <span v-if="kpi.sub" class="kpi__sub">{{ kpi.sub }}</span>
      </div>
    </div>

    <div class="home-grid">
      <div class="home-grid__main">
        <section class="panel">
          <div class="panel__head">
            <span class="panel__title">{{ $t('workbench.admin.pendingLeaves') }}</span>
            <router-link to="/admin/leaves" class="panel__more">{{ $t('workbench.admin.goReview') }} ›</router-link>
          </div>
          <div class="panel__body">
            <ul v-if="data.pendingApplies.length" class="rows">
              <li v-for="a in data.pendingApplies" :key="a.id">
                <router-link :to="`/admin/leaves?open=${a.id}`" class="rows__item rows__item--link">
                  <UserAvatar :name="a.studentName" :size="30" />
                  <div class="rows__main">
                    <div class="rows__title">
                      {{ a.studentName || '—' }}
                      <span class="rows__meta num">{{ a.time }} · {{ $t('workbench.admin.days', { n: a.day }) }}</span>
                    </div>
                    <div class="rows__text">{{ a.content }}</div>
                  </div>
                </router-link>
              </li>
            </ul>
            <div v-else class="empty-note">{{ $t('workbench.admin.noPendingLeaves') }}</div>
          </div>
        </section>

        <section class="panel">
          <div class="panel__head">
            <span class="panel__title">{{ $t('workbench.admin.unscheduled') }}</span>
            <router-link to="/admin/courses?filter=unscheduled" class="panel__more">{{ $t('workbench.admin.goSchedule') }} ›</router-link>
          </div>
          <div class="panel__body">
            <ul v-if="data.unscheduledCourses.length" class="rows">
              <li v-for="c in data.unscheduledCourses" :key="c.id">
                <!-- 点一门课直接打开它的排课向导 -->
                <router-link :to="`/admin/courses?filter=unscheduled&edit=${c.id}`" class="rows__item rows__item--link">
                  <span class="rows__swatch" :style="{ background: courseColor(c.name) }"></span>
                  <div class="rows__main">
                    <div class="rows__title">{{ c.name }}</div>
                    <div class="rows__text">{{ c.teacherName || '—' }} · {{ c.status || '—' }}</div>
                  </div>
                </router-link>
              </li>
            </ul>
            <div v-else class="empty-note">{{ $t('workbench.admin.allScheduled') }}</div>
          </div>
        </section>
      </div>

      <aside class="home-grid__side">
        <ExamsPanel :exams="data.exams" more-to="/admin/exams" />

        <section class="panel">
          <div class="panel__head">
            <span class="panel__title">{{ $t('workbench.admin.notices') }}</span>
            <router-link to="/messages?tab=notices" class="panel__more">{{ $t('home.viewAll') }} ›</router-link>
          </div>
          <div class="panel__body">
            <ul v-if="data.notices.length" class="notices">
              <li v-for="n in data.notices" :key="n.id">
                <router-link :to="`/messages?tab=notices&notice=${n.id}`" class="notices__title">{{ n.title }}</router-link>
                <span class="notices__time num">{{ n.time }}</span>
              </li>
            </ul>
            <div v-else class="empty-note">{{ $t('common.empty') }}</div>
          </div>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Calendar, DataBoard, EditPen, Reading } from '@element-plus/icons-vue'
import { useUser } from '@/components/useUser'
import UserAvatar from '@/components/UserAvatar.vue'
import HomeHero from './HomeHero.vue'
import ExamsPanel from './ExamsPanel.vue'
import { useHomeText } from './useHomeText'
import { courseColor } from '@/utils/courseColor'
import { t } from '@/i18n'

const props = defineProps<{ data: Record<string, any> }>()

const { user } = useUser()
const { greeting, semesterLine } = useHomeText(() => props.data.semester)

const actions = [
  { to: '/admin/courses?create=1', label: 'workbench.admin.actions.openCourse', icon: Reading },
  { to: '/messages?tab=notices&compose=1', label: 'workbench.admin.actions.notice', icon: EditPen },
  { to: '/admin/semester', label: 'workbench.admin.actions.semester', icon: Calendar },
  { to: '/dashboard', label: 'workbench.admin.actions.screen', icon: DataBoard },
]

const kpis = computed(() => {
  const k = props.data.kpis || {}
  return [
    { key: 'students', label: 'workbench.admin.kpi.students', value: k.studentCount ?? 0 },
    { key: 'teachers', label: 'workbench.admin.kpi.teachers', value: k.teacherCount ?? 0 },
    {
      key: 'courses',
      label: 'workbench.admin.kpi.activeCourses',
      value: k.activeCourseCount ?? 0,
      sub: t('workbench.admin.kpi.ofCourses', { n: k.courseCount ?? 0 }),
    },
    { key: 'choices', label: 'workbench.admin.kpi.choices', value: k.choiceCount ?? 0 },
    { key: 'leaves', label: 'workbench.admin.kpi.pendingLeaves', value: k.pendingApply ?? 0, alert: (k.pendingApply ?? 0) > 0 },
    { key: 'homework', label: 'workbench.admin.kpi.ungraded', value: k.ungradedHomework ?? 0 },
    { key: 'logins', label: 'workbench.admin.kpi.loginToday', value: k.loginToday ?? 0 },
  ]
})
</script>

<style scoped>
.quick {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.quick__item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 132px;
  padding: 12px 14px;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius);
  background: var(--xm-bg-card);
  color: var(--xm-text-primary);
  font-size: 14px;
  font-weight: 600;
  transition: border-color 0.2s ease;
}

.quick__item:hover,
.quick__item:focus-visible {
  border-color: var(--xm-brand);
  color: var(--xm-brand);
}

.quick__icon {
  font-size: 18px;
  color: var(--xm-brand);
}

.kpis {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}

.kpi {
  display: grid;
  /* 同一行里有的卡片多一行小字：内容贴顶排列，不要被拉开 */
  align-content: start;
  gap: 2px;
  padding: 14px 16px;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius);
  background: var(--xm-bg-card);
}

.kpi__label {
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.kpi__value {
  font-size: 28px;
  line-height: 1.2;
  color: var(--xm-text-primary);
}

.kpi__sub {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

/* 有待处理事项时数字变成警示色，而不是整块染色 */
.kpi--alert .kpi__value {
  color: var(--xm-warn);
}

.rows {
  list-style: none;
  display: grid;
  gap: 12px;
}

.rows__item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.rows__item--link {
  margin: -6px -8px;
  padding: 6px 8px;
  border-radius: var(--xm-radius-sm);
}

.rows__item--link:hover {
  background: var(--xm-bg-hover);
}

.rows__swatch {
  width: 10px;
  height: 30px;
  border-radius: 4px;
  flex-shrink: 0;
}

.rows__main {
  min-width: 0;
}

.rows__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--xm-text-primary);
}

.rows__meta {
  margin-left: 6px;
  font-size: 12px;
  font-weight: 400;
  color: var(--xm-text-secondary);
}

.rows__text {
  margin-top: 2px;
  font-size: 13px;
  color: var(--xm-text-secondary);
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notices {
  list-style: none;
  display: grid;
  gap: 10px;
}

.notices li {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.notices__title {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 14px;
  color: var(--xm-text-primary);
}

.notices__title:hover {
  color: var(--xm-brand);
}

.notices__time {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--xm-text-secondary);
}

@media (max-width: 640px) {
  .quick {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    width: 100%;
  }

  .quick__item {
    min-width: 0;
  }
}
</style>

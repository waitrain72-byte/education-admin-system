<template>
  <!-- 首页骨架：按身份画出三种首页各自的版式，面板标题和「全部课程」这类入口照常显示，只有数据位置是占位块 -->
  <SkeletonRegion class="home">
    <HomeHero>
      <template #eyebrow><SkBlock class="hs-eyebrow" tone="brand" :w="240" :h="13" /></template>
      <template #title><SkBlock class="hs-title" tone="brand" :w="210" :h="26" /></template>
      <template v-if="role !== 'ADMIN'" #default>
        <SkBlock v-for="w in [104, 96, 92]" :key="w" tone="brand" :w="w" :h="18" />
      </template>
      <template #aside>
        <div v-if="role === 'ADMIN'" class="hs-quick">
          <div v-for="i in 4" :key="i" class="hs-quick__item">
            <SkBlock :w="18" :h="18" />
            <SkBlock :w="64" />
          </div>
        </div>
        <div v-else class="hs-next">
          <SkBlock :w="120" :h="12" />
          <SkBlock :w="150" :h="18" />
          <SkBlock :w="96" :h="12" />
          <SkBlock :w="72" :h="12" />
          <SkBlock v-if="role === 'TEACHER'" class="hs-next__action" :w="72" :h="24" />
        </div>
      </template>
    </HomeHero>

    <div v-if="role === 'ADMIN'" class="hs-kpis">
      <div v-for="i in 7" :key="i" class="hs-kpi">
        <SkBlock :w="64" :h="12" />
        <SkBlock :w="i % 3 === 0 ? 40 : 56" :h="28" />
      </div>
    </div>

    <div class="home-grid">
      <!-- 学生、教师 -->
      <div v-if="role !== 'ADMIN'" class="home-grid__main">
        <section class="panel">
          <div class="panel__head">
            <span class="panel__title">{{ $t('workbench.todayTitle') }}</span>
            <SkBlock :w="40" :h="12" />
          </div>
          <div class="panel__body">
            <div class="hs-today">
              <div v-for="i in 2" :key="i" class="hs-today__row">
                <span class="sk-stack hs-today__time">
                  <SkBlock :w="40" :h="16" />
                  <SkBlock :w="34" :h="11" />
                </span>
                <SkBlock :w="4" :h="38" />
                <span class="sk-stack">
                  <SkBlock :w="i === 1 ? '46%' : '58%'" />
                  <SkBlock :w="i === 1 ? '30%' : '24%'" :h="11" />
                </span>
                <SkBlock :w="52" :h="22" />
              </div>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel__head">
            <span class="panel__title">{{ $t(role === 'TEACHER' ? 'workbench.teachingCourses' : 'workbench.myCourses') }}</span>
            <span class="panel__links">
              <router-link v-if="role === 'TEACHER' && hasPermission('dashboard:view')" to="/dashboard" class="panel__more">
                {{ $t('pages.dashboard.entry') }} ›
              </router-link>
              <router-link to="/courses" class="panel__more">{{ $t('workbench.allCourses') }} ›</router-link>
            </span>
          </div>
          <div class="panel__body">
            <div class="course-grid">
              <CourseCardSkeleton v-for="i in 3" :key="i" :index="i" />
            </div>
          </div>
        </section>
      </div>

      <!-- 管理员 -->
      <div v-else class="home-grid__main">
        <section class="panel">
          <div class="panel__head">
            <span class="panel__title">{{ $t('workbench.admin.pendingLeaves') }}</span>
            <router-link to="/admin/leaves" class="panel__more">{{ $t('workbench.admin.goReview') }} ›</router-link>
          </div>
          <div class="panel__body"><SkeletonList :rows="3" lead="avatar" /></div>
        </section>
        <section class="panel">
          <div class="panel__head">
            <span class="panel__title">{{ $t('workbench.admin.unscheduled') }}</span>
            <router-link to="/admin/courses?filter=unscheduled" class="panel__more">{{ $t('workbench.admin.goSchedule') }} ›</router-link>
          </div>
          <div class="panel__body"><SkeletonList :rows="2" lead="bar" /></div>
        </section>
      </div>

      <aside class="home-grid__side">
        <template v-if="role === 'STUDENT'">
          <section class="panel">
            <div class="panel__head">
              <span class="panel__title">{{ $t('workbench.progress') }}</span>
              <router-link to="/grades" class="panel__more">{{ $t('transcript.entry') }} ›</router-link>
            </div>
            <div class="panel__body hs-progress">
              <SkBlock class="hs-ring" :w="116" :h="116" shape="circle" />
              <dl class="hs-progress__list">
                <div v-for="key in ['gpa', 'attendance', 'ongoing']" :key="key">
                  <dt>{{ $t('workbench.stats.' + key) }}</dt>
                  <dd><SkBlock :w="key === 'ongoing' ? 22 : 44" :h="16" /></dd>
                </div>
              </dl>
            </div>
          </section>
          <section class="panel">
            <div class="panel__head">
              <span class="panel__title">{{ $t('workbench.dueTitle') }}</span>
            </div>
            <div class="panel__body"><SkeletonList :rows="2" lead="bar" /></div>
          </section>
        </template>

        <template v-else-if="role === 'TEACHER'">
          <section class="panel">
            <div class="panel__head">
              <span class="panel__title">{{ $t('workbench.todos') }}</span>
            </div>
            <div class="panel__body"><SkeletonList :rows="2" :lines="1" trail="text" /></div>
          </section>
          <section class="panel">
            <div class="panel__head">
              <span class="panel__title">{{ $t('workbench.attention.title') }}</span>
            </div>
            <div class="panel__body"><SkeletonList :rows="2" lead="avatar" trail="button" /></div>
          </section>
        </template>

        <section class="panel">
          <div class="panel__head">
            <span class="panel__title">{{ $t('workbench.exams') }}</span>
            <router-link v-if="role === 'ADMIN'" to="/admin/exams" class="panel__more">{{ $t('home.viewAll') }} ›</router-link>
          </div>
          <div class="panel__body"><SkeletonList :rows="2" lead="date" trail="pill" /></div>
        </section>

        <section v-if="role === 'ADMIN'" class="panel">
          <div class="panel__head">
            <span class="panel__title">{{ $t('workbench.admin.notices') }}</span>
            <router-link to="/messages?tab=notices" class="panel__more">{{ $t('home.viewAll') }} ›</router-link>
          </div>
          <div class="panel__body"><SkeletonList :rows="3" :lines="1" trail="text" /></div>
        </section>
      </aside>
    </div>
  </SkeletonRegion>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUser } from '@/components/useUser'
import SkBlock from '@/components/skeleton/SkBlock.vue'
import SkeletonList from '@/components/skeleton/SkeletonList.vue'
import SkeletonRegion from '@/components/skeleton/SkeletonRegion.vue'
import CourseCardSkeleton from '@/components/skeleton/CourseCardSkeleton.vue'
import HomeHero from './HomeHero.vue'

const { user, hasPermission } = useUser()
const role = computed(() => user.value.role)
</script>

<style scoped>
/* 占位块高度取字号，上下补回行高差，骨架换成内容时页头不跳 */
.hs-eyebrow {
  margin: 3px 0;
}

.hs-title {
  margin: 4px 0;
}

/* 与 NextClassCard 同尺寸：左侧一条竖条 */
.hs-next {
  position: relative;
  display: grid;
  gap: 8px;
  width: 300px;
  max-width: 100%;
  padding: 16px 18px 16px 22px;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius-lg);
  background: var(--xm-bg-card);
  overflow: hidden;
}

.hs-next::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 6px;
  background: var(--xm-sk-base);
}

.hs-next__action {
  margin-top: 4px;
}

/* 与管理员首页的快捷入口同尺寸 */
.hs-quick {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.hs-quick__item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 132px;
  padding: 14px;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius);
  background: var(--xm-bg-card);
}

.hs-kpis {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}

.hs-kpi {
  display: grid;
  gap: 8px;
  padding: 14px 16px;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius);
  background: var(--xm-bg-card);
}

.hs-today {
  display: grid;
  gap: 6px;
}

.hs-today__row {
  display: grid;
  grid-template-columns: 54px 4px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
}

.panel__links {
  display: flex;
  gap: 16px;
}

.hs-today__time {
  gap: 6px;
}

.hs-progress {
  display: flex;
  align-items: center;
  gap: 20px;
}

/* 学分环：圆形挖空成环 */
.hs-ring {
  -webkit-mask: radial-gradient(circle, transparent 47px, #000 48px);
  mask: radial-gradient(circle, transparent 47px, #000 48px);
}

.hs-progress__list {
  display: grid;
  gap: 10px;
  flex: 1;
  margin: 0;
}

.hs-progress__list div {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.hs-progress__list dt {
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.hs-progress__list dd {
  margin: 0;
}
</style>

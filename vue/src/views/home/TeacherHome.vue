<template>
  <div class="home">
    <HomeHero :eyebrow="semesterLine" :title="$t('workbench.teacherHello', { name: user.name, greeting })">
      <span>{{ $t('workbench.todayCount') }} <b class="num">{{ data.todayCourses.length }}</b></span>
      <span>{{ $t('workbench.ungradedCount') }} <b class="num">{{ ungradedTotal }}</b></span>
      <span>{{ $t('workbench.warningCount') }} <b class="num">{{ data.warningCount }}</b></span>
      <template #aside>
        <NextClassCard :courses="data.todayCourses" :show-teacher="false">
          <template #action="{ course }">
            <el-button class="next-enter" type="primary" size="small" @click="router.push(`/course/${course.id}`)">
              {{ $t('workbench.enterClass') }}
            </el-button>
          </template>
        </NextClassCard>
      </template>
    </HomeHero>

    <div class="home-grid">
      <div class="home-grid__main">
        <section class="panel">
          <div class="panel__head">
            <span class="panel__title">{{ $t('workbench.todayTitle') }}</span>
            <span class="panel__hint">{{ data.semester.weekday }}</span>
          </div>
          <div class="panel__body">
            <TodayClasses :courses="data.todayCourses" :show-teacher="false">
              <template #action="{ course, state }">
                <el-button v-if="state !== 'done'" size="small" @click="router.push(`/course/${course.id}`)">
                  {{ $t('workbench.enterClass') }}
                </el-button>
              </template>
            </TodayClasses>
          </div>
        </section>

        <section class="panel">
          <div class="panel__head">
            <span class="panel__title">{{ $t('workbench.teachingCourses') }}</span>
            <router-link to="/courses" class="panel__more">{{ $t('workbench.allCourses') }} ›</router-link>
          </div>
          <div class="panel__body">
            <div v-if="activeCourses.length" class="course-grid">
              <CourseCard v-for="c in activeCourses" :key="c.id" :course="c" :to="`/course/${c.id}`">
                <template #foot>
                  <span class="num">{{ $t('workbench.students', { n: c.studentCount }) }}</span>
                  <span v-if="c.ungraded" class="pill pill--warn">{{ $t('workbench.toGrade', { n: c.ungraded }) }}</span>
                </template>
              </CourseCard>
            </div>
            <div v-else class="empty-note">{{ $t('workbench.noTeaching') }}</div>
          </div>
        </section>
      </div>

      <aside class="home-grid__side">
        <section class="panel">
          <div class="panel__head">
            <span class="panel__title">{{ $t('workbench.todos') }}</span>
          </div>
          <div class="panel__body">
            <ul v-if="data.todos.length" class="todos">
              <li v-for="todo in data.todos" :key="todo.type + todo.courseId" class="todos__row">
                <span class="todos__dot"></span>
                <span class="todos__text">{{ $t('workbench.todo.grading', { course: todo.courseName, n: todo.count }) }}</span>
                <router-link :to="gradingLink(todo.courseId)" class="panel__more">{{ $t('workbench.handle') }}</router-link>
              </li>
            </ul>
            <div v-else class="empty-note">{{ $t('workbench.noTodos') }}</div>
          </div>
        </section>

        <ExamsPanel :exams="data.exams" />
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUser } from '@/components/useUser'
import CourseCard from '@/components/CourseCard.vue'
import HomeHero from './HomeHero.vue'
import NextClassCard from './NextClassCard.vue'
import TodayClasses from './TodayClasses.vue'
import ExamsPanel from './ExamsPanel.vue'
import { useHomeText } from './useHomeText'

const props = defineProps<{ data: Record<string, any> }>()

const router = useRouter()
const { user } = useUser()
const { greeting, semesterLine } = useHomeText(() => props.data.semester)

const activeCourses = computed(() => (props.data.courses || []).filter((c: any) => c.status !== '已结课'))
const ungradedTotal = computed(() =>
  (props.data.courses || []).reduce((sum: number, c: any) => sum + (Number(c.ungraded) || 0), 0),
)

/** 批改作业的入口（课程空间的作业页上线前，先进旧的作业页） */
const gradingLink = (courseId: number) => `/course/${courseId}/assignments`
</script>

<style scoped>
.next-enter {
  justify-self: start;
  margin-top: 8px;
}

.todos {
  list-style: none;
  display: grid;
  gap: 10px;
}

.todos__row {
  display: grid;
  grid-template-columns: 8px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: var(--xm-text-regular);
}

.todos__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--xm-warn);
}

.todos__text {
  min-width: 0;
}
</style>

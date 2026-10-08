<template>
  <!-- 教师首页（与 Web 端 views/home/TeacherHome.vue 同一组内容）：问候 + 今日课程 / 待批 / 需关注人数 + 下一节课、
       今天的课、我教的课、待办、需要关注的学生、近期考试 -->
  <view>
    <home-hero
      :eyebrow="semesterLine"
      :title="$t('workbench.teacherHello', { name: user.name || user.username, greeting })"
    >
      <text
        >{{ $t('workbench.todayCount') }} <text class="stat xm-num">{{ (data.todayCourses || []).length }}</text></text
      >
      <text
        >{{ $t('workbench.ungradedCount') }} <text class="stat xm-num">{{ ungradedTotal }}</text></text
      >
      <text
        >{{ $t('workbench.warningCount') }} <text class="stat xm-num">{{ data.warningCount || 0 }}</text></text
      >
      <template #aside>
        <next-class
          :courses="data.todayCourses || []"
          :show-teacher="false"
          :enter-label="$t('workbench.enterClass')"
        />
      </template>
    </home-hero>

    <view class="xm-card">
      <view class="xm-section-head">
        <text class="xm-section-title">{{ $t('workbench.todayTitle') }}</text>
        <text class="xm-label">{{ weekLabel(data.semester.weekday, true) }}</text>
      </view>
      <today-classes
        :courses="data.todayCourses || []"
        :show-teacher="false"
      />
    </view>

    <view class="xm-card">
      <view class="xm-section-head">
        <text class="xm-section-title">{{ $t('workbench.teachingCourses') }}</text>
        <text
          class="xm-link"
          @click="goCourses"
          >{{ $t('workbench.allCourses') }} ›</text
        >
      </view>
      <scroll-view
        v-if="activeCourses.length"
        scroll-x
        class="strip"
        :show-scrollbar="false"
      >
        <view
          v-for="c in activeCourses"
          :key="c.id"
          class="strip-item"
        >
          <xm-course-card
            :course="c"
            :url="'/pages-course/space/space?id=' + c.id"
            compact
          >
            <template #foot>
              <text class="xm-num">{{ $t('workbench.students', { n: c.studentCount || 0 }) }}</text>
              <text
                v-if="c.ungraded"
                class="xm-tag xm-tag-warning"
                >{{ $t('workbench.toGrade', { n: c.ungraded }) }}</text
              >
            </template>
          </xm-course-card>
        </view>
      </scroll-view>
      <view
        v-else
        class="empty-note"
        >{{ $t('workbench.noTeaching') }}</view
      >
    </view>

    <view class="xm-card">
      <view class="xm-section-head">
        <text class="xm-section-title">{{ $t('workbench.todos') }}</text>
      </view>
      <view v-if="data.todos && data.todos.length">
        <view
          v-for="todo in data.todos"
          :key="todo.type + todo.courseId"
          class="todo"
          @click="openGrading(todo.courseId)"
        >
          <view class="todo-dot" />
          <text class="todo-text">{{ $t('workbench.todo.grading', { course: todo.courseName, n: todo.count }) }}</text>
          <text class="xm-link">{{ $t('workbench.handle') }}</text>
        </view>
      </view>
      <view
        v-else
        class="empty-note"
        >{{ $t('workbench.noTodos') }}</view
      >
    </view>

    <attention-card ref="attentionRef" />

    <exams-card :exams="data.exams || []" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { weekLabel } from '@/utils/courseText'
import { useHomeText } from './useHomeText'
import HomeHero from './HomeHero.vue'
import NextClass from './NextClass.vue'
import TodayClasses from './TodayClasses.vue'
import ExamsCard from './ExamsCard.vue'
import AttentionCard from './AttentionCard.vue'

const props = defineProps({
  data: { type: Object, required: true },
})

const user = computed(() => useUserStore().user)
const { greeting, semesterLine } = useHomeText(() => props.data.semester)
const attentionRef = ref(null)

const activeCourses = computed(() => (props.data.courses || []).filter((c) => c.status !== '已结课'))
const ungradedTotal = computed(() => (props.data.courses || []).reduce((sum, c) => sum + (Number(c.ungraded) || 0), 0))

const goCourses = () => uni.switchTab({ url: '/pages/courses/courses' })
const openGrading = (courseId) => uni.navigateTo({ url: `/pages-course/space/space?id=${courseId}&tab=assignments` })

/** 下拉刷新时首页一起刷新「需要关注的学生」 */
defineExpose({ reloadExtras: () => attentionRef.value && attentionRef.value.load() })
</script>

<style lang="scss" scoped>
.stat {
  margin-left: 6rpx;
  font-size: 30rpx;
  font-weight: bold;
  color: var(--xm-text);
}

.strip {
  white-space: nowrap;
  margin: 0 -28rpx;
  padding: 0 28rpx;
  width: calc(100% + 56rpx);
  box-sizing: border-box;
}

.strip-item {
  display: inline-block;
  width: 500rpx;
  margin-right: 20rpx;
  vertical-align: top;
  white-space: normal;
}

.strip-item:last-child {
  margin-right: 28rpx;
}

.todo {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 18rpx 0;
}

.todo + .todo {
  border-top: 1rpx solid var(--xm-border);
}

.todo-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  flex-shrink: 0;
  background: var(--xm-warning);
}

.todo-text {
  flex: 1;
  min-width: 0;
  font-size: 26rpx;
  color: var(--xm-text);
}

.empty-note {
  padding: 16rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: var(--xm-text-2);
}
</style>

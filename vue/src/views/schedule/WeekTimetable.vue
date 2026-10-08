<template>
  <div ref="scroller" class="wt-scroll">
    <div class="wt" role="grid" :aria-label="$t('schedule.week')">
      <!-- 表头：星期、日期、当天的考试 / 截止 / 请假 -->
      <div class="wt__corner" role="columnheader"></div>
      <div
        v-for="day in data.days"
        :key="day.date"
        class="wt__head"
        :class="{ 'is-today': day.today, 'is-off': !day.inSemester }"
        role="columnheader"
      >
        <div class="wt__weekday">{{ formatDay(day.date, locale, { weekday: 'short' }) }}</div>
        <div class="wt__date num">{{ formatDay(day.date, locale) }}</div>
        <div class="wt__events">
          <span v-for="leave in leavesOn(day.date)" :key="'l' + leave.id" class="wt-chip" :class="'wt-chip--leave-' + leaveKey(leave.status)">
            {{ $t('schedule.leave') }}
          </span>
          <component
            :is="ev.type === 'deadline' ? 'router-link' : 'span'"
            v-for="ev in eventsOn(day.date)"
            :key="ev.type + ev.id"
            :to="ev.type === 'deadline' ? `/course/${ev.courseId}/assignments?open=${ev.id}` : undefined"
            class="wt-chip"
            :class="['wt-chip--' + ev.type, { 'is-done': ev.done }]"
            :title="`${ev.time} ${ev.courseName ? ev.courseName + ' · ' : ''}${ev.title}`"
          >
            {{ ev.type === 'exam' ? $t('schedule.exam') : ev.done ? $t('schedule.done') : $t('schedule.deadline') }} {{ ev.time }}
            <span class="wt-chip__title">{{ ev.title }}</span>
          </component>
        </div>
      </div>

      <!-- 五个大节 -->
      <template v-for="(segment, si) in SEGMENTS" :key="segment">
        <div class="wt__time" role="rowheader">
          <div class="wt__seg">{{ segmentShortName(segment) }}</div>
          <div class="wt__range num">{{ parseSegmentTime(segment)[0] }}<br />{{ parseSegmentTime(segment)[1] }}</div>
        </div>
        <div
          v-for="day in data.days"
          :key="day.date + si"
          class="wt__cell"
          :class="{ 'is-today': day.today, 'is-off': !day.inSemester }"
          role="gridcell"
        >
          <router-link
            v-for="c in coursesAt(day, segment)"
            :key="c.id"
            :to="`/course/${c.id}`"
            class="wt-block"
            :class="{ 'is-pending': c.status !== '已开课', 'is-now': day.today && classState(segment, clock.hhmm) === 'ongoing' }"
            :style="{ '--cc': courseColor(c.name) }"
          >
            <span class="wt-block__name">{{ c.name }}</span>
            <span class="wt-block__meta">{{ c.room || '—' }}<template v-if="showTeacher"> · {{ c.teacherName }}</template></span>
          </router-link>
        </div>
      </template>
    </div>

    <p v-if="unscheduled.length" class="wt-note">
      {{ $t('schedule.unscheduled', { names: unscheduled.map((c: Record<string, any>) => c.name).join(locale === 'zh-CN' ? '、' : ', ') }) }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { courseColor } from '@/utils/courseColor'
import { SEGMENTS, classState, formatDay, parseSegmentTime, segmentShortName } from '@/utils/schedule'
import { useServerClock } from '@/composables/useServerClock'
import { currentLocale } from '@/composables/useLocale'

/**
 * 周课表：七天 × 五个大节。学期外的日子不排课；未开课的课程虚线显示；今天正在上的那节课描边高亮。
 * 表头列出当天的考试、作业截止（学生端标出已交）和请假。
 */
const props = defineProps<{
  data: Record<string, any>
  showTeacher: boolean
}>()

const clock = useServerClock()
const locale = computed(() => currentLocale())

const coursesAt = (day: Record<string, any>, segment: string) =>
  day.inSemester
    ? (props.data.courses || []).filter((c: Record<string, any>) => c.week === day.weekday && c.segment === segment)
    : []

const eventsOn = (date: string) => (props.data.events || []).filter((e: Record<string, any>) => e.date === date)

const leavesOn = (date: string) =>
  (props.data.leaves || []).filter((l: Record<string, any>) => l.from <= date && date <= l.to && l.status !== '审核不通过')

const leaveKey = (status: string) => (status === '审核通过' ? 'approved' : status === '审核不通过' ? 'rejected' : 'pending')

/** 手机上课表横向滚动：打开时把「今天」那一列滚到可见处 */
const scroller = ref<HTMLElement>()
const scrollToToday = async () => {
  await nextTick()
  const box = scroller.value
  const today = box?.querySelector<HTMLElement>('.wt__head.is-today')
  if (box && today && box.scrollWidth > box.clientWidth) {
    box.scrollLeft = Math.max(0, today.offsetLeft - 72)
  }
}
onMounted(scrollToToday)
watch(() => props.data, scrollToToday)

/** 选了 / 开了但还没排上课表（没有星期或大节）的课 */
const unscheduled = computed(() =>
  (props.data.courses || []).filter(
    (c: Record<string, any>) => !c.week || !(SEGMENTS as readonly string[]).includes(c.segment),
  ),
)
</script>

<style scoped>
.wt-scroll {
  overflow-x: auto;
}

.wt {
  display: grid;
  grid-template-columns: 72px repeat(7, minmax(104px, 1fr));
  min-width: 800px;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius-lg);
  background: var(--xm-bg-card);
  overflow: hidden;
}

.wt__corner,
.wt__head {
  border-bottom: 1px solid var(--xm-border);
}

.wt__head {
  display: grid;
  align-content: start;
  gap: 2px;
  min-height: 74px;
  padding: 10px 8px 8px;
  border-left: 1px solid var(--xm-border);
}

.wt__head.is-today {
  background: var(--xm-brand-soft);
}

.wt__head.is-off,
.wt__cell.is-off {
  background: repeating-linear-gradient(135deg, var(--xm-bg-sunken) 0 6px, transparent 6px 12px);
}

.wt__weekday {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.wt__head.is-today .wt__weekday,
.wt__head.is-today .wt__date {
  color: var(--xm-brand);
  font-weight: 700;
}

.wt__date {
  font-size: 15px;
  font-weight: 600;
  color: var(--xm-text-primary);
}

.wt__events {
  display: grid;
  gap: 3px;
  margin-top: 4px;
}

.wt-chip {
  display: block;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 11px;
  line-height: 18px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.wt-chip__title {
  margin-left: 2px;
  opacity: 0.85;
}

.wt-chip--exam {
  background: var(--xm-bad-soft);
  color: var(--xm-bad);
}

.wt-chip--deadline {
  background: var(--xm-warn-soft);
  color: var(--xm-warn);
}

.wt-chip--deadline.is-done {
  background: var(--xm-ok-soft);
  color: var(--xm-ok);
}

.wt-chip--leave-approved {
  background: var(--xm-info-soft);
  color: var(--xm-info);
}

.wt-chip--leave-pending {
  border: 1px dashed var(--xm-info);
  color: var(--xm-info);
}

.wt__time {
  display: grid;
  align-content: center;
  justify-items: center;
  gap: 2px;
  padding: 8px 4px;
  border-bottom: 1px solid var(--xm-border);
  text-align: center;
}

.wt__seg {
  font-size: 12px;
  font-weight: 600;
  color: var(--xm-text-regular);
}

.wt__range {
  font-size: 11px;
  line-height: 1.35;
  color: var(--xm-text-secondary);
}

.wt__cell {
  display: grid;
  align-content: start;
  gap: 4px;
  min-height: 84px;
  padding: 5px;
  border-bottom: 1px solid var(--xm-border);
  border-left: 1px solid var(--xm-border);
}

.wt__cell.is-today {
  background: color-mix(in srgb, var(--xm-brand-soft) 45%, transparent);
}

.wt-block {
  display: grid;
  gap: 2px;
  padding: 7px 8px;
  border-radius: var(--xm-radius-sm);
  background: var(--cc);
  color: #fff;
  line-height: 1.3;
}

.wt-block:hover {
  filter: brightness(1.06);
}

.wt-block.is-pending {
  border: 1.5px dashed var(--cc);
  background: transparent;
  color: var(--cc);
}

.wt-block.is-now {
  box-shadow:
    0 0 0 2px var(--xm-bg-card),
    0 0 0 4px var(--cc);
}

.wt-block__name {
  font-size: 13px;
  font-weight: 600;
}

.wt-block__meta {
  font-size: 11px;
  opacity: 0.9;
}

.wt-note {
  margin-top: 10px;
  font-size: 12px;
  color: var(--xm-text-secondary);
}
</style>

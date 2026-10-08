<template>
  <div class="mc" role="grid" :aria-label="$t('schedule.month')">
    <div v-for="d in weekdayHeads" :key="d" class="mc__head" role="columnheader">{{ d }}</div>
    <component
      :is="selectable ? 'button' : 'div'"
      v-for="day in data.days"
      :key="day.date"
      :type="selectable ? 'button' : undefined"
      class="mc__day"
      :class="{
        'is-out': !day.inMonth,
        'is-today': day.today,
        'is-selected': inSelection(day.date),
        'is-edge': day.date === selection.from || day.date === selection.to,
        'is-selectable': selectable,
      }"
      role="gridcell"
      :aria-selected="selectable ? inSelection(day.date) : undefined"
      :aria-label="dayLabel(day)"
      @click="selectable && emit('pick', day.date)"
    >
      <span v-for="leave in leavesOn(day.date)" :key="'l' + leave.id" class="mc__leave" :class="'mc__leave--' + leaveKey(leave.status)"></span>
      <span class="mc__num num">{{ Number(day.date.slice(8)) }}</span>
      <span class="mc__items">
        <span v-for="c in day.classes.slice(0, 2)" :key="c.courseId + c.segment" class="mc__class" :style="{ '--cc': courseColor(c.courseName) }">
          <span class="mc__class-time num">{{ c.start }}</span>{{ c.courseName }}
        </span>
        <span v-if="day.classes.length > 2" class="mc__more">+{{ day.classes.length - 2 }}</span>
        <span
          v-for="ev in eventsOn(day.date)"
          :key="ev.type + ev.id"
          class="mc__event"
          :class="['mc__event--' + ev.type, { 'is-done': ev.done }]"
          :title="`${ev.time} ${ev.courseName ? ev.courseName + ' · ' : ''}${ev.title}`"
        >
          {{ ev.type === 'exam' ? $t('schedule.exam') : ev.done ? $t('schedule.done') : $t('schedule.deadline') }} · {{ ev.title }}
        </span>
      </span>
      <span v-if="day.classes.length" class="mc__dots" aria-hidden="true">
        <span v-for="c in day.classes" :key="'d' + c.courseId + c.segment" :style="{ background: courseColor(c.courseName) }"></span>
      </span>
    </component>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { courseColor } from '@/utils/courseColor'
import { formatDay } from '@/utils/schedule'
import { currentLocale } from '@/composables/useLocale'
import { t } from '@/i18n'

/**
 * 月历：一格一天，列出当天的课、考试、作业截止；请假用顶部色条表示（已批准实线、待审核虚线）。
 * 学生端可以点两下选一段日期请假（selectable），选中区间高亮。
 */
const props = defineProps<{
  data: Record<string, any>
  selectable: boolean
  selection: { from: string | null; to: string | null }
}>()
const emit = defineEmits<{ pick: [date: string] }>()

const locale = computed(() => currentLocale())

/** 周一到周日的简写表头：取月历第一周的七天按界面语言格式化 */
const weekdayHeads = computed(() =>
  (props.data.days || []).slice(0, 7).map((d: Record<string, any>) => formatDay(d.date, locale.value, { weekday: 'short' })),
)

const eventsOn = (date: string) => (props.data.events || []).filter((e: Record<string, any>) => e.date === date)

const leavesOn = (date: string) =>
  (props.data.leaves || []).filter((l: Record<string, any>) => l.from <= date && date <= l.to && l.status !== '审核不通过')

const leaveKey = (status: string) => (status === '审核通过' ? 'approved' : 'pending')

const inSelection = (date: string) => {
  const { from, to } = props.selection
  return !!from && !!to && from <= date && date <= to
}

const dayLabel = (day: Record<string, any>) => {
  const parts = [formatDay(day.date, locale.value, { month: 'long', day: 'numeric', weekday: 'long' })]
  if (day.classes.length) parts.push(t('schedule.classes', { n: day.classes.length }))
  return parts.join(locale.value === 'zh-CN' ? '，' : ', ')
}
</script>

<style scoped>
.mc {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius-lg);
  background: var(--xm-bg-card);
  overflow: hidden;
}

.mc__head {
  padding: 8px 10px;
  border-bottom: 1px solid var(--xm-border);
  font-size: 12px;
  color: var(--xm-text-secondary);
  text-align: center;
}

.mc__day {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 3px;
  min-height: 108px;
  padding: 8px 6px 6px;
  border: 0;
  border-right: 1px solid var(--xm-border);
  border-bottom: 1px solid var(--xm-border);
  background: var(--xm-bg-card);
  color: inherit;
  font: inherit;
  text-align: left;
}

.mc__day:nth-child(7n) {
  border-right: 0;
}

.mc__day.is-selectable {
  cursor: pointer;
}

.mc__day.is-selectable:hover {
  background: var(--xm-bg-hover);
}

.mc__day.is-out {
  background: var(--xm-bg-sunken);
}

.mc__day.is-out .mc__num {
  color: var(--xm-text-secondary);
  opacity: 0.6;
}

.mc__day.is-selected {
  background: var(--xm-brand-soft);
}

.mc__day.is-edge {
  box-shadow: inset 0 0 0 2px var(--xm-brand);
}

.mc__num {
  font-size: 13px;
  font-weight: 600;
  color: var(--xm-text-primary);
}

.mc__day.is-today .mc__num {
  display: inline-grid;
  place-items: center;
  align-self: flex-start;
  min-width: 22px;
  height: 22px;
  padding: 0 4px;
  border-radius: 999px;
  background: var(--xm-brand);
  color: var(--xm-on-brand);
}

.mc__leave {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
}

.mc__leave--approved {
  background: var(--xm-info);
}

.mc__leave--pending {
  background: repeating-linear-gradient(90deg, var(--xm-info) 0 6px, transparent 6px 10px);
}

.mc__items {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.mc__class,
.mc__event {
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 11px;
  line-height: 17px;
}

.mc__class {
  padding-left: 6px;
  border-left: 3px solid var(--cc);
  color: var(--xm-text-regular);
}

.mc__class-time {
  margin-right: 3px;
  color: var(--xm-text-secondary);
}

.mc__more {
  font-size: 11px;
  color: var(--xm-text-secondary);
}

.mc__event {
  padding: 0 5px;
  border-radius: 3px;
}

.mc__event--exam {
  background: var(--xm-bad-soft);
  color: var(--xm-bad);
}

.mc__event--deadline {
  background: var(--xm-warn-soft);
  color: var(--xm-warn);
}

.mc__event--deadline.is-done {
  background: var(--xm-ok-soft);
  color: var(--xm-ok);
}

/* 手机上格子太窄放不下文字：只留日期和彩色圆点 */
.mc__dots {
  display: none;
  gap: 3px;
  flex-wrap: wrap;
}

.mc__dots span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

@media (max-width: 640px) {
  .mc__day {
    min-height: 58px;
    padding: 6px 4px;
  }

  .mc__items {
    display: none;
  }

  .mc__dots {
    display: flex;
  }
}
</style>

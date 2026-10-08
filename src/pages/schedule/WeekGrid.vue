<template>
  <!-- 周课表（与 Web 端 views/schedule/WeekTimetable.vue 一致）：七天 × 五个大节。
       学期外的日子不排课；未开课的课程虚线显示；今天正在上的那节课描边高亮；
       表头用小圆点标出当天的考试（红）、作业截止（橙，已交为绿）和请假（蓝），明细在下方「本周安排」 -->
  <view class="wg">
    <view class="wg-row wg-head-row">
      <view class="wg-time wg-corner" />
      <view
        v-for="day in days"
        :key="day.date"
        class="wg-head"
        :class="{ 'is-today': day.today, 'is-off': !day.inSemester }"
      >
        <text class="wg-weekday">{{ day.weekdayText }}</text>
        <text class="wg-date xm-num">{{ day.dateText }}</text>
        <view class="wg-marks">
          <view
            v-for="mark in day.marks"
            :key="mark"
            class="wg-mark"
            :class="'wg-mark-' + mark"
          />
        </view>
      </view>
    </view>

    <view
      v-for="row in rows"
      :key="row.segment"
      class="wg-row"
    >
      <view class="wg-time">
        <text class="wg-seg xm-num">{{ row.no }}</text>
        <text class="wg-range xm-num">{{ row.start }}</text>
        <text class="wg-range xm-num">{{ row.end }}</text>
      </view>
      <view
        v-for="cell in row.cells"
        :key="cell.key"
        class="wg-cell"
        :class="{ 'is-today': cell.today, 'is-off': cell.off }"
      >
        <view
          v-for="b in cell.blocks"
          :key="b.id"
          class="wg-block"
          :class="{ 'is-pending': b.pending, 'is-now': b.now }"
          :style="'--cc:' + b.color"
          @click="$emit('open-course', b.id)"
        >
          <text class="wg-block-name">{{ b.name }}</text>
          <!-- 列很窄：教室、教师各占一行，不用「·」连在一起折行 -->
          <text
            v-if="b.room"
            class="wg-block-meta"
            >{{ b.room }}</text
          >
          <text
            v-if="b.teacher"
            class="wg-block-meta"
            >{{ b.teacher }}</text
          >
        </view>
      </view>
    </view>
  </view>
  <view
    v-if="unscheduledNames"
    class="wg-note"
    >{{ $t('schedule.unscheduled', { names: unscheduledNames }) }}</view
  >
</template>

<script setup>
import { computed } from 'vue'
import { courseColor } from '@/utils/courseColor'
import { SEGMENTS, classState, parseSegmentTime } from '@/utils/schedule'
import { weekLabel } from '@/utils/courseText'
import { useServerClock } from '@/composables/useServerClock'
import { isZh } from '@/i18n'

const props = defineProps({
  data: { type: Object, required: true },
  showTeacher: { type: Boolean, default: false },
})

defineEmits(['open-course'])

const clock = useServerClock()

/** 请假是否覆盖这一天（未批准的不显示） */
const leaveOn = (date) =>
  (props.data.leaves || []).some((l) => l.from <= date && date <= l.to && l.status !== '审核不通过')

const days = computed(() =>
  (props.data.days || []).map((day) => {
    const events = (props.data.events || []).filter((e) => e.date === day.date)
    const marks = []
    if (events.some((e) => e.type === 'exam')) marks.push('exam')
    if (events.some((e) => e.type === 'deadline' && !e.done)) marks.push('deadline')
    if (events.some((e) => e.type === 'deadline' && e.done)) marks.push('done')
    if (leaveOn(day.date)) marks.push('leave')
    return {
      ...day,
      weekdayText: weekLabel(day.weekday),
      dateText: `${Number(day.date.slice(5, 7))}/${Number(day.date.slice(8, 10))}`,
      marks,
    }
  }),
)

/** 五行（大节）× 七列（天）的格子，课程块预先算好，模板里不再嵌套过滤 */
const rows = computed(() =>
  SEGMENTS.map((segment, si) => {
    const [start, end] = parseSegmentTime(segment)
    const ongoing = classState(segment, clock.value.hhmm) === 'ongoing'
    return {
      segment,
      no: si + 1,
      start,
      end,
      cells: days.value.map((day) => ({
        key: day.date + si,
        today: day.today,
        off: !day.inSemester,
        blocks: day.inSemester
          ? (props.data.courses || [])
              .filter((c) => c.week === day.weekday && c.segment === segment)
              .map((c) => ({
                id: c.id,
                name: c.name,
                room: c.room || '',
                teacher: props.showTeacher ? c.teacherName || '' : '',
                color: courseColor(c.name),
                pending: c.status !== '已开课',
                now: day.today && ongoing,
              }))
          : [],
      })),
    }
  }),
)

/** 选了 / 开了但还没排上课表（没有星期或大节）的课 */
const unscheduledNames = computed(() =>
  (props.data.courses || [])
    .filter((c) => !c.week || !SEGMENTS.includes(c.segment))
    .map((c) => c.name)
    .join(isZh() ? '、' : ', '),
)
</script>

<style lang="scss" scoped>
.wg {
  border: 1rpx solid var(--xm-border);
  border-radius: 24rpx;
  background: var(--xm-bg-card);
  overflow: hidden;
}

.wg-row {
  display: flex;
}

.wg-row + .wg-row {
  border-top: 1rpx solid var(--xm-border);
}

.wg-time {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2rpx;
  width: 64rpx;
  flex-shrink: 0;
  padding: 8rpx 0;
}

.wg-seg {
  font-size: 26rpx;
  font-weight: bold;
  color: var(--xm-text);
}

.wg-range {
  font-size: 18rpx;
  line-height: 1.3;
  color: var(--xm-text-3);
}

.wg-head,
.wg-cell {
  flex: 1;
  min-width: 0;
  border-left: 1rpx solid var(--xm-border);
}

.wg-head {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2rpx;
  padding: 12rpx 0 10rpx;
}

.wg-head.is-today {
  background: var(--xm-brand-soft);
}

.wg-head.is-off,
.wg-cell.is-off {
  background: repeating-linear-gradient(135deg, var(--xm-bg-sunken) 0 10rpx, transparent 10rpx 20rpx);
}

.wg-weekday {
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.wg-date {
  font-size: 24rpx;
  font-weight: 600;
  color: var(--xm-text);
}

.wg-head.is-today .wg-weekday,
.wg-head.is-today .wg-date {
  color: var(--xm-brand);
  font-weight: bold;
}

.wg-marks {
  display: flex;
  gap: 4rpx;
  height: 10rpx;
  margin-top: 4rpx;
}

.wg-mark {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
}

.wg-mark-exam {
  background: var(--xm-danger);
}

.wg-mark-deadline {
  background: var(--xm-warning);
}

.wg-mark-done {
  background: var(--xm-success);
}

.wg-mark-leave {
  background: var(--xm-info);
}

.wg-cell {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
  min-height: 176rpx;
  padding: 6rpx;
  box-sizing: border-box;
}

.wg-cell.is-today {
  background: var(--xm-brand-soft);
}

/* 课程块：课程色实底白字；未开课虚线描边；正在上的这节外圈描边 */
.wg-block {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
  padding: 10rpx 8rpx;
  border-radius: 12rpx;
  background: var(--cc);
  color: #ffffff;
  overflow: hidden;
  box-sizing: border-box;
}

.wg-block.is-pending {
  border: 2rpx dashed var(--cc);
  background: transparent;
  color: var(--cc);
}

.wg-block.is-now {
  box-shadow:
    0 0 0 3rpx var(--xm-bg-card),
    0 0 0 6rpx var(--cc);
}

.wg-block-name {
  font-size: 22rpx;
  font-weight: 600;
  line-height: 1.3;
  word-break: break-all;
}

.wg-block-meta {
  font-size: 18rpx;
  line-height: 1.3;
  opacity: 0.9;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.wg-note {
  margin-top: 16rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: var(--xm-text-2);
}
</style>

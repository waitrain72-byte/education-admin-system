<template>
  <!-- 月历（与 Web 端 views/schedule/MonthCalendar.vue 的手机版式一致）：一格一天，格子里只放日期和彩色圆点
       （课程色 = 当天的课，红 = 考试，橙 = 作业截止），请假用顶部色条（已批准实线、待审核虚线）；
       点一天在下方看明细；学生点两下选一段日期请假，选中区间高亮 -->
  <view class="mc">
    <view
      v-for="head in heads"
      :key="head"
      class="mc-head"
      >{{ head }}</view
    >
    <view
      v-for="cell in cells"
      :key="cell.date"
      class="mc-day"
      :class="{
        'is-out': !cell.inMonth,
        'is-today': cell.today,
        'is-selected': cell.selected,
        'is-edge': cell.edge,
        'is-focus': cell.date === focus,
      }"
      @click="$emit('pick', cell.date)"
    >
      <view
        v-if="cell.leave"
        class="mc-leave"
        :class="'mc-leave-' + cell.leave"
      />
      <text class="mc-num xm-num">{{ cell.day }}</text>
      <view class="mc-dots">
        <view
          v-for="(dot, i) in cell.dots"
          :key="i"
          class="mc-dot"
          :style="'background:' + dot"
        />
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { courseColor } from '@/utils/courseColor'
import { weekLabel } from '@/utils/courseText'
import { WEEKDAYS } from '@/utils/schedule'

const props = defineProps({
  data: { type: Object, required: true },
  selection: { type: Object, default: () => ({ from: null, to: null }) },
  /** 当前查看明细的那一天 */
  focus: { type: String, default: '' },
})

defineEmits(['pick'])

/** 周一到周日的简写表头 */
const heads = computed(() => WEEKDAYS.map((w) => weekLabel(w)))

/** 一格最多画几个圆点（再多也看不清） */
const MAX_DOTS = 4

const cells = computed(() => {
  const { from, to } = props.selection || {}
  const events = props.data.events || []
  const leaves = (props.data.leaves || []).filter((l) => l.status !== '审核不通过')
  return (props.data.days || []).map((day) => {
    const dots = (day.classes || []).map((c) => courseColor(c.courseName))
    const dayEvents = events.filter((e) => e.date === day.date)
    if (dayEvents.some((e) => e.type === 'exam')) dots.push('var(--xm-danger)')
    if (dayEvents.some((e) => e.type === 'deadline' && !e.done)) dots.push('var(--xm-warning)')
    const leave = leaves.find((l) => l.from <= day.date && day.date <= l.to)
    return {
      date: day.date,
      day: Number(day.date.slice(8)),
      inMonth: day.inMonth,
      today: day.today,
      selected: !!from && !!to && from <= day.date && day.date <= to,
      edge: day.date === from || day.date === to,
      leave: leave ? (leave.status === '审核通过' ? 'approved' : 'pending') : '',
      dots: dots.slice(0, MAX_DOTS),
    }
  })
})
</script>

<style lang="scss" scoped>
.mc {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  border: 1rpx solid var(--xm-border);
  border-radius: 24rpx;
  background: var(--xm-bg-card);
  overflow: hidden;
}

.mc-head {
  padding: 14rpx 0;
  border-bottom: 1rpx solid var(--xm-border);
  font-size: 22rpx;
  color: var(--xm-text-2);
  text-align: center;
}

.mc-day {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
  min-height: 104rpx;
  padding: 12rpx 4rpx 8rpx;
  border-right: 1rpx solid var(--xm-border);
  border-bottom: 1rpx solid var(--xm-border);
  box-sizing: border-box;
}

.mc-day:nth-child(7n) {
  border-right: 0;
}

.mc-day.is-out {
  background: var(--xm-bg-sunken);
}

.mc-day.is-out .mc-num {
  color: var(--xm-text-3);
}

.mc-day.is-selected {
  background: var(--xm-brand-soft);
}

.mc-day.is-edge,
.mc-day.is-focus {
  box-shadow: inset 0 0 0 3rpx var(--xm-brand);
}

.mc-num {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 44rpx;
  height: 44rpx;
  border-radius: 999rpx;
  font-size: 26rpx;
  font-weight: 600;
  color: var(--xm-text);
}

.mc-day.is-today .mc-num {
  background: var(--xm-brand);
  color: var(--xm-on-brand);
}

.mc-leave {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 6rpx;
}

.mc-leave-approved {
  background: var(--xm-info);
}

.mc-leave-pending {
  background: repeating-linear-gradient(90deg, var(--xm-info) 0 10rpx, transparent 10rpx 16rpx);
}

.mc-dots {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 4rpx;
}

.mc-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
}
</style>

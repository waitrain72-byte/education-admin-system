<template>
  <!-- 日期 + 时间（easycom 自动注册）：两个原生选择器，v-model 是「yyyy-MM-dd HH:mm」；
       只选了一半时暂存半截值，保存前用 utils/examCountdown 的 isCompleteExamTime 校验 -->
  <view class="xm-datetime">
    <picker
      mode="date"
      :value="parts.date"
      class="xm-datetime-date"
      @change="onDate"
    >
      <view
        class="xm-select"
        :class="{ placeholder: !parts.date }"
      >
        <text class="xm-num">{{ parts.date || datePlaceholder || $t('mobile.datetime.date') }}</text>
        <xm-icon
          name="calendar"
          :size="28"
        />
      </view>
    </picker>
    <picker
      mode="time"
      :value="parts.time"
      class="xm-datetime-time"
      @change="onTime"
    >
      <view
        class="xm-select"
        :class="{ placeholder: !parts.time }"
      >
        <text class="xm-num">{{ parts.time || $t('mobile.datetime.time') }}</text>
        <xm-icon
          name="clock"
          :size="28"
        />
      </view>
    </picker>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { joinExamTime, splitExamTime } from '@/utils/examCountdown'

defineOptions({ options: { virtualHost: true } })

const props = defineProps({
  modelValue: { type: String, default: '' },
  datePlaceholder: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue'])

const parts = computed(() => splitExamTime(props.modelValue))

const onDate = (e) => emit('update:modelValue', joinExamTime(e.detail.value, parts.value.time))
const onTime = (e) => emit('update:modelValue', joinExamTime(parts.value.date, e.detail.value))
</script>

<style lang="scss" scoped>
.xm-datetime {
  display: flex;
  gap: 16rpx;
}

.xm-datetime-date {
  flex: 3;
  min-width: 0;
}

.xm-datetime-time {
  flex: 2;
  min-width: 0;
}
</style>

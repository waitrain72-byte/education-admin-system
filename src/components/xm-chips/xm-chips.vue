<template>
  <!-- 筛选胶囊（easycom 自动注册）：一行可横向滑动，选中项品牌色；count 不为空时在文字后显示数量。
       <xm-chips v-model="filter" :options="[{ value: 'all', label: '全部', count: 12 }]" /> -->
  <scroll-view
    scroll-x
    class="xm-chips"
    :show-scrollbar="false"
  >
    <view
      v-for="o in options"
      :key="o.value"
      class="xm-chip"
      :class="{ on: o.value === modelValue }"
      @click="pick(o.value)"
    >
      <text>{{ o.label }}</text>
      <text
        v-if="o.count != null"
        class="xm-chip-count xm-num"
        >{{ o.count }}</text
      >
    </view>
  </scroll-view>
</template>

<script setup>
defineOptions({ options: { virtualHost: true } })

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  /** [{ value, label, count? }] */
  options: { type: Array, default: () => [] },
})

const emit = defineEmits(['update:modelValue', 'change'])

const pick = (value) => {
  if (value === props.modelValue) return
  emit('update:modelValue', value)
  emit('change', value)
}
</script>

<style lang="scss" scoped>
.xm-chips {
  white-space: nowrap;
  width: 100%;
}

.xm-chip {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  height: 60rpx;
  margin-right: 12rpx;
  padding: 0 24rpx;
  border: 1rpx solid var(--xm-border);
  border-radius: 999rpx;
  background: var(--xm-bg-card);
  font-size: 26rpx;
  color: var(--xm-text-2);
  vertical-align: middle;
  box-sizing: border-box;
}

.xm-chip.on {
  border-color: var(--xm-brand);
  background: var(--xm-brand);
  color: var(--xm-on-brand);
  font-weight: 600;
}

.xm-chip-count {
  font-size: 22rpx;
  opacity: 0.75;
}
</style>

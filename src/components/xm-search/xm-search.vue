<template>
  <!-- 搜索框（easycom 自动注册）：放大镜 + 输入 + 清除，输入即筛选；键盘右下角「搜索」触发 confirm -->
  <view class="xm-search">
    <view class="xm-search-icon">
      <xm-icon
        name="search"
        :size="30"
      />
    </view>
    <input
      class="xm-search-input"
      :value="modelValue"
      :placeholder="placeholder"
      confirm-type="search"
      @input="onInput"
      @confirm="$emit('confirm', modelValue)"
    />
    <view
      v-if="modelValue"
      class="xm-search-clear"
      @click="clear"
    >
      <xm-icon
        name="x-circle"
        :size="30"
      />
    </view>
  </view>
</template>

<script setup>
defineOptions({ options: { virtualHost: true } })

defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue', 'confirm', 'clear'])

const onInput = (e) => emit('update:modelValue', e.detail.value)
const clear = () => {
  emit('update:modelValue', '')
  emit('clear')
}
</script>

<style lang="scss" scoped>
.xm-search {
  display: flex;
  align-items: center;
  gap: 12rpx;
  height: 76rpx;
  padding: 0 20rpx;
  border: 1rpx solid var(--xm-border);
  border-radius: 999rpx;
  background: var(--xm-bg-card);
  box-sizing: border-box;
}

.xm-search-icon,
.xm-search-clear {
  display: flex;
  color: var(--xm-text-3);
  flex-shrink: 0;
}

.xm-search-input {
  flex: 1;
  min-width: 0;
  height: 100%;
  font-size: 26rpx;
  color: var(--xm-text);
}
</style>

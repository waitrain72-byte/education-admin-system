<template>
  <view class="xm-card xm-row">
    <input
      class="xm-input"
      style="flex: 1"
      :value="modelValue"
      :placeholder="placeholder"
      @input="onInput"
    />
    <slot name="filter" />
    <button
      class="xm-btn xm-btn-primary"
      @click="$emit('search')"
    >
      {{ $t('common.search') }}
    </button>
    <button
      class="xm-btn xm-btn-plain"
      @click="$emit('reset')"
    >
      {{ $t('common.reset') }}
    </button>
  </view>
</template>

<script setup>
/**
 * 通用搜索卡（easycom 自动注册）：关键字输入 + 查询/重置按钮，
 * 中间 filter 插槽可放级联筛选等额外条件。替代各管理页复制粘贴的搜索区模板。
 */
defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue', 'search', 'reset'])
const onInput = (e) => emit('update:modelValue', e.detail.value)
</script>

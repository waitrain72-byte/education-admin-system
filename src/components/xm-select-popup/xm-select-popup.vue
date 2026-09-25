<template>
  <view v-if="visible">
    <view
      class="xm-mask is-top"
      @click="$emit('close')"
      @touchmove.stop.prevent="noop"
    ></view>
    <view class="xm-popup is-top select-popup">
      <view class="xm-popup-title">{{ title }}</view>
      <view class="select-search">
        <xm-icon
          name="search"
          :size="30"
        />
        <input
          class="select-search-input"
          :value="keyword"
          :placeholder="placeholder || $t('common.searchKeyword')"
          confirm-type="search"
          @input="keyword = $event.detail.value"
        />
        <view
          v-if="keyword"
          class="select-search-clear"
          @click="keyword = ''"
        >
          <xm-icon
            name="close"
            :size="28"
          />
        </view>
      </view>
      <scroll-view
        scroll-y
        class="select-list"
      >
        <view
          v-for="opt in filtered"
          :key="opt.value"
          class="select-item"
          :class="{ on: opt.value === value }"
          @click="$emit('select', opt)"
        >
          <view class="select-item-main">
            <view class="select-item-label">{{ opt.label }}</view>
            <view
              v-if="opt.desc"
              class="select-item-desc"
              >{{ opt.desc }}</view
            >
          </view>
          <xm-icon
            v-if="opt.value === value"
            name="check"
            :size="36"
          />
        </view>
        <xm-empty
          v-if="!filtered.length"
          icon="search"
          :text="$t('common.noMatch')"
        />
      </scroll-view>
    </view>
  </view>
</template>

<script setup>
/**
 * 可搜索的底部选择器（easycom 自动注册）：选项多时替代需要来回滚动的原生 picker（如 400+ 间教室）。
 * - options：[{ value, label, desc?, pinned? }]，按 label + desc 模糊匹配（不区分大小写）；pinned 的选项始终显示
 *   （如「自动分配」）
 * - 选中后 emit('select', option)，由页面决定写回哪个字段、是否关闭
 * - 可叠在 xm-form-popup 之上使用（遮罩与面板层级更高）
 */
import { ref, computed, watch } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '' },
  options: { type: Array, default: () => [] },
  value: { type: [String, Number], default: '' },
  placeholder: { type: String, default: '' },
})
defineEmits(['close', 'select'])

const keyword = ref('')
// 每次打开都从空关键字开始，避免上次的筛选条件把选项藏起来
watch(
  () => props.visible,
  (v) => {
    if (v) keyword.value = ''
  },
)

const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return props.options
  return props.options.filter((o) => o.pinned || `${o.label} ${o.desc || ''}`.toLowerCase().includes(k))
})

const noop = () => {}
</script>

<style lang="scss" scoped>
.select-popup {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.select-search {
  display: flex;
  align-items: center;
  gap: 12rpx;
  height: 76rpx;
  padding: 0 20rpx;
  margin-bottom: 12rpx;
  border-radius: 999rpx;
  color: var(--xm-text-2);
  background: var(--xm-bg-input);
  flex-shrink: 0;
}

.select-search-input {
  flex: 1;
  min-width: 0;
  height: 76rpx;
  font-size: 28rpx;
  color: var(--xm-text);
}

.select-search-clear {
  display: flex;
  padding: 8rpx;
}

/* 固定高度：scroll-view 需要确定高度才能滚动 */
.select-list {
  height: 55vh;
}

.select-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 24rpx 8rpx;
  border-bottom: 1rpx solid var(--xm-border);
}

.select-item:active {
  background: var(--xm-bg-hover);
}

.select-item-main {
  flex: 1;
  min-width: 0;
}

.select-item-label {
  font-size: 28rpx;
  color: var(--xm-text);
  word-break: break-all;
}

.select-item-desc {
  margin-top: 4rpx;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

/* 当前选中项：主色文字 + 对勾 */
.select-item.on {
  color: var(--xm-brand);
}

.select-item.on .select-item-label {
  color: var(--xm-brand);
  font-weight: bold;
}
</style>

<template>
  <view class="xm-empty-state">
    <view class="empty-icon">{{ icon }}</view>
    <view class="empty-text">{{ text || $t('common.empty') }}</view>
    <button
      v-if="actionText"
      class="xm-btn xm-btn-plain empty-action"
      @click="$emit('action')"
    >
      {{ actionText }}
    </button>
  </view>
</template>

<script setup>
/**
 * 通用空态（easycom 自动注册）：图标 + 文案 + 可选操作按钮。
 * - 列表页默认用法：<xm-empty v-if="!list.length && !loading" :action-text="$t('common.reload')" @action="load(true)" />
 * - 自定义文案/图标：<xm-empty icon="🔔" :text="$t('message.empty')" />
 */
defineProps({
  icon: { type: String, default: '📭' },
  text: { type: String, default: '' },
  actionText: { type: String, default: '' },
})
defineEmits(['action'])
</script>

<style lang="scss" scoped>
.xm-empty-state {
  padding: 64rpx 24rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.empty-icon {
  font-size: 72rpx;
  margin-bottom: 16rpx;
}

.empty-text {
  font-size: 26rpx;
  color: var(--xm-text-2);
  text-align: center;
}

.empty-action {
  margin-top: 24rpx;
}
</style>

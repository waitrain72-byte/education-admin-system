<template>
  <view class="xm-empty-state">
    <view class="empty-icon">
      <xm-icon
        :name="icon"
        :size="72"
      />
    </view>
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
 * - 自定义文案/图标：<xm-empty icon="bell" :text="$t('message.empty')" />（icon 为 xm-icon 图标名）
 */
defineProps({
  icon: { type: String, default: 'inbox' },
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

/* 图标：浅色圆底 + 弱化描边色，安静不抢眼 */
.empty-icon {
  width: 136rpx;
  height: 136rpx;
  border-radius: 50%;
  margin-bottom: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--xm-text-2);
  background: var(--xm-bg-input);
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

<template>
  <view
    v-if="visible"
    class="list-footer"
    :class="{ 'is-more': !loading && !finished }"
    @click="$emit('load-more')"
  >
    <text v-if="loading">{{ $t('common.loading') }}</text>
    <text v-else-if="finished">{{ $t('common.noMore') }}</text>
    <text v-else>{{ $t('common.loadMore') }}</text>
  </view>
</template>

<script setup>
/**
 * 通用列表底部反馈条（easycom 自动注册）：
 * - loading：加载中…（请求进行中）
 * - finished：没有更多了（已加载完全部数据）
 * - 默认：加载更多（点击手动触发，同时页面支持触底自动加载）
 */
defineProps({
  visible: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  finished: { type: Boolean, default: false },
})
defineEmits(['load-more'])
</script>

<style lang="scss" scoped>
/* 细字居中的底部提示；可点击的「加载更多」用主色提示可操作。
 * 底部多留 120rpx：右下角悬浮新增按钮（高 104rpx、距底 64rpx）不会盖住最后一张卡片 */
.list-footer {
  padding: 16rpx 0 120rpx;
  text-align: center;
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.list-footer.is-more {
  color: var(--xm-brand);
}
</style>

<template>
  <view
    v-if="visible"
    class="xm-empty list-footer"
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

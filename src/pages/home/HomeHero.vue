<template>
  <!-- 首页页头（与 Web 端 views/home/HomeHero.vue 同一设计）：品牌色浅底 + 课表格线，下方放「下一节课」或快捷入口 -->
  <view class="hero">
    <view class="hero-eyebrow">{{ eyebrow }}</view>
    <view class="hero-title">{{ title }}</view>
    <view
      v-if="$slots.default"
      class="hero-stats"
    >
      <slot />
    </view>
    <view
      v-if="$slots.aside"
      class="hero-aside"
    >
      <slot name="aside" />
    </view>
  </view>
</template>

<script setup>
defineProps({
  eyebrow: { type: String, default: '' },
  title: { type: String, default: '' },
})
</script>

<style lang="scss" scoped>
.hero {
  position: relative;
  margin-bottom: 24rpx;
  padding: 32rpx 28rpx;
  border: 1rpx solid var(--xm-border);
  border-radius: 28rpx;
  background-color: var(--xm-brand-soft);
  background-image:
    linear-gradient(90deg, var(--xm-brand-soft) 30%, transparent 100%),
    repeating-linear-gradient(0deg, transparent 0 47rpx, var(--xm-border) 47rpx 48rpx),
    repeating-linear-gradient(90deg, transparent 0 95rpx, var(--xm-border) 95rpx 96rpx);
  overflow: hidden;
}

.hero-eyebrow {
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.hero-title {
  margin-top: 8rpx;
  font-size: 40rpx;
  font-weight: bold;
  line-height: 1.3;
  color: var(--xm-text);
}

.hero-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx 32rpx;
  margin-top: 18rpx;
  font-size: 26rpx;
  color: var(--xm-text-2);
}

.hero-aside {
  margin-top: 24rpx;
}
</style>

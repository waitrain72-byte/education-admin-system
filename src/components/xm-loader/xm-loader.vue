<template>
  <view
    v-if="visible"
    class="xm-loader-mask"
    @touchmove.stop.prevent
  >
    <view class="xm-loader">
      <view class="xm-loader-track" />
      <view class="xm-loader-track" />
      <view class="xm-loader-track" />
      <view class="xm-loader-track" />
    </view>
    <text class="xm-loader-text">{{ $t('common.loading') }}</text>
  </view>
</template>

<script setup>
/**
 * 统一加载动画（easycom 自动注册，方案 B「追逐圆点」，iOS/微信原生 loader 同款轨迹）：
 * - 全局唯一驱动源：utils/loading.ts 持有加载状态，本组件创建时订阅并立即同步当前状态，
 *   页面 onShow 先于组件挂载发出的加载也不会丢（订阅制替代事件广播，无时序竞态）
 * - 不做"仅栈顶显示"过滤：页面栈里的旧页面被新页面覆盖时蒙层不可见，过渡/返回时
 *   若加载仍在进行，显示蒙层反而是正确的加载反馈；实例比较在小程序运行时不可靠，
 *   曾导致蒙层被整体抑制（宁多显示不丢动画）
 * - 蒙层阻断触摸/滚动穿透；配色走主题变量（--xm-mask / --xm-brand），自动适配深浅色
 */
import { onUnmounted, ref } from 'vue'
import { onLoadingChange } from '@/utils/loading'

const visible = ref(false)

const unsubscribe = onLoadingChange((value) => {
  visible.value = value
})
onUnmounted(unsubscribe)
</script>

<style lang="scss" scoped>
.xm-loader-mask {
  position: fixed;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
  background: var(--xm-mask);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  /* 淡入过渡：蒙层出现不生硬，配合 utils/loading.ts 的最短展示时长 */
  animation: xm-loader-fade-in 0.18s ease both;
}

@keyframes xm-loader-fade-in {
  from {
    opacity: 0;
  }

  to {
    opacity: 1;
  }
}

.xm-loader {
  position: relative;
  width: 88rpx;
  height: 88rpx;
}

/* 每条轨道自转带动圆点沿同一圆环追逐，负延迟错开 + 静态透明度递减形成彗星尾迹 */
.xm-loader-track {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  animation: xm-loader-rotate 1.5s linear infinite;
}

.xm-loader-track::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 24rpx;
  height: 24rpx;
  margin: -12rpx 0 0 -12rpx;
  border-radius: 50%;
  background: var(--xm-brand);
  transform: translateX(32rpx);
}

.xm-loader-track:nth-child(2) {
  animation-delay: -0.16s;

  &::after {
    opacity: 0.8;
  }
}

.xm-loader-track:nth-child(3) {
  animation-delay: -0.32s;

  &::after {
    opacity: 0.6;
  }
}

.xm-loader-track:nth-child(4) {
  animation-delay: -0.48s;

  &::after {
    opacity: 0.4;
  }
}

@keyframes xm-loader-rotate {
  to {
    transform: rotate(360deg);
  }
}

.xm-loader-text {
  margin-top: 24rpx;
  font-size: 26rpx;
  color: var(--xm-text-2);
}
</style>

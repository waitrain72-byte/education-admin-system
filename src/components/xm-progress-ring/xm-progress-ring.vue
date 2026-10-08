<template>
  <!-- 环形进度（与 Web 端 components/ProgressRing.vue 一致）：value / max，超过 max 按满环画；max 为空或 0 时只画底环。
       小程序里没有内联 SVG，用锥形渐变画环，中间盖一个卡片底色的圆 -->
  <view
    class="ring"
    :style="ringStyle"
  >
    <view
      class="ring-hole"
      :style="holeStyle"
    >
      <slot />
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { rpx } from '@/composables/useScreen'

defineOptions({ options: { virtualHost: true } })

const props = defineProps({
  value: { type: Number, default: 0 },
  max: { type: Number, default: 100 },
  /** 直径（rpx） */
  size: { type: Number, default: 220 },
  /** 环宽（rpx） */
  stroke: { type: Number, default: 20 },
})

const ratio = computed(() => {
  if (!props.max || props.max <= 0) return 0
  return Math.min(1, Math.max(0, (Number(props.value) || 0) / props.max))
})

const ringStyle = computed(() => {
  const deg = Math.round(ratio.value * 360)
  return (
    `width:${rpx(props.size)};height:${rpx(props.size)};` +
    `background:conic-gradient(var(--xm-brand) 0deg ${deg}deg, var(--xm-bg-sunken) ${deg}deg 360deg);`
  )
})

// 不用 inset 简写：老一些的安卓 WebView 不认
const holeStyle = computed(() => {
  const s = rpx(props.stroke)
  return `top:${s};right:${s};bottom:${s};left:${s};`
})
</script>

<style lang="scss" scoped>
.ring {
  position: relative;
  flex-shrink: 0;
  border-radius: 50%;
}

.ring-hole {
  position: absolute;
  border-radius: 50%;
  background: var(--xm-bg-card);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}
</style>

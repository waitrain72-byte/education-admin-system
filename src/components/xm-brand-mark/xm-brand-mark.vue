<template>
  <!-- 品牌标（与 Web 端 layout/BrandMark.vue 同一图形）：课表的三行色条，长短不一，右下一个圆点表示「今天这一节」 -->
  <view
    class="brand-mark"
    :style="sizeStyle"
  >
    <view class="bar bar-1" />
    <view class="bar bar-2" />
    <view class="bar bar-3" />
    <view class="dot" />
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { rpx } from '@/composables/useScreen'

defineOptions({ options: { virtualHost: true } })

const props = defineProps({
  /** 边长（rpx） */
  size: { type: Number, default: 64 },
})

// 子元素按 32 格的比例摆放，用百分比跟着边长缩放
const sizeStyle = computed(() => `width:${rpx(props.size)};height:${rpx(props.size)};`)
</script>

<style lang="scss" scoped>
.brand-mark {
  position: relative;
  flex-shrink: 0;
  border-radius: 28%;
  background: var(--xm-brand);
}

.bar,
.dot {
  position: absolute;
  background: var(--xm-on-brand);
}

.bar {
  left: 25%;
  height: 10%;
  border-radius: 999px;
}

.bar-1 {
  top: 26.5%;
  width: 50%;
}

.bar-2 {
  top: 45%;
  width: 34.4%;
  opacity: 0.82;
}

.bar-3 {
  top: 63.4%;
  width: 20.3%;
  opacity: 0.64;
}

.dot {
  left: 60%;
  top: 60.3%;
  width: 16.3%;
  height: 16.3%;
  border-radius: 50%;
}
</style>

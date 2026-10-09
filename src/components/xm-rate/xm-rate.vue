<template>
  <!-- 星级评分（easycom 自动注册）：v-model 1~5；readonly 时只展示，可以显示小数（如综合评分 4.3 颗星） -->
  <view class="xm-rate">
    <view
      v-for="n in 5"
      :key="n"
      class="xm-rate-star"
      :style="boxStyle"
      @click="pick(n)"
    >
      <view class="xm-rate-empty">
        <xm-icon
          name="star"
          :size="size"
        />
      </view>
      <view
        class="xm-rate-full"
        :style="'width:' + fillOf(n) + '%'"
      >
        <xm-icon
          name="star-fill"
          :size="size"
        />
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { rpx } from '@/composables/useScreen'

defineOptions({ options: { virtualHost: true } })

const props = defineProps({
  modelValue: { type: Number, default: 0 },
  readonly: { type: Boolean, default: false },
  /** 单颗星的边长（rpx） */
  size: { type: Number, default: 44 },
})

const emit = defineEmits(['update:modelValue'])

const boxStyle = computed(() => `width:${rpx(props.size)};height:${rpx(props.size)};`)

/** 第 n 颗星填满多少（0~100）：4.3 分时第 5 颗填 30% */
const fillOf = (n) => Math.round(Math.max(0, Math.min(1, (Number(props.modelValue) || 0) - (n - 1))) * 100)

const pick = (n) => {
  if (!props.readonly) emit('update:modelValue', n)
}
</script>

<style lang="scss" scoped>
.xm-rate {
  display: inline-flex;
  gap: 8rpx;
}

.xm-rate-star {
  position: relative;
  flex-shrink: 0;
}

.xm-rate-empty {
  display: flex;
  color: var(--xm-text-3);
  opacity: 0.6;
}

.xm-rate-full {
  position: absolute;
  left: 0;
  top: 0;
  display: flex;
  overflow: hidden;
  color: #f5a623;
}
</style>

<template>
  <div class="ring" :style="{ width: size + 'px', height: size + 'px' }" role="img" :aria-label="ariaLabel">
    <svg :viewBox="`0 0 ${size} ${size}`" :width="size" :height="size">
      <circle :cx="size / 2" :cy="size / 2" :r="radius" class="ring__track" :stroke-width="stroke" fill="none" />
      <circle
        :cx="size / 2"
        :cy="size / 2"
        :r="radius"
        class="ring__bar"
        :stroke-width="stroke"
        fill="none"
        stroke-linecap="round"
        :stroke-dasharray="circumference"
        :stroke-dashoffset="offset"
        :transform="`rotate(-90 ${size / 2} ${size / 2})`"
      />
    </svg>
    <div class="ring__center">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

/** 环形进度：value / max，超过 max 按满环画；max 为空或 0 时只画底环 */
const props = withDefaults(
  defineProps<{
    value: number
    max?: number | null
    size?: number
    stroke?: number
    ariaLabel?: string
  }>(),
  { max: 100, size: 120, stroke: 10, ariaLabel: '' },
)

const radius = computed(() => (props.size - props.stroke) / 2)
const circumference = computed(() => 2 * Math.PI * radius.value)
const ratio = computed(() => {
  if (!props.max || props.max <= 0) return 0
  return Math.min(1, Math.max(0, props.value / props.max))
})
const offset = computed(() => circumference.value * (1 - ratio.value))
</script>

<style scoped>
.ring {
  position: relative;
  flex-shrink: 0;
}

.ring__track {
  stroke: var(--xm-bg-sunken);
}

.ring__bar {
  stroke: var(--xm-brand);
  transition: stroke-dashoffset 0.6s ease;
}

.ring__center {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  align-content: center;
  text-align: center;
}

@media (prefers-reduced-motion: reduce) {
  .ring__bar {
    transition: none;
  }
}
</style>

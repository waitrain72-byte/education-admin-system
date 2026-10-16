<template>
  <!-- 与 CourseCard 同尺寸：封面（标签、课名、时间）+ 底栏两段字；index 只用来让每张卡的字长短不一 -->
  <div class="cc-sk" aria-hidden="true">
    <div class="cc-sk__cover">
      <SkBlock class="cc-sk__bg" tone="cover" w="100%" h="100%" />
      <div class="cc-sk__inner">
        <SkBlock tone="on-cover" shape="pill" :w="38" :h="18" />
        <SkBlock class="cc-sk__name" tone="on-cover" :w="NAME_WIDTHS[index % NAME_WIDTHS.length]" :h="18" />
        <SkBlock tone="on-cover" :w="TIME_WIDTHS[index % TIME_WIDTHS.length]" :h="12" />
      </div>
    </div>
    <div class="cc-sk__foot">
      <SkBlock :w="44" :h="12" />
      <SkBlock :w="40" :h="12" />
    </div>
  </div>
</template>

<script setup lang="ts">
import SkBlock from './SkBlock.vue'

withDefaults(defineProps<{ index?: number }>(), { index: 0 })

const NAME_WIDTHS = ['56%', '70%', '44%', '62%', '50%', '66%']
const TIME_WIDTHS = ['74%', '60%', '82%', '68%']
</script>

<style scoped>
.cc-sk {
  display: flex;
  flex-direction: column;
  min-width: 0;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius-lg);
  background: var(--xm-bg-card);
  overflow: hidden;
}

.cc-sk__cover {
  position: relative;
  min-height: 112px;
}

.cc-sk__bg {
  position: absolute;
  inset: 0;
  border-radius: 0;
}

.cc-sk__inner {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 112px;
  padding: 14px 16px 16px;
}

.cc-sk__name {
  margin-top: auto;
}

.cc-sk__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 16px;
  min-height: 41px;
}
</style>

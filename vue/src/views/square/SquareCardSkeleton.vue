<template>
  <!-- 与 SquareCard 同尺寸：封面（标签、课名、时间）+ 教师、简介两行、名额条、选课按钮 -->
  <div class="sq-sk" aria-hidden="true">
    <div class="sq-sk__cover">
      <SkBlock class="sq-sk__bg" tone="cover" w="100%" h="100%" />
      <div class="sq-sk__inner">
        <span class="sq-sk__tags">
          <SkBlock tone="on-cover" shape="pill" :w="38" :h="20" />
          <SkBlock tone="on-cover" shape="pill" :w="46" :h="20" />
        </span>
        <SkBlock class="sq-sk__name" tone="on-cover" :w="NAME_WIDTHS[index % NAME_WIDTHS.length]" :h="18" />
        <SkBlock tone="on-cover" :w="TIME_WIDTHS[index % TIME_WIDTHS.length]" :h="12" />
      </div>
    </div>
    <div class="sq-sk__body">
      <SkBlock :w="56" :h="13" />
      <span class="sk-stack sq-sk__intro">
        <SkBlock w="92%" :h="11" />
        <SkBlock :w="index % 2 ? '58%' : '70%'" :h="11" />
      </span>
      <span class="sq-sk__seats">
        <SkBlock class="sq-sk__bar" :h="5" shape="pill" />
        <SkBlock :w="52" :h="12" />
      </span>
      <span class="sq-sk__foot">
        <SkBlock :w="64" :h="24" />
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import SkBlock from '@/components/skeleton/SkBlock.vue'

withDefaults(defineProps<{ index?: number }>(), { index: 0 })

const NAME_WIDTHS = ['56%', '70%', '44%', '62%', '50%', '66%']
const TIME_WIDTHS = ['64%', '52%', '72%', '58%']
</script>

<style scoped>
.sq-sk {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius-lg);
  background: var(--xm-bg-card);
  overflow: hidden;
}

.sq-sk__cover {
  position: relative;
  min-height: 112px;
}

.sq-sk__bg {
  position: absolute;
  inset: 0;
  border-radius: 0;
}

.sq-sk__inner {
  position: relative;
  display: grid;
  gap: 8px;
  align-content: start;
  min-height: 112px;
  padding: 14px 16px;
}

.sq-sk__tags {
  display: flex;
  gap: 4px;
}

.sq-sk__name {
  margin-top: 6px;
}

.sq-sk__body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
  padding: 12px 16px 14px;
}

.sq-sk__intro {
  gap: 7px;
}

.sq-sk__seats {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: auto;
}

.sq-sk__bar {
  flex: 1;
}

.sq-sk__foot {
  display: flex;
  justify-content: flex-end;
  min-height: 28px;
  align-items: center;
}
</style>

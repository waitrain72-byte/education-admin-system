<template>
  <!-- 列表占位：每行「头像 / 色条 / 日期块 / 类型胶囊」+ 一到两行字 + 末尾的胶囊、按钮或时间，覆盖各页常见的列表样式 -->
  <ul class="sk-list" :style="{ gap: gap + 'px' }" aria-hidden="true">
    <li v-for="i in rows" :key="i" class="sk-list__row">
      <SkBlock v-if="lead === 'avatar'" :w="avatar" :h="avatar" shape="circle" />
      <SkBlock v-else-if="lead === 'bar'" :w="4" :h="lines > 1 ? 36 : 20" />
      <SkBlock v-else-if="lead === 'date'" :w="40" :h="40" />
      <SkBlock v-else-if="lead === 'pill'" :w="44" :h="22" />
      <span class="sk-stack sk-list__text">
        <SkBlock :w="TITLE_WIDTHS[(i - 1) % TITLE_WIDTHS.length]" />
        <SkBlock v-if="lines > 1" :w="META_WIDTHS[(i - 1) % META_WIDTHS.length]" :h="11" />
      </span>
      <SkBlock v-if="trail === 'pill'" :w="52" :h="22" />
      <SkBlock v-else-if="trail === 'button'" :w="56" :h="24" />
      <SkBlock v-else-if="trail === 'text'" :w="72" :h="12" />
    </li>
  </ul>
</template>

<script setup lang="ts">
import SkBlock from './SkBlock.vue'

withDefaults(
  defineProps<{
    rows?: number
    lead?: 'none' | 'avatar' | 'bar' | 'date' | 'pill'
    avatar?: number
    lines?: 1 | 2
    trail?: 'none' | 'pill' | 'button' | 'text'
    gap?: number
  }>(),
  { rows: 3, lead: 'none', avatar: 30, lines: 2, trail: 'none', gap: 14 },
)

// 每行长短不一才像真的文字；固定序列，重新渲染不会抖
const TITLE_WIDTHS = ['62%', '48%', '74%', '55%', '68%', '42%']
const META_WIDTHS = ['38%', '52%', '30%', '45%']
</script>

<style scoped>
.sk-list {
  display: grid;
  margin: 0;
  padding: 0;
  list-style: none;
}

.sk-list__row {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.sk-list__text {
  flex: 1;
}
</style>

<template>
  <span class="sk" :class="classes" :style="style" aria-hidden="true"></span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

/** 一个占位块；样式与扫光在 layout.css 的 .sk */
const props = defineProps<{
  /** 宽：数字按 px，字符串原样（如 '60%'）；不传就撑满 */
  w?: number | string
  /** 高：默认 14px，正好一行正文 */
  h?: number | string
  shape?: 'pill' | 'circle'
  /** 放在什么底上：品牌浅底、课程封面、封面上的文字 */
  tone?: 'brand' | 'cover' | 'on-cover'
}>()

const size = (v: number | string | undefined) => (typeof v === 'number' ? `${v}px` : v)

const classes = computed(() => [props.shape ? `sk--${props.shape}` : '', props.tone ? `sk--${props.tone}` : ''])
const style = computed(() => ({ width: size(props.w), height: size(props.h ?? 14) }))
</script>

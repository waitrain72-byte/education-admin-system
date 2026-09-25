<template>
  <view
    class="xm-icon"
    :class="{ 'is-text': !uri }"
    :style="styleText"
    >{{ uri ? '' : name }}</view
  >
</template>

<script setup>
/**
 * 线性图标（easycom 自动注册）：<xm-icon name="bell" :size="40" />
 * - 用 CSS mask 把 SVG 当遮罩、底色取 currentColor：图标颜色跟随父元素文字色，主题色 / 深浅色自动适配
 *   （unibest 等项目在小程序里用 UnoCSS 图标也是同一原理），替代各机型渲染不一致的 emoji
 * - name 不在图标表里时按文本原样显示，兼容旧的 emoji 用法
 */
import { computed } from 'vue'
import { ICONS } from './icons'
import { rpx } from '@/composables/useScreen'

defineOptions({ options: { virtualHost: true } })

const props = defineProps({
  name: { type: String, default: '' },
  /** 数字按 rpx 处理，也可传带单位的字符串 */
  size: { type: [Number, String], default: 40 },
  /** 不传则继承文字颜色 */
  color: { type: String, default: '' },
})

// data URI 按图标名缓存：同一图标在列表里反复出现时不必重复编码
const uriCache = {}
const iconUri = (name) => {
  if (!ICONS[name]) return ''
  if (!uriCache[name]) {
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#000" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${ICONS[name]}</svg>`
    uriCache[name] = 'data:image/svg+xml,' + encodeURIComponent(svg)
  }
  return uriCache[name]
}

const uri = computed(() => iconUri(props.name))
// 数字按 rpx 处理：手机等比缩放，宽屏（>414px）封顶为固定 px，与构建期样式规则一致
const sizeText = computed(() => (typeof props.size === 'number' ? rpx(props.size) : props.size))

const styleText = computed(() => {
  const size = sizeText.value
  if (!uri.value) return `font-size:${size};line-height:1;` + (props.color ? `color:${props.color};` : '')
  const mask = `url("${uri.value}")`
  return (
    `width:${size};height:${size};` +
    `background-color:${props.color || 'currentColor'};` +
    `-webkit-mask-image:${mask};mask-image:${mask};`
  )
})
</script>

<style lang="scss" scoped>
.xm-icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
  -webkit-mask-repeat: no-repeat;
  mask-repeat: no-repeat;
  -webkit-mask-position: center;
  mask-position: center;
  -webkit-mask-size: 100% 100%;
  mask-size: 100% 100%;
}

.xm-icon.is-text {
  width: auto;
  height: auto;
}
</style>

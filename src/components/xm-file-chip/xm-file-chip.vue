<template>
  <!-- 附件（与 Web 端 components/FileChip.vue 一致）：按扩展名显示带颜色的类型标 + 原文件名，
       点一下打开：图片预览，文档下载后用系统打开（打不开时把地址复制到剪贴板） -->
  <view
    class="file-chip"
    :class="'file-chip-' + kind"
    @click="open"
  >
    <text class="file-chip-icon">{{ ext || '·' }}</text>
    <text class="file-chip-name">{{ name || fallback || fileNameOf(url) || $t('space.assignments.attachment') }}</text>
    <text
      v-if="size != null"
      class="file-chip-size xm-num"
      >{{ formatSize(size) }}</text
    >
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { fileExt, fileKind, formatSize } from '@/utils/courseSpace'
import { fileNameOf, openAttachment } from '@/utils/attachment'

defineOptions({ options: { virtualHost: true } })

const props = defineProps({
  url: { type: String, default: '' },
  name: { type: String, default: '' },
  size: { type: Number, default: null },
  fallback: { type: String, default: '' },
})

const ext = computed(() => fileExt(props.name || props.url).slice(0, 4))
const kind = computed(() => fileKind(props.name || props.url))

const open = () => openAttachment(props.url)
</script>

<style lang="scss" scoped>
.file-chip {
  display: inline-flex;
  align-items: center;
  gap: 12rpx;
  max-width: 100%;
  padding: 10rpx 18rpx 10rpx 10rpx;
  border: 1rpx solid var(--xm-border);
  border-radius: 14rpx;
  background: var(--xm-bg-card);
  box-sizing: border-box;
}

.file-chip:active {
  border-color: var(--xm-brand);
}

/* 类型标：扩展名缩写，颜色按文件类别 */
.file-chip-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 56rpx;
  height: 44rpx;
  padding: 0 6rpx;
  border-radius: 8rpx;
  font-size: 18rpx;
  font-weight: bold;
  text-transform: uppercase;
  color: #ffffff;
  background: #6b7772;
  box-sizing: border-box;
}

.file-chip-pdf .file-chip-icon {
  background: #c2372c;
}

.file-chip-doc .file-chip-icon {
  background: #2565a8;
}

.file-chip-sheet .file-chip-icon {
  background: #1d8048;
}

.file-chip-slide .file-chip-icon {
  background: #b35a12;
}

.file-chip-image .file-chip-icon {
  background: #6d48c9;
}

.file-chip-archive .file-chip-icon {
  background: #9a5c00;
}

.file-chip-name {
  flex: 1;
  min-width: 0;
  font-size: 26rpx;
  color: var(--xm-text);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.file-chip-size {
  flex-shrink: 0;
  font-size: 22rpx;
  color: var(--xm-text-2);
}
</style>

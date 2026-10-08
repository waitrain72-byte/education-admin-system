<template>
  <a
    class="file-chip"
    :class="'file-chip--' + kind"
    :href="href"
    :download="name || ''"
    target="_blank"
    rel="noopener"
    :title="name || undefined"
  >
    <span class="file-chip__icon" aria-hidden="true">{{ ext || '·' }}</span>
    <span class="file-chip__name">{{ name || fallback || $t('space.assignments.attachment') }}</span>
    <span v-if="size != null" class="file-chip__size num">{{ formatSize(size) }}</span>
  </a>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { resolveFileUrl } from '@/utils/file'
import { fileExt, fileKind, formatSize } from '@/utils/courseSpace'

/**
 * 附件链接：按扩展名显示带颜色的类型标，点击以原文件名下载。
 * 存储名是内容哈希（如 3f2a….pdf），download 属性让浏览器按原文件名保存。
 */
const props = withDefaults(
  defineProps<{
    url: string
    name?: string | null
    size?: number | null
    fallback?: string
  }>(),
  { name: '', size: null, fallback: '' },
)

const href = computed(() => resolveFileUrl(props.url))
const ext = computed(() => fileExt(props.name || props.url).slice(0, 4))
const kind = computed(() => fileKind(props.name || props.url))
</script>

<style scoped>
.file-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
  padding: 6px 10px 6px 6px;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius);
  background: var(--xm-bg-card);
  color: var(--xm-text-primary);
  font-size: 13px;
  line-height: 1.3;
}

.file-chip:hover {
  border-color: var(--xm-brand);
  color: var(--xm-brand);
}

.file-chip__icon {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  border-radius: 6px;
  background: var(--xm-bg-sunken);
  color: var(--xm-text-secondary);
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
}

.file-chip--pdf .file-chip__icon {
  background: var(--xm-bad-soft);
  color: var(--xm-bad);
}

.file-chip--doc .file-chip__icon {
  background: var(--xm-info-soft);
  color: var(--xm-info);
}

.file-chip--sheet .file-chip__icon {
  background: var(--xm-ok-soft);
  color: var(--xm-ok);
}

.file-chip--slide .file-chip__icon,
.file-chip--archive .file-chip__icon {
  background: var(--xm-warn-soft);
  color: var(--xm-warn);
}

.file-chip--image .file-chip__icon {
  background: var(--xm-brand-soft);
  color: var(--xm-brand);
}

.file-chip__name {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.file-chip__size {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--xm-text-secondary);
}
</style>

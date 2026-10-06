<template>
  <span class="user-avatar" :style="boxStyle" :title="name || undefined">
    <img v-if="src && !broken" :src="src" :alt="name || ''" @error="broken = true" />
    <span v-else class="user-avatar__initial" :style="{ background: color }">{{ initial }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { resolveFileUrl } from '@/utils/file'
import { courseColor } from '@/utils/courseColor'

/**
 * 头像：有图显示图（地址经 resolveFileUrl 归一），没有图或加载失败时显示姓氏首字。
 * 首字底色按姓名哈希取色，同一个人在名单、签到格子、消息里颜色一致。
 */
const props = withDefaults(
  defineProps<{
    name?: string | null
    avatar?: string | null
    size?: number
  }>(),
  { name: '', avatar: '', size: 32 },
)

const broken = ref(false)
const src = computed(() => resolveFileUrl(props.avatar || ''))
watch(src, () => {
  broken.value = false
})

const initial = computed(() => (props.name || '?').trim().charAt(0).toUpperCase() || '?')
const color = computed(() => courseColor(props.name || ''))
const boxStyle = computed(() => ({
  width: props.size + 'px',
  height: props.size + 'px',
  fontSize: Math.round(props.size * 0.42) + 'px',
}))
</script>

<style scoped>
.user-avatar {
  display: inline-flex;
  flex-shrink: 0;
  border-radius: 50%;
  overflow: hidden;
  background: var(--xm-bg-sunken);
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.user-avatar__initial {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  color: #fff;
  font-weight: 600;
  line-height: 1;
}
</style>

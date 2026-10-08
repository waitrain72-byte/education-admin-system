<template>
  <!-- 头像（与 Web 端 components/UserAvatar.vue 一致）：有图显示图，没有图或加载失败时显示姓名首字，
       首字底色按姓名哈希取色，同一个人在名单、签到格子、消息里颜色一致 -->
  <view
    class="user-avatar"
    :style="boxStyle"
  >
    <image
      v-if="src && !broken"
      :src="src"
      class="user-avatar-img"
      mode="aspectFill"
      @error="broken = true"
    />
    <view
      v-else
      class="user-avatar-initial"
      :style="initialStyle"
      >{{ initial }}</view
    >
  </view>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { resolveFileUrl } from '@/utils/request'
import { courseColor } from '@/utils/courseColor'
import { rpx } from '@/composables/useScreen'

defineOptions({ options: { virtualHost: true } })

const props = defineProps({
  name: { type: String, default: '' },
  avatar: { type: String, default: '' },
  /** 边长（rpx） */
  size: { type: Number, default: 64 },
})

const broken = ref(false)
const src = computed(() => resolveFileUrl(props.avatar || ''))
watch(src, () => {
  broken.value = false
})

const initial = computed(() => (props.name || '?').trim().charAt(0).toUpperCase() || '?')
const boxStyle = computed(
  () => `width:${rpx(props.size)};height:${rpx(props.size)};font-size:${rpx(Math.round(props.size * 0.42))};`,
)
const initialStyle = computed(() => `background:${courseColor(props.name || '')};`)
</script>

<style lang="scss" scoped>
.user-avatar {
  display: inline-flex;
  flex-shrink: 0;
  border-radius: 50%;
  overflow: hidden;
  background: var(--xm-bg-sunken);
}

.user-avatar-img {
  width: 100%;
  height: 100%;
  display: block;
}

.user-avatar-initial {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: #ffffff;
  font-weight: 600;
  line-height: 1;
}
</style>

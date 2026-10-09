<template>
  <!-- 单个附件（与 Web 端 components/FileUploadButton.vue 一致）：没有时显示上传按钮，有了显示文件和「移除」 -->
  <view class="file-picker">
    <template v-if="modelValue && modelValue.url">
      <view class="file-picker-file">
        <xm-file-chip
          :url="modelValue.url"
          :name="modelValue.name"
        />
      </view>
      <text
        v-if="!disabled"
        class="file-picker-remove"
        @click="$emit('update:modelValue', null)"
        >{{ $t('space.assignments.remove') }}</text
      >
    </template>
    <button
      v-else
      class="xm-btn xm-btn-plain xm-btn-sm"
      :disabled="disabled || progress !== null"
      @click="pick"
    >
      <xm-icon
        name="paperclip"
        :size="28"
      />
      {{ progress !== null ? $t('mobile.file.uploading', { p: progress }) : label || $t('space.assignments.upload') }}
    </button>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { pickAttachment, uploadFile } from '@/utils/upload'

defineOptions({ options: { virtualHost: true } })

defineProps({
  /** { url, name } | null */
  modelValue: { type: Object, default: null },
  label: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])

/** 上传进度 0~100；null = 没在上传 */
const progress = ref(null)

const pick = async () => {
  const picked = await pickAttachment()
  if (!picked) return
  progress.value = 0
  try {
    const url = await uploadFile(picked.path, {
      loading: false,
      onProgress: (p) => {
        progress.value = p
      },
    })
    emit('update:modelValue', { url, name: picked.name })
  } catch {
    // 不支持的类型、超大等提示已由上传层统一弹出
  } finally {
    progress.value = null
  }
}
</script>

<style lang="scss" scoped>
.file-picker {
  display: flex;
  align-items: center;
  gap: 20rpx;
  min-width: 0;
}

.file-picker-file {
  flex: 1;
  min-width: 0;
}

.file-picker-remove {
  flex-shrink: 0;
  font-size: 26rpx;
  color: var(--xm-danger);
}
</style>

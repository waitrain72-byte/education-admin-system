<template>
  <div class="upload-field">
    <template v-if="modelValue?.url">
      <FileChip :url="modelValue.url" :name="modelValue.name" />
      <el-button link type="danger" :disabled="disabled" @click="emit('update:modelValue', null)">
        {{ $t('space.assignments.remove') }}
      </el-button>
    </template>
    <el-upload
      v-else
      :action="UPLOAD_URL"
      :headers="{ token: user.token }"
      :accept="UPLOAD_ACCEPT"
      :show-file-list="false"
      :disabled="disabled || uploading"
      :before-upload="beforeUpload"
      :on-success="onSuccess"
      :on-error="onError"
    >
      <el-button :loading="uploading" :disabled="disabled">{{ label || $t('space.assignments.upload') }}</el-button>
    </el-upload>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { UploadFile, UploadRawFile } from 'element-plus'
import FileChip from '@/components/FileChip.vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from '@/utils/element-plus'
import { MAX_UPLOAD_BYTES, UPLOAD_ACCEPT, UPLOAD_URL } from '@/utils/courseSpace'
import { t } from '@/i18n'

export interface UploadedFile {
  url: string
  name: string
}

/** 单个附件：没有时显示上传按钮，有了显示文件和「移除」 */
defineProps<{
  modelValue: UploadedFile | null
  label?: string
  disabled?: boolean
}>()
const emit = defineEmits<{ 'update:modelValue': [value: UploadedFile | null] }>()

const user = useUserStore()
const uploading = ref(false)

const beforeUpload = (file: UploadRawFile) => {
  if (file.size > MAX_UPLOAD_BYTES) {
    ElMessage.warning(t('space.upload.tooLarge'))
    return false
  }
  uploading.value = true
  return true
}

const onSuccess = (response: any, file: UploadFile) => {
  uploading.value = false
  if (response?.code !== '200' || !response.data) {
    // 上传接口的报错（如不支持的文件类型）比通用的「参数异常」更有用，直接显示
    ElMessage.error(response?.msg || t('space.assignments.uploadFailed'))
    return
  }
  emit('update:modelValue', { url: response.data, name: file.name })
}

const onError = () => {
  uploading.value = false
  ElMessage.error(t('space.assignments.uploadFailed'))
}
</script>

<style scoped>
.upload-field {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  min-width: 0;
}
</style>

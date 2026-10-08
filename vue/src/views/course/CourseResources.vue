<template>
  <div class="rs">
    <section v-if="canTeach" class="panel rs-upload">
      <el-upload
        drag
        multiple
        :action="UPLOAD_URL"
        :headers="{ token: user.token }"
        :accept="UPLOAD_ACCEPT"
        :show-file-list="false"
        :before-upload="beforeUpload"
        :on-success="onUploaded"
        :on-error="onUploadError"
      >
        <div class="rs-upload__inner">
          <span class="rs-upload__title">{{ $t('space.resources.upload') }}</span>
          <span class="rs-upload__hint">{{ $t('space.resources.uploadHint') }}</span>
        </div>
      </el-upload>
    </section>

    <section class="panel">
      <div v-if="!list.length && loading" class="panel__body">
        <el-skeleton animated :rows="4" />
      </div>
      <div v-else-if="!list.length" class="rs-empty">
        <div class="rs-empty__title">{{ $t('space.resources.empty') }}</div>
        <p v-if="canTeach" class="rs-empty__hint">{{ $t('space.resources.emptyTeacher') }}</p>
      </div>
      <ul v-else class="rs-list">
        <li v-for="item in list" :key="item.id" class="rs-item">
          <FileChip class="rs-item__file" :url="item.file" :name="item.name" />
          <span class="rs-item__meta">
            <span class="num">{{ formatSize(item.size) }}</span>
            <span>{{ item.uploaderName || '—' }}</span>
            <span class="num">{{ item.createTime }}</span>
          </span>
          <span class="rs-item__actions">
            <a class="rs-item__download" :href="resolveFileUrl(item.file)" :download="item.name">{{ $t('space.resources.download') }}</a>
            <el-button v-if="canTeach" link type="danger" @click="remove(item)">{{ $t('common.delete') }}</el-button>
          </span>
        </li>
      </ul>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import type { UploadFile, UploadRawFile } from 'element-plus'
import FileChip from '@/components/FileChip.vue'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from '@/utils/element-plus'
import { resolveFileUrl } from '@/utils/file'
import { formatSize, MAX_UPLOAD_BYTES, UPLOAD_ACCEPT, UPLOAD_URL } from '@/utils/courseSpace'
import { useCourseEvents } from '@/composables/useCourseEvents'
import { t } from '@/i18n'

const props = defineProps<{ overview: Record<string, any> }>()

const route = useRoute()
const user = useUserStore()
const courseId = computed(() => Number(route.params.id))
const relation = computed<string>(() => props.overview.relation || 'visitor')
const canTeach = computed(() => relation.value === 'teacher' || relation.value === 'admin')

const list = ref<Record<string, any>[]>([])
const loading = ref(false)

const load = async () => {
  loading.value = true
  try {
    list.value = (await request.get<Record<string, any>[]>(`/course/${courseId.value}/resources`)) || []
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    loading.value = false
  }
}

watch(courseId, load, { immediate: true })
useCourseEvents(() => courseId.value, ['resources'], load)

const beforeUpload = (file: UploadRawFile) => {
  if (file.size > MAX_UPLOAD_BYTES) {
    ElMessage.warning(`${file.name}：${t('space.upload.tooLarge')}`)
    return false
  }
  return true
}

/** 每个文件上传成功后登记成一条资料 */
const onUploaded = async (response: any, file: UploadFile) => {
  if (response?.code !== '200' || !response.data) {
    ElMessage.error(response?.msg || t('space.resources.failed', { name: file.name }))
    return
  }
  try {
    await request.post(`/course/${courseId.value}/resources`, {
      name: file.name,
      file: response.data,
      size: file.size ?? null,
    })
    ElMessage.success(t('space.resources.uploaded', { name: file.name }))
    await load()
  } catch {
    // 错误提示已由拦截器统一处理
  }
}

const onUploadError = (_error: Error, file: UploadFile) => {
  ElMessage.error(t('space.resources.failed', { name: file.name }))
}

const remove = async (item: Record<string, any>) => {
  try {
    await ElMessageBox.confirm(t('space.resources.deleteConfirm', { name: item.name }), t('common.confirmDeleteTitle'), {
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await request.delete(`/course/${courseId.value}/resources/${item.id}`)
    ElMessage.success(t('space.resources.deleted'))
    await load()
  } catch {
    // 错误提示已由拦截器统一处理
  }
}
</script>

<style scoped>
.rs {
  display: grid;
  /* 列宽可以缩到 0：里面的宽表格自己横向滚动，不把整页撑出屏幕 */
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
}

.rs-upload {
  padding: 14px;
}

.rs-upload :deep(.el-upload-dragger) {
  padding: 22px 16px;
  border-radius: var(--xm-radius);
  background: var(--xm-bg-sunken);
}

.rs-upload__inner {
  display: grid;
  gap: 4px;
}

.rs-upload__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--xm-brand);
}

.rs-upload__hint {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.rs-empty {
  display: grid;
  justify-items: center;
  gap: 6px;
  padding: 44px 20px;
  text-align: center;
}

.rs-empty__title {
  font-size: 15px;
  color: var(--xm-text-regular);
}

.rs-empty__hint {
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.rs-list {
  list-style: none;
  display: grid;
}

.rs-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 16px;
  padding: 12px 20px;
  border-bottom: 1px solid var(--xm-border);
}

.rs-item:last-child {
  border-bottom: 0;
}

.rs-item__file {
  justify-self: start;
  border-color: transparent;
  background: none;
  padding-left: 0;
}

.rs-item__meta {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: var(--xm-text-secondary);
  white-space: nowrap;
}

.rs-item__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.rs-item__download {
  font-size: 13px;
  color: var(--xm-brand);
}

.rs-item__download:hover {
  text-decoration: underline;
}

@media (max-width: 720px) {
  .rs-item {
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 6px 12px;
  }

  .rs-item__meta {
    grid-column: 1 / -1;
    grid-row: 2;
    flex-wrap: wrap;
    white-space: normal;
  }
}
</style>

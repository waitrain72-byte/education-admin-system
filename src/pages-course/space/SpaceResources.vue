<template>
  <!-- 课程空间 · 资料（与 Web 端 views/course/CourseResources.vue 一致）：老师上传课件、讲义，选课同学打开 / 下载 -->
  <view>
    <view
      v-if="canTeach"
      class="xm-card upload"
      @click="upload"
    >
      <view class="upload-icon">
        <xm-icon
          name="upload"
          :size="44"
        />
      </view>
      <view class="upload-text">
        <view class="upload-title">{{
          progress ? $t('mobile.file.uploading', { p: progress.percent }) : $t('space.resources.upload')
        }}</view>
        <view class="upload-hint">{{ progress ? progress.name : $t('space.resources.uploadHint') }}</view>
      </view>
    </view>

    <view
      v-if="!list.length && loading"
      class="xm-card"
    >
      <view class="skeleton skeleton-line" />
      <view class="skeleton skeleton-line short" />
    </view>
    <xm-empty
      v-else-if="!list.length"
      icon="folder"
      :text="canTeach ? $t('space.resources.emptyTeacher') : $t('space.resources.empty')"
    />
    <view
      v-else
      class="xm-card list"
    >
      <view
        v-for="item in list"
        :key="item.id"
        class="item"
      >
        <xm-file-chip
          :url="item.file"
          :name="item.name"
        />
        <view class="item-foot">
          <view class="item-meta">
            <text class="xm-num">{{ formatSize(item.size) }}</text>
            <text>{{ item.uploaderName || '—' }}</text>
            <text class="xm-num">{{ (item.createTime || '').slice(0, 16) }}</text>
          </view>
          <text
            v-if="canTeach"
            class="item-delete"
            @click="remove(item)"
            >{{ $t('common.delete') }}</text
          >
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { resourceApi } from '@/api'
import { SILENT } from '@/utils/request'
import { formatSize } from '@/utils/courseSpace'
import { pickAttachments, uploadFile } from '@/utils/upload'
import { useCourseEvents } from '@/composables/useCourseEvents'
import { confirm } from '@/utils/confirm'
import { t } from '@/i18n'

/** 一次最多选几个文件（微信选聊天文件上限 100，这里取够用的数） */
const MAX_PICK = 9

const props = defineProps({
  overview: { type: Object, required: true },
  courseId: { type: Number, required: true },
})

const relation = computed(() => props.overview.relation || 'visitor')
const canTeach = computed(() => relation.value === 'teacher' || relation.value === 'admin')

const list = ref([])
const loading = ref(false)
/** 正在上传的文件：{ name, percent }；null = 没在上传 */
const progress = ref(null)

const load = async () => {
  loading.value = true
  try {
    list.value = (await resourceApi.list(props.courseId, SILENT)) || []
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    loading.value = false
  }
}

watch(() => props.courseId, load, { immediate: true })

useCourseEvents(() => props.courseId, ['resources'], load)

/** 选几个文件，逐个上传后登记成课程资料；某个失败不影响其余 */
const upload = async () => {
  if (progress.value) return
  const files = await pickAttachments(MAX_PICK)
  for (const file of files) {
    progress.value = { name: file.name, percent: 0 }
    try {
      const url = await uploadFile(file.path, {
        loading: false,
        onProgress: (p) => {
          progress.value = { name: file.name, percent: p }
        },
      })
      await resourceApi.add(props.courseId, { name: file.name, file: url, size: file.size || null })
      uni.showToast({ title: t('space.resources.uploaded', { name: file.name }), icon: 'none' })
    } catch {
      // 上传层、请求层已提示具体原因
    }
  }
  progress.value = null
  if (files.length) await load()
}

const remove = async (item) => {
  if (!(await confirm(t('space.resources.deleteConfirm', { name: item.name })))) return
  try {
    await resourceApi.remove(props.courseId, item.id)
    uni.showToast({ title: t('space.resources.deleted'), icon: 'none' })
    await load()
  } catch {
    // 提示已由请求层统一弹出
  }
}

defineExpose({ reload: load })
</script>

<style lang="scss" scoped>
.upload {
  display: flex;
  align-items: center;
  gap: 24rpx;
  border: 2rpx dashed var(--xm-border);
  box-shadow: none;
}

.upload:active {
  border-color: var(--xm-brand);
}

.upload-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 88rpx;
  height: 88rpx;
  border-radius: 24rpx;
  background: var(--xm-brand-soft);
  color: var(--xm-brand);
  flex-shrink: 0;
}

.upload-text {
  flex: 1;
  min-width: 0;
}

.upload-title {
  font-size: 30rpx;
  font-weight: 600;
  color: var(--xm-text);
}

.upload-hint {
  margin-top: 4rpx;
  font-size: 22rpx;
  color: var(--xm-text-2);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.item {
  padding: 18rpx 0;
}

.item + .item {
  border-top: 1rpx solid var(--xm-border);
}

.item-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-top: 10rpx;
}

.item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6rpx 20rpx;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.item-delete {
  flex-shrink: 0;
  font-size: 24rpx;
  color: var(--xm-danger);
}
</style>

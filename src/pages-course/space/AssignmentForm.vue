<template>
  <!-- 布置 / 编辑作业（与 Web 端 views/course/AssignmentForm.vue 一致）。editing 为空时是新建 -->
  <xm-form-popup
    :visible="visible"
    :title="editing ? $t('space.assignments.edit') : $t('space.assignments.create')"
    :saving="saving"
    :confirm-text="editing ? $t('common.save') : $t('space.assignments.create')"
    @close="$emit('close')"
    @save="save"
  >
    <view class="xm-form-item">
      <view class="xm-form-label required">{{ $t('space.assignments.title') }}</view>
      <input
        class="xm-input"
        v-model="form.title"
        maxlength="100"
        :placeholder="$t('space.assignments.titlePlaceholder')"
      />
    </view>
    <view class="xm-form-item">
      <view class="xm-form-label">{{ $t('space.assignments.content') }}</view>
      <textarea
        class="xm-textarea content"
        v-model="form.content"
        maxlength="5000"
        :placeholder="$t('space.assignments.contentPlaceholder')"
        :show-confirm-bar="false"
      />
    </view>
    <view class="xm-form-item">
      <view class="xm-form-label required">{{ $t('space.assignments.deadline') }}</view>
      <xm-datetime
        v-model="form.deadline"
        :date-placeholder="$t('space.assignments.deadlinePlaceholder')"
      />
    </view>
    <view class="xm-form-item">
      <view class="xm-form-label">{{ $t('space.assignments.fullScore') }}</view>
      <input
        class="xm-input"
        v-model="form.fullScore"
        type="number"
        maxlength="4"
      />
    </view>
    <view class="xm-form-item">
      <view class="xm-form-label">{{ $t('space.assignments.attachment') }}</view>
      <xm-file-picker v-model="attachment" />
    </view>
  </xm-form-popup>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { assignmentApi } from '@/api'
import { isCompleteExamTime } from '@/utils/examCountdown'
import { t } from '@/i18n'

const props = defineProps({
  visible: { type: Boolean, default: false },
  courseId: { type: Number, required: true },
  /** 正在编辑的作业；null 表示新建 */
  editing: { type: Object, default: null },
})

const emit = defineEmits(['close', 'saved'])

const saving = ref(false)
const form = reactive({ title: '', content: '', deadline: '', fullScore: '100' })
const attachment = ref(null)

watch(
  () => props.visible,
  (open) => {
    if (!open) return
    const a = props.editing
    form.title = (a && a.title) || ''
    form.content = (a && a.content) || ''
    form.deadline = (a && a.deadline && a.deadline.slice(0, 16)) || ''
    form.fullScore = String((a && a.fullScore) || 100)
    attachment.value = a && a.attachment ? { url: a.attachment, name: a.attachmentName || '' } : null
  },
  { immediate: true },
)

const save = async () => {
  if (!form.title.trim()) {
    uni.showToast({ title: t('space.assignments.titleRequired'), icon: 'none' })
    return
  }
  if (!form.deadline) {
    uni.showToast({ title: t('space.assignments.deadlineRequired'), icon: 'none' })
    return
  }
  if (!isCompleteExamTime(form.deadline)) {
    uni.showToast({ title: t('mobile.datetime.incomplete'), icon: 'none' })
    return
  }
  // 满分 1~1000 的整数，填错了按 100
  const full = Math.round(Number(form.fullScore))
  const body = {
    title: form.title.trim(),
    content: form.content,
    deadline: form.deadline,
    fullScore: full >= 1 && full <= 1000 ? full : 100,
    attachment: (attachment.value && attachment.value.url) || null,
    attachmentName: (attachment.value && attachment.value.name) || null,
  }
  saving.value = true
  try {
    const saved = props.editing
      ? await assignmentApi.update(props.courseId, props.editing.id, body)
      : await assignmentApi.create(props.courseId, body)
    uni.showToast({
      title: props.editing ? t('space.assignments.saved') : t('space.assignments.created'),
      icon: 'none',
    })
    emit('saved', saved, !props.editing)
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
.content {
  min-height: 240rpx;
}
</style>

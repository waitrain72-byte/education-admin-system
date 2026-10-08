<template>
  <el-dialog
    :model-value="modelValue"
    :title="editing ? $t('space.assignments.edit') : $t('space.assignments.create')"
    width="600px"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item :label="$t('space.assignments.title')" prop="title">
        <el-input v-model="form.title" maxlength="100" show-word-limit :placeholder="$t('space.assignments.titlePlaceholder')" />
      </el-form-item>
      <el-form-item :label="$t('space.assignments.content')" prop="content">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="6"
          maxlength="5000"
          show-word-limit
          :placeholder="$t('space.assignments.contentPlaceholder')"
        />
      </el-form-item>
      <div class="form-row">
        <el-form-item :label="$t('space.assignments.deadline')" prop="deadline" class="form-row__grow">
          <el-date-picker
            v-model="form.deadline"
            type="datetime"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm"
            :placeholder="$t('space.assignments.deadlinePlaceholder')"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="$t('space.assignments.fullScore')" prop="fullScore">
          <el-input-number v-model="form.fullScore" :min="1" :max="1000" :step="10" :precision="0" controls-position="right" />
        </el-form-item>
      </div>
      <el-form-item :label="$t('space.assignments.attachment')">
        <FileUploadButton v-model="attachment" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">{{ $t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="saving" @click="save">{{ editing ? $t('common.save') : $t('space.assignments.create') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import FileUploadButton, { type UploadedFile } from '@/components/FileUploadButton.vue'
import request from '@/utils/request'
import { ElMessage } from '@/utils/element-plus'
import { t } from '@/i18n'

/** 布置 / 编辑作业的弹窗。editing 为空时是新建 */
const props = defineProps<{
  modelValue: boolean
  courseId: number
  editing: Record<string, any> | null
}>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: [assignment: Record<string, any>, created: boolean]
}>()

const formRef = ref<FormInstance>()
const saving = ref(false)
const form = reactive({ title: '', content: '', deadline: '', fullScore: 100 })
const attachment = ref<UploadedFile | null>(null)

const rules = computed<FormRules>(() => ({
  title: [{ required: true, whitespace: true, message: t('space.assignments.titleRequired'), trigger: 'blur' }],
  deadline: [{ required: true, message: t('space.assignments.deadlineRequired'), trigger: 'change' }],
}))

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    const a = props.editing
    form.title = a?.title || ''
    form.content = a?.content || ''
    form.deadline = a?.deadline || ''
    form.fullScore = a?.fullScore || 100
    attachment.value = a?.attachment ? { url: a.attachment, name: a.attachmentName || '' } : null
  },
  { immediate: true },
)

const save = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  const body = {
    title: form.title,
    content: form.content,
    deadline: form.deadline,
    fullScore: form.fullScore,
    attachment: attachment.value?.url || null,
    attachmentName: attachment.value?.name || null,
  }
  saving.value = true
  try {
    const base = `/course/${props.courseId}/assignments`
    const saved = props.editing
      ? await request.put<Record<string, any>>(`${base}/${props.editing.id}`, body)
      : await request.post<Record<string, any>>(base, body)
    ElMessage.success(props.editing ? t('space.assignments.saved') : t('space.assignments.created'))
    emit('saved', saved, !props.editing)
    emit('update:modelValue', false)
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.form-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.form-row__grow {
  flex: 1;
  min-width: 220px;
}
</style>

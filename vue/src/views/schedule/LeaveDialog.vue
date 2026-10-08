<template>
  <el-dialog
    :model-value="modelValue"
    :title="$t('leave.dialogTitle')"
    width="520px"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="ld">
      <div class="ld__row">
        <span class="ld__label">{{ $t('leave.range') }}</span>
        <span class="ld__value">{{ $t('leave.selected', { from: fmt(from), to: fmt(to), n: days }) }}</span>
      </div>

      <div class="ld__affected">
        <div class="ld__label">{{ $t('leave.affected', { n: affected.length }) }}</div>
        <el-skeleton v-if="loading" animated :rows="2" />
        <p v-else-if="!affected.length" class="ld__empty">{{ $t('leave.affectedEmpty') }}</p>
        <ul v-else class="ld__list">
          <li v-for="c in affected" :key="c.date + c.courseId" class="ld__item">
            <span class="ld__bar" :style="{ background: courseColor(c.courseName) }"></span>
            <span class="ld__when num">{{ fmt(c.date) }} {{ c.start }}</span>
            <span class="ld__course">{{ c.courseName }}</span>
            <span class="ld__teacher">{{ c.teacherName }}</span>
          </li>
        </ul>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item :label="$t('leave.reason')" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            :placeholder="$t('leave.reasonPlaceholder')"
          />
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">{{ $t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">{{ $t('leave.submit') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/utils/request'
import { ElMessage } from '@/utils/element-plus'
import { courseColor } from '@/utils/courseColor'
import { daysBetween, formatDay } from '@/utils/schedule'
import { currentLocale } from '@/composables/useLocale'
import { t } from '@/i18n'

/** 请假申请：先列出这几天要上的课，再填理由提交（审核由教务处在后台完成） */
const props = defineProps<{
  modelValue: boolean
  from: string
  to: string
}>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submitted: []
}>()

const formRef = ref<FormInstance>()
const form = reactive({ content: '' })
const rules = computed<FormRules>(() => ({
  content: [{ required: true, whitespace: true, message: t('leave.reasonRequired'), trigger: 'blur' }],
}))
const affected = ref<Record<string, any>[]>([])
const loading = ref(false)
const submitting = ref(false)

const days = computed(() => daysBetween(props.from, props.to) + 1)
const fmt = (iso: string) => formatDay(iso, currentLocale(), { month: 'short', day: 'numeric', weekday: 'short' })

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return
    form.content = ''
    affected.value = []
    loading.value = true
    try {
      affected.value = (await request.get<Record<string, any>[]>('/apply/preview', {
        params: { from: props.from, days: days.value },
      })) || []
    } catch {
      // 日期不合法等提示已由拦截器统一处理
    } finally {
      loading.value = false
    }
  },
  { immediate: true },
)

const submit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await request.post('/apply/add', { content: form.content, time: props.from, day: days.value })
    ElMessage.success(t('leave.submitted'))
    emit('submitted')
    emit('update:modelValue', false)
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.ld {
  display: grid;
  gap: 16px;
}

.ld__row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
}

.ld__label {
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.ld__value {
  font-size: 15px;
  font-weight: 600;
  color: var(--xm-text-primary);
}

.ld__affected {
  display: grid;
  gap: 8px;
  padding: 12px 14px;
  border-radius: var(--xm-radius);
  background: var(--xm-bg-sunken);
}

.ld__empty {
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.ld__list {
  display: grid;
  gap: 6px;
  list-style: none;
  max-height: 220px;
  overflow-y: auto;
}

.ld__item {
  display: grid;
  grid-template-columns: 4px auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.ld__bar {
  width: 4px;
  height: 18px;
  border-radius: 2px;
}

.ld__when {
  color: var(--xm-text-secondary);
  white-space: nowrap;
}

.ld__course {
  color: var(--xm-text-primary);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.ld__teacher {
  color: var(--xm-text-secondary);
}
</style>

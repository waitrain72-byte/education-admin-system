<template>
  <div>
    <div class="page-head">
      <div>
        <h1 class="page-title">{{ $t('admin.semester.title') }}</h1>
        <p class="page-sub">{{ $t('admin.semester.sub') }}</p>
      </div>
    </div>

    <section class="panel panel--pad semester" :aria-busy="skeleton">
      <!-- 骨架：三个字段的名称照常显示，输入框和右边的教学周预览是占位（免得先露出空表单） -->
      <template v-if="skeleton">
        <div class="sk-region semester-sk">
          <span class="sr-only">{{ $t('common.loading') }}</span>
          <div v-for="key in ['name', 'startDate', 'weeks']" :key="key" class="semester-sk__field">
            <span class="semester-sk__label">{{ $t('admin.semester.' + key) }}</span>
            <SkBlock :w="key === 'weeks' ? 150 : '100%'" :h="32" />
          </div>
          <SkBlock :w="64" :h="32" />
        </div>
        <div class="semester__preview sk-region" aria-hidden="true">
          <div class="semester__label">{{ $t('admin.semester.preview') }}</div>
          <SkBlock class="semester-sk__week" :w="120" :h="30" />
          <SkBlock :w="140" :h="13" />
          <ol class="semester__bar">
            <li v-for="w in 18" :key="w" class="semester-sk__cell"><SkBlock w="100%" :h="14" /></li>
          </ol>
        </div>
      </template>
      <template v-else>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="semester__form">
          <el-form-item prop="name" :label="$t('admin.semester.name')">
            <el-input id="semester-name" v-model="form.name" maxlength="50" :placeholder="$t('admin.semester.namePlaceholder')" />
          </el-form-item>
          <el-form-item prop="startDate" :label="$t('admin.semester.startDate')">
            <el-date-picker
              id="semester-start"
              v-model="form.startDate"
              type="date"
              value-format="YYYY-MM-DD"
              :placeholder="$t('admin.semester.startPlaceholder')"
              style="width: 100%"
            />
          </el-form-item>
          <p v-if="notMonday" class="semester__warn">{{ $t('admin.semester.notMonday') }}</p>
          <el-form-item prop="weeks" :label="$t('admin.semester.weeks')">
            <el-input-number id="semester-weeks" v-model="form.weeks" :min="1" :max="30" />
          </el-form-item>
          <el-button type="primary" :loading="saving" @click="save">{{ $t('common.save') }}</el-button>
        </el-form>

        <div class="semester__preview">
          <div class="semester__label">{{ $t('admin.semester.preview') }}</div>
          <div class="semester__week num">{{ previewWeek }}</div>
          <div class="semester__sub">{{ current.today }} {{ current.weekday }}</div>
          <ol class="semester__bar" :aria-label="$t('admin.semester.preview')">
            <li
              v-for="w in form.weeks || 0"
              :key="w"
              :class="{ 'is-past': week > 0 && w < week, 'is-now': w === week }"
              :title="$t('workbench.weekN', { n: w })"
            ></li>
          </ol>
        </div>
      </template>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from '@/utils/element-plus'
import request from '@/utils/request'
import { useSkeleton } from '@/composables/useSkeleton'
import SkBlock from '@/components/skeleton/SkBlock.vue'
import { t } from '@/i18n'

defineOptions({ name: 'SemesterSettings' })

const formRef = ref<FormInstance>()
const saving = ref(false)
const loading = ref(false)
const skeleton = useSkeleton(() => loading.value)
const form = reactive({ name: '', startDate: '', weeks: 18 })
const current = ref<Record<string, any>>({})

const rules = computed<FormRules>(() => ({
  name: [{ required: true, message: t('admin.semester.nameRequired'), trigger: 'blur' }],
  startDate: [{ required: true, message: t('admin.semester.startRequired'), trigger: 'change' }],
}))

/** 按表单里的开学日期，预览「今天」是第几周（与后端同一算法：开学当天第 1 周，满 7 天加 1） */
const week = computed(() => {
  if (!form.startDate || !current.value.today) return 0
  const start = Date.UTC(+form.startDate.slice(0, 4), +form.startDate.slice(5, 7) - 1, +form.startDate.slice(8, 10))
  const today = Date.UTC(+current.value.today.slice(0, 4), +current.value.today.slice(5, 7) - 1, +current.value.today.slice(8, 10))
  if (today < start) return 0
  return Math.floor((today - start) / 86400000 / 7) + 1
})

const previewWeek = computed(() => {
  if (!form.startDate) return '—'
  if (week.value === 0) return t('workbench.beforeSemester')
  if (week.value > form.weeks) return t('workbench.afterSemester')
  return t('workbench.weekN', { n: week.value })
})

/** 开学日期不是周一时提醒：教学周按 7 天一周切分，从周一开始最直观 */
const notMonday = computed(() => {
  if (!form.startDate) return false
  const d = new Date(Date.UTC(+form.startDate.slice(0, 4), +form.startDate.slice(5, 7) - 1, +form.startDate.slice(8, 10)))
  return d.getUTCDay() !== 1
})

const load = async () => {
  loading.value = true
  try {
    const data = await request.get<Record<string, any>>('/config/semester')
    current.value = data || {}
    form.name = data?.name || ''
    form.startDate = data?.startDate || ''
    form.weeks = data?.weeks || 18
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    loading.value = false
  }
}

const save = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    current.value = await request.put<Record<string, any>>('/config/semester', { ...form, name: form.name.trim() })
    ElMessage.success(t('common.saveSuccess'))
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.semester {
  display: grid;
  grid-template-columns: minmax(0, 420px) minmax(0, 1fr);
  gap: 40px;
}

.semester__warn {
  margin: -6px 0 14px;
  font-size: 13px;
  color: var(--xm-warn);
}

.semester__preview {
  align-self: start;
  padding: 20px;
  border-radius: var(--xm-radius);
  background: var(--xm-bg-sunken);
}

.semester__label {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.semester__week {
  margin-top: 6px;
  font-size: 30px;
  font-weight: 700;
  color: var(--xm-text-primary);
}

.semester__sub {
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.semester__bar {
  list-style: none;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(14px, 1fr));
  gap: 4px;
  margin-top: 18px;
}

.semester__bar li {
  height: 14px;
  border-radius: 3px;
  background: var(--xm-bg-card);
  border: 1px solid var(--xm-border);
}

.semester__bar li.is-past {
  background: var(--el-color-primary-light-7);
  border-color: transparent;
}

.semester__bar li.is-now {
  background: var(--xm-brand);
  border-color: transparent;
}

/* ---------- 骨架：字段间距与 el-form-item 一致 ---------- */
.semester-sk {
  display: grid;
  gap: 22px;
  align-content: start;
}

.semester-sk__field {
  display: grid;
  gap: 8px;
}

.semester-sk__label {
  font-size: 14px;
  color: var(--xm-text-regular);
}

.semester-sk__week {
  margin: 6px 0;
}

.semester__bar li.semester-sk__cell {
  border: 0;
  background: none;
}

@media (max-width: 860px) {
  .semester {
    grid-template-columns: minmax(0, 1fr);
    gap: 24px;
  }
}
</style>

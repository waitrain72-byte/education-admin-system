<template>
  <CrudPage
      url="/examplan"
      :columns="columns"
      :fields="fields"
      :dialog-title="$t('pages.examplan.dialogTitle')"
      :rules="rules"
      :search="search"
      :before-save="normalizeCourse"
  >
    <!-- 考试时间 + 倒计时标签（今天 / 明天 / 还有 N 天 / 已结束）；历史数据没有考试时间时显示 - -->
    <template #examTime="{ row }">
      <template v-if="row.examTime">
        <span>{{ row.examTime }}</span>
        <ExamCountdownTag :exam-time="row.examTime" />
      </template>
      <span v-else>-</span>
    </template>
    <!-- 标题下面一行是所属课程（空 = 全校考试），不另占一列，免得把标题挤没 -->
    <template #name="{ row }">
      <div class="exam-title" :title="row.name">{{ row.name }}</div>
      <div class="exam-course">
        {{ row.courseId ? row.courseName || '-' : $t('pages.examplan.schoolWide') }}
      </div>
    </template>
  </CrudPage>
</template>

<script setup lang="ts">
defineOptions({ name: 'Examplan' })

import { computed, onMounted, ref } from 'vue'
import request from '@/utils/request'
import { t } from '@/i18n'
import CrudPage from '@/components/CrudPage.vue'
import ExamCountdownTag from '@/components/ExamCountdownTag.vue'
import { type CrudColumn } from '@/components/CrudTable.vue'
import { type SchemaField } from '@/components/SchemaForm.vue'

/** 所属课程的下拉选项：同名课程（不同老师开的班）后面带上任课教师 */
const courseOptions = ref<{ id: number; label: string }[]>([])

onMounted(async () => {
  try {
    const courses = (await request.get<Record<string, any>[]>('/course/selectAll')) || []
    courseOptions.value = courses.map((c) => ({
      id: c.id,
      label: c.teacherName ? `${c.name}（${c.teacherName}）` : c.name,
    }))
  } catch {
    // 提示已由请求拦截器统一弹出；下拉为空时仍可发布全校考试
  }
})

const columns = computed<CrudColumn[]>(() => [
  { prop: 'id', label: t('pages.examplan.id'), width: 80, align: 'center', sortable: true },
  { prop: 'name', label: t('pages.examplan.title') },
  { prop: 'content', label: t('pages.examplan.content'), showOverflowTooltip: true },
  { prop: 'examTime', label: t('pages.examplan.examTime'), width: 240 },
  { prop: 'time', label: t('pages.examplan.time') },
])

const fields = computed<SchemaField[]>(() => [
  { prop: 'name', label: t('pages.examplan.title') },
  {
    prop: 'courseId',
    label: t('pages.examplan.course'),
    type: 'select',
    options: courseOptions.value,
    optionLabel: 'label',
    placeholder: t('pages.examplan.coursePlaceholder'),
  },
  { prop: 'content', label: t('pages.examplan.content'), type: 'textarea', rows: 5 },
  { prop: 'examTime', label: t('pages.examplan.examTime'), type: 'datetime', placeholder: t('pages.examplan.examTimePlaceholder') },
])

/** 清空下拉后是空串：转成 null 交给后端，表示改回全校考试 */
const normalizeCourse = (form: Record<string, any>) => {
  if (form.courseId === '' || form.courseId === undefined) form.courseId = null
}

const rules = computed(() => ({
  name: [{ required: true, message: t('pages.examplan.ruleTitleRequired'), trigger: 'blur' }],
  content: [{ required: true, message: t('pages.examplan.ruleContentRequired'), trigger: 'blur' }],
  examTime: [{ required: true, message: t('pages.examplan.ruleExamTimeRequired'), trigger: 'change' }],
}))

const search = computed(() => [
  { key: 'name', placeholder: t('pages.examplan.searchPlaceholder') },
])
</script>

<style scoped>
.exam-title,
.exam-course {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.exam-course {
  font-size: 12px;
  color: var(--xm-text-secondary);
}
</style>

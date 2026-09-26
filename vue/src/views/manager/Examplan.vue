<template>
  <CrudPage
      url="/examplan"
      :columns="columns"
      :fields="fields"
      :dialog-title="$t('pages.examplan.dialogTitle')"
      :rules="rules"
      :search="search"
  >
    <!-- 考试时间 + 倒计时标签（今天 / 明天 / 还有 N 天 / 已结束）；历史数据没有考试时间时显示 - -->
    <template #examTime="{ row }">
      <template v-if="row.examTime">
        <span>{{ row.examTime }}</span>
        <ExamCountdownTag :exam-time="row.examTime" />
      </template>
      <span v-else>-</span>
    </template>
  </CrudPage>
</template>

<script setup lang="ts">
defineOptions({ name: 'Examplan' })

import { computed } from 'vue'
import { t } from '@/i18n'
import CrudPage from '@/components/CrudPage.vue'
import ExamCountdownTag from '@/components/ExamCountdownTag.vue'
import { type CrudColumn } from '@/components/CrudTable.vue'
import { type SchemaField } from '@/components/SchemaForm.vue'

const columns = computed<CrudColumn[]>(() => [
  { prop: 'id', label: t('pages.examplan.id'), width: 80, align: 'center', sortable: true },
  { prop: 'name', label: t('pages.examplan.title'), showOverflowTooltip: true },
  { prop: 'content', label: t('pages.examplan.content'), showOverflowTooltip: true },
  { prop: 'examTime', label: t('pages.examplan.examTime'), width: 240 },
  { prop: 'time', label: t('pages.examplan.time') },
])

const fields = computed<SchemaField[]>(() => [
  { prop: 'name', label: t('pages.examplan.title') },
  { prop: 'content', label: t('pages.examplan.content'), type: 'textarea', rows: 5 },
  { prop: 'examTime', label: t('pages.examplan.examTime'), type: 'datetime', placeholder: t('pages.examplan.examTimePlaceholder') },
])

const rules = computed(() => ({
  name: [{ required: true, message: t('pages.examplan.ruleTitleRequired'), trigger: 'blur' }],
  content: [{ required: true, message: t('pages.examplan.ruleContentRequired'), trigger: 'blur' }],
  examTime: [{ required: true, message: t('pages.examplan.ruleExamTimeRequired'), trigger: 'change' }],
}))

const search = computed(() => [
  { key: 'name', placeholder: t('pages.examplan.searchPlaceholder') },
])
</script>

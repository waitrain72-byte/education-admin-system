<template>
  <CrudPage
      url="/examplan"
      :columns="columns"
      :fields="fields"
      :dialog-title="$t('pages.examplan.dialogTitle')"
      :rules="rules"
      :search="search"
  />
</template>

<script setup lang="ts">
defineOptions({ name: 'Examplan' })

import { computed } from 'vue'
import { t } from '@/i18n'
import CrudPage from '@/components/CrudPage.vue'
import { type CrudColumn } from '@/components/CrudTable.vue'
import { type SchemaField } from '@/components/SchemaForm.vue'

const columns = computed<CrudColumn[]>(() => [
  { prop: 'id', label: t('pages.examplan.id'), width: 80, align: 'center', sortable: true },
  { prop: 'name', label: t('pages.examplan.title'), showOverflowTooltip: true },
  { prop: 'content', label: t('pages.examplan.content'), showOverflowTooltip: true },
  { prop: 'time', label: t('pages.examplan.time') },
])

const fields = computed<SchemaField[]>(() => [
  { prop: 'name', label: t('pages.examplan.title') },
  { prop: 'content', label: t('pages.examplan.content'), type: 'textarea', rows: 5 },
])

const rules = computed(() => ({
  name: [{ required: true, message: t('pages.examplan.ruleTitleRequired'), trigger: 'blur' }],
  content: [{ required: true, message: t('pages.examplan.ruleContentRequired'), trigger: 'blur' }],
}))

const search = computed(() => [
  { key: 'name', placeholder: t('pages.examplan.searchPlaceholder') },
])
</script>

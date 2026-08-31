<template>
  <CrudPage
      url="/college"
      :columns="columns"
      :fields="fields"
      :dialog-title="$t('pages.college.dialogTitle')"
      :rules="rules"
      :search="search"
  />
</template>

<script setup lang="ts">
defineOptions({ name: 'College' })

import { computed } from 'vue'
import { t } from '@/i18n'
import CrudPage from '@/components/CrudPage.vue'
import { type CrudColumn } from '@/components/CrudTable.vue'
import { type SchemaField } from '@/components/SchemaForm.vue'

const columns = computed<CrudColumn[]>(() => [
  { prop: 'id', label: t('pages.college.id'), width: 80, align: 'center', sortable: true },
  { prop: 'name', label: t('pages.college.name'), showOverflowTooltip: true },
  { prop: 'content', label: t('pages.college.content'), showOverflowTooltip: true },
])

const fields = computed<SchemaField[]>(() => [
  { prop: 'name', label: t('pages.college.name') },
  { prop: 'content', label: t('pages.college.content'), type: 'textarea', rows: 5 },
])

const rules = computed(() => ({
  name: [{ required: true, message: t('pages.college.ruleNameRequired'), trigger: 'blur' }],
}))

const search = computed(() => [
  { key: 'name', placeholder: t('pages.college.searchPlaceholder') },
])
</script>

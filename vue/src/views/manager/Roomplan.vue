<template>
  <CrudPage
      url="/roomplan"
      :columns="columns"
      :fields="fields"
      :dialog-title="$t('pages.roomplan.dialogTitle')"
      :rules="rules"
      :search="search"
  />
</template>

<script setup lang="ts">
defineOptions({ name: 'Roomplan' })

import { computed } from 'vue'
import { t } from '@/i18n'
import CrudPage, { type SearchField } from '@/components/CrudPage.vue'
import { type CrudColumn } from '@/components/CrudTable.vue'
import { type SchemaField } from '@/components/SchemaForm.vue'

const columns = computed<CrudColumn[]>(() => [
  { prop: 'id', label: t('pages.roomplan.id'), width: 80, align: 'center', sortable: true },
  { prop: 'name', label: t('pages.roomplan.name'), showOverflowTooltip: true },
  { prop: 'status', label: t('pages.roomplan.status'), showOverflowTooltip: true },
  { prop: 'content', label: t('pages.roomplan.description'), showOverflowTooltip: true },
  { prop: 'num', label: t('pages.roomplan.num') },
])

const statusOptions = computed(() => [
  { value: '空闲', label: t('pages.roomplan.free') },
  { value: '占用', label: t('pages.roomplan.occupied') },
])

const fields = computed<SchemaField[]>(() => [
  { prop: 'name', label: t('pages.roomplan.name') },
  {
    prop: 'status',
    label: t('pages.roomplan.status'),
    type: 'select',
    placeholder: t('pages.roomplan.statusPlaceholder'),
    options: statusOptions.value,
    optionLabel: 'label',
    optionValue: 'value',
  },
  { prop: 'num', label: t('pages.roomplan.num') },
  { prop: 'content', label: t('pages.roomplan.contentLabel'), type: 'textarea', rows: 5 },
])

const rules = computed(() => ({
  name: [{ required: true, message: t('pages.roomplan.ruleNameRequired'), trigger: 'blur' }],
  status: [{ required: true, message: t('pages.roomplan.ruleStatusRequired'), trigger: 'blur' }],
  num: [{ required: true, message: t('pages.roomplan.ruleNumRequired'), trigger: 'blur' }],
  content: [{ required: true, message: t('pages.roomplan.ruleContentRequired'), trigger: 'blur' }],
}))

const search = computed<SearchField[]>(() => [
  { key: 'name', placeholder: t('pages.roomplan.searchPlaceholder') },
  {
    key: 'status',
    placeholder: t('pages.roomplan.statusPlaceholder'),
    type: 'select',
    options: statusOptions.value,
    optionLabel: 'label',
    optionValue: 'value',
  },
])
</script>

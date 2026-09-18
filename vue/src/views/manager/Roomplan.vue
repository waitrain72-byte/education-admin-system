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
  { prop: 'code', label: t('pages.roomplan.code'), width: 110 },
  { prop: 'name', label: t('pages.roomplan.name'), showOverflowTooltip: true },
  { prop: 'type', label: t('pages.roomplan.type'), showOverflowTooltip: true },
  { prop: 'status', label: t('pages.roomplan.status'), showOverflowTooltip: true },
  { prop: 'content', label: t('pages.roomplan.description'), showOverflowTooltip: true },
  { prop: 'num', label: t('pages.roomplan.num') },
])

const statusOptions = computed(() => [
  { value: '空闲', label: t('pages.roomplan.free') },
  { value: '占用', label: t('pages.roomplan.occupied') },
])

const typeOptions = computed(() => [
  { value: '授课教室', label: t('pages.roomplan.typeTeaching') },
  { value: '运动场馆', label: t('pages.roomplan.typeVenue') },
  { value: '固定占用', label: t('pages.roomplan.typeFixed') },
])

const fields = computed<SchemaField[]>(() => [
  { prop: 'code', label: t('pages.roomplan.code'), placeholder: t('pages.roomplan.codePlaceholder') },
  { prop: 'name', label: t('pages.roomplan.name') },
  {
    prop: 'type',
    label: t('pages.roomplan.type'),
    type: 'select',
    placeholder: t('pages.roomplan.typePlaceholder'),
    options: typeOptions.value,
    optionLabel: 'label',
    optionValue: 'value',
  },
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
  code: [{ required: true, message: t('pages.roomplan.ruleCodeRequired'), trigger: 'blur' }],
  name: [{ required: true, message: t('pages.roomplan.ruleNameRequired'), trigger: 'blur' }],
  type: [{ required: true, message: t('pages.roomplan.ruleTypeRequired'), trigger: 'change' }],
  status: [{ required: true, message: t('pages.roomplan.ruleStatusRequired'), trigger: 'change' }],
  num: [{ required: true, message: t('pages.roomplan.ruleNumRequired'), trigger: 'blur' }],
  content: [{ required: true, message: t('pages.roomplan.ruleContentRequired'), trigger: 'blur' }],
}))

const search = computed<SearchField[]>(() => [
  { key: 'code', placeholder: t('pages.roomplan.codePlaceholder') },
  { key: 'name', placeholder: t('pages.roomplan.searchPlaceholder') },
  {
    key: 'type',
    placeholder: t('pages.roomplan.typePlaceholder'),
    type: 'select',
    options: typeOptions.value,
    optionLabel: 'label',
    optionValue: 'value',
  },
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

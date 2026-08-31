<template>
  <CrudPage
      url="/notice"
      :columns="columns"
      :fields="fields"
      :dialog-title="$t('pages.notice.dialogTitle')"
      :rules="rules"
      :search="search"
  />
</template>

<script setup lang="ts">
defineOptions({ name: 'Notice' })

import { computed } from 'vue'
import { t } from '@/i18n'
import CrudPage from '@/components/CrudPage.vue'
import { type CrudColumn } from '@/components/CrudTable.vue'
import { type SchemaField } from '@/components/SchemaForm.vue'

const columns = computed<CrudColumn[]>(() => [
  { prop: 'id', label: t('pages.notice.id'), width: 80, align: 'center', sortable: true },
  { prop: 'title', label: t('pages.notice.title'), showOverflowTooltip: true },
  { prop: 'content', label: t('pages.notice.content'), showOverflowTooltip: true },
  { prop: 'time', label: t('pages.notice.time') },
  { prop: 'user', label: t('pages.notice.creator') },
])

const fields = computed<SchemaField[]>(() => [
  { prop: 'title', label: t('pages.notice.title') },
  { prop: 'content', label: t('pages.notice.content'), type: 'textarea', rows: 5 },
])

const rules = computed(() => ({
  title: [{ required: true, message: t('pages.notice.ruleTitleRequired'), trigger: 'blur' }],
  content: [{ required: true, message: t('pages.notice.ruleContentRequired'), trigger: 'blur' }],
}))

const search = computed(() => [
  { key: 'title', placeholder: t('pages.notice.searchPlaceholder') },
])
</script>

<template>
  <CrudPage
      url="/speciality"
      :columns="columns"
      :fields="fields"
      :dialog-title="$t('pages.speciality.dialogTitle')"
      :rules="rules"
      :search="search"
  />
</template>

<script setup lang="ts">
defineOptions({ name: 'Speciality' })

import { computed, onMounted } from 'vue'
import { t } from '@/i18n'
import CrudPage from '@/components/CrudPage.vue'
import { type CrudColumn } from '@/components/CrudTable.vue'
import { type SchemaField } from '@/components/SchemaForm.vue'
import { useOptions } from '@/composables/useOptions'

const columns = computed<CrudColumn[]>(() => [
  { prop: 'id', label: t('pages.speciality.id'), width: 80, align: 'center', sortable: true },
  { prop: 'name', label: t('pages.speciality.name'), showOverflowTooltip: true },
  { prop: 'content', label: t('pages.speciality.content'), showOverflowTooltip: true },
  { prop: 'collegeName', label: t('pages.speciality.college'), showOverflowTooltip: true },
  { prop: 'score', label: t('pages.speciality.score'), showOverflowTooltip: true },
])

const { options: collegeData, load: loadCollege } = useOptions('/college/selectAll')

const fields = computed<SchemaField[]>(() => [
  { prop: 'name', label: t('pages.speciality.name') },
  { prop: 'content', label: t('pages.speciality.content'), type: 'textarea', rows: 5 },
  {
    prop: 'collegeId',
    label: t('pages.speciality.college'),
    type: 'select',
    placeholder: t('pages.speciality.collegePlaceholder'),
    options: collegeData.value,
    optionLabel: 'name',
    optionValue: 'id',
  },
  { prop: 'score', label: t('pages.speciality.score') },
])

const rules = computed(() => ({
  name: [{ required: true, message: t('pages.speciality.ruleNameRequired'), trigger: 'blur' }],
}))

const search = computed(() => [
  { key: 'name', placeholder: t('pages.speciality.searchPlaceholder') },
])

onMounted(() => {
  loadCollege()
})
</script>

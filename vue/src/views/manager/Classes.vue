<template>
  <CrudPage
      url="/classes"
      :columns="columns"
      :fields="fields"
      :dialog-title="$t('pages.classes.dialogTitle')"
      :rules="rules"
      :search="search"
  />
</template>

<script setup lang="ts">
defineOptions({ name: 'Classes' })

import { computed, onMounted } from 'vue'
import { t } from '@/i18n'
import CrudPage from '@/components/CrudPage.vue'
import { type CrudColumn } from '@/components/CrudTable.vue'
import { type SchemaField } from '@/components/SchemaForm.vue'
import { useOptions } from '@/composables/useOptions'

const columns = computed<CrudColumn[]>(() => [
  { prop: 'id', label: t('pages.classes.id'), width: 80, align: 'center', sortable: true },
  { prop: 'name', label: t('pages.classes.name'), showOverflowTooltip: true },
  { prop: 'content', label: t('pages.classes.content'), showOverflowTooltip: true },
  { prop: 'specialityName', label: t('pages.classes.speciality'), showOverflowTooltip: true },
  { prop: 'teacherName', label: t('pages.classes.teacher'), showOverflowTooltip: true },
])

const { options: specialityData, load: loadSpeciality } = useOptions('/speciality/selectAll')
const { options: teacherData, load: loadTeacher } = useOptions('/teacher/selectAll')

const fields = computed<SchemaField[]>(() => [
  { prop: 'name', label: t('pages.classes.name') },
  { prop: 'content', label: t('pages.classes.content'), type: 'textarea', rows: 5 },
  {
    prop: 'specialityId',
    label: t('pages.classes.speciality'),
    type: 'select',
    placeholder: t('pages.classes.specialityPlaceholder'),
    options: specialityData.value,
    optionLabel: 'name',
    optionValue: 'id',
  },
  {
    prop: 'teacherId',
    label: t('pages.classes.teacher'),
    type: 'select',
    placeholder: t('pages.classes.teacherPlaceholder'),
    options: teacherData.value,
    optionLabel: 'name',
    optionValue: 'id',
  },
])

const rules = computed(() => ({
  name: [{ required: true, message: t('pages.classes.ruleNameRequired'), trigger: 'blur' }],
}))

const search = computed(() => [
  { key: 'name', placeholder: t('pages.classes.searchPlaceholder') },
])

onMounted(() => {
  loadSpeciality()
  loadTeacher()
})
</script>

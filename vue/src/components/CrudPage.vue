<template>
  <div>
    <!-- 搜索栏 -->
    <div v-if="search && search.length" class="search">
      <template v-for="s in search" :key="s.key">
        <el-select
            v-if="s.type === 'select'"
            v-model="searchForm[s.key]"
            :placeholder="s.placeholder"
            style="width: 200px; margin-left: 5px"
            clearable
        >
          <el-option
              v-for="item in s.options || []"
              :key="item[s.optionValue || 'value']"
              :label="item[s.optionLabel || 'label']"
              :value="item[s.optionValue || 'value']"
          />
        </el-select>
        <el-input
            v-else
            v-model="searchForm[s.key]"
            :placeholder="s.placeholder"
            style="width: 200px"
        />
      </template>
      <el-button type="info" plain style="margin-left: 10px" @click="load(1)">{{ $t('common.search') }}</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">{{ $t('common.reset') }}</el-button>
    </div>

    <!-- 操作栏 -->
    <div v-if="canManage" class="operation">
      <el-button type="primary" plain @click="handleAdd">{{ $t('common.add') }}</el-button>
      <el-button type="danger" plain @click="delBatch">{{ $t('common.batchDelete') }}</el-button>
      <slot name="toolbar" />
    </div>

    <!-- 表格 -->
    <CrudTable
        :data="tableData"
        :columns="columns"
        :page-num="pageNum"
        :page-size="pageSize"
        :total="total"
        :loading="loading"
        :selectable="canManage"
        :show-actions="canManage && showActions"
        :actions-width="actionsWidth"
        @selection-change="handleSelectionChange"
        @page-change="load"
    >
      <template #actions="scope">
        <slot name="actions" :row="scope.row" :edit="handleEdit" :remove="del">
          <el-button link type="primary" size="small" @click="handleEdit(scope.row)">{{ $t('common.edit') }}</el-button>
          <el-button link type="danger" size="small" @click="del(scope.row.id)">{{ $t('common.delete') }}</el-button>
        </slot>
      </template>
    </CrudTable>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="dialogTitle" :width="dialogWidth" :close-on-click-modal="false" destroy-on-close>
      <SchemaForm ref="schemaForm" :model="form" :fields="fields" :rules="rules" :label-width="labelWidth" :padding-right="paddingRight" />
      <template #footer>
        <el-button @click="formVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="saveWithValidate">{{ $t('common.ok') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, computed, ref, onMounted } from 'vue'
import type { ComputedRef } from 'vue'
import type { FormRules } from 'element-plus'
import { useUser } from '@/components/useUser.ts'
import { useCrud } from '@/composables/useCrud'
import CrudTable, { type CrudColumn } from '@/components/CrudTable.vue'
import SchemaForm, { type SchemaField } from '@/components/SchemaForm.vue'

export interface SearchField {
    key: string
    placeholder?: string
    /** 搜索控件类型：input（默认）/ select */
    type?: 'input' | 'select'
    /** select 类型搜索框的选项与取值字段 */
    options?: any[]
    optionLabel?: string
    optionValue?: string
}

const props = withDefaults(defineProps<{
    /** 接口基础路径，如 '/college' */
    url: string
    /** 表格列 */
    columns: CrudColumn[]
    /** 表单字段（已翻译 label；select 字段通过 options 传入选项） */
    fields: SchemaField[]
    /** 弹窗标题 */
    dialogTitle: string
    rules?: FormRules | ComputedRef<FormRules>
    /** 搜索条件（声明式）；为空则不显示搜索栏 */
    search?: SearchField[]
    /** 允许进行新增/批量删除操作的角色，默认仅 ADMIN */
    manageRoles?: string[]
    /** 是否显示表格操作列，默认 true */
    showActions?: boolean
    /** 操作列宽度 */
    actionsWidth?: number
    /** 弹窗宽度 */
    dialogWidth?: string
    labelWidth?: string
    paddingRight?: string
    /** 保存前/保存后钩子 */
    beforeSave?: (form: Record<string, any>) => void | Promise<void>
    afterSave?: (form: Record<string, any>) => void | Promise<void>
    /** 单个删除确认文案 */
    deleteConfirmMessage?: string
}>(), {
    search: () => [],
    manageRoles: () => ['ADMIN'],
    showActions: true,
    actionsWidth: 180,
    dialogWidth: '40%',
    labelWidth: '100px',
    paddingRight: '50px',
})

defineOptions({ name: 'CrudPage' })

const { hasRole } = useUser()
const canManage = computed(() => props.manageRoles.length === 0 || hasRole(...props.manageRoles))

const searchForm = reactive<Record<string, any>>({})
const getParams = () => ({ ...searchForm })

const {
    tableData, pageNum, pageSize, total, loading,
    formVisible, form, rules,
    load, handleAdd, handleEdit, save, del,
    handleSelectionChange, delBatch,
} = useCrud({
    url: props.url,
    rules: props.rules,
    getParams,
    beforeSave: props.beforeSave,
    afterSave: props.afterSave,
    deleteConfirmMessage: props.deleteConfirmMessage,
})

const schemaForm = ref<any>()

/** 先做表单校验，再由 useCrud 完成新增/编辑 */
const saveWithValidate = async () => {
    const valid = await schemaForm.value?.validate?.().catch(() => false)
    if (valid === false) return
    await save()
}

const reset = () => {
    for (const k of Object.keys(searchForm)) searchForm[k] = ''
    load(1)
}

onMounted(() => load(1))
</script>

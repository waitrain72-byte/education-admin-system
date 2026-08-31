<template>
  <el-form
      ref="internalFormRef"
      :model="model"
      :label-width="labelWidth"
      :style="formStyle"
      :rules="rules"
  >
    <el-form-item v-for="field in fields" :key="field.prop" :prop="field.prop" :label="field.label">
      <!-- 普通输入框 -->
      <el-input
          v-if="!field.type || field.type === 'input'"
          v-model="model[field.prop]"
          autocomplete="off"
          :placeholder="field.placeholder"
      />
      <!-- 多行文本 -->
      <el-input
          v-else-if="field.type === 'textarea'"
          v-model="model[field.prop]"
          type="textarea"
          :rows="field.rows || 5"
          autocomplete="off"
          :placeholder="field.placeholder"
      />
      <!-- 下拉选择 -->
      <el-select
          v-else-if="field.type === 'select'"
          v-model="model[field.prop]"
          :placeholder="field.placeholder"
          style="width: 100%"
          :clearable="field.clearable !== false"
      >
        <el-option
            v-for="item in field.options || []"
            :key="item[field.optionValue || 'id']"
            :label="item[field.optionLabel || 'name']"
            :value="item[field.optionValue || 'id']"
        />
      </el-select>
      <!-- 自定义渲染：页面通过具名插槽提供整段表单项 -->
      <slot v-else-if="field.type === 'slot'" :name="field.prop" :model="model" :field="field" />
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { FormInstance } from 'element-plus'

export interface SchemaField {
    prop: string
    label: string
    type?: 'input' | 'textarea' | 'select' | 'slot'
    placeholder?: string
    rows?: number
    options?: any[]
    optionLabel?: string
    optionValue?: string
    clearable?: boolean
}

defineOptions({ name: 'SchemaForm' })

const props = withDefaults(defineProps<{
    /** 表单数据（响应式对象，子组件直接修改其字段） */
    model: Record<string, any>
    /** 字段定义数组 */
    fields: SchemaField[]
    rules?: any
    labelWidth?: string
    paddingRight?: string
}>(), {
    labelWidth: '100px',
    paddingRight: '50px',
})

const internalFormRef = ref<FormInstance>()
const formStyle = { paddingRight: props.paddingRight }

/** 供父组件在校验流程中调用 */
function validate() {
    return internalFormRef.value?.validate?.()
}

/** 校验失败时清空/复位 */
function resetFields() {
    internalFormRef.value?.resetFields?.()
}

defineExpose({ formRef: internalFormRef, validate, resetFields })
</script>

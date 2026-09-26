<template>
  <picker
    v-if="mode === 'date' || mode === 'time'"
    :mode="mode"
    :value="plainValue"
    :disabled="disabled"
    @change="onPlainChange"
  >
    <view
      class="xm-input xm-picker-field"
      :class="{ 'is-placeholder': !text }"
    >
      <text class="xm-picker-text">{{ text || placeholder }}</text>
    </view>
  </picker>
  <picker
    v-else
    :range="labels"
    :value="selectorValue"
    :disabled="disabled"
    @change="onSelect"
  >
    <view
      class="xm-input xm-picker-field"
      :class="{ 'is-placeholder': !text }"
    >
      <text class="xm-picker-text">{{ text || placeholder }}</text>
    </view>
  </picker>
</template>

<script setup>
/**
 * 表单下拉选择（easycom 自动注册），v-model 双向绑定，替代各页面手写的
 * 「值数组 + 文案数组 + 当前下标 + onXxxChange」四件套：
 *
 *   枚举：<xm-picker v-model="form.type" :options="enumOptions('courseType')" :placeholder="…" />
 *   接口列表：<xm-picker v-model="form.courseId" :options="courseData" label-key="name" value-key="id" @change="onCourse" />
 *   日期：<xm-picker v-model="form.time" mode="date" :placeholder="…" />
 *   时间：<xm-picker v-model="clock" mode="time" :placeholder="…" />（值为 HH:mm）
 *
 * - change 事件回传选中的整条选项（级联加载等副作用写在页面的 @change 里）
 * - 值为空、或不在选项里（选项未加载 / id 已失效）时显示占位文字，字色弱化
 */
import { computed } from 'vue'
import { isEmptyValue, pickerIndex, pickerText } from '@/utils/picker'

defineOptions({ options: { virtualHost: true } })

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  options: { type: Array, default: () => [] },
  labelKey: { type: String, default: 'label' },
  valueKey: { type: String, default: 'value' },
  placeholder: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  /** selector（默认）| date（值为 yyyy-MM-dd）| time（值为 HH:mm） */
  mode: { type: String, default: 'selector' },
})
const emit = defineEmits(['update:modelValue', 'change'])

const labels = computed(() => props.options.map((o) => (o == null ? '' : String(o[props.labelKey] ?? ''))))
const index = computed(() => pickerIndex(props.options, props.modelValue, props.valueKey))
// 原生 picker 打开时默认停在当前值，没有当前值时停在第一项
const selectorValue = computed(() => (index.value >= 0 ? index.value : 0))
// 日期 / 时间模式：值就是原生选择器的字符串，直接展示
const plainValue = computed(() => (isEmptyValue(props.modelValue) ? '' : String(props.modelValue)))

const text = computed(() => {
  if (props.mode === 'date' || props.mode === 'time') return plainValue.value
  return pickerText(props.options, props.modelValue, props.labelKey, props.valueKey)
})

const onSelect = (e) => {
  const option = props.options[Number(e.detail.value)]
  if (!option) return
  emit('update:modelValue', option[props.valueKey])
  emit('change', option)
}

const onPlainChange = (e) => {
  emit('update:modelValue', e.detail.value)
  emit('change', e.detail.value)
}
</script>

<style lang="scss" scoped>
/* 与原各页面的 picker 展示框一致：输入框外观 + 文字垂直居中；占位态弱化字色 */
.xm-picker-field {
  display: flex;
  align-items: center;
}

.xm-picker-field.is-placeholder {
  color: var(--xm-text-2);
}

/* 过长的选项文案单行省略，不把输入框撑高 */
.xm-picker-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
</style>

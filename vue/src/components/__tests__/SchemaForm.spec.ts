import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus, { ElDatePicker } from 'element-plus'
import SchemaForm from '@/components/SchemaForm.vue'

describe('SchemaForm datetime 字段', () => {
  it('渲染为选到分钟的日期时间选择器，选择结果按 YYYY-MM-DD HH:mm 字符串写回表单', async () => {
    const model = { examTime: '' }
    const wrapper = mount(SchemaForm, {
      props: {
        model,
        'onUpdate:model': () => {},
        fields: [{ prop: 'examTime', label: '考试时间', type: 'datetime', placeholder: '请选择考试时间' }],
      },
      global: { plugins: [ElementPlus] },
    })
    const picker = wrapper.findComponent(ElDatePicker)
    expect(picker.exists()).toBe(true)
    expect(picker.props('type')).toBe('datetime')
    expect(picker.props('format')).toBe('YYYY-MM-DD HH:mm')
    expect(picker.props('valueFormat')).toBe('YYYY-MM-DD HH:mm')
    expect(picker.props('placeholder')).toBe('请选择考试时间')

    picker.vm.$emit('update:modelValue', '2026-12-20 09:00')
    await wrapper.vm.$nextTick()
    expect(model.examTime).toBe('2026-12-20 09:00')
  })
})

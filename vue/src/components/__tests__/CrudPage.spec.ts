import { describe, it, expect, vi } from 'vitest'
import { defineComponent, h, ref } from 'vue'
import { mount } from '@vue/test-utils'

// CrudPage 的数据与权限逻辑不在本测试范围：替换成固定返回
vi.mock('@/composables/useCrud', () => ({
  useCrud: () => ({
    tableData: ref([{ id: 1, name: '期末考试', examTime: '2026-12-20 09:00' }]),
    pageNum: ref(1),
    pageSize: ref(10),
    total: ref(1),
    loading: ref(false),
    formVisible: ref(false),
    form: ref({}),
    rules: {},
    load: vi.fn(),
    handleAdd: vi.fn(),
    handleEdit: vi.fn(),
    save: vi.fn(),
    del: vi.fn(),
    handleSelectionChange: vi.fn(),
    delBatch: vi.fn(),
  }),
}))
vi.mock('@/components/useUser.ts', () => ({ useUser: () => ({ hasRole: () => true }) }))

const CrudPage = (await import('@/components/CrudPage.vue')).default

// 表格替身：每列有插槽就用插槽渲染第一行，没有就输出 default，便于断言转发结果
const CrudTableStub = defineComponent({
  name: 'CrudTable',
  props: { data: { type: Array, default: () => [] }, columns: { type: Array, default: () => [] } },
  setup(props, { slots }) {
    return () =>
      h(
        'div',
        (props.columns as any[]).map((col) =>
          h('div', { class: `cell-${col.prop}` }, slots[col.prop] ? slots[col.prop]!({ row: props.data[0] }) : 'default'),
        ),
      )
  },
})

const mountPage = (pageSlots: Record<string, any>) =>
  mount(CrudPage, {
    props: {
      url: '/examplan',
      columns: [
        { prop: 'name', label: '标题' },
        { prop: 'examTime', label: '考试时间' },
      ],
      fields: [],
      dialogTitle: '信息',
    },
    slots: pageSlots,
    global: {
      stubs: { CrudTable: CrudTableStub, SchemaForm: true, ElDialog: true, ElButton: true, ElInput: true, ElSelect: true, ElOption: true },
    },
  })

describe('CrudPage 列插槽转发', () => {
  it('页面提供与列同名的插槽时转给表格，拿到当前行数据', () => {
    const wrapper = mountPage({ examTime: (scope: any) => h('b', `考试：${scope.row.examTime}`) })
    expect(wrapper.find('.cell-examTime').text()).toBe('考试：2026-12-20 09:00')
    expect(wrapper.find('.cell-name').text()).toBe('default')
  })

  it('没有提供插槽的列保持表格默认渲染', () => {
    const wrapper = mountPage({})
    expect(wrapper.find('.cell-examTime').text()).toBe('default')
    expect(wrapper.find('.cell-name').text()).toBe('default')
  })
})

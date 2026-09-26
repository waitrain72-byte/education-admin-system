import { describe, it, expect, vi } from 'vitest'
import { defineComponent, h, ref } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import { t } from '@/i18n'

// 当前登录用户与接口调用记录：各用例切换角色，断言成绩页按角色请求课程下拉数据
const state = vi.hoisted(() => ({
  user: { id: 1, role: 'ADMIN' },
  calls: [] as { url: string; params: any }[],
}))

vi.mock('@/components/useUser.ts', () => ({ useUser: () => ({ user: ref(state.user) }) }))
// Element Plus 的命令式 API（连同样式）统一经 @/utils/element-plus 深导入，测试里替换掉
vi.mock('@/utils/element-plus', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
  ElMessageBox: { confirm: vi.fn() },
}))
vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(async (url: string, config: any = {}) => {
      state.calls.push({ url, params: config.params })
      if (url === '/course/selectAll') return [{ id: 1, name: '高等数学' }, { id: 6, name: '中国近代史纲要' }]
      // 选课记录：id 是选课记录自己的，课程 id 在 courseId
      if (url === '/choice/selectAll') return [{ id: 90, courseId: 5, name: '线性代数' }]
      if (url === '/score/selectPage') return { list: [], total: 0 }
      return []
    }),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

const Score = (await import('@/views/manager/Score.vue')).default

// 下拉替身：直接渲染选项，便于读出每项的值与文案
const ElSelectStub = defineComponent({
  name: 'ElSelect',
  setup(_, { slots }) {
    return () => h('div', slots.default?.())
  },
})
const ElOptionStub = defineComponent({
  name: 'ElOption',
  props: { label: String, value: [String, Number] },
  setup(props) {
    return () => h('i', { class: 'opt', 'data-value': props.value }, props.label)
  },
})

const mountAs = async (role: string, id: number) => {
  state.user = { id, role }
  state.calls = []
  const wrapper = mount(Score, {
    global: {
      mocks: { $t: t },
      stubs: {
        ElSelect: ElSelectStub,
        ElOption: ElOptionStub,
        ElButton: true,
        ElDialog: true,
        ElForm: true,
        ElFormItem: true,
        ElInput: true,
        CrudTable: true,
      },
    },
  })
  await flushPromises()
  return wrapper
}
const courseCalls = () => state.calls.filter((c) => c.url !== '/score/selectPage')
const searchOptions = (wrapper: any) =>
  wrapper.findAll('.search .opt').map((o: any) => ({ value: o.attributes('data-value'), label: o.text() }))

describe('成绩页课程下拉按角色取数', () => {
  it('管理员：取全部课程，不带 teacherId（三类账号 id 会重复，不能拿管理员 id 当教师 id）', async () => {
    const wrapper = await mountAs('ADMIN', 1)
    expect(courseCalls()).toEqual([{ url: '/course/selectAll', params: {} }])
    expect(searchOptions(wrapper)).toEqual([
      { value: '1', label: '高等数学' },
      { value: '6', label: '中国近代史纲要' },
    ])
  })

  it('教师：只取本人任教的课程', async () => {
    await mountAs('TEACHER', 2)
    expect(courseCalls()).toEqual([{ url: '/course/selectAll', params: { teacherId: 2 } }])
  })

  it('学生：取本人已选的课程，下拉的值是课程 id 而不是选课记录 id', async () => {
    const wrapper = await mountAs('STUDENT', 3)
    expect(courseCalls()).toEqual([{ url: '/choice/selectAll', params: {} }])
    expect(searchOptions(wrapper)).toEqual([{ value: '5', label: '线性代数' }])
  })
})

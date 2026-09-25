import { beforeEach, describe, expect, it, vi } from 'vitest'

// 记录页面生命周期回调，测试里手动触发；请求层与登录检查替换为可控的 mock
const { hooks, getMock, putMock, delMock, loginState } = vi.hoisted(() => ({
  hooks: {},
  getMock: vi.fn(),
  putMock: vi.fn(),
  delMock: vi.fn(),
  loginState: { ok: true },
}))
vi.mock('@dcloudio/uni-app', () => ({
  onShow: (fn) => (hooks.onShow = fn),
  onHide: (fn) => (hooks.onHide = fn),
  onReachBottom: (fn) => (hooks.onReachBottom = fn),
  onPullDownRefresh: (fn) => (hooks.onPullDownRefresh = fn),
}))
vi.mock('@/utils/request', () => ({ get: getMock, post: vi.fn(), put: putMock, del: delMock }))
vi.mock('@/utils/authGuard', () => ({ ensureLoggedIn: () => loginState.ok }))

const { useListPage } = await import('@/composables/useListPage')
const { useResetPassword } = await import('@/composables/useResetPassword')
const { createCrudApi, teacherApi } = await import('@/api')
const { useUserStore } = await import('@/stores/user')
const { createPinia, setActivePinia } = await import('pinia')
const { clearStorage } = await import('./setup')

const uniMock = globalThis.uni
const page = (rows, total = rows.length) => ({ list: rows, total })

beforeEach(() => {
  clearStorage()
  vi.clearAllMocks()
  vi.useRealTimers()
  loginState.ok = true
  setActivePinia(createPinia())
  useUserStore().updateUser({ id: 1, role: 'ADMIN', token: 'tk' })
  getMock.mockResolvedValue(page([{ id: 1 }]))
})

describe('useListPage 列表页骨架', () => {
  it('显示页面：设置标题并按 query 加载第一页，附属数据随首次加载一起拉取', async () => {
    const loadExtras = vi.fn()
    const p = useListPage({ api: createCrudApi('/notice'), title: 'menu.notice', query: { title: '' }, loadExtras })
    hooks.onShow()
    await vi.waitFor(() => expect(p.list.value.length).toBe(1))
    expect(uniMock.setNavigationBarTitle).toHaveBeenCalledWith({ title: '教务通知' })
    expect(getMock.mock.calls[0][0]).toBe('/notice/selectPage')
    expect(getMock.mock.calls[0][1]).toEqual({ pageNum: 1, pageSize: 10 })
    expect(loadExtras).toHaveBeenCalledTimes(1)

    // 再次显示（选图 / 预览后返回）：不重复加载，附属数据也不重复拉
    hooks.onShow()
    expect(getMock).toHaveBeenCalledTimes(1)
    expect(loadExtras).toHaveBeenCalledTimes(1)
  })

  it('query 作为请求参数；resetQuery 恢复初值并重新查询', async () => {
    const p = useListPage({ api: createCrudApi('/apply'), query: { status: '', content: '' } })
    p.query.status = '待审核'
    await p.resetQuery()
    expect(p.query).toEqual({ status: '', content: '' })
    p.query.content = '病假'
    await p.search()
    expect(getMock.mock.calls.at(-1)[1]).toEqual({ pageNum: 1, pageSize: 10, content: '病假' })
  })

  it('roles 不含当前角色：提示无权限并返回，不加载列表', () => {
    vi.useFakeTimers()
    useUserStore().updateUser({ id: 2, role: 'STUDENT', token: 'tk' })
    useListPage({ api: createCrudApi('/admin'), title: 'menu.admin', roles: ['ADMIN'] })
    hooks.onShow()
    expect(uniMock.showToast).toHaveBeenCalledWith(expect.objectContaining({ icon: 'none' }))
    vi.advanceTimersByTime(800)
    expect(uniMock.navigateBack).toHaveBeenCalled()
    expect(getMock).not.toHaveBeenCalled()
  })

  it('未登录：不加载（由登录拦截跳转）', () => {
    loginState.ok = false
    useListPage({ api: createCrudApi('/notice') })
    hooks.onShow()
    expect(getMock).not.toHaveBeenCalled()
  })

  it('批量管理：退出管理态时清空勾选；触底加载下一页', async () => {
    getMock.mockResolvedValue(page([{ id: 1 }], 25))
    const p = useListPage({ api: createCrudApi('/course') })
    hooks.onShow()
    await vi.waitFor(() => expect(p.list.value.length).toBe(1))
    p.toggleManage()
    p.toggleSelect(1)
    expect(p.selectedIds.value).toEqual([1])
    p.toggleManage()
    expect(p.selectedIds.value).toEqual([])

    hooks.onReachBottom()
    await vi.waitFor(() => expect(getMock.mock.calls.at(-1)[1]).toEqual({ pageNum: 2, pageSize: 10 }))
  })

  it('deleteConfirm：删除确认框使用页面自己的文案', async () => {
    uniMock.showModal.mockImplementationOnce((o) => o.success({ confirm: true }))
    delMock.mockResolvedValueOnce(null)
    const p = useListPage({ api: createCrudApi('/score'), deleteConfirm: 'pages.score.deleteConfirm' })
    await p.del(3)
    expect(uniMock.showModal.mock.calls[0][0].content).toContain('学分')
    expect(delMock.mock.calls[0][0]).toBe('/score/delete/3')
  })

  it('handleEdit(row, overrides)：复制整行并覆盖指定字段', () => {
    const p = useListPage({ api: createCrudApi('/apply') })
    const row = { id: 5, status: '审核通过', descr: '同意' }
    p.handleEdit(row, { status: '待审核', descr: '' })
    expect(p.form.value).toEqual({ id: 5, status: '待审核', descr: '' })
    expect(row.status).toBe('审核通过')
    expect(p.formVisible.value).toBe(true)
  })
})

describe('useResetPassword 重置密码', () => {
  it('确认后调用 PUT {url}/resetPassword/{id} 并提示；取消则不请求', async () => {
    const reset = useResetPassword(teacherApi, 'teacher')
    uniMock.showModal.mockImplementationOnce((o) => o.success({ confirm: false }))
    await reset({ id: 3, username: 'wang' })
    expect(putMock).not.toHaveBeenCalled()

    uniMock.showModal.mockImplementationOnce((o) => o.success({ confirm: true }))
    putMock.mockResolvedValueOnce(null)
    await reset({ id: 3, username: 'wang' })
    expect(uniMock.showModal.mock.calls.at(-1)[0].content).toContain('wang')
    expect(putMock.mock.calls[0][0]).toBe('/teacher/resetPassword/3')
    expect(uniMock.showToast).toHaveBeenCalled()
  })
})

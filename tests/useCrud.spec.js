import { beforeEach, describe, expect, it, vi } from 'vitest'

const { getMock, postMock, putMock, delMock } = vi.hoisted(() => ({
  getMock: vi.fn(),
  postMock: vi.fn(),
  putMock: vi.fn(),
  delMock: vi.fn(),
}))

vi.mock('@/utils/request', () => ({ get: getMock, post: postMock, put: putMock, del: delMock }))
vi.mock('@dcloudio/uni-app', () => ({ onPullDownRefresh: vi.fn() }))

const { useCrud } = await import('@/composables/useCrud')
const { useManage } = await import('@/composables/useManage')
const { clearStorage } = await import('./setup')

const uniMock = globalThis.uni

const page = (rows, total) => ({ data: { code: '200', data: { list: rows, total } } })

beforeEach(() => {
  clearStorage()
  vi.clearAllMocks()
})

describe('useCrud 通用 CRUD', () => {
  it('load(true)：重置第一页、注入跨页行号 _index', async () => {
    getMock.mockResolvedValueOnce(page([{ id: 11 }, { id: 12 }], 15))
    const crud = useCrud({ url: '/thing' })

    await crud.load(true)

    expect(crud.list.value.length).toBe(2)
    expect(crud.list.value[0]._index).toBe(1)
    expect(crud.list.value[1]._index).toBe(2)
    expect(getMock.mock.calls[0][0]).toBe('/thing/selectPage')
    expect(getMock.mock.calls[0][1]).toEqual({ pageNum: 1, pageSize: 10 })
    // 首屏（重置且非下拉）走统一加载动画
    expect(getMock.mock.calls[0][2]).toEqual({ loading: true })
  })

  it('loadNext：追加分页并按 id 去重，静默加载', async () => {
    getMock.mockResolvedValueOnce(page([{ id: 1 }, { id: 2 }], 5))
    const crud = useCrud({ url: '/thing' })
    await crud.load(true)

    getMock.mockResolvedValueOnce(page([{ id: 2 }, { id: 3 }], 5))
    crud.loadNext()
    await vi.waitFor(() => expect(crud.list.value.length).toBe(3))

    expect(crud.list.value.map((r) => r.id)).toEqual([1, 2, 3])
    expect(getMock.mock.calls[1][1]).toEqual({ pageNum: 2, pageSize: 10 })
    expect(getMock.mock.calls[1][2]).toEqual({ loading: false })
  })

  it('空过滤条件不传给后端', async () => {
    getMock.mockResolvedValueOnce(page([], 0))
    const crud = useCrud({ url: '/thing', getParams: () => ({ name: '', age: null, flag: undefined, ok: 'y' }) })

    await crud.load(true)

    expect(getMock.mock.calls[0][1]).toEqual({ pageNum: 1, pageSize: 10, ok: 'y' })
  })

  it('save：校验失败时提示且不发请求', async () => {
    const crud = useCrud({ url: '/thing', validate: () => '名称必填' })
    await crud.save()
    expect(uniMock.showToast).toHaveBeenCalledWith(expect.objectContaining({ title: '名称必填' }))
    expect(postMock).not.toHaveBeenCalled()
  })

  it('save：防重复提交——保存进行中忽略再次调用', async () => {
    let resolvePut
    putMock.mockReturnValue(new Promise((resolve) => (resolvePut = resolve)))
    const crud = useCrud({ url: '/thing' })
    crud.form.value = { id: 5, name: 'x' }

    const first = crud.save()
    const second = crud.save()
    resolvePut({ data: { code: '200' } })
    await Promise.all([first, second])

    expect(putMock).toHaveBeenCalledTimes(1)
    expect(crud.saving.value).toBe(false)
  })

  it('save：成功后关表单并刷新列表', async () => {
    putMock.mockResolvedValueOnce({ data: { code: '200' } })
    getMock.mockResolvedValue(page([], 0))
    const crud = useCrud({ url: '/thing' })
    crud.form.value = { id: 5, name: 'x' }
    crud.formVisible.value = true

    await crud.save()

    expect(crud.formVisible.value).toBe(false)
    expect(getMock).toHaveBeenCalled()
  })

  it('del：确认后调用删除接口并刷新', async () => {
    delMock.mockResolvedValueOnce({ data: { code: '200' } })
    getMock.mockResolvedValue(page([], 0))
    uniMock.showModal.mockImplementationOnce((o) => o.success({ confirm: true }))
    const crud = useCrud({ url: '/thing' })

    crud.del(9)
    await vi.waitFor(() => expect(delMock).toHaveBeenCalledWith('/thing/delete/9'))
    await vi.waitFor(() => expect(getMock).toHaveBeenCalled())
  })

  it('del：取消确认不发请求', async () => {
    uniMock.showModal.mockImplementationOnce((o) => o.success({ confirm: false }))
    const crud = useCrud({ url: '/thing' })

    crud.del(9)
    await new Promise((r) => setTimeout(r, 0))
    expect(delMock).not.toHaveBeenCalled()
  })
})

describe('useManage 批量管理', () => {
  it('勾选切换与退出清空', () => {
    const selectedIds = { value: [] }
    const { manageMode, toggleManage, toggleSelect } = useManage(selectedIds)

    toggleSelect(1)
    toggleSelect(2)
    toggleSelect(1)
    expect(selectedIds.value).toEqual([2])

    toggleManage()
    expect(manageMode.value).toBe(true)
    toggleSelect(3)
    expect(selectedIds.value).toEqual([2, 3])

    toggleManage()
    expect(manageMode.value).toBe(false)
    expect(selectedIds.value).toEqual([])
  })
})

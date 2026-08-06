import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useCrud } from '@/composables/useCrud'

// request 既作为函数调用（save），也作为对象调用方法（load/del），mock 需同时支持两种形态
const { requestMock } = vi.hoisted(() => {
  const req: any = vi.fn()
  req.get = vi.fn()
  req.post = vi.fn()
  req.put = vi.fn()
  req.delete = vi.fn()
  return { requestMock: req }
})

vi.mock('@/utils/request', () => ({ default: requestMock }))

vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
  ElMessageBox: { confirm: vi.fn(() => Promise.resolve('confirm')) },
}))

const okResponse = (data: any) => ({ data: { code: '200', msg: '成功', data } })

describe('useCrud', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // 默认分页接口返回空列表，各用例可用 mockResolvedValueOnce 覆盖
    requestMock.get.mockResolvedValue(okResponse({ list: [], total: 0 }))
  })

  it('load 请求分页接口并填充列表/总数', async () => {
    const { load, tableData, total, pageNum, loading } = useCrud({
      url: '/college',
      getParams: () => ({ name: 'a' }),
    })
    requestMock.get.mockResolvedValueOnce(okResponse({ list: [{ id: 1 }], total: 10 }))
    await load(2)
    expect(pageNum.value).toBe(2)
    expect(tableData.value).toEqual([{ id: 1 }])
    expect(total.value).toBe(10)
    expect(loading.value).toBe(false)
    expect(requestMock.get).toHaveBeenCalledWith('/college/selectPage', {
      params: { pageNum: 2, pageSize: 10, name: 'a' },
    })
  })

  it('load 请求失败时保持空列表且不抛异常', async () => {
    const { load, tableData, total } = useCrud({ url: '/college' })
    requestMock.get.mockRejectedValueOnce(new Error('network'))
    await load(1)
    expect(tableData.value).toEqual([])
    expect(total.value).toBe(0)
  })

  it('空字符串/null 的查询条件不传给后端，避免被当成有效过滤值', async () => {
    const { load } = useCrud({
      url: '/roomplan',
      getParams: () => ({ name: '', status: '空闲', content: null }),
    })
    requestMock.get.mockResolvedValueOnce(okResponse({ list: [{ id: 1 }], total: 1 }))
    await load(1)
    expect(requestMock.get).toHaveBeenCalledWith('/roomplan/selectPage', {
      params: { pageNum: 1, pageSize: 10, status: '空闲' },
    })
  })

  it('save 新增时走 POST /add', async () => {
    const { save, form, formRef } = useCrud({ url: '/college' })
    formRef.value = { validate: vi.fn().mockResolvedValue(true) } as any
    form.value = { name: 'x' }
    requestMock.mockResolvedValueOnce(okResponse(null))
    await save()
    expect(requestMock).toHaveBeenCalledWith({ url: '/college/add', method: 'POST', data: { name: 'x' } })
  })

  it('save 编辑时走 PUT /update', async () => {
    const { save, form, formRef } = useCrud({ url: '/college' })
    formRef.value = { validate: vi.fn().mockResolvedValue(true) } as any
    form.value = { id: 3, name: 'x' }
    requestMock.mockResolvedValueOnce(okResponse(null))
    await save()
    expect(requestMock).toHaveBeenCalledWith({ url: '/college/update', method: 'PUT', data: { id: 3, name: 'x' } })
  })

  it('save 校验失败时不发请求', async () => {
    const { save, form, formRef } = useCrud({ url: '/college' })
    formRef.value = { validate: vi.fn().mockRejectedValue({}) } as any
    form.value = { name: '' }
    await save()
    expect(requestMock).not.toHaveBeenCalled()
  })

  it('beforeSave 钩子在提交前生效', async () => {
    const { save, form, formRef } = useCrud({
      url: '/score',
      beforeSave: (formData) => {
        formData.studentId = 7
      },
    })
    formRef.value = { validate: vi.fn().mockResolvedValue(true) } as any
    form.value = { id: 1, score: 90 }
    requestMock.mockResolvedValueOnce(okResponse(null))
    await save()
    expect(requestMock).toHaveBeenCalledWith({
      url: '/score/update',
      method: 'PUT',
      data: { id: 1, score: 90, studentId: 7 },
    })
  })

  it('del 确认后调用 DELETE 并刷新列表', async () => {
    const { del, tableData } = useCrud({ url: '/college' })
    requestMock.get.mockResolvedValueOnce(okResponse({ list: [{ id: 2 }], total: 1 }))
    requestMock.delete.mockResolvedValueOnce(okResponse(null))
    del(1)
    await vi.waitFor(() => {
      expect(requestMock.delete).toHaveBeenCalledWith('/college/delete/1')
      expect(tableData.value).toEqual([{ id: 2 }])
    })
  })

  it('delBatch 使用选中的 id 列表批量删除', async () => {
    const { delBatch, handleSelectionChange, tableData } = useCrud({ url: '/college' })
    handleSelectionChange([{ id: 1 }, { id: 2 }])
    requestMock.get.mockResolvedValueOnce(okResponse({ list: [], total: 0 }))
    requestMock.delete.mockResolvedValueOnce(okResponse(null))
    delBatch()
    await vi.waitFor(() => {
      expect(requestMock.delete).toHaveBeenCalledWith('/college/delete/batch', { data: [1, 2] })
      expect(tableData.value).toEqual([])
    })
  })
})

import { beforeEach, describe, expect, it, vi } from 'vitest'
import { clearStorage } from './setup'

const { chooseImage, pickAttachment, pickAttachments, uploadFile, MAX_UPLOAD_MB, DOC_EXTENSIONS } =
  await import('@/utils/upload')
const { ApiError } = await import('@/utils/request')
const { useUserStore } = await import('@/stores/user')
const { createPinia, setActivePinia } = await import('pinia')

const uniMock = globalThis.uni

/** 让 uni.uploadFile 以给定响应走 success 回调 */
const respond = (res) =>
  uniMock.uploadFile.mockImplementationOnce((o) => {
    o.success(res)
    o.complete && o.complete()
    return { onProgressUpdate: vi.fn() }
  })

beforeEach(() => {
  clearStorage()
  vi.clearAllMocks()
  setActivePinia(createPinia())
  useUserStore().updateUser({ id: 1, role: 'STUDENT', token: 'tk' })
})

describe('uploadFile 上传', () => {
  it('200：resolve 文件地址，并携带 token 头', async () => {
    respond({ statusCode: 200, data: JSON.stringify({ code: '200', data: '/files/abc.pdf' }) })
    await expect(uploadFile('wxfile://tmp.pdf')).resolves.toBe('/files/abc.pdf')
    const opts = uniMock.uploadFile.mock.calls[0][0]
    expect(opts.url).toMatch(/\/files\/upload$/)
    expect(opts.name).toBe('file')
    expect(opts.header).toEqual({ token: 'tk' })
  })

  it('业务失败：提示后端文案并 reject ApiError', async () => {
    respond({ statusCode: 200, data: JSON.stringify({ code: '400', msg: '不支持的文件类型' }) })
    const err = await uploadFile('x.exe').catch((e) => e)
    expect(err).toBeInstanceOf(ApiError)
    expect(uniMock.showToast).toHaveBeenCalled()
  })

  it('401：清空登录态并回登录页', async () => {
    respond({ statusCode: 200, data: { code: '401', msg: '登录已过期' } })
    await expect(uploadFile('a.png')).rejects.toThrow('401')
    expect(useUserStore().token).toBe('')
    expect(uniMock.reLaunch).toHaveBeenCalledWith({ url: '/pages/login/login' })
  })

  it('非 JSON 响应（如 413 网关页）：提示上传失败', async () => {
    respond({ statusCode: 413, data: '<html>Request Entity Too Large</html>' })
    await expect(uploadFile('big.zip')).rejects.toThrow('HTTP 413')
    expect(uniMock.showToast.mock.calls[0][0].title).toBe('上传失败，请重试')
  })

  it('进度回调：透传 0~100 的百分比', async () => {
    const onProgressUpdate = vi.fn((cb) => cb({ progress: 42 }))
    uniMock.uploadFile.mockImplementationOnce((o) => {
      setTimeout(() => o.success({ statusCode: 200, data: '{"code":"200","data":"/files/a.png"}' }), 0)
      return { onProgressUpdate }
    })
    const onProgress = vi.fn()
    await uploadFile('a.png', { loading: false, onProgress })
    expect(onProgress).toHaveBeenCalledWith(42)
  })
})

describe('chooseImage 选择图片', () => {
  it('超过大小上限：提示并返回 null，不进入上传', async () => {
    uniMock.chooseImage.mockImplementationOnce((o) =>
      o.success({ tempFiles: [{ path: 'big.png', size: (MAX_UPLOAD_MB + 1) * 1024 * 1024 }] }),
    )
    await expect(chooseImage()).resolves.toBeNull()
    expect(uniMock.showToast.mock.calls[0][0].title).toContain(String(MAX_UPLOAD_MB))
  })

  it('用户取消：静默返回 null', async () => {
    uniMock.chooseImage.mockImplementationOnce((o) => o.fail({ errMsg: 'chooseImage:fail cancel' }))
    await expect(chooseImage()).resolves.toBeNull()
    expect(uniMock.showToast).not.toHaveBeenCalled()
  })

  it('相册图片没有原文件名：取临时路径的文件名', async () => {
    uniMock.chooseImage.mockImplementationOnce((o) =>
      o.success({ tempFiles: [{ path: 'wxfile://tmp/abc.png', size: 10 }] }),
    )
    await expect(chooseImage()).resolves.toEqual({ path: 'wxfile://tmp/abc.png', size: 10, name: 'abc.png' })
  })
})

describe('pickAttachments 选附件', () => {
  it('聊天文件：按后端白名单限制扩展名，带回原文件名', async () => {
    uniMock.chooseMessageFile = vi.fn((o) =>
      o.success({ tempFiles: [{ path: 'wxfile://tmp_3f2a.pdf', size: 1024, name: '第6章 集合框架.pdf' }] }),
    )
    uniMock.showActionSheet = vi.fn((o) => o.success({ tapIndex: 0 }))
    await expect(pickAttachment()).resolves.toEqual({
      path: 'wxfile://tmp_3f2a.pdf',
      size: 1024,
      name: '第6章 集合框架.pdf',
    })
    expect(uniMock.chooseMessageFile.mock.calls[0][0].extension).toEqual(DOC_EXTENSIONS)
    delete uniMock.chooseMessageFile
  })

  it('只有相册一个来源时直接打开，不弹菜单', async () => {
    uniMock.chooseImage.mockImplementationOnce((o) => o.success({ tempFiles: [{ path: 'a.png', size: 1 }] }))
    uniMock.showActionSheet = vi.fn()
    await expect(pickAttachments(3)).resolves.toHaveLength(1)
    expect(uniMock.showActionSheet).not.toHaveBeenCalled()
    expect(uniMock.chooseImage.mock.calls[0][0].count).toBe(3)
  })

  it('微信小程序：菜单里有聊天文件和相册；多选时超限的剔除，其余照常返回', async () => {
    uniMock.chooseMessageFile = vi.fn((o) =>
      o.success({
        tempFiles: [
          { path: 'a.pdf', size: 1, name: 'a.pdf' },
          { path: 'big.zip', size: (MAX_UPLOAD_MB + 1) * 1024 * 1024, name: 'big.zip' },
        ],
      }),
    )
    uniMock.showActionSheet = vi.fn((o) => o.success({ tapIndex: 0 }))
    const files = await pickAttachments(9)
    expect(uniMock.showActionSheet.mock.calls[0][0].itemList).toHaveLength(2)
    expect(files.map((f) => f.name)).toEqual(['a.pdf'])
    delete uniMock.chooseMessageFile
  })

  it('关掉菜单：返回空数组', async () => {
    uniMock.chooseMessageFile = vi.fn()
    uniMock.showActionSheet = vi.fn((o) => o.fail({ errMsg: 'showActionSheet:fail cancel' }))
    await expect(pickAttachments()).resolves.toEqual([])
    expect(uniMock.chooseMessageFile).not.toHaveBeenCalled()
    delete uniMock.chooseMessageFile
  })
})

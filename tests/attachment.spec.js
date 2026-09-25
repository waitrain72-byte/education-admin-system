import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fileNameOf, isImageFile, openAttachment } from '@/utils/attachment'
import { resolveFileUrl } from '@/utils/request'

const uniMock = globalThis.uni

beforeEach(() => {
  vi.clearAllMocks()
  uniMock.previewImage = vi.fn()
  uniMock.downloadFile = vi.fn()
  uniMock.openDocument = vi.fn()
  uniMock.setClipboardData = vi.fn()
})

describe('fileNameOf 附件文件名', () => {
  it('取路径最后一段，去掉查询串，中文名解码', () => {
    expect(fileNameOf('/files/abc.png')).toBe('abc.png')
    expect(fileNameOf('http://192.168.1.5:9091/files/r.pdf?t=1')).toBe('r.pdf')
    expect(fileNameOf('/files/%E5%AE%9E%E9%AA%8C%E6%8A%A5%E5%91%8A.docx')).toBe('实验报告.docx')
  })

  it('空值为空串；非法转义原样返回；取不到文件名时回退整个地址', () => {
    expect(fileNameOf('')).toBe('')
    expect(fileNameOf(null)).toBe('')
    expect(fileNameOf('/files/100%.pdf')).toBe('100%.pdf')
    expect(fileNameOf('/files/')).toBe('/files/')
  })
})

describe('isImageFile 图片附件判断', () => {
  it('按扩展名判断（不区分大小写，可带查询串）', () => {
    expect(isImageFile('/files/a.PNG')).toBe(true)
    expect(isImageFile('/files/a.jpeg?x=1')).toBe(true)
    expect(isImageFile('/files/a.pdf')).toBe(false)
    expect(isImageFile('')).toBe(false)
  })
})

describe('openAttachment 查看附件', () => {
  it('图片：用归一后的完整地址直接预览', () => {
    openAttachment('/files/a.png')
    expect(uniMock.previewImage).toHaveBeenCalledWith({ urls: [resolveFileUrl('/files/a.png')] })
    expect(uniMock.downloadFile).not.toHaveBeenCalled()
  })

  it('文档：下载成功后用系统文档打开', () => {
    uniMock.downloadFile.mockImplementationOnce((o) => o.success({ statusCode: 200, tempFilePath: 'tmp/r.pdf' }))
    openAttachment('/files/r.pdf')
    expect(uniMock.downloadFile.mock.calls[0][0].url).toBe(resolveFileUrl('/files/r.pdf'))
    expect(uniMock.openDocument).toHaveBeenCalledWith(
      expect.objectContaining({ filePath: 'tmp/r.pdf', showMenu: true }),
    )
    expect(uniMock.setClipboardData).not.toHaveBeenCalled()
  })

  it('下载失败、非 200、打开失败时都改为复制链接', () => {
    const url = resolveFileUrl('/files/r.pdf')
    uniMock.downloadFile.mockImplementationOnce((o) => o.fail({}))
    openAttachment('/files/r.pdf')
    uniMock.downloadFile.mockImplementationOnce((o) => o.success({ statusCode: 404 }))
    openAttachment('/files/r.pdf')
    uniMock.downloadFile.mockImplementationOnce((o) => o.success({ statusCode: 200, tempFilePath: 'tmp/r.pdf' }))
    uniMock.openDocument.mockImplementationOnce((o) => o.fail({}))
    openAttachment('/files/r.pdf')
    expect(uniMock.setClipboardData.mock.calls).toEqual([[{ data: url }], [{ data: url }], [{ data: url }]])
  })

  it('空地址不做任何事', () => {
    openAttachment('')
    expect(uniMock.previewImage).not.toHaveBeenCalled()
    expect(uniMock.downloadFile).not.toHaveBeenCalled()
  })
})

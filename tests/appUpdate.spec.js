import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { checkAppUpdate } from '@/utils/appUpdate'

const uniMock = globalThis.uni

// 模拟微信的 UpdateManager：记下回调，测试里手动触发「下载完成 / 下载失败」
const createManager = () => {
  const hooks = {}
  return {
    hooks,
    onUpdateReady: (fn) => (hooks.ready = fn),
    onUpdateFailed: (fn) => (hooks.failed = fn),
    applyUpdate: vi.fn(),
  }
}

beforeEach(() => vi.clearAllMocks())
afterEach(() => {
  delete uniMock.getUpdateManager
})

describe('checkAppUpdate 新版本提示', () => {
  it('不支持 getUpdateManager 的平台（H5）直接跳过', () => {
    expect(() => checkAppUpdate()).not.toThrow()
  })

  it('新版本下载完成：用户确认后重启应用新版本，取消则不重启', async () => {
    const manager = createManager()
    uniMock.getUpdateManager = () => manager
    checkAppUpdate()

    uniMock.showModal.mockImplementationOnce((o) => o.success({ confirm: false }))
    await manager.hooks.ready()
    expect(manager.applyUpdate).not.toHaveBeenCalled()

    uniMock.showModal.mockImplementationOnce((o) => o.success({ confirm: true }))
    await manager.hooks.ready()
    expect(manager.applyUpdate).toHaveBeenCalledTimes(1)
    expect(uniMock.showModal.mock.calls.at(-1)[0].title).toBe('更新提示')
  })

  it('新版本下载失败：只提示，不提供取消', () => {
    const manager = createManager()
    uniMock.getUpdateManager = () => manager
    checkAppUpdate()
    manager.hooks.failed()
    expect(uniMock.showModal).toHaveBeenCalledWith(expect.objectContaining({ showCancel: false }))
  })
})

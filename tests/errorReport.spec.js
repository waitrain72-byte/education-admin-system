import { beforeEach, describe, expect, it, vi } from 'vitest'

const { reportError, __resetErrorReport } = await import('@/utils/errorReport')
const uniMock = globalThis.uni

const logError = vi.fn()

beforeEach(() => {
  vi.clearAllMocks()
  __resetErrorReport()
  uniMock.getRealtimeLogManager.mockReturnValue({ error: logError })
  vi.spyOn(console, 'error').mockImplementation(() => {})
})

describe('reportError 全局错误上报', () => {
  it('未预期的异常：写入微信实时日志并打印控制台', () => {
    expect(reportError('runtime', new Error('boom'))).toBe(true)
    expect(logError).toHaveBeenCalledTimes(1)
    expect(logError.mock.calls[0][0]).toBe('[runtime]')
    expect(logError.mock.calls[0][1]).toContain('boom')
  })

  it('请求层的业务错误 ApiError 与 401 属于预期流程，不上报', () => {
    const apiError = Object.assign(new Error('课程已满'), { name: 'ApiError' })
    expect(reportError('unhandledRejection', apiError)).toBe(false)
    expect(reportError('unhandledRejection', new Error('401'))).toBe(false)
    expect(logError).not.toHaveBeenCalled()
  })

  it('同一条错误短时间内只上报一次，避免循环报错刷爆日志配额', () => {
    reportError('runtime', new Error('loop'))
    reportError('runtime', new Error('loop'))
    reportError('runtime', new Error('loop'))
    expect(logError).toHaveBeenCalledTimes(1)
  })

  it('不同错误各自上报', () => {
    reportError('runtime', new Error('a'))
    reportError('runtime', new Error('b'))
    expect(logError).toHaveBeenCalledTimes(2)
  })

  it('实时日志不可用的平台：回退控制台，不抛异常', () => {
    __resetErrorReport()
    uniMock.getRealtimeLogManager.mockImplementation(() => {
      throw new Error('not supported')
    })
    expect(() => reportError('runtime', 'plain string')).not.toThrow()
    expect(console.error).toHaveBeenCalled()
  })
})

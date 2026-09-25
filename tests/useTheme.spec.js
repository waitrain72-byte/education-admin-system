import { afterEach, describe, expect, it } from 'vitest'

const uniMock = globalThis.uni
const { readSystemDark } = await import('@/composables/useTheme')

afterEach(() => {
  delete uniMock.getAppBaseInfo
  delete uniMock.getSystemInfoSync
})

describe('readSystemDark 冷启动读取系统深浅色', () => {
  it('优先用 getAppBaseInfo 的 theme', () => {
    uniMock.getAppBaseInfo = () => ({ theme: 'dark' })
    expect(readSystemDark()).toBe(true)
    uniMock.getAppBaseInfo = () => ({ theme: 'light' })
    expect(readSystemDark()).toBe(false)
  })

  it('没有 getAppBaseInfo 的旧基础库回退 getSystemInfoSync', () => {
    uniMock.getSystemInfoSync = () => ({ theme: 'dark' })
    expect(readSystemDark()).toBe(true)
  })

  it('未开启 darkmode（theme 为 undefined）或接口抛错时按浅色处理', () => {
    uniMock.getAppBaseInfo = () => ({})
    expect(readSystemDark()).toBe(false)
    uniMock.getAppBaseInfo = () => {
      throw new Error('not supported')
    }
    expect(readSystemDark()).toBe(false)
  })
})

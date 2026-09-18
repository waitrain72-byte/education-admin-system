import { vi } from 'vitest'

/**
 * uni 全局 mock：以 Map 模拟同步存储，其余 API 只记录调用。
 * setupFiles 在测试模块加载前执行，保证 i18n 等模块顶层读取 storage 时不报错。
 */
const storage = new Map()

export const uniMock = {
  getStorageSync: vi.fn((k) => (storage.has(k) ? storage.get(k) : '')),
  setStorageSync: vi.fn((k, v) => storage.set(k, v)),
  removeStorageSync: vi.fn((k) => storage.delete(k)),
  showToast: vi.fn(),
  showLoading: vi.fn(),
  hideLoading: vi.fn(),
  showModal: vi.fn(),
  request: vi.fn(),
  uploadFile: vi.fn(),
  chooseImage: vi.fn(),
  getNetworkType: vi.fn(),
  reLaunch: vi.fn(),
  navigateBack: vi.fn(),
  stopPullDownRefresh: vi.fn(),
  setNavigationBarTitle: vi.fn(),
  setTabBarBadge: vi.fn(),
  removeTabBarBadge: vi.fn(),
  $emit: vi.fn(),
  $on: vi.fn(),
  $off: vi.fn(),
  onNetworkStatusChange: vi.fn(),
  connectSocket: vi.fn(),
  closeSocket: vi.fn(),
  sendSocketMessage: vi.fn(),
  onSocketOpen: vi.fn(),
  onSocketMessage: vi.fn(),
  onSocketClose: vi.fn(),
  onSocketError: vi.fn(),
}

globalThis.uni = uniMock

/** 清空模拟存储（各测试文件的 beforeEach 调用） */
export function clearStorage() {
  storage.clear()
}

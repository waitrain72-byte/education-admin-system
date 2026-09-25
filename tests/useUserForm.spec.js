import { beforeEach, describe, expect, it, vi } from 'vitest'

const { chooseImageMock, uploadFileMock } = vi.hoisted(() => ({ chooseImageMock: vi.fn(), uploadFileMock: vi.fn() }))
vi.mock('@/utils/upload', () => ({ chooseImage: chooseImageMock, uploadFile: uploadFileMock }))

const { useAvatarUpload, syncCurrentUser } = await import('@/composables/useUserForm')
const { useUserStore } = await import('@/stores/user')
const { createPinia, setActivePinia } = await import('pinia')
const { clearStorage } = await import('./setup')

beforeEach(() => {
  clearStorage()
  vi.clearAllMocks()
  setActivePinia(createPinia())
  useUserStore().updateUser({ id: 1, role: 'ADMIN', token: 'tk', name: '管理员', avatar: '/files/a.png' })
})

describe('syncCurrentUser 保存后同步当前账号', () => {
  it('同角色同 id：同步指定字段', () => {
    syncCurrentUser('ADMIN', ['name', 'avatar'])({ id: 1, name: '新名字', avatar: '/files/b.png', phone: '1' })
    const user = useUserStore().user
    expect(user.name).toBe('新名字')
    expect(user.avatar).toBe('/files/b.png')
    expect(user.phone).toBeUndefined()
  })

  it('不同角色即使 id 相同也不同步（1 号教师不是 1 号管理员）', () => {
    syncCurrentUser('TEACHER', ['name', 'avatar'])({ id: 1, name: '王老师', avatar: '/files/t.png' })
    expect(useUserStore().user.name).toBe('管理员')
  })

  it('同角色不同 id：不同步', () => {
    syncCurrentUser('ADMIN', ['name'])({ id: 2, name: '别人' })
    expect(useUserStore().user.name).toBe('管理员')
  })
})

describe('useAvatarUpload 头像上传', () => {
  it('选图上传成功后写回 form.avatar；取消选图不上传', async () => {
    const form = { value: {} }
    const upload = useAvatarUpload(form)
    chooseImageMock.mockResolvedValueOnce(null)
    await upload()
    expect(uploadFileMock).not.toHaveBeenCalled()

    chooseImageMock.mockResolvedValueOnce({ path: 'tmp.png', size: 10 })
    uploadFileMock.mockResolvedValueOnce('/files/x.png')
    await upload()
    expect(form.value.avatar).toBe('/files/x.png')
  })
})

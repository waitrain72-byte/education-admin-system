import { beforeEach, describe, expect, it, vi } from 'vitest'

// 请求层替换为可控的 mock：只验证接口层拼出的方法 / 路径 / 参数 / 请求选项
const req = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  del: vi.fn(),
  saveCookie: vi.fn(),
  clearCookie: vi.fn(),
}))
vi.mock('@/utils/request', () => req)

const api = await import('@/api')
const uniMock = globalThis.uni
const SILENT = { loading: false }

beforeEach(() => vi.clearAllMocks())

describe('createCrudApi：与后端 CrudController 的 7 个标准接口一一对应', () => {
  const crud = api.createCrudApi('/thing')

  it('查询类走 GET，参数与请求选项原样透传', () => {
    crud.selectPage({ pageNum: 1, pageSize: 10, name: 'a' }, SILENT)
    crud.selectAll({ teacherId: 3 })
    crud.selectById(7, SILENT)
    expect(req.get.mock.calls).toEqual([
      ['/thing/selectPage', { pageNum: 1, pageSize: 10, name: 'a' }, SILENT],
      ['/thing/selectAll', { teacherId: 3 }, undefined],
      ['/thing/selectById/7', undefined, SILENT],
    ])
  })

  it('新增 POST、修改 PUT、删除 DELETE（批量删除请求体为 id 数组）', () => {
    crud.add({ name: 'x' })
    crud.update({ id: 1, name: 'y' })
    crud.delete(5)
    crud.deleteBatch([1, 2])
    expect(req.post).toHaveBeenCalledWith('/thing/add', { name: 'x' }, undefined)
    expect(req.put).toHaveBeenCalledWith('/thing/update', { id: 1, name: 'y' }, undefined)
    expect(req.del.mock.calls).toEqual([
      ['/thing/delete/5', undefined, undefined],
      ['/thing/delete/batch', [1, 2], undefined],
    ])
  })
})

describe('业务模块：标准接口 + 各 Controller 的额外接口', () => {
  it('16 个业务模块都具备标准接口', () => {
    const modules = [
      'noticeApi',
      'examplanApi',
      'roomplanApi',
      'collegeApi',
      'specialityApi',
      'classesApi',
      'courseApi',
      'choiceApi',
      'scoreApi',
      'commentApi',
      'applyApi',
      'homeworkApi',
      'attendanceApi',
      'adminApi',
      'teacherApi',
      'studentApi',
    ]
    for (const name of modules) {
      for (const fn of ['selectPage', 'selectAll', 'selectById', 'add', 'update', 'delete', 'deleteBatch']) {
        expect(typeof api[name][fn], `${name}.${fn}`).toBe('function')
      }
    }
    api.courseApi.selectPage({ pageNum: 1 })
    expect(req.get).toHaveBeenCalledWith('/course/selectPage', { pageNum: 1 }, undefined)
  })

  it('额外接口的路径与参数', () => {
    api.courseApi.roomFree({ week: '星期一', segment: 's1' }, SILENT)
    api.courseApi.recommend({ limit: 4 }, SILENT)
    api.choiceApi.getCurriculum(SILENT)
    api.attendanceApi.getPie(SILENT)
    api.scoreApi.getLine(SILENT)
    expect(req.get.mock.calls).toEqual([
      ['/course/roomFree', { week: '星期一', segment: 's1' }, SILENT],
      ['/course/recommend', { limit: 4 }, SILENT],
      ['/choice/getCurriculum', undefined, SILENT],
      ['/attendance/getPie', undefined, SILENT],
      ['/score/getLine', undefined, SILENT],
    ])
  })

  it('三类账号：重置密码走各自的表；userApiOf 按角色取接口', () => {
    api.adminApi.resetPassword(1)
    api.teacherApi.resetPassword(2)
    api.studentApi.resetPassword(3)
    expect(req.put.mock.calls.map((c) => c[0])).toEqual([
      '/admin/resetPassword/1',
      '/teacher/resetPassword/2',
      '/student/resetPassword/3',
    ])
    expect(api.userApiOf('ADMIN')).toBe(api.adminApi)
    expect(api.userApiOf('TEACHER')).toBe(api.teacherApi)
    expect(api.userApiOf('STUDENT')).toBe(api.studentApi)
    expect(api.userApiOf('GUEST')).toBeNull()
  })
})

describe('accountApi：账号与偏好', () => {
  const { accountApi } = api

  it('登录 / 注册 / 改密 / 权限码的路径与请求体', () => {
    accountApi.login({ username: 'a' })
    accountApi.register({ username: 'b', role: 'STUDENT' })
    accountApi.updatePassword({ password: 'o', newPassword: 'n' })
    accountApi.getPermissions(SILENT)
    expect(req.post.mock.calls).toEqual([
      ['/login', { username: 'a' }, undefined],
      ['/register', { username: 'b', role: 'STUDENT' }, undefined],
    ])
    expect(req.put).toHaveBeenCalledWith('/updatePassword', { password: 'o', newPassword: 'n' }, undefined)
    expect(req.get).toHaveBeenCalledWith('/permission/my', undefined, SILENT)
  })

  it('偏好读取带请求选项，保存时包成后端要求的字段', () => {
    accountApi.getLocale(SILENT)
    accountApi.getTheme(SILENT)
    accountApi.getThemeColor(SILENT)
    accountApi.updateLocale('en-US', SILENT)
    accountApi.updateTheme('dark', SILENT)
    accountApi.updateThemeColor('#16a34a', SILENT)
    expect(req.get.mock.calls.map((c) => c[0])).toEqual(['/locale', '/theme', '/themeColor'])
    expect(req.put.mock.calls).toEqual([
      ['/locale', { locale: 'en-US' }, SILENT],
      ['/theme', { theme: 'dark' }, SILENT],
      ['/themeColor', { themeColor: '#16a34a' }, SILENT],
    ])
  })

  it('验证码：先清旧会话 Cookie，取二进制图片后保存新 Cookie，返回 data URL', async () => {
    const res = { data: new ArrayBuffer(3), header: { 'Set-Cookie': 'JSESSIONID=1' } }
    uniMock.request.mockImplementationOnce((o) => o.success(res))
    uniMock.arrayBufferToBase64 = vi.fn(() => 'R0lG')
    const url = await accountApi.captcha()
    expect(req.clearCookie).toHaveBeenCalledTimes(1)
    const opts = uniMock.request.mock.calls[0][0]
    expect(opts.url).toMatch(/\/captcha\?t=\d+$/)
    expect(opts.responseType).toBe('arraybuffer')
    expect(req.saveCookie).toHaveBeenCalledWith(res)
    expect(url).toBe('data:image/gif;base64,R0lG')
    expect(req.clearCookie.mock.invocationCallOrder[0]).toBeLessThan(uniMock.request.mock.invocationCallOrder[0])
  })

  it('验证码取图失败时 reject（由页面提示「验证码加载失败」）', async () => {
    uniMock.request.mockImplementationOnce((o) => o.fail({ errMsg: 'request:fail' }))
    await expect(accountApi.captcha()).rejects.toEqual({ errMsg: 'request:fail' })
    expect(req.saveCookie).not.toHaveBeenCalled()
  })
})

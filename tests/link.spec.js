import { beforeEach, describe, expect, it, vi } from 'vitest'
import { buildQuery, openLink, parseQuery, resolveLink, takeTabQuery } from '@/utils/link'

const uniMock = globalThis.uni

beforeEach(() => {
  vi.clearAllMocks()
  uniMock.switchTab = vi.fn()
})

describe('resolveLink：站内消息里的 Web 端路由 → 小程序页面', () => {
  it('课程空间：分区与参数换成课程页的查询参数', () => {
    expect(resolveLink('/course/8?post=3')).toEqual({
      url: '/pages-course/space/space',
      tab: false,
      query: { id: '8', tab: 'overview', post: '3' },
    })
    expect(resolveLink('/course/8/assignments?open=5').query).toEqual({ id: '8', tab: 'assignments', open: '5' })
    expect(resolveLink('/course/8/grades').query.tab).toBe('grades')
    // 认不出的分区按概览打开
    expect(resolveLink('/course/8/unknown').query.tab).toBe('overview')
  })

  it('tabBar 页面：标记 tab，参数单独带出', () => {
    expect(resolveLink('/schedule?view=month&month=2026-10')).toEqual({
      url: '/pages/schedule/schedule',
      tab: true,
      query: { view: 'month', month: '2026-10' },
    })
    expect(resolveLink('/home')).toEqual({ url: '/pages/home/home', tab: true, query: {} })
    expect(resolveLink('/messages?tab=notices&notice=4').query).toEqual({ tab: 'notices', notice: '4' })
  })

  it('成绩单、选课广场、个人中心', () => {
    expect(resolveLink('/grades').url).toBe('/pages-course/transcript/transcript')
    expect(resolveLink('/square').url).toBe('/pages-course/square/square')
    expect(resolveLink('/profile').url).toBe('/pages/person/person')
  })

  it('教务后台：新页面直达，合并前的旧地址换到合并后的页面', () => {
    expect(resolveLink('/admin/leaves?open=2')).toEqual({
      url: '/pages-admin/leaves/leaves',
      tab: false,
      query: { open: '2' },
    })
    expect(resolveLink('/admin/colleges').url).toBe('/pages-admin/org/org')
    expect(resolveLink('/admin/teachers')).toEqual({
      url: '/pages-admin/people/people',
      tab: false,
      query: { tab: 'teachers' },
    })
    // 小程序没有的后台页面（权限、日志等）
    expect(resolveLink('/admin/operlog')).toBeNull()
  })

  it('空值、外部地址、认不出的地址返回 null', () => {
    for (const link of [null, '', 'https://example.com', '/dashboard', 'course/8']) {
      expect(resolveLink(link)).toBeNull()
    }
  })
})

describe('openLink 跳转', () => {
  it('普通页面 navigateTo，参数拼进地址', () => {
    expect(openLink('/course/8/assignments?open=5')).toBe(true)
    expect(uniMock.navigateTo).toHaveBeenCalledWith({
      url: '/pages-course/space/space?id=8&tab=assignments&open=5',
    })
  })

  it('tabBar 页面 switchTab，参数寄存给页面取走（取一次即清空）', () => {
    expect(openLink('/schedule?view=month')).toBe(true)
    expect(uniMock.switchTab).toHaveBeenCalledWith({ url: '/pages/schedule/schedule' })
    expect(uniMock.$emit).toHaveBeenCalledWith('xm:tab-query', '/pages/schedule/schedule')
    expect(takeTabQuery('/pages/schedule/schedule')).toEqual({ view: 'month' })
    expect(takeTabQuery('/pages/schedule/schedule')).toBeNull()
  })

  it('认不出的地址不跳转，返回 false', () => {
    expect(openLink('/dashboard')).toBe(false)
    expect(uniMock.navigateTo).not.toHaveBeenCalled()
    expect(uniMock.switchTab).not.toHaveBeenCalled()
  })
})

describe('查询参数', () => {
  it('解析与拼接互逆，空值不拼', () => {
    expect(parseQuery('a=1&b=%E4%B8%AD')).toEqual({ a: '1', b: '中' })
    expect(buildQuery({ a: '1', b: '中', c: '', d: null })).toBe('?a=1&b=%E4%B8%AD')
    expect(buildQuery({})).toBe('')
  })
})

import { afterEach, describe, expect, it } from 'vitest'
import { shareMessage, currentRoute } from '@/utils/share'
import { setLocale } from '@/i18n'

afterEach(() => {
  setLocale('zh-CN')
  delete globalThis.getCurrentPages
})

describe('shareMessage 转发内容', () => {
  it('公开信息页转发当前页，标题带页面名', () => {
    expect(shareMessage('pages/notice/notice')).toEqual({
      title: '教务通知 - 教务管理系统',
      path: '/pages/notice/notice',
    })
    expect(shareMessage('pages/examplan/examplan').path).toBe('/pages/examplan/examplan')
  })

  it('个人 / 管理类页面转发首页', () => {
    for (const route of ['pages/person/person', 'pages/score/score', 'admin/admin', '']) {
      expect(shareMessage(route)).toEqual({ title: '教务管理系统', path: '/pages/home/home' })
    }
  })

  it('标题跟随当前语言', () => {
    setLocale('en-US')
    expect(shareMessage('pages/home/home').title).toBe('Educational Management System')
  })
})

describe('currentRoute 当前页面路由', () => {
  it('取页面栈栈顶；没有页面时为空串', () => {
    globalThis.getCurrentPages = () => [{ route: 'pages/home/home' }, { route: 'pages/notice/notice' }]
    expect(currentRoute()).toBe('pages/notice/notice')
    globalThis.getCurrentPages = () => []
    expect(currentRoute()).toBe('')
  })
})

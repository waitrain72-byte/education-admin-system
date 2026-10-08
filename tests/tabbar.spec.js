import { afterEach, describe, expect, it } from 'vitest'
import { MESSAGES_TAB_INDEX, TABS, isTabPage, tabItem } from '@/utils/tabbar'
import { courseStatusLabel, courseTypeLabel, courseWhen, segmentLabel, weekLabel } from '@/utils/courseText'
import { setLocale } from '@/i18n'

afterEach(() => setLocale('zh-CN'))

describe('tabBar 配置', () => {
  it('五个位置：首页 / 课程 / 日程 / 消息 / 我的，未读角标挂在消息上', () => {
    expect(TABS.map((t) => t.key)).toEqual(['home', 'courses', 'schedule', 'messages', 'mine'])
    expect(MESSAGES_TAB_INDEX).toBe(3)
  })

  it('管理员的第三个位置换成「后台」（同一个页面，只换文字和图标）', () => {
    const look = { dark: false, custom: false }
    expect(tabItem(2, 'STUDENT', look)).toMatchObject({ index: 2, text: '日程' })
    expect(tabItem(2, 'ADMIN', look)).toEqual({
      index: 2,
      text: '后台',
      iconPath: '/static/tabbar/console.png',
      selectedIconPath: '/static/tabbar/console-on.png',
    })
  })

  it('选中图标：默认玉绿，深色模式用提亮版，自定义主题色时用中性墨色', () => {
    expect(tabItem(0, 'STUDENT', { dark: true, custom: false }).selectedIconPath).toBe(
      '/static/tabbar/home-on-dark.png',
    )
    expect(tabItem(0, 'STUDENT', { dark: false, custom: true }).selectedIconPath).toBe('/static/tabbar/home-ink.png')
    expect(tabItem(0, 'STUDENT', { dark: true, custom: true }).selectedIconPath).toBe(
      '/static/tabbar/home-ink-dark.png',
    )
  })

  it('文字跟随语言', () => {
    setLocale('en-US')
    expect(tabItem(4, 'TEACHER', { dark: false, custom: false }).text).toBe('Me')
  })

  it('判断是否 tabBar 页面（带不带开头的 / 都行）', () => {
    expect(isTabPage('pages/messages/messages')).toBe(true)
    expect(isTabPage('/pages/home/home')).toBe(true)
    expect(isTabPage('pages/person/person')).toBe(false)
  })
})

describe('课程字段的显示文字', () => {
  it('星期、大节按语言显示，认不出的值原样显示', () => {
    expect(weekLabel('星期三')).toBe('周三')
    expect(weekLabel('星期三', true)).toBe('星期三')
    expect(segmentLabel('第二大节（10:30 ~ 12:10）')).toBe('第二大节')
    expect(weekLabel('周末')).toBe('周末')
    setLocale('en-US')
    expect(weekLabel('星期三')).toBe('Wed')
    expect(segmentLabel('第二大节（10:30 ~ 12:10）')).toBe('Period 2')
  })

  it('上课时间一行：排了课是「星期 大节 · 教室」，没排是「未排课」', () => {
    expect(courseWhen({ week: '星期一', segment: '第一大节（08:30 ~ 10:10）', room: '7701' })).toBe(
      '周一 第一大节 · 7701',
    )
    expect(courseWhen({ week: '', segment: '', room: '' })).toBe('未排课')
  })

  it('类型与状态', () => {
    expect(courseTypeLabel('选修')).toBe('选修')
    expect(courseTypeLabel(null)).toBe('—')
    expect(courseStatusLabel('已结课')).toBe('已结课')
    setLocale('en-US')
    expect(courseTypeLabel('必修')).toBe('Required')
  })
})

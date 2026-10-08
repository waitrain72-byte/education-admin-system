import { describe, it, expect } from 'vitest'
import {
  attendanceMeta,
  attendanceRateOf,
  fileExt,
  fileKind,
  formatCountdown,
  formatSize,
  matchesPerson,
  previewTotal,
  wallMs,
  weightsValid,
} from '@/utils/courseSpace'

describe('课程空间工具函数', () => {
  it('服务器时间字符串相减就是真实间隔，与浏览器时区无关', () => {
    const start = wallMs('2026-10-08 01:58:26')
    const end = wallMs('2026-10-08 02:03:26')
    expect(end - start).toBe(5 * 60 * 1000)
    // 只到分钟的时间也能解析
    expect(wallMs('2026-10-08 02:03') - wallMs('2026-10-08 02:00')).toBe(3 * 60 * 1000)
    expect(Number.isNaN(wallMs('明天'))).toBe(true)
    expect(Number.isNaN(wallMs(null))).toBe(true)
  })

  it('倒计时显示 m:ss，负数按 0', () => {
    expect(formatCountdown(299)).toBe('4:59')
    expect(formatCountdown(5)).toBe('0:05')
    expect(formatCountdown(-3)).toBe('0:00')
  })

  it('考勤状态映射到语言包键和胶囊颜色', () => {
    expect(attendanceMeta('缺勤')?.key).toBe('absent')
    expect(attendanceMeta('缺勤')?.pill).toBe('pill--bad')
    expect(attendanceMeta('请假')?.key).toBe('leave')
    expect(attendanceMeta('不存在')).toBeNull()
  })

  it('出勤率与后端口径一致：迟到早退算出勤，请假不计入分母', () => {
    expect(attendanceRateOf({ 正常: 6, 迟到: 1, 早退: 1, 缺勤: 1, 请假: 1 })).toBe(88.9)
    expect(attendanceRateOf({ 请假: 2 })).toBeNull()
    expect(attendanceRateOf({})).toBeNull()
    expect(attendanceRateOf(null)).toBeNull()
  })

  it('文件大小与类型', () => {
    expect(formatSize(512)).toBe('512 B')
    expect(formatSize(1536)).toBe('1.5 KB')
    expect(formatSize(1048576)).toBe('1.0 MB')
    expect(formatSize(25 * 1048576)).toBe('25 MB')
    expect(formatSize(null)).toBe('—')
    expect(fileExt('第6章 集合框架.PDF')).toBe('pdf')
    expect(fileKind('报告.docx')).toBe('doc')
    expect(fileKind('数据.xlsx')).toBe('sheet')
    expect(fileKind('课件.pptx')).toBe('slide')
    expect(fileKind('源码.zip')).toBe('archive')
    expect(fileKind('没有扩展名')).toBe('file')
  })

  it('按姓名或学号筛选，忽略大小写与空格', () => {
    const p = { name: '张三', username: 'ZhangSan' }
    expect(matchesPerson(p, '张')).toBe(true)
    expect(matchesPerson(p, ' zhang ')).toBe(true)
    expect(matchesPerson(p, '李')).toBe(false)
    expect(matchesPerson(p, '')).toBe(true)
  })

  it('权重：每项 0~100 的整数、合计正好 100', () => {
    expect(weightsValid({ attendance: 10, homework: 20, ordinary: 20, exam: 50 })).toBe(true)
    expect(weightsValid({ attendance: 10, homework: 20, ordinary: 20, exam: 49 })).toBe(false)
    expect(weightsValid({ attendance: 10.5, homework: 19.5, ordinary: 20, exam: 50 })).toBe(false)
    expect(weightsValid({ attendance: null, homework: 30, ordinary: 20, exam: 50 })).toBe(false)
  })

  it('总评预览与后端规则一致：有权重的项都有分才算，保留一位小数', () => {
    const w = { attendance: 10, homework: 20, ordinary: 20, exam: 50 }
    expect(previewTotal(w, { attendance: 100, homework: 80, ordinary: 60, exam: 90 })).toBe(83)
    expect(previewTotal(w, { attendance: 100, homework: null, ordinary: 60, exam: 90 })).toBeNull()
    // 权重为 0 的项不参与
    expect(previewTotal({ attendance: 0, homework: 0, ordinary: 30, exam: 70 }, { attendance: null, homework: null, ordinary: 60, exam: 90 })).toBe(81)
    // 33.3*0.3 + 66.7*0.7 = 56.68 → 56.7（与 GradeCalculatorTest 同一组数）
    expect(previewTotal({ attendance: 0, homework: 0, ordinary: 30, exam: 70 }, { attendance: null, homework: null, ordinary: 33.3, exam: 66.7 })).toBe(56.7)
  })
})

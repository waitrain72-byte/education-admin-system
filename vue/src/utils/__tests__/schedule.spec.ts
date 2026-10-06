import { describe, it, expect } from 'vitest'
import {
  classState,
  greetingKey,
  minutesUntil,
  nextClass,
  parseSegmentTime,
  segmentIndex,
  segmentShortName,
  weekdayIndex,
} from '@/utils/schedule'
import { COURSE_COLORS, courseColor, nameHash } from '@/utils/courseColor'

const SEG1 = '第一大节（08:30 ~ 10:10）'
const SEG2 = '第二大节（10:30 ~ 12:10）'
const SEG3 = '第三大节（14:00 ~ 15:40）'

describe('节次解析', () => {
  it('从中文节次里取出起止时间，单数字小时补零', () => {
    expect(parseSegmentTime(SEG1)).toEqual(['08:30', '10:10'])
    expect(parseSegmentTime('加课（9:05~10:00）')).toEqual(['09:05', '10:00'])
    expect(parseSegmentTime('第一大节')).toEqual(['', ''])
    expect(parseSegmentTime(undefined)).toEqual(['', ''])
  })

  it('简称、节次先后、星期下标', () => {
    expect(segmentShortName(SEG2)).toBe('第二大节')
    expect(segmentIndex(SEG3)).toBe(2)
    expect(segmentIndex('不存在的节次')).toBe(5)
    expect(weekdayIndex('星期一')).toBe(0)
    expect(weekdayIndex('星期日')).toBe(6)
    expect(weekdayIndex('周一')).toBe(-1)
  })
})

describe('上课状态（按传入的服务器时间，不看浏览器时区）', () => {
  it('开始前、上课中、下课后', () => {
    expect(classState(SEG2, '10:29')).toBe('upcoming')
    expect(classState(SEG2, '10:30')).toBe('ongoing')
    expect(classState(SEG2, '12:10')).toBe('ongoing')
    expect(classState(SEG2, '12:11')).toBe('done')
    expect(classState('没有时间的节次', '10:00')).toBe('')
  })

  it('距离上课还有多少分钟（已开始为 0）', () => {
    expect(minutesUntil(SEG3, '13:15')).toBe(45)
    expect(minutesUntil(SEG3, '14:20')).toBe(0)
    expect(minutesUntil('无', '13:00')).toBeNull()
  })

  it('下一节课：正在上的优先，其次是最近的一节，全部结束返回 null', () => {
    const courses = [{ id: 3, segment: SEG3 }, { id: 1, segment: SEG1 }, { id: 2, segment: SEG2 }]
    expect(nextClass(courses, '07:00')?.id).toBe(1)
    expect(nextClass(courses, '10:40')?.id).toBe(2)
    expect(nextClass(courses, '12:30')?.id).toBe(3)
    expect(nextClass(courses, '21:00')).toBeNull()
  })

  it('问候语按小时切换', () => {
    expect(greetingKey('07:30')).toBe('workbench.greeting.morning')
    expect(greetingKey('12:00')).toBe('workbench.greeting.noon')
    expect(greetingKey('15:00')).toBe('workbench.greeting.afternoon')
    expect(greetingKey('22:00')).toBe('workbench.greeting.evening')
    expect(greetingKey('03:00')).toBe('workbench.greeting.evening')
  })
})

describe('课程配色', () => {
  it('同名课程同色，取色规则与小程序端一致（同样的哈希、同样的 8 色顺序）', () => {
    expect(courseColor('高等数学')).toBe(courseColor('高等数学'))
    expect(COURSE_COLORS).toContain(courseColor('线性代数'))
    // 小程序端：hash = hash * 31 + charCode（无符号 32 位）
    let hash = 0
    for (const ch of 'Java 程序设计') hash = (hash * 31 + ch.charCodeAt(0)) >>> 0
    expect(nameHash('Java 程序设计')).toBe(hash)
    expect(courseColor('Java 程序设计')).toBe(COURSE_COLORS[hash % 8])
  })

  it('空名称也能取到颜色，不报错', () => {
    expect(COURSE_COLORS).toContain(courseColor(''))
    expect(COURSE_COLORS).toContain(courseColor(undefined))
  })
})

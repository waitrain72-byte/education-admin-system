import { describe, it, expect } from 'vitest'
import {
  parseSegmentTime,
  toMinutes,
  classStatus,
  parseCurriculumCell,
  decorateToday,
} from '../src/utils/todaySchedule'

/** 构造当天某时刻 */
const at = (hh, mm) => new Date(2026, 8, 23, hh, mm)

describe('parseSegmentTime', () => {
  it('从节次文本中解析起止时间', () => {
    expect(parseSegmentTime('第一大节（08:30 ~ 10:10）')).toEqual(['08:30', '10:10'])
    expect(parseSegmentTime('第三大节(14:00~15:40)')).toEqual(['14:00', '15:40'])
  })

  it('无时间 / 空值返回空串', () => {
    expect(parseSegmentTime('第一大节')).toEqual(['', ''])
    expect(parseSegmentTime(undefined)).toEqual(['', ''])
  })
})

describe('toMinutes', () => {
  it('HH:mm 转当日分钟数', () => {
    expect(toMinutes('08:30')).toBe(510)
    expect(toMinutes('0:05')).toBe(5)
  })
})

describe('classStatus', () => {
  const seg = '第一大节（08:30 ~ 10:10）'

  it('按当前时间区分 未开始 / 进行中 / 已结束', () => {
    expect(classStatus(seg, at(8, 0))).toBe('todo')
    expect(classStatus(seg, at(9, 0))).toBe('doing')
    expect(classStatus(seg, at(11, 0))).toBe('done')
  })

  it('开始、结束的那一分钟都算进行中', () => {
    expect(classStatus(seg, at(8, 30))).toBe('doing')
    expect(classStatus(seg, at(10, 10))).toBe('doing')
  })

  it('节次没有时间时不给状态', () => {
    expect(classStatus('第一大节', at(9, 0))).toBe('')
  })
})

describe('parseCurriculumCell', () => {
  it('拆出课程名与括号里的教师（中英文括号都支持）', () => {
    expect(parseCurriculumCell('高等数学（张老师）')).toMatchObject({ name: '高等数学', sub: '张老师' })
    expect(parseCurriculumCell('Java(李老师)')).toMatchObject({ name: 'Java', sub: '李老师' })
  })

  it('多行单元格：后续行拼到 sub', () => {
    expect(parseCurriculumCell('数据结构（王老师）\n1-16周')).toMatchObject({
      name: '数据结构',
      sub: '王老师 · 1-16周',
    })
  })

  it('无括号时整行作为课程名', () => {
    expect(parseCurriculumCell('  体育  ')).toMatchObject({ name: '体育', sub: '', text: '体育' })
  })
})

describe('decorateToday', () => {
  it('补齐起止时间与状态，并按开始时间排序；无时间的排最后', () => {
    const list = decorateToday(
      [
        { key: 'c', segment: '第三大节（14:00 ~ 15:40）', name: 'C' },
        { key: 'x', segment: '晚自习', name: 'X' },
        { key: 'a', segment: '第一大节（08:30 ~ 10:10）', name: 'A' },
      ],
      at(9, 0),
    )
    expect(list.map((c) => c.name)).toEqual(['A', 'C', 'X'])
    expect(list[0]).toMatchObject({ start: '08:30', end: '10:10', status: 'doing' })
    expect(list[1].status).toBe('todo')
    expect(list[2]).toMatchObject({ start: '', status: '' })
  })

  it('不修改传入的数组元素', () => {
    const input = [{ key: 'a', segment: '第一大节（08:30 ~ 10:10）', name: 'A' }]
    decorateToday(input, at(9, 0))
    expect(input[0].status).toBeUndefined()
  })
})

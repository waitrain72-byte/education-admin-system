import { describe, it, expect } from 'vitest'
import {
  parseSegmentTime,
  toMinutes,
  classStatus,
  parseCurriculumCell,
  decorateToday,
  buildWeekGrid,
  hasWeekendClass,
  nowRowPosition,
  SEGMENTS,
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

describe('buildWeekGrid 周课表网格', () => {
  const choices = [
    { name: '数据结构', week: '星期一', segment: '第一大节（08:30 ~ 10:10）', room: 'A101' },
    { name: '体育', week: '星期六', segment: '第三大节（14:00 ~ 15:40）', room: '操场' },
    { name: '冲突课', week: '星期一', segment: '第一大节（08:30 ~ 10:10）' },
    { name: '夜课', week: '星期三', segment: '第六大节（21:00 ~ 21:45）' },
  ]

  it('固定五个大节行，按星期落格，同格多门课保留为数组', () => {
    const rows = buildWeekGrid(choices)
    expect(rows.slice(0, 5).map((r) => r.segment)).toEqual(SEGMENTS)
    expect(rows[0].start).toBe('08:30')
    expect(rows[0].cells.monday.map((c) => c.name)).toEqual(['数据结构', '冲突课'])
    expect(rows[2].cells.saturday[0].room).toBe('操场')
    expect(rows[1].cells.monday).toEqual([])
  })

  it('未知大节追加在末尾；空数据也返回完整空网格', () => {
    const rows = buildWeekGrid(choices)
    expect(rows).toHaveLength(6)
    expect(rows[5].cells.wednesday[0].name).toBe('夜课')
    expect(buildWeekGrid(null)).toHaveLength(5)
  })

  it('hasWeekendClass：只有工作日课时为 false', () => {
    expect(hasWeekendClass(buildWeekGrid(choices))).toBe(true)
    expect(hasWeekendClass(buildWeekGrid(choices.filter((c) => c.week !== '星期六')))).toBe(false)
  })
})

describe('nowRowPosition 当前时间线位置', () => {
  const rows = buildWeekGrid([])

  it('上课中按已上比例落在行内', () => {
    // 第一大节 08:30~10:10 共 100 分钟，09:20 已上 50 分钟
    expect(nowRowPosition(rows, at(9, 20))).toBeCloseTo(0.5)
    expect(nowRowPosition(rows, at(14, 0))).toBe(2)
  })

  it('课间停在下一节行首；首节前与末节后不画线', () => {
    expect(nowRowPosition(rows, at(10, 20))).toBe(1)
    expect(nowRowPosition(rows, at(7, 0))).toBe(-1)
    expect(nowRowPosition(rows, at(21, 0))).toBe(-1)
  })
})

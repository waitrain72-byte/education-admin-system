import { beforeEach, describe, expect, it, vi } from 'vitest'

const { roomFreeMock } = vi.hoisted(() => ({ roomFreeMock: vi.fn() }))
vi.mock('@/api', () => ({ courseApi: { roomFree: roomFreeMock } }))

const { roomDesc, freeRoomQuery, assignRoomBeforeSave } = await import('@/pages/course/room')
const { t } = await import('@/i18n')

const uniMock = globalThis.uni

beforeEach(() => vi.clearAllMocks())

describe('roomDesc 教室简述', () => {
  it('运动场馆用名称；教室取使用说明里的用途，缺省回退名称', () => {
    expect(roomDesc({ type: '运动场馆', name: '体育馆', content: '室内篮球' })).toBe('体育馆')
    expect(roomDesc({ type: '授课教室', name: '一号楼 101', content: '多媒体教室（投影 + 音响）' })).toBe('多媒体教室')
    expect(roomDesc({ type: '授课教室', name: '一号楼 101', content: '机房\n90 台电脑' })).toBe('机房')
    expect(roomDesc({ type: '授课教室', name: '一号楼 101', content: '' })).toBe('一号楼 101')
  })
})

describe('freeRoomQuery 空闲教室查询参数', () => {
  it('人数 / 课程 id 为空时不传（新增课程不排除自身）', () => {
    expect(freeRoomQuery({ week: '星期一', segment: 's1', num: '', id: undefined })).toEqual({
      week: '星期一',
      segment: 's1',
      num: undefined,
      excludeId: undefined,
    })
    expect(freeRoomQuery({ week: '星期一', segment: 's1', num: 60, id: 3 })).toMatchObject({ num: 60, excludeId: 3 })
  })
})

describe('assignRoomBeforeSave 保存前自动分配教室', () => {
  it('已选教室：直接放行，不查询', async () => {
    expect(await assignRoomBeforeSave({ room: 'A-101', week: '星期一', segment: 's1' })).toBe('')
    expect(roomFreeMock).not.toHaveBeenCalled()
  })

  it('没选教室也没选时段：提示必须选教室', async () => {
    expect(await assignRoomBeforeSave({ room: '' })).toBe(t('pages.course.ruleRoomRequired'))
    expect(roomFreeMock).not.toHaveBeenCalled()
  })

  it('该时段没有空闲教室：提示并中止保存', async () => {
    roomFreeMock.mockResolvedValueOnce([])
    expect(await assignRoomBeforeSave({ week: '星期一', segment: 's1' })).toBe(t('pages.course.noFreeRoom'))
  })

  it('自动取第一间空闲教室写回表单并提示', async () => {
    roomFreeMock.mockResolvedValueOnce([
      { code: 'A-101', type: '授课教室', name: '一号楼 101', content: '多媒体教室（投影）', num: 60 },
      { code: 'B-201', type: '授课教室', name: '二号楼 201', content: '', num: 80 },
    ])
    const form = { id: 5, week: '星期一', segment: 's1', num: 50 }
    expect(await assignRoomBeforeSave(form)).toBe('')
    expect(form.room).toBe('A-101')
    expect(roomFreeMock).toHaveBeenCalledWith({ week: '星期一', segment: 's1', num: 50, excludeId: 5 })
    expect(uniMock.showToast).toHaveBeenCalledWith({
      title: t('pages.course.autoAssigned', { code: 'A-101', name: '多媒体教室' }),
      icon: 'none',
    })
  })

  it('查询失败向上抛出（由保存流程统一处理，提示已由请求层弹出）', async () => {
    roomFreeMock.mockRejectedValueOnce(new Error('net'))
    await expect(assignRoomBeforeSave({ week: '星期一', segment: 's1' })).rejects.toThrow('net')
  })
})

import { courseApi } from '@/api'
import { t } from '@/i18n'

/**
 * 排课教室的公共逻辑（课程表单的教室选择器与保存前自动分配共用）。
 * 空闲教室按「星期 + 大节」查询，后端按容量就近排序，第一间即「自动分配」的结果。
 */

/** 教室选择器置顶的「自动分配」项的取值 */
export const AUTO_ROOM = '__auto__'

/** 教室简述：运动场馆用名称；教室优先用「使用说明」里的用途（多媒体教室 / 机房等），缺省回退名称 */
export function roomDesc(room) {
  if (room.type === '运动场馆') return room.name
  return (
    String(room.content || '')
      .split('（')[0]
      .split('\n')[0]
      .trim() || room.name
  )
}

/** 查询空闲教室的参数：人数用于按容量筛选，编辑时排除课程自身已占用的教室 */
export const freeRoomQuery = (form) => ({
  week: form.week,
  segment: form.segment,
  num: form.num || undefined,
  excludeId: form.id || undefined,
})

/**
 * 保存前的教室处理（直接用作 useListPage 的 beforeSave）：
 * 教室留空且已选时段时，自动分配第一间空闲教室 / 场地并提示；返回错误文案，空串表示可以保存。
 */
export async function assignRoomBeforeSave(form) {
  if (!form.room && form.week && form.segment) {
    const rooms = (await courseApi.roomFree(freeRoomQuery(form))) || []
    if (!rooms.length) return t('pages.course.noFreeRoom')
    form.room = rooms[0].code
    uni.showToast({
      title: t('pages.course.autoAssigned', { code: rooms[0].code, name: roomDesc(rooms[0]) }),
      icon: 'none',
    })
  }
  if (!form.room) return t('pages.course.ruleRoomRequired')
  return ''
}

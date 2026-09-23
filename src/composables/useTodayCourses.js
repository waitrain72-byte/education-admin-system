import { ref } from 'vue'
import { get } from '@/utils/request'
import { WEEKDAY_FIELDS, WEEKDAY_ZH, decorateToday, parseCurriculumCell } from '@/utils/todaySchedule'

/**
 * 首页「今日课程」数据：
 * - 学生：选课课表接口按当前登录学生返回整周课表，取今天一列
 * - 教师：按本人 teacherId 过滤授课课程，再取今天星期几的课
 * - 管理员：无此卡片
 */
export function useTodayCourses(userStore) {
  const todayCourses = ref([])

  const loadStudent = async () => {
    const rows = (await get('/choice/getCurriculum')) || []
    const field = WEEKDAY_FIELDS[new Date().getDay()]
    let list = rows
      .filter((r) => r[field])
      .map((r) => {
        const { text, name, sub } = parseCurriculumCell(r[field])
        return { key: r.segment + text, segment: r.segment, name, sub }
      })
    // 单元格不含教室：按「课程名 + 教师」反查课程表补齐 room，第二行组成「教室 · 老师」（与演示稿一致）
    try {
      const courses = (await get('/course/selectAll')) || []
      list = list.map((c) => {
        const hit = courses.find((k) => k.name === c.name && (!c.sub || k.teacherName === c.sub))
        return { ...c, sub: [hit && hit.room, c.sub].filter(Boolean).join(' · ') }
      })
    } catch {
      // 反查失败时保留「老师」作为第二行，不影响课表展示
    }
    return list
  }

  const loadTeacher = async () => {
    const teacherId = userStore.user.id
    const page = await get('/course/selectPage', { pageNum: 1, pageSize: 100, teacherId })
    const todayZh = WEEKDAY_ZH[new Date().getDay()]
    return ((page && page.list) || [])
      .filter((c) => c.teacherId === teacherId && c.week === todayZh)
      .map((c) => ({ key: c.segment + c.name, segment: c.segment, name: c.name, sub: c.room || '' }))
  }

  const loadTodayCourses = async () => {
    const role = userStore.role
    const list = role === 'STUDENT' ? await loadStudent() : role === 'TEACHER' ? await loadTeacher() : []
    todayCourses.value = decorateToday(list)
  }

  return { todayCourses, loadTodayCourses }
}

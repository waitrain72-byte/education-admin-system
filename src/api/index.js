import { get, post, put } from '@/utils/request'
import { createCrudApi } from './crud'

/**
 * 接口层：所有业务接口的路径都集中在 src/api，页面与组合式函数只调用这里的函数，不再手写路径。
 * - 一个后端 Controller 对应一个 xxxApi：标准 7 个接口由 createCrudApi 生成（见 crud.js），
 *   Controller 里额外的接口（统计、推荐、重置密码等）在对应对象里补充，方法名与后端一致
 * - 后端改路径只改这里；新增一个 CRUD 模块只需加一行 createCrudApi('/xxx')
 * - 登录、注册、验证码、偏好设置等账号类接口见 account.js
 * - 固定的传输层地址不在这里：文件上传见 utils/upload.ts，实时通知见 utils/websocket.ts
 */

export { createCrudApi } from './crud'
export { accountApi } from './account'

/* ---------- 信息公告 ---------- */
export const noticeApi = createCrudApi('/notice')
export const examplanApi = createCrudApi('/examplan')
export const roomplanApi = createCrudApi('/roomplan')

/* ---------- 行政管理 ---------- */
export const collegeApi = createCrudApi('/college')
export const specialityApi = createCrudApi('/speciality')
export const classesApi = createCrudApi('/classes')

/* ---------- 教学管理 ---------- */
export const courseApi = {
  ...createCrudApi('/course'),
  /** 某时段容量足够的空闲教室（授课教室 + 运动场馆，按容量就近排序）：{ week, segment, num?, excludeId? } */
  roomFree: (params, opts) => get('/course/roomFree', params, opts),
  /** 为当前学生推荐课程（协同过滤）：{ limit? } */
  recommend: (params, opts) => get('/course/recommend', params, opts),
}

export const choiceApi = {
  ...createCrudApi('/choice'),
  /** 当前登录学生的整周课表（按大节分行、星期分列） */
  getCurriculum: (opts) => get('/choice/getCurriculum', undefined, opts),
}

export const scoreApi = {
  ...createCrudApi('/score'),
  /** 成绩分布统计（首页柱状图） */
  getLine: (opts) => get('/score/getLine', undefined, opts),
}

export const commentApi = createCrudApi('/comment')

/** 学业预警（后端 WarningController，非通用 CRUD；按成绩与考勤加权计算风险指数） */
export const warningApi = {
  /** 预警列表（按风险指数降序；后端按角色限定范围：管理员全部 / 教师本人任课学生 / 学生本人） */
  list: (opts) => get('/warning/list', undefined, opts),
  /** 向学生实时推送预警提醒（WebSocket，学生端收到后进消息中心） */
  notify: (studentId, opts) => post(`/warning/notify/${studentId}`, undefined, opts),
}

/* ---------- 教务管理 ---------- */
export const applyApi = createCrudApi('/apply')
export const homeworkApi = createCrudApi('/homework')

export const attendanceApi = {
  ...createCrudApi('/attendance'),
  /** 考勤状态统计（首页环形图） */
  getPie: (opts) => get('/attendance/getPie', undefined, opts),
}

/* ---------- 用户管理：管理员 / 教师 / 学生分表存储，另有重置密码 ---------- */
const createUserApi = (base) => ({
  ...createCrudApi(base),
  /** 重置为后端默认密码 */
  resetPassword: (id, opts) => put(`${base}/resetPassword/${id}`, undefined, opts),
})
export const adminApi = createUserApi('/admin')
export const teacherApi = createUserApi('/teacher')
export const studentApi = createUserApi('/student')

/** 按角色取账号表对应的接口（个人信息、「我的」页读写当前账号自己的资料） */
export const userApiOf = (role) => ({ ADMIN: adminApi, TEACHER: teacherApi, STUDENT: studentApi })[role] || null

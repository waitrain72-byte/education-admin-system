import { del, get, post, put } from '@/utils/request'
import { createCrudApi } from './crud'

/**
 * 接口层：所有业务接口的路径都集中在 src/api，页面与组合式函数只调用这里的函数，不再手写路径。
 * - 一个后端 Controller 对应一个 xxxApi：标准 7 个接口由 createCrudApi 生成（见 crud.js），
 *   Controller 里额外的接口在对应对象里补充
 * - 与 Web 端改版后的接口一致：首页工作台、课程空间（签到 / 作业 / 成绩册 / 评价 / 资料 / 成员 / 公告）、
 *   课程广场、日程、请假、消息中心、成绩单、教务后台的人员与组织架构
 * - 登录、注册、验证码、偏好设置等账号类接口见 account.js
 * - 固定的传输层地址不在这里：文件上传见 utils/upload.ts，实时通知见 utils/websocket.ts
 */

export { createCrudApi } from './crud'
export { accountApi } from './account'

/* ---------- 首页、搜索、学期、消息 ---------- */
export const workbenchApi = {
  /** 分角色首页数据：今天的课、待办、学分绩点、预警、管理员指标等（带服务器当前时间 now） */
  summary: (opts) => get('/workbench/summary', undefined, opts),
  /** 全局搜索：课程、通知、学生、教师 */
  search: (q, opts) => get('/search', { q }, opts),
}

export const configApi = {
  /** 当前学期：名称、开学日期、教学周数 */
  semester: (opts) => get('/config/semester', undefined, opts),
  saveSemester: (data, opts) => put('/config/semester', data, opts),
}

export const messageApi = {
  /** 我的消息（分页）：{ pageNum, pageSize, unreadOnly? } */
  page: (params, opts) => get('/message/page', params, opts),
  unreadCount: (opts) => get('/message/unreadCount', undefined, opts),
  read: (id, opts) => put(`/message/read/${id}`, undefined, opts),
  readAll: (opts) => put('/message/readAll', undefined, opts),
  remove: (id, opts) => del(`/message/${id}`, undefined, opts),
}

/* ---------- 公告、考试、教室、组织架构 ---------- */
export const noticeApi = createCrudApi('/notice')
export const examplanApi = createCrudApi('/examplan')
export const roomplanApi = createCrudApi('/roomplan')
export const collegeApi = createCrudApi('/college')
export const specialityApi = createCrudApi('/speciality')
export const classesApi = createCrudApi('/classes')

/* ---------- 课程与课程空间 ---------- */
export const courseApi = {
  ...createCrudApi('/course'),
  /** 课程卡片：学生 = 已选、教师 = 所授、管理员 = 全部 */
  mine: (opts) => get('/course/mine', undefined, opts),
  /** 课程广场：{ keyword?, type?, week?, status?, available?, includeEnded? } */
  square: (params, opts) => get('/course/square', params, opts),
  enroll: (id, opts) => post(`/course/${id}/enroll`, undefined, opts),
  drop: (id, opts) => del(`/course/${id}/enroll`, undefined, opts),
  /** 课程空间概览：课程信息、与当前登录人的关系、简介、权重、待办 */
  overview: (id, opts) => get(`/course/${id}/overview`, undefined, opts),
  members: (id, opts) => get(`/course/${id}/members`, undefined, opts),
  posts: (id, opts) => get(`/course/${id}/posts`, undefined, opts),
  addPost: (id, data, opts) => post(`/course/${id}/posts`, data, opts),
  deletePost: (id, postId, opts) => del(`/course/${id}/posts/${postId}`, undefined, opts),
  updateIntro: (id, intro, opts) => put(`/course/${id}/intro`, { intro }, opts),
  /** 某时段空着、坐得下的教室（按容量就近排序）：{ week, segment, num?, typeFilter?, excludeId? } */
  roomFree: (params, opts) => get('/course/roomFree', params, opts),
  /** 为当前学生推荐课程（协同过滤）：{ limit? } */
  recommend: (params, opts) => get('/course/recommend', params, opts),
}

/** 课堂签到与考勤（课程空间「签到」页） */
export const checkinApi = {
  /** 考勤视图：进行中的签到、某天的名单或学生本人的记录；{ date? } */
  view: (courseId, params, opts) => get(`/course/${courseId}/attendance`, params, opts),
  start: (courseId, data, opts) => post(`/course/${courseId}/attendance/sessions`, data, opts),
  finish: (courseId, sessionId, opts) =>
    post(`/course/${courseId}/attendance/sessions/${sessionId}/finish`, undefined, opts),
  checkin: (courseId, code, opts) => post(`/course/${courseId}/attendance/checkin`, { code }, opts),
  /** 老师手工改考勤：{ date, records: [{ studentId, status }] } */
  saveRecords: (courseId, data, opts) => put(`/course/${courseId}/attendance/records`, data, opts),
}

/** 作业任务（课程空间「作业」页） */
export const assignmentApi = {
  list: (courseId, opts) => get(`/course/${courseId}/assignments`, undefined, opts),
  /** 改版前没挂在任务下的旧作业提交 */
  loose: (courseId, opts) => get(`/course/${courseId}/assignments/loose`, undefined, opts),
  detail: (courseId, id, opts) => get(`/course/${courseId}/assignments/${id}`, undefined, opts),
  create: (courseId, data, opts) => post(`/course/${courseId}/assignments`, data, opts),
  update: (courseId, id, data, opts) => put(`/course/${courseId}/assignments/${id}`, data, opts),
  remove: (courseId, id, opts) => del(`/course/${courseId}/assignments/${id}`, undefined, opts),
  submit: (courseId, id, data, opts) => post(`/course/${courseId}/assignments/${id}/submit`, data, opts),
  grade: (courseId, homeworkId, data, opts) =>
    put(`/course/${courseId}/assignments/submissions/${homeworkId}`, data, opts),
}

/** 成绩册（课程空间「成绩」页） */
export const gradebookApi = {
  view: (courseId, opts) => get(`/course/${courseId}/gradebook`, undefined, opts),
  mine: (courseId, opts) => get(`/course/${courseId}/gradebook/mine`, undefined, opts),
  save: (courseId, data, opts) => put(`/course/${courseId}/gradebook`, data, opts),
  publish: (courseId, opts) => post(`/course/${courseId}/gradebook/publish`, undefined, opts),
  unpublish: (courseId, opts) => post(`/course/${courseId}/gradebook/unpublish`, undefined, opts),
}

/** 课程评价（五个维度打分 + 文字） */
export const evaluationApi = {
  view: (courseId, opts) => get(`/course/${courseId}/evaluations`, undefined, opts),
  submit: (courseId, data, opts) => post(`/course/${courseId}/evaluations`, data, opts),
}

/** 课程资料 */
export const resourceApi = {
  list: (courseId, opts) => get(`/course/${courseId}/resources`, undefined, opts),
  add: (courseId, data, opts) => post(`/course/${courseId}/resources`, data, opts),
  remove: (courseId, id, opts) => del(`/course/${courseId}/resources/${id}`, undefined, opts),
}

/* ---------- 日程、请假、成绩 ---------- */
export const scheduleApi = {
  /** 周课表：{ date? }（yyyy-MM-dd，落在哪一周就看哪一周） */
  week: (params, opts) => get('/schedule/week', params, opts),
  /** 月历：{ month? }（yyyy-MM） */
  month: (params, opts) => get('/schedule/month', params, opts),
}

export const applyApi = {
  ...createCrudApi('/apply'),
  /** 请假前预览：从 from 起连续 days 天里要上的课 */
  preview: (params, opts) => get('/apply/preview', params, opts),
  /** 某条请假影响的课（审批时看） */
  affected: (id, opts) => get(`/apply/${id}/affected`, undefined, opts),
}

export const scoreApi = {
  ...createCrudApi('/score'),
  /** 成绩分布统计 */
  getLine: (opts) => get('/score/getLine', undefined, opts),
  /** 学生本人的成绩单：所有课的已发布成绩与学分、绩点汇总 */
  transcript: (opts) => get('/score/transcript', undefined, opts),
}

/** 学业预警：列表按角色限定范围；提醒只能发给自己课上的学生（管理员不限） */
export const warningApi = {
  list: (opts) => get('/warning/list', undefined, opts),
  notify: (studentId, opts) => post(`/warning/notify/${studentId}`, undefined, opts),
}

/* ---------- 旧版接口（教学记录类页面在用，页面替换完后删除） ---------- */
export const choiceApi = {
  ...createCrudApi('/choice'),
  getCurriculum: (opts) => get('/choice/getCurriculum', undefined, opts),
}
export const commentApi = createCrudApi('/comment')
export const homeworkApi = createCrudApi('/homework')
export const attendanceApi = {
  ...createCrudApi('/attendance'),
  getPie: (opts) => get('/attendance/getPie', undefined, opts),
}

/* ---------- 人员：管理员 / 教师 / 学生分表存储，另有重置密码 ---------- */
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

/** 教务后台「人员」与「组织架构」：一页查三类账号，学院 → 专业 → 班级树 */
export const peopleApi = {
  /** { keyword?, collegeId?, specialityId?, classId?, pageNum, pageSize } */
  students: (params, opts) => get('/people/students', params, opts),
  teachers: (params, opts) => get('/people/teachers', params, opts),
  admins: (params, opts) => get('/people/admins', params, opts),
  org: (opts) => get('/people/org', undefined, opts),
}

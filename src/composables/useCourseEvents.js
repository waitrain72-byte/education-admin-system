import { onUnmounted } from 'vue'

/** 同一批事件在这段时间内只触发一次回调（30 个学生几秒内签到，老师端只刷新一两次） */
const COALESCE_MS = 400

/**
 * 订阅课程空间的静默事件（后端 CourseSpaceService.emit 推送 { type: 'course', event, courseId }，
 * utils/websocket 收到后 uni.$emit('ws:course')）：同一门课的指定事件到达时回调，组件卸载时自动退订。
 * 与 Web 端 composables/useCourseEvents.ts 一致。
 *
 * @param {() => number} courseId 返回当前课程 ID 的函数
 * @param {string[]} events 关心的事件名：attendance / assignments / posts / resources
 * @param {() => void} handler
 */
export function useCourseEvents(courseId, events, handler) {
  let timer = null
  const listener = (message) => {
    if (!message || message.type !== 'course' || Number(message.courseId) !== Number(courseId())) return
    if (!events.includes(String(message.event))) return
    if (timer) return
    timer = setTimeout(() => {
      timer = null
      handler()
    }, COALESCE_MS)
  }
  uni.$on('ws:course', listener)
  onUnmounted(() => {
    uni.$off('ws:course', listener)
    if (timer) clearTimeout(timer)
  })
}

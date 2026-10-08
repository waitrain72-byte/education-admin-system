import { onBeforeUnmount } from 'vue'
import { onSocketMessage } from '@/composables/useNoticeSocket'

/** 同一批事件在这段时间内只触发一次回调（30 个学生几秒内签到，老师端只刷新一两次） */
const COALESCE_MS = 400

/**
 * 订阅课程空间的静默事件（后端 CourseSpaceService.emit 推送 { type: 'course', event, courseId }）：
 * 同一门课的指定事件到达时回调，组件卸载时自动退订。
 *
 * @param courseId 返回当前课程 ID 的函数（路由切到别的课时自动跟着变）
 * @param events   关心的事件名：attendance / assignments / posts / resources
 */
export function useCourseEvents(courseId: () => number, events: string[], handler: () => void): void {
    let timer: ReturnType<typeof setTimeout> | null = null
    const off = onSocketMessage((message) => {
        if (message?.type !== 'course' || Number(message.courseId) !== courseId()) return
        if (!events.includes(String(message.event))) return
        if (timer) return
        timer = setTimeout(() => {
            timer = null
            handler()
        }, COALESCE_MS)
    })
    onBeforeUnmount(() => {
        off()
        if (timer) clearTimeout(timer)
    })
}

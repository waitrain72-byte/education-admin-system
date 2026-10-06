import { computed } from 'vue'
import { t } from '@/i18n'
import { greetingKey } from '@/utils/schedule'
import { useServerClock } from '@/composables/useServerClock'

/**
 * 首页页头的两段文字：问候语（按服务器时间的上午 / 下午……）与「学期 · 第 N 周 · 星期X」。
 * 开学前、超过教学周数时分别显示「学期还没开始」「本学期已结束」。
 */
export function useHomeText(semester: () => Record<string, any> | undefined) {
    const clock = useServerClock()

    const greeting = computed(() => t(greetingKey(clock.value.hhmm)))

    const semesterLine = computed(() => {
        const s = semester() || {}
        const parts: string[] = []
        if (s.name) parts.push(s.name)
        const week = s.currentWeek
        if (week === 0) {
            parts.push(t('workbench.beforeSemester'))
        } else if (typeof week === 'number' && s.weeks && week > s.weeks) {
            parts.push(t('workbench.afterSemester'))
        } else if (typeof week === 'number') {
            parts.push(t('workbench.weekN', { n: week }))
        }
        if (s.weekday) parts.push(s.weekday)
        return parts.join(' · ')
    })

    return { greeting, semesterLine }
}

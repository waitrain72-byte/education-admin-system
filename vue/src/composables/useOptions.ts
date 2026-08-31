import { ref } from 'vue'
import type { Ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { apiMessage } from '@/i18n'

export interface UseOptionsOptions {
    /** 结果取自返回数据里的哪个字段（默认直接用整个 data） */
    key?: string
    /** 每次加载时的通用查询参数 */
    params?: () => Record<string, any>
}

/**
 * 通用下拉选项加载组合式函数：
 * 统一处理「请求 → 校验 code === '200' → 赋值 options → 失败提示」这一段在每个页面重复的样板逻辑。
 * 适用于学院/专业/班级/课程等下拉选项的异步加载。
 *
 * 用法：
 *   const { options, load } = useOptions('/college/selectAll')
 *   const { options: courseData, load: loadCourse } = useOptions('/course/selectAll', { params: () => ({ teacherId: user.value.id }) })
 */
export function useOptions<T = any>(url: string, opts: UseOptionsOptions = {}) {
    const options = ref<T[]>([])
    const loading = ref(false)

    const load = async (overrideParams?: Record<string, any>) => {
        loading.value = true
        try {
            const res: any = await request.get(url, {
                params: { ...(opts.params?.() || {}), ...(overrideParams || {}) },
            })
            if (res.data.code === '200') {
                const data = opts.key ? res.data.data?.[opts.key] : res.data.data
                options.value = (data || []) as T[]
            } else {
                ElMessage.error(apiMessage(res.data))
            }
        } catch {
            // 错误提示已由 axios 拦截器统一处理
        } finally {
            loading.value = false
        }
    }

    return { options, loading, load }
}

export type UseOptionsReturn<T = any> = { options: Ref<T[]>; loading: Ref<boolean>; load: (p?: Record<string, any>) => Promise<void> }

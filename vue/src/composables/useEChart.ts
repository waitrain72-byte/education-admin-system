import { onBeforeUnmount, onMounted, watch } from 'vue'
import type { Ref } from 'vue'
import * as echarts from 'echarts/core'
import type { ECharts, EChartsCoreOption } from 'echarts/core'

/**
 * 通用 ECharts 生命周期组合式函数：
 * 封装「init → setOption → resize → dispose」这组在每个图表页都会重复的样板逻辑，
 * 并自动处理窗口 resize 与组件卸载时的内存释放。
 *
 * 用法：
 *   const el = ref<HTMLElement>()
 *   const getOption = () => ({ ... })
 *   const { setOption } = useEChart(el, getOption)
 *
 * @param container 图表容器 DOM 的 Ref（模板中 <div ref="el" />）
 * @param getOption 返回当前生效的 option（可为响应式依赖，变化时自动重绘）
 */
export function useEChart(container: Ref<HTMLElement | undefined>, getOption: () => EChartsCoreOption) {
    let chart: ECharts | null = null
    const handleResize = () => chart?.resize()

    onMounted(() => {
        chart = echarts.init(container.value!)
        chart.setOption(getOption())
        window.addEventListener('resize', handleResize)
    })

    onBeforeUnmount(() => {
        window.removeEventListener('resize', handleResize)
        chart?.dispose()
        chart = null
    })

    const setOption = (option: EChartsCoreOption) => chart?.setOption(option)

    // 当 getOption 内部引用的响应式状态变化时自动重绘
    watch(getOption, () => {
        chart?.setOption(getOption())
    })

    return { setOption, getInstance: () => chart }
}

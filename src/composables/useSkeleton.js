import { onScopeDispose, ref, watch } from 'vue'

/** 骨架出现前先等这么久：这之前就加载完的，骨架根本不露面（与 theme.scss 里 .sk-region 的动画延迟一致） */
export const SKELETON_DELAY = 200
/** 骨架一旦露面，至少停留这么久再换成内容，避免灰块一闪而过 */
export const SKELETON_MIN = 500

/**
 * 首屏骨架的开关（与 Web 端 composables/useSkeleton.ts 同一规则）。传入「还在等数据」的条件，
 * 如 `() => loading.value && !data.value`：
 * - 条件为真时立刻为真；骨架区域自己先隐身 SKELETON_DELAY 再淡入（样式负责）；
 * - 条件变假时，骨架还没露面就立刻为假，直接显示内容；已经露面的，等它露够 SKELETON_MIN 再为假。
 * 模板里用它代替原来的 loading 判断放在 v-if 链的最前面，内容已经到了也先让骨架演完。
 */
export function useSkeleton(waiting) {
  const show = ref(waiting())
  let startedAt = show.value ? Date.now() : 0
  let timer = null

  const clear = () => {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
  }

  watch(waiting, (now) => {
    clear()
    if (now) {
      // 骨架还挂着（在等最短停留）时又开始加载：接着用，不重新计时
      if (!show.value) startedAt = Date.now()
      show.value = true
      return
    }
    const visibleFor = Date.now() - startedAt - SKELETON_DELAY
    const wait = SKELETON_MIN - visibleFor
    if (visibleFor < 0 || wait <= 0) {
      show.value = false
      return
    }
    timer = setTimeout(() => {
      timer = null
      show.value = false
    }, wait)
  })

  onScopeDispose(clear)
  return show
}

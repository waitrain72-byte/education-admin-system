/**
 * 统一加载动画控制（方案 B「追逐圆点」，与 xm-loader 组件配套）：
 * - 计数式开关：并发请求叠加时蒙层只在第一个请求出现、最后一个结束才收起，不会闪烁或提前消失
 * - 最短展示时长：请求过快结束（如本机调试时几毫秒就返回）也保证动画完整可见，
 *   收起动作延迟到凑满 MIN_VISIBLE_MS；排队收起期间若又有新请求，则撤销收起复用当前蒙层
 * - 状态订阅制：加载状态由本模块持有，xm-loader 订阅时立即回调一次当前状态。
 *   页面跳转时 onShow 里的请求往往先于新页面蒙层组件挂载，若用 uni.$emit 广播会丢掉
 *   「显示」事件（表现为进入页面没有过渡动画、蒙层迟到闪烁）；订阅制下组件挂载即同步
 *   已开始的加载状态，彻底消除时序竞态
 */

/** 动画最短展示毫秒数：太短看不见，太长拖节奏；体感"清晰可见但不卡"的常用取值 */
const MIN_VISIBLE_MS = 500

let count = 0
let shownAt = 0
let visible = false
let pendingHideTimer: ReturnType<typeof setTimeout> | null = null

type LoadingListener = (value: boolean) => void
const listeners = new Set<LoadingListener>()

function notify(): void {
  for (const fn of [...listeners]) {
    try {
      fn(visible)
    } catch {
      // 单个订阅者异常不影响其他订阅者
    }
  }
}

/**
 * 订阅加载状态变化，返回取消订阅函数。
 * 订阅时立即回调一次当前状态：新页面蒙层挂载时能立刻接上"已在进行中"的加载。
 */
export function onLoadingChange(fn: LoadingListener): () => void {
  listeners.add(fn)
  fn(visible)
  return () => listeners.delete(fn)
}

export function showLoading(): void {
  count += 1
  // 收起动画还在排队时来了新请求：撤销收起，复用当前蒙层继续转（重新起算最短时长）
  if (pendingHideTimer) {
    clearTimeout(pendingHideTimer)
    pendingHideTimer = null
  }
  if (count === 1) {
    shownAt = Date.now()
    visible = true
    notify()
  }
}

export function hideLoading(): void {
  if (count > 0) count -= 1
  if (count === 0 && !pendingHideTimer) {
    const elapsed = Date.now() - shownAt
    const wait = Math.max(0, MIN_VISIBLE_MS - elapsed)
    pendingHideTimer = setTimeout(() => {
      pendingHideTimer = null
      visible = false
      notify()
    }, wait)
  }
}

/** 页面被整体替换（如 401 踢回登录）时强制复位，避免计数残留导致蒙层永不消失 */
export function resetLoading(): void {
  if (pendingHideTimer) {
    clearTimeout(pendingHideTimer)
    pendingHideTimer = null
  }
  count = 0
  visible = false
  notify()
}

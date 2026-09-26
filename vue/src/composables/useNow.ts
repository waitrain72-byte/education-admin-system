import { ref } from 'vue'

/**
 * 共享的「当前时间」：每分钟刷新一次，全局只有一个定时器。
 * 倒计时这类按天显示的内容依赖它，页面开着跨过零点后会自动更新（「明天」变「今天」），不必刷新页面。
 */
const now = ref(new Date())
let timer: ReturnType<typeof setInterval> | null = null

export function useNow() {
  if (!timer) {
    timer = setInterval(() => {
      now.value = new Date()
    }, 60 * 1000)
  }
  return now
}

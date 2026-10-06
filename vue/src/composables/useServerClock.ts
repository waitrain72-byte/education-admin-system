import { computed, ref } from 'vue'

/**
 * 服务器时间（学校的业务时区）：首页、日程判断「正在上课」「下一节」都用它，不用浏览器本地时间。
 *
 * 学校按北京时间排课，但用户电脑的时区不一定是北京时间（本机就设成了别的时区）：
 * 直接用浏览器时间会把正在上的课判成已结束。这里记下服务器给的「墙上时间」和当时的本地时刻，
 * 之后按本地流逝的时间往前推。墙上时间用 UTC 毫秒数承载，只用 getUTC* 读取，不做任何时区换算。
 */
let baseServer: number | null = null
let baseLocal = 0

/** 每 30 秒跳一次，驱动依赖时间的计算属性重新求值 */
const tick = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

/** 用服务器返回的「yyyy-MM-dd HH:mm」校准；格式不对时忽略 */
export function syncServerClock(serverNow: string | null | undefined): void {
    const m = /^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})/.exec(serverNow || '')
    if (!m) return
    baseServer = Date.UTC(+m[1], +m[2] - 1, +m[3], +m[4], +m[5])
    baseLocal = Date.now()
    tick.value += 1
}

function localWallClock(): number {
    const d = new Date()
    return Date.UTC(d.getFullYear(), d.getMonth(), d.getDate(), d.getHours(), d.getMinutes(), d.getSeconds())
}

/** 当前服务器墙上时间（UTC 承载，读取请用 getUTC*）；未校准时退回本地时间 */
export function serverNow(): Date {
    return new Date(baseServer === null ? localWallClock() : baseServer + (Date.now() - baseLocal))
}

const pad = (n: number) => String(n).padStart(2, '0')

export function formatHHmm(d: Date): string {
    return `${pad(d.getUTCHours())}:${pad(d.getUTCMinutes())}`
}

export function formatDate(d: Date): string {
    return `${d.getUTCFullYear()}-${pad(d.getUTCMonth() + 1)}-${pad(d.getUTCDate())}`
}

/** 响应式的服务器时间：{ date: 'yyyy-MM-dd', hhmm: 'HH:mm' }，每 30 秒更新 */
export function useServerClock() {
    if (!timer && typeof window !== 'undefined') {
        timer = setInterval(() => {
            tick.value += 1
        }, 30 * 1000)
    }
    return computed(() => {
        // 读 tick 建立依赖：定时器和校准都会让它变化
        void tick.value
        const now = serverNow()
        return { date: formatDate(now), hhmm: formatHHmm(now) }
    })
}

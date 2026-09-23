/**
 * 全局错误上报（App.vue 的 onError / onUnhandledRejection / onPageNotFound 调用）。
 *
 * - 优先写入微信「实时日志」：真机上用户遇到的报错可在 小程序后台 → 开发管理 → 运维中心 → 实时日志 查到，
 *   不必让用户复现、也不必连着开发者工具
 * - 其它平台 / 开发环境回退到 console.error
 * - 请求层的业务错误（ApiError）与 401 已经给过用户提示、属于预期流程，不作为异常上报
 * - 同一条错误 10 秒内只上报一次，避免循环报错刷爆日志配额
 */

const DEDUPE_MS = 10000
const recent = new Map()

let logger = null
function getLogger() {
  if (logger !== null) return logger
  try {
    logger = typeof uni.getRealtimeLogManager === 'function' ? uni.getRealtimeLogManager() : false
  } catch {
    logger = false
  }
  return logger
}

function describe(value) {
  if (value == null) return String(value)
  if (value instanceof Error) return `${value.name}: ${value.message}\n${value.stack || ''}`
  if (typeof value === 'object') {
    try {
      return JSON.stringify(value)
    } catch {
      return String(value)
    }
  }
  return String(value)
}

/** 预期内的失败：请求层已提示过用户，不是程序缺陷 */
function isExpected(value) {
  if (!value || typeof value !== 'object') return false
  return value.name === 'ApiError' || value.message === '401'
}

/**
 * 去重键：按「错误类型 + 消息」而不是完整堆栈。
 * 堆栈里含抛出位置的行列号，同一个错误从不同调用点抛出时堆栈各不相同，
 * 若以堆栈去重，循环报错照样会刷爆日志；堆栈本身仍会完整写入日志。
 */
function dedupeKey(kind, value) {
  if (value instanceof Error) return `${kind}|${value.name}|${value.message}`
  return `${kind}|${describe(value).slice(0, 200)}`
}

export function reportError(kind, value) {
  if (isExpected(value)) return false
  const text = describe(value)
  const key = dedupeKey(kind, value)
  const now = Date.now()
  for (const [k, at] of recent) {
    if (now - at > DEDUPE_MS) recent.delete(k)
  }
  if (recent.has(key)) return false
  recent.set(key, now)

  const log = getLogger()
  if (log) {
    try {
      log.error(`[${kind}]`, text)
    } catch {
      // 实时日志不可用时回退控制台
    }
  }
  console.error(`[${kind}]`, value)
  return true
}

/** 仅供单测复位内部状态 */
export function __resetErrorReport() {
  recent.clear()
  logger = null
}

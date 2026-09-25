/**
 * 按账号隔离的本地缓存（首页、课表：进入页面先渲染缓存秒开，弱网 / 断网也有内容，再静默刷新写回）。
 * account 用 userStore.accountKey（「角色-id」）：三类账号分表存储，只用 id 会让 1 号管理员和 1 号学生串数据。
 * 读写失败（存储已满、隐私模式等）静默忽略，页面照常走网络加载。
 */
const keyOf = (name, account) => `xm-${name}-cache-${account}`

/** 读取缓存；没有缓存或读取失败返回 null */
export function readUserCache(name, account) {
  if (!account) return null
  try {
    const value = uni.getStorageSync(keyOf(name, account))
    return value === '' || value === undefined ? null : value
  } catch {
    return null
  }
}

/** 写入缓存（覆盖） */
export function writeUserCache(name, account, data) {
  if (!account) return
  try {
    uni.setStorageSync(keyOf(name, account), data)
  } catch {
    // 存储不可用时仅本次会话无缓存
  }
}

import { isZh } from '@/i18n'

/**
 * 首页用到的日期展示与问候语（头卡、今日课程卡共用），按当前语言输出。
 * 纯函数（时间由调用方传入），便于单测（tests/dateText.spec.js）。
 */

const WEEK_ZH = '日一二三四五六'
const WEEK_EN = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
const MONTH_EN = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

/** 「9月24日 周四」/「Thu, Sep 24」 */
export function dayText(d = new Date()) {
  if (isZh()) return `${d.getMonth() + 1}月${d.getDate()}日 周${WEEK_ZH[d.getDay()]}`
  return `${WEEK_EN[d.getDay()]}, ${MONTH_EN[d.getMonth()]} ${d.getDate()}`
}

/** 按时段返回问候语词条键：5~11 早上 / 11~13 中午 / 13~18 下午 / 其余晚上 */
export function greetingKey(d = new Date()) {
  const h = d.getHours()
  if (h >= 5 && h < 11) return 'home.greetMorning'
  if (h >= 11 && h < 13) return 'home.greetNoon'
  if (h >= 13 && h < 18) return 'home.greetAfternoon'
  return 'home.greetEvening'
}

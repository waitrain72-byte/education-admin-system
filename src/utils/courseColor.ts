/**
 * 课程配色：按课程名哈希从固定色板取色，同一课程稳定同色、不同课程尽量错开。
 * 供首页「今日课程」与「我的课表」周视图共用，保证两处颜色语言一致。
 * 色板与演示稿一致（蓝/绿/橙/紫/天蓝…），不含红色——红色保留给「缺勤/删除」等语义。
 */
const COURSE_COLORS = ['#5b6cff', '#22b866', '#f59e0b', '#06b6d4', '#3b82f6', '#8b5cf6', '#e64980', '#10b981']

export function courseColor(name: string): string {
  const text = String(name || '').split('\n')[0]
  let hash = 0
  for (let i = 0; i < text.length; i += 1) hash = (hash * 31 + text.charCodeAt(i)) >>> 0
  return COURSE_COLORS[hash % COURSE_COLORS.length]
}

/** 软色底 + 同色描边 + 深色文字的色块样式 */
export function courseBlockStyle(text: string): Record<string, string> {
  const color = courseColor(text)
  return { background: color + '24', border: '1rpx solid ' + color + '59', color }
}

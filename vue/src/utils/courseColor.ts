/**
 * 课程封面色：按课程名哈希从固定色板取色，同一门课在首页、课程列表、课程空间、日程里颜色一致。
 *
 * 取色规则与小程序端（mobile 分支 src/utils/courseColor.ts）相同——同样的哈希、同样 8 个色相的顺序，
 * 所以同一门课两端是同一个色系；这里的色值更深一些，因为 Web 端是白字压在色块上，
 * 每个颜色与白字的对比度都 ≥ 5（WCAG AA），深浅主题下都不需要换色。
 * 不含正红：红色留给「缺勤」「逾期」「删除」这类语义。
 */
export const COURSE_COLORS = [
    '#4f5bd5', // 靛
    '#1d7a4a', // 绿
    '#9a5b06', // 琥珀
    '#0e7490', // 青
    '#2563eb', // 蓝
    '#6d48c9', // 紫
    '#b8336a', // 玫
    '#0f7b63', // 玉绿
] as const

/** 字符串哈希（与小程序端同一算法：hash * 31 + charCode，无符号 32 位） */
export function nameHash(name: string): number {
    const text = String(name || '').split('\n')[0]
    let hash = 0
    for (let i = 0; i < text.length; i += 1) {
        hash = (hash * 31 + text.charCodeAt(i)) >>> 0
    }
    return hash
}

export function courseColor(name: string | null | undefined): string {
    return COURSE_COLORS[nameHash(name || '') % COURSE_COLORS.length]
}

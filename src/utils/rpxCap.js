/**
 * rpx 大屏封顶（构建期 PostCSS 插件 + 运行期换算函数共用，本文件不依赖 uni / vue，vite.config.js 可直接引入）。
 *
 * 问题：rpx 按屏宽等比换算（屏宽 / 750），平板、折叠屏、PC 窗口上所有元素会被放大到手机的 2~3 倍，
 * 字号巨大、多列布局也排不下。
 *
 * 做法：为每条含 rpx 的样式规则追加一份 `@media (min-width: 415px)` 覆盖规则，把 rpx 换成
 * 「414px 宽（大屏手机）时的 px 值」：
 * - 手机（≤414px，含 320 小屏到 414 大屏）：仍按 rpx 等比缩放，与原设计完全一致
 * - 更宽的设备：元素尺寸停在大屏手机的水平不再变大，多出来的宽度交给布局（多列、居中、限宽）
 * 只用媒体查询 + px，不依赖 min()/max()，小程序 WXSS 与 H5 都能直接生效；写样式时照常用 rpx 即可。
 */

/** 封顶宽度（px）：超过此宽度后尺寸不再随屏宽放大 */
export const RPX_CAP_WIDTH = 414

const RPX_GLOBAL = /(-?\d*\.?\d+)rpx\b/g
const HAS_RPX = /\drpx\b/

/** 把样式值里的 rpx 换算成封顶宽度下的 px：'calc(24rpx + env(x))' → 'calc(13.25px + env(x))' */
export function rpxToCappedPx(value, capWidth = RPX_CAP_WIDTH) {
  return String(value).replace(RPX_GLOBAL, (_, n) => {
    const px = Math.round(((parseFloat(n) * capWidth) / 750) * 100) / 100
    return px === 0 ? '0' : `${px}px`
  })
}

/**
 * PostCSS 插件：在每条含 rpx 的规则后追加宽屏 px 覆盖规则。
 * - 覆盖规则紧跟原规则，层叠顺序与原样式一致（后写的同名规则在宽屏下依旧后生效）
 * - 原规则已在 @media 里时，与宽屏条件合并成一个 @media（不嵌套，兼容性最好）
 * - @keyframes 里的帧不处理（入场动画的位移量无关布局）
 */
export default function rpxCapPlugin({ capWidth = RPX_CAP_WIDTH } = {}) {
  const query = `(min-width: ${capWidth + 1}px)`
  return {
    postcssPlugin: 'xm-rpx-cap',
    Once(root, { AtRule }) {
      // 同一个 @media 里有多条规则时，生成的覆盖块依次排在它后面，保持相对顺序
      const lastInserted = new Map()
      root.walkRules((rule) => {
        const parent = rule.parent
        if (parent.type === 'atrule' && /keyframes$/i.test(parent.name)) return
        const decls = rule.nodes.filter((n) => n.type === 'decl' && HAS_RPX.test(n.value))
        if (!decls.length) return
        const clone = rule.clone()
        clone.removeAll()
        for (const d of decls) clone.append(d.clone({ value: rpxToCappedPx(d.value, capWidth) }))
        if (parent.type === 'atrule' && parent.name === 'media') {
          const media = new AtRule({ name: 'media', params: `${parent.params} and ${query}` })
          media.append(clone)
          ;(lastInserted.get(parent) || parent).after(media)
          lastInserted.set(parent, media)
        } else if (parent.type === 'root') {
          const media = new AtRule({ name: 'media', params: query })
          media.append(clone)
          rule.after(media)
        }
      })
    },
  }
}
rpxCapPlugin.postcss = true

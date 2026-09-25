import { describe, expect, it } from 'vitest'
import postcss from 'postcss'
import rpxCapPlugin, { rpxToCappedPx, RPX_CAP_WIDTH } from '@/utils/rpxCap'

const run = (css) => postcss([rpxCapPlugin()]).process(css, { from: undefined }).css

describe('rpxToCappedPx 换算', () => {
  it('按 414px 宽换算，保留两位小数，支持 calc 与负值', () => {
    expect(RPX_CAP_WIDTH).toBe(414)
    expect(rpxToCappedPx('24rpx')).toBe('13.25px')
    expect(rpxToCappedPx('calc(24rpx + env(safe-area-inset-bottom))')).toBe(
      'calc(13.25px + env(safe-area-inset-bottom))',
    )
    expect(rpxToCappedPx('0 6rpx 24rpx rgba(0,0,0,.1)')).toBe('0 3.31px 13.25px rgba(0,0,0,.1)')
    expect(rpxToCappedPx('-6rpx')).toBe('-3.31px')
    expect(rpxToCappedPx('0rpx 16rpx')).toBe('0 8.83px')
  })
})

describe('rpxCapPlugin PostCSS 插件', () => {
  it('原规则不动，紧跟一条只含 rpx 声明的宽屏 px 覆盖规则', () => {
    const out = run('.a{padding:24rpx;color:red}.b{margin:8px}')
    expect(out).toBe('.a{padding:24rpx;color:red}@media (min-width: 415px){.a{padding:13.25px}}.b{margin:8px}')
  })

  it('保留 !important', () => {
    expect(run('.a{width:100rpx !important}')).toContain('@media (min-width: 415px){.a{width:55.2px !important}}')
  })

  it('已在 @media 里的规则：与宽屏条件合并，且保持多条规则的先后顺序', () => {
    const out = run('@media (min-width: 960px){.a{right:40rpx}.b{left:20rpx}}')
    expect(out).toBe(
      '@media (min-width: 960px){.a{right:40rpx}.b{left:20rpx}}' +
        '@media (min-width: 960px) and (min-width: 415px){.a{right:22.08px}}' +
        '@media (min-width: 960px) and (min-width: 415px){.b{left:11.04px}}',
    )
  })

  it('@keyframes 不处理；不含 rpx 的规则不生成覆盖', () => {
    const css = '@keyframes up{from{transform:translateY(16rpx)}to{transform:none}}.x{color:red}'
    expect(run(css)).toBe(css)
  })
})

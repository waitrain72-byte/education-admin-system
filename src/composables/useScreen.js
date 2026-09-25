import { ref } from 'vue'
import { RPX_CAP_WIDTH, rpxToCappedPx } from '@/utils/rpxCap'

/**
 * 屏幕宽度（响应式）与运行期 rpx 换算：给 JS 里拼出来的内联尺寸（图标大小、课表行高等）用，
 * 规则与构建期的 rpx 封顶插件一致——手机按 rpx 等比，超过 414px 宽改用固定 px。
 * iPad 旋转、PC 端拖动窗口（app.json resizable）时通过 onWindowResize 实时更新。
 */
function readWindowWidth() {
  try {
    const info = typeof uni.getWindowInfo === 'function' ? uni.getWindowInfo() : uni.getSystemInfoSync()
    return (info && info.windowWidth) || 375
  } catch {
    return 375
  }
}

export const windowWidth = ref(readWindowWidth())

if (typeof uni.onWindowResize === 'function') {
  uni.onWindowResize((res) => {
    const w = res && res.size && res.size.windowWidth
    if (w) windowWidth.value = w
  })
}

/** 运行期 rpx：rpx(28) → 手机上 '28rpx'，宽屏上 '15.46px' */
export function rpx(n) {
  return windowWidth.value > RPX_CAP_WIDTH ? rpxToCappedPx(`${n}rpx`) : `${n}rpx`
}

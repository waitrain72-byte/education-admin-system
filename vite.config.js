import { defineConfig, loadEnv } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import rpxCapPlugin from './src/utils/rpxCap.js'

/**
 * 发布前的接口地址校验。
 *
 * 微信小程序正式发布强制要求：接口必须是 HTTPS + 已备案域名，并在「小程序后台 → 开发管理 →
 * 服务器域名」登记。局域网 IP / HTTP 地址只能在开发者工具勾选「不校验合法域名」时预览。
 *
 * - 平时构建：地址不合规时打印醒目警告，但不中断（.env.production 默认用局域网 IP，便于真机预览生产包）
 * - 正式发布：`RELEASE=1 npm run build:mp-weixin`，地址不合规则直接构建失败，避免把预览配置发出去
 */
function releaseUrlGuard() {
  let problems = []
  return {
    name: 'release-url-guard',
    configResolved(config) {
      if (config.command !== 'build') return
      const env = loadEnv(config.mode, process.cwd(), 'VITE_')
      const url = env.VITE_API_BASE_URL || ''
      problems = []
      if (!/^https:\/\//i.test(url)) problems.push('不是 HTTPS')
      if (/\/\/(localhost|127\.|10\.|192\.168\.|172\.(1[6-9]|2\d|3[01])\.)/i.test(url))
        problems.push('是局域网 / 本机地址')
      if (/:\d+(\/|$)/.test(url.replace(/^https?:\/\//i, '//'))) problems.push('带了端口号（正式域名一般走 443）')
      if (!problems.length) return

      const lines = [
        `接口地址 VITE_API_BASE_URL = ${url || '(未设置)'}`,
        `问题：${problems.join('、')}`,
        '这个包只能在开发者工具勾选「不校验合法域名」时预览，不能提交审核发布。',
        '正式发布前：把 .env.production 改成已备案的 https 域名，并在小程序后台登记服务器域名。',
      ]
      if (process.env.RELEASE === '1') {
        throw new Error('[release-url-guard] RELEASE=1 下拒绝构建：\n  ' + lines.join('\n  '))
      }
      const bar = '='.repeat(72)
      console.warn(`\n\x1b[33m${bar}\n  ⚠  发布前必改\n  ${lines.join('\n  ')}\n${bar}\x1b[0m\n`)
    },
  }
}

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [uni(), releaseUrlGuard()],
  css: {
    postcss: {
      // 大屏适配：rpx 超过 414px 屏宽后改用固定 px，平板 / 折叠屏 / PC 上不再被放大（见 src/utils/rpxCap.js）。
      // 与 uni-app 自带的 PostCSS 插件合并执行，后者在最后一步再处理剩余的 rpx（H5 转 rem，小程序保留 rpx）
      plugins: [rpxCapPlugin()],
    },
  },
})

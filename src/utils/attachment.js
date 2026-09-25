import { resolveFileUrl } from '@/utils/request'

/**
 * 附件的展示与查看（作业附件；后端存的是 /files/xxx 或历史的绝对地址）。
 */

/** 附件地址里的文件名（中文名解码显示）；文件名含不成对的 % 等非法转义时原样返回，避免列表渲染报错 */
export function fileNameOf(url) {
  if (!url) return ''
  const path = String(url).split('?')[0]
  const name = path.slice(path.lastIndexOf('/') + 1)
  try {
    return decodeURIComponent(name) || url
  } catch {
    return name || url
  }
}

/** 是否图片附件（按扩展名判断，允许带查询串） */
export const isImageFile = (url) => /\.(png|jpe?g|gif|webp|bmp)(\?.*)?$/i.test(String(url || ''))

/**
 * 查看附件：图片直接预览；其余下载后用系统文档打开（PDF / Word 等），
 * 下载或打开失败时把地址复制到剪贴板，方便用户到浏览器里打开。
 */
export function openAttachment(rawUrl) {
  // 历史数据存的是老绝对地址：先归一成当前 baseUrl 的完整地址（真机才能访问）
  const url = resolveFileUrl(rawUrl)
  if (!url) return
  if (isImageFile(url)) {
    uni.previewImage({ urls: [url] })
    return
  }
  uni.downloadFile({
    url,
    success: (res) => {
      if (res.statusCode === 200) {
        uni.openDocument({
          filePath: res.tempFilePath,
          showMenu: true,
          fail: () => uni.setClipboardData({ data: url }),
        })
      } else {
        uni.setClipboardData({ data: url })
      }
    },
    fail: () => {
      uni.setClipboardData({ data: url })
    },
  })
}

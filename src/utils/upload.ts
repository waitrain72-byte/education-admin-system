import { baseUrl } from './config'
import { useUserStore } from '@/stores/user'
import { t } from '@/i18n'
import { hideLoading, showLoading } from './loading'
import { handleUnauthorized, toApiError, toastRequestFail } from './request'

/**
 * 文件选择与上传（头像 / 作业附件共用，替代原先 5 个页面各自复制的 uni.uploadFile 代码）：
 * - 与请求层同一套契约：200 解包返回文件地址，业务失败提示并 reject ApiError，401 统一踢回登录页
 * - 上传前按后端限制（20MB、扩展名白名单）先在本地拦截，避免传完才失败
 * - 支持进度回调（作业附件按钮上显示百分比）
 */

/** 与后端 spring.servlet.multipart.max-file-size 一致 */
export const MAX_UPLOAD_MB = 20

/** 聊天文件可选的文档类型：与后端 FileController 的扩展名白名单一致（图片走相册入口） */
export const DOC_EXTENSIONS = ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'txt', 'md', 'zip', 'rar', '7z']

export interface PickedFile {
  path: string
  size: number
}

const isCancel = (err: any): boolean => /cancel/i.test((err && err.errMsg) || '')

/** 超过大小限制时提示并返回 true */
function tooLarge(size: number): boolean {
  if (size > MAX_UPLOAD_MB * 1024 * 1024) {
    uni.showToast({ title: t('request.tooLarge', { mb: MAX_UPLOAD_MB }), icon: 'none' })
    return true
  }
  return false
}

/** 从相册 / 相机选一张图片（压缩图）；用户取消或超限时 resolve null */
export function chooseImage(): Promise<PickedFile | null> {
  return new Promise((resolve) => {
    uni.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      success: (res: any) => {
        const file = (res.tempFiles && res.tempFiles[0]) || { path: res.tempFilePaths[0], size: 0 }
        resolve(tooLarge(file.size) ? null : { path: file.path, size: file.size })
      },
      fail: (err: any) => {
        if (!isCancel(err)) uni.showToast({ title: t('request.failed'), icon: 'none' })
        resolve(null)
      },
    })
  })
}

/** 当前平台能否从微信聊天记录选文件（仅微信小程序提供 chooseMessageFile） */
export const canChooseChatFile = (): boolean => typeof (uni as any).chooseMessageFile === 'function'

/** 从微信聊天记录选一个文档（PDF / Word / Excel …）；用户取消或超限时 resolve null */
export function chooseChatFile(): Promise<PickedFile | null> {
  return new Promise((resolve) => {
    ;(uni as any).chooseMessageFile({
      count: 1,
      type: 'file',
      extension: DOC_EXTENSIONS,
      success: (res: any) => {
        const file = res.tempFiles[0]
        resolve(tooLarge(file.size) ? null : { path: file.path, size: file.size })
      },
      fail: (err: any) => {
        if (!isCancel(err)) uni.showToast({ title: t('request.failed'), icon: 'none' })
        resolve(null)
      },
    })
  })
}

export interface UploadOptions {
  /** 是否显示统一加载动画，默认开启；页面自己展示进度时可关闭 */
  loading?: boolean
  /** 上传进度 0~100 */
  onProgress?: (percent: number) => void
}

/** 上传到 /files/upload，resolve 后端返回的文件地址（原样存库，展示时再用 resolveFileUrl 归一） */
export function uploadFile(filePath: string, options: UploadOptions = {}): Promise<string> {
  return new Promise((resolve, reject) => {
    const userStore = useUserStore()
    const withLoader = options.loading !== false
    if (withLoader) showLoading()
    const task: any = uni.uploadFile({
      url: `${baseUrl}/files/upload`,
      filePath,
      name: 'file',
      header: userStore.token ? { token: userStore.token } : {},
      success: (res: any) => {
        let data: any = null
        try {
          data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
        } catch {
          // 非 JSON 响应（如网关 413 页面）按上传失败处理
        }
        if (data && data.code === '401') {
          handleUnauthorized(data)
          reject(new Error('401'))
          return
        }
        if (data && data.code === '200') {
          resolve(data.data)
          return
        }
        if (data && data.code) {
          reject(toApiError(data))
          return
        }
        uni.showToast({ title: t('request.uploadFailed'), icon: 'none' })
        reject(new Error('upload failed: HTTP ' + res.statusCode))
      },
      fail: (err: any) => {
        toastRequestFail(err)
        reject(err)
      },
      complete: () => {
        if (withLoader) hideLoading()
      },
    })
    if (options.onProgress && task && typeof task.onProgressUpdate === 'function') {
      task.onProgressUpdate((p: any) => options.onProgress!(p.progress))
    }
  })
}

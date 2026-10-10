import { baseUrl } from './config'
import { useUserStore } from '@/stores/user'
import { t } from '@/i18n'
import { hideLoading, showLoading } from './loading'
import { handleUnauthorized, toApiError, toastRequestFail } from './request'

/**
 * 文件选择与上传（头像、作业附件、课程资料共用）：
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
  /** 原文件名（作业附件、课程资料按原名显示；相册图片没有原名时取临时路径的文件名） */
  name: string
}

/** 临时路径里的文件名（相册图片没有原文件名时用） */
const baseName = (path: string): string =>
  String(path || '')
    .split(/[\\/]/)
    .pop() || ''

const isCancel = (err: any): boolean => /cancel/i.test((err && err.errMsg) || '')

/** 超过大小限制时提示并返回 true */
function tooLarge(size: number): boolean {
  if (size > MAX_UPLOAD_MB * 1024 * 1024) {
    uni.showToast({ title: t('request.tooLarge', { mb: MAX_UPLOAD_MB }), icon: 'none' })
    return true
  }
  return false
}

/**
 * 调起一个选择接口，返回选中的文件（超限的提示后剔除；用户取消返回空数组）。
 * - chooseImage：相册 / 相机（压缩图），所有端都有
 * - chooseMessageFile：微信聊天记录里的文件，只有微信小程序有
 * - chooseFile：本机文件，只有 H5 / App 有
 */
function pickFrom(api: 'chooseImage' | 'chooseMessageFile' | 'chooseFile', count: number): Promise<PickedFile[]> {
  return new Promise((resolve) => {
    const options: Record<string, any> = {
      count,
      success: (res: any) => {
        const files: any[] =
          res.tempFiles && res.tempFiles.length
            ? res.tempFiles
            : (res.tempFilePaths || []).map((path: string) => ({ path, size: 0 }))
        resolve(
          files
            .filter((file) => !tooLarge(file.size))
            .map((file) => ({ path: file.path, size: file.size, name: file.name || baseName(file.path) })),
        )
      },
      fail: (err: any) => {
        if (!isCancel(err)) uni.showToast({ title: t('request.failed'), icon: 'none' })
        resolve([])
      },
    }
    if (api === 'chooseImage') options.sizeType = ['compressed']
    if (api === 'chooseMessageFile') {
      options.type = 'file'
      options.extension = DOC_EXTENSIONS
    }
    ;(uni as any)[api](options)
  })
}

const first = (list: PickedFile[]): PickedFile | null => list[0] || null

/** 从相册 / 相机选一张图片（压缩图）；用户取消或超限时 resolve null */
export const chooseImage = (): Promise<PickedFile | null> => pickFrom('chooseImage', 1).then(first)

/** 当前平台能否从微信聊天记录选文件（仅微信小程序提供 chooseMessageFile） */
export const canChooseChatFile = (): boolean => typeof (uni as any).chooseMessageFile === 'function'

/** 当前平台能否从本机选文件（H5 / App 提供 chooseFile；微信小程序没有，只能从聊天记录选） */
export const canChooseDeviceFile = (): boolean => typeof (uni as any).chooseFile === 'function'

/**
 * 选附件（作业、课程资料）：列出当前平台能用的来源让用户挑——
 * 微信小程序是「聊天记录里的文件 / 相册图片」，H5 是「本机文件 / 相册图片」。
 * 只有一个来源时直接打开；用户取消时返回空数组。count 是最多选几个。
 */
export function pickAttachments(count = 1): Promise<PickedFile[]> {
  const sources: Array<{ label: string; api: 'chooseImage' | 'chooseMessageFile' | 'chooseFile' }> = []
  if (canChooseChatFile()) sources.push({ label: t('mobile.file.fromChat'), api: 'chooseMessageFile' })
  if (canChooseDeviceFile()) sources.push({ label: t('mobile.file.fromDevice'), api: 'chooseFile' })
  sources.push({ label: t('mobile.file.fromAlbum'), api: 'chooseImage' })
  if (sources.length === 1) return pickFrom(sources[0].api, count)
  return new Promise((resolve) => {
    uni.showActionSheet({
      itemList: sources.map((s) => s.label),
      success: (res: any) => resolve(pickFrom(sources[res.tapIndex].api, count)),
      fail: () => resolve([]),
    })
  })
}

/** 选一个附件；用户取消时 resolve null */
export const pickAttachment = (): Promise<PickedFile | null> => pickAttachments(1).then(first)

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

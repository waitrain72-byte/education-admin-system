import request from '@/utils/request'

/**
 * Blob 文件下载组合式函数：
 * 统一封装「请求 → 生成 blob URL → <a download> 触发下载 → 回收 URL」这一段重复逻辑。
 * 适用于 Excel 导出、模板下载等场景。
 *
 * 用法：
 *   const { download } = useDownload('/student/export')
 *   download('学生列表.xlsx')
 */
/** 导出超时：大表导出远超默认的 10 秒，单独放宽到 60 秒 */
const EXPORT_TIMEOUT = 60000

export function useDownload(url: string) {
    const download = async (filename: string, params?: Record<string, any>) => {
        // 二进制响应没有 { code, msg, data } 包装，拦截器原样返回 Blob
        const blob = await request.get<Blob>(url, { params, responseType: 'blob', timeout: EXPORT_TIMEOUT })
        const blobUrl = URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = blobUrl
        link.download = filename
        link.click()
        URL.revokeObjectURL(blobUrl)
    }

    return { download }
}

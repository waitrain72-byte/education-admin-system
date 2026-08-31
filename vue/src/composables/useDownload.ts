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
export function useDownload(url: string) {
    const download = async (filename: string, params?: Record<string, any>) => {
        const res: any = await request.get(url, { params, responseType: 'blob' })
        const blobUrl = URL.createObjectURL(res.data)
        const link = document.createElement('a')
        link.href = blobUrl
        link.download = filename
        link.click()
        URL.revokeObjectURL(blobUrl)
    }

    return { download }
}

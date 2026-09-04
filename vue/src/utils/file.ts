/**
 * 文件/头像地址归一化：把历史遗留的绝对地址统一转成走代理的相对路径 /api/files/xxx。
 *
 * 背景：数据库里部分老数据存的是完整地址（如 http://localhost:9091/files/xxx.png），
 * 其中文件名可能与磁盘不一致，且换部署环境后主机部分必然失效；
 * 新上传返回的则是 /api/files/xxx（开发环境由 Vite 代理、生产由 nginx 反代到后端 /files/）。
 * 统一归一后，同一份数据在任何环境都能正确加载，localStorage 中的旧用户缓存也能正常显示。
 *
 * 仅用于展示层；上传/保存时仍提交后端返回的原始值。
 */
export function resolveFileUrl(url?: string): string {
    if (!url) return ''
    const fileIndex = url.indexOf('/files/')
    if (/^https?:\/\//i.test(url)) {
        // 老绝对地址：剥掉协议与主机，统一挂到 /api 前缀走代理
        return fileIndex >= 0 ? '/api' + url.slice(fileIndex) : url
    }
    if (url.startsWith('/api/files/')) {
        return url
    }
    if (url.startsWith('/files/')) {
        return '/api' + url
    }
    return url
}

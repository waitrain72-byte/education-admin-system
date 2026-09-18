import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { resolve } from 'path'
import { existsSync, readdirSync, readFileSync } from 'node:fs'

/**
 * 建立「Element Plus 组件名 → es/components 下的目录名」映射。
 *
 * 不能简单用 kebab-case 推导：约 30 个子组件（ElMenuItem、ElTableColumn、ElOption、
 * ElFormItem、ElStep、ElAside、ElDropdownItem…）并没有自己的目录，而是由父组件目录
 * 一并导出。这里直接扫描 element-plus 的导出声明，得到权威且随版本自适应的映射。
 */
function buildElementPlusComponentMap(): Map<string, string> {
    const map = new Map<string, string>()
    const base = resolve(__dirname, 'node_modules/element-plus/es/components')
    if (!existsSync(base)) return map
    for (const dir of readdirSync(base)) {
        const entry = resolve(base, dir, 'index.mjs')
        if (!existsSync(entry)) continue
        const source = readFileSync(entry, 'utf8')
        for (const match of source.matchAll(/export\s*\{([^}]*)\}/g)) {
            for (const spec of match[1].split(',')) {
                // 形如 `vLoading as ElLoadingDirective` 的别名取导出名
                const exported = spec.trim().split(/\s+as\s+/).pop()?.trim()
                if (exported && /^El[A-Z]/.test(exported) && !map.has(exported)) {
                    map.set(exported, dir)
                }
            }
        }
    }
    return map
}

const elementPlusComponentMap = buildElementPlusComponentMap()

/**
 * Element Plus 组件解析器：把模板里的 <el-xxx> 解析为**深导入**，而不是官方
 * ElementPlusResolver 使用的桶导入 `element-plus/es`。
 *
 * 为什么必须自己写：桶入口会连带引入 defaults.mjs / make-installer.mjs，它们在该
 * 模块顶层调用 makeInstaller([...全部组件])。这些模块一旦被 manualChunks 归入具名
 * chunk，Rollup 就必须把整个 chunk 视作入口完整保留，无法 tree-shake —— 实测产物里
 * 因此出现 145 个组件定义（含项目从未使用的 ElCascader/ElTransfer/ElTour/ElWatermark），
 * element-plus chunk 高达 823 KB。改用深导入后桶入口根本不进入依赖图，
 * tree-shaking 与下面的分块策略都能正常工作。
 *
 * 映射表由 element-plus 自身导出声明扫描得到，升级 Element Plus 无需手工维护。
 */
function deepElementPlusResolver() {
    return {
        type: 'component' as const,
        resolve: (name: string) => {
            // ElIconXxx → @element-plus/icons-vue（与官方 resolver 行为一致）
            if (/^ElIcon.+/.test(name)) {
                return { name: name.replace(/^ElIcon/, ''), from: '@element-plus/icons-vue' }
            }
            const dir = elementPlusComponentMap.get(name)
            if (!dir) return
            return { name, from: `element-plus/es/components/${dir}/index` }
        },
    }
}

/**
 * 指令同样走深导入：否则 v-loading 会把 element-plus/es 桶入口重新拉回依赖图，
 * 前面的组件解析就白做了。
 */
function deepElementPlusDirectiveResolver() {
    const directives: Record<string, { importName: string; dir: string }> = {
        Loading: { importName: 'ElLoadingDirective', dir: 'loading' },
        Popover: { importName: 'ElPopoverDirective', dir: 'popover' },
        InfiniteScroll: { importName: 'ElInfiniteScroll', dir: 'infinite-scroll' },
    }
    return {
        type: 'directive' as const,
        resolve: (name: string) => {
            const key = name.replace(/^v/, '')
            const directive = directives[key]
            if (!directive) return
            return {
                name: directive.importName,
                from: `element-plus/es/components/${directive.dir}/index`,
            }
        },
    }
}

export default defineConfig({
    plugins: [
        vue(),
        // Element Plus 组件/指令按需自动引入；解析到深导入，避免全量打包（见上方说明）
        Components({
            resolvers: [deepElementPlusResolver(), deepElementPlusDirectiveResolver()],
        }),
    ],
    resolve: {
        alias: {
            '@': resolve(__dirname, 'src'),
        },
    },
    server: {
        port: 8080,
        proxy: {
            '/api': {
                target: 'http://localhost:9091',
                changeOrigin: true,
                // WebSocket 也要经此转发，否则 /api/ws/notice/{token} 无法连接（实时通知失效）
                ws: true,
                rewrite: (path) => path.replace(/^\/api/, ''),
            },
        },
    },
    build: {
        // 修复深导入后最大的 chunk 是 vue 全家桶（约 528KB / gzip 187KB），阈值按此设定
        chunkSizeWarningLimit: 600,
        rollupOptions: {
            output: {
                // 将大型第三方依赖拆分为独立 chunk，利于浏览器缓存与并行加载
                manualChunks(id) {
                    if (!id.includes('node_modules')) return undefined
                    if (id.includes('echarts')) return 'echarts'
                    if (id.includes('element-plus') || id.includes('@element-plus')) return 'element-plus'
                    if (id.includes('vue')) return 'vue'
                    if (id.includes('axios')) return 'axios'
                    return 'vendor'
                },
            },
        },
    },
})

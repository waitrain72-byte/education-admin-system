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
 * 收集 es/components 下所有自带样式入口（style/css）的目录名。
 *
 * 样式目录与 JS 目录不是一回事：像 ElBreadcrumbItem / ElTabPane 这类子组件，
 * **JS 由父目录导出**（breadcrumb/index、tabs/index，所以上面的映射表是对的），
 * 但**样式有自己的目录**（breadcrumb-item/style/css、tab-pane/style/css）。
 * 若样式路径直接复用 JS 目录，这些子组件的样式就会漏掉（面包屑分隔符、标签页内容区掉样式）。
 */
function buildElementPlusStyleDirs(): Set<string> {
    const dirs = new Set<string>()
    const base = resolve(__dirname, 'node_modules/element-plus/es/components')
    if (!existsSync(base)) return dirs
    for (const dir of readdirSync(base)) {
        if (existsSync(resolve(base, dir, 'style/css.mjs'))) {
            dirs.add(dir)
        }
    }
    return dirs
}

const elementPlusStyleDirs = buildElementPlusStyleDirs()

/** ElBreadcrumbItem → breadcrumb-item */
function kebabOf(componentName: string): string {
    return componentName
        .replace(/^El/, '')
        .replace(/([a-z0-9])([A-Z])/g, '$1-$2')
        .toLowerCase()
}

/**
 * 组件对应的样式入口：优先用与组件名同名的样式目录（覆盖子组件独立样式的情况），
 * 没有才回退到 JS 所在目录。都没有样式目录时返回 undefined（如无渲染的 ElConfigProvider）。
 */
function styleEntryOf(componentName: string, jsDir: string): string | undefined {
    const own = kebabOf(componentName)
    const dir = elementPlusStyleDirs.has(own) ? own : jsDir
    return elementPlusStyleDirs.has(dir) ? `element-plus/es/components/${dir}/style/css` : undefined
}

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
 *
 * `sideEffects` 让 unplugin 顺带注入该组件的样式入口，从而 CSS 也按需引入：
 * 否则就只能在 main.ts 里全量 import element-plus/dist/index.css（355 KB 首屏加载），
 * JS 侧的 tree-shaking 成果会被 CSS 抵消一半。
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
            return {
                name,
                from: `element-plus/es/components/${dir}/index`,
                sideEffects: styleEntryOf(name, dir),
            }
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
                sideEffects: `element-plus/es/components/${directive.dir}/style/css`,
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

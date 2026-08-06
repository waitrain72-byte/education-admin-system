import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { resolve } from 'path'

export default defineConfig({
    plugins: [
        vue(),
        // Element Plus 组件按需自动引入，避免全量注册带来的包体积浪费
        Components({
            resolvers: [ElementPlusResolver({ importStyle: false })],
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
                rewrite: (path) => path.replace(/^\/api/, ''),
            },
        },
    },
    build: {
        // element-plus 独立分块约 820KB（gzip 260KB），按需拆分后利于缓存，故上调警告阈值
        chunkSizeWarningLimit: 900,
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

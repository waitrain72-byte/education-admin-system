/**
 * 构建后处理：把 dist/build/mp-weixin 下编译生成的 .wxml / .js / .wxss
 * 展开成多行缩进格式，便于在微信开发者工具里阅读（uni-app 构建默认压缩）。
 * 通过 package.json 中 build:mp-weixin 命令在构建后自动执行。
 *
 * 说明：只影响阅读体验，不影响功能；产物重新生成后由构建命令自动重新执行。
 */
const fs = require('fs')
const path = require('path')
const { html, js, css } = require('js-beautify')

const root = path.join(__dirname, 'dist', 'build', 'mp-weixin')
if (!fs.existsSync(root)) {
    console.log('dist/build/mp-weixin 不存在，跳过格式化')
    process.exit(0)
}

const formatters = {
    '.wxml': (code) =>
        html(code, {
            indent_size: 2,
            preserve_newlines: true,
            max_preserve_newlines: 2,
            wrap_line_length: 0,
            // input/button 等默认被视为内联元素而挤在一行，置空后所有标签都按块级换行
            inline: [],
            unformatted: [],
            content_unformatted: ['text'],
        }),
    '.js': (code) => js(code, { indent_size: 2, preserve_newlines: true, max_preserve_newlines: 2, wrap_line_length: 0 }),
    '.wxss': (code) => css(code, { indent_size: 2 }),
}

const counts = { '.wxml': 0, '.js': 0, '.wxss': 0 }

function walk(dir) {
    for (const name of fs.readdirSync(dir)) {
        const p = path.join(dir, name)
        const st = fs.statSync(p)
        if (st.isDirectory()) walk(p)
        else {
            const ext = path.extname(name)
            const format = formatters[ext]
            if (!format) continue
            const before = fs.readFileSync(p, 'utf8')
            const after = format(before)
            if (after !== before) {
                fs.writeFileSync(p, after, 'utf8')
                counts[ext]++
            }
        }
    }
}

walk(root)
console.log('formatted:', counts['.wxml'] + ' wxml, ' + counts['.js'] + ' js, ' + counts['.wxss'] + ' wxss')

/**
 * 在 .vue 模板根节点的直接子元素之间插入空行（视觉分块），
 * 嵌套层级与内联标签一律不动。
 * 用 vue/compiler-sfc 解析定位，比正则安全；插入后跑 Prettier 归一化缩进。
 */
const fs = require('fs')
const path = require('path')
const { parse } = require('vue/compiler-sfc')

const root = path.join(__dirname, 'src')

function walk(dir, files) {
    for (const name of fs.readdirSync(dir)) {
        const p = path.join(dir, name)
        const st = fs.statSync(p)
        if (st.isDirectory()) walk(p, files)
        else if (name.endsWith('.vue')) files.push(p)
    }
}

const files = []
walk(root, files)
let touched = 0

for (const file of files) {
    const source = fs.readFileSync(file, 'utf8')
    let descriptor
    try {
        descriptor = parse(source, { filename: file }).descriptor
    } catch {
        continue
    }
    const tpl = descriptor.template
    if (!tpl || !tpl.ast) continue

    // 模板根元素（xm-page）的直接子元素节点
    const rootEl = tpl.ast.children.find((n) => n.type === 1)
    if (!rootEl || !rootEl.children) continue

    const elementChildren = rootEl.children.filter((n) => n.type === 1)
    if (elementChildren.length < 2) continue

    // 从后往前插，避免偏移量失效；相邻子元素之间若无空行则插入一个空行
    const insertions = []
    for (let i = 1; i < elementChildren.length; i++) {
        const prevEnd = elementChildren[i - 1].loc.end.offset
        const start = elementChildren[i].loc.start.offset
        const gap = source.slice(prevEnd, start)
        if (!gap.includes('\n\n')) insertions.push(start)
    }
    if (!insertions.length) continue

    let result = source
    for (const offset of insertions.reverse()) {
        result = result.slice(0, offset) + '\n' + result.slice(offset)
    }
    fs.writeFileSync(file, result, 'utf8')
    touched++
    console.log(path.relative(root, file) + ' +' + insertions.length)
}
console.log('files updated: ' + touched)

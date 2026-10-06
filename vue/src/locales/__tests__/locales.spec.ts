import { describe, it, expect } from 'vitest'
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join, resolve } from 'node:path'
import { localeMessages } from '@/i18n'

/** 消息树的全部叶子键（a.b.c） */
function leafKeys(obj: Record<string, any>, prefix = ''): string[] {
  return Object.entries(obj).flatMap(([key, value]) => {
    const path = prefix ? `${prefix}.${key}` : key
    return value && typeof value === 'object' ? leafKeys(value, path) : [path]
  })
}

function hasKey(obj: Record<string, any>, path: string): boolean {
  let node: any = obj
  for (const part of path.split('.')) {
    if (!node || typeof node !== 'object' || !(part in node)) return false
    node = node[part]
  }
  return typeof node === 'string'
}

function sourceFiles(dir: string): string[] {
  return readdirSync(dir).flatMap((name) => {
    const full = join(dir, name)
    if (statSync(full).isDirectory()) return name === '__tests__' || name === 'locales' ? [] : sourceFiles(full)
    return /\.(vue|ts)$/.test(name) ? [full] : []
  })
}

describe('语言包', () => {
  it('中英文的键完全一致（少翻一条就会在界面上露出键名）', () => {
    const zh = leafKeys(localeMessages['zh-CN']).sort()
    const en = leafKeys(localeMessages['en-US']).sort()
    expect(zh.filter((k) => !en.includes(k))).toEqual([])
    expect(en.filter((k) => !zh.includes(k))).toEqual([])
  })

  it('代码里用字面量写的 t / $t / i18n 键都在语言包里', () => {
    // 本文件在 vue/src/locales/__tests__ 下，往上三级是 vue/
    const root = resolve(__dirname, '../../..')
    const missing: string[] = []
    let checked = 0
    // 只检查写死的整串键，如 $t('nav.home')；拼接出来的键（'shell.roles.' + role）跳过
    const pattern = /(?:\$t|\bt)\(\s*'([a-zA-Z][\w-]*(?:\.[\w-]+)+)'\s*[,)]/g
    // 路由 meta.name / 导航配置里的键：label: 'nav.home'、name: 'menu.course'
    const metaPattern = /(?:label|name):\s*'((?:nav|menu|shell|space|admin|workbench|pages|layout|courses)\.[\w.-]+)'/g
    for (const file of sourceFiles(resolve(root, 'src'))) {
      const text = readFileSync(file, 'utf8')
      for (const re of [pattern, metaPattern]) {
        for (const match of text.matchAll(re)) {
          checked += 1
          if (!hasKey(localeMessages['zh-CN'], match[1])) {
            missing.push(`${file.replace(root, '')}: ${match[1]}`)
          }
        }
      }
    }
    // 防止正则失效后「什么都没查到」也算通过
    expect(checked).toBeGreaterThan(300)
    expect(missing).toEqual([])
  })
})

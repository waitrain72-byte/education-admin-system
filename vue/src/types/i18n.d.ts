// vue-i18n globalInjection 注入的全局 $t：
// 部分语言的 TS 服务（如 IDEA 的 Vue 插件）读不到 vue-i18n 自带的类型增强，
// 这里显式声明一遍，让模板中的 $t 在所有编辑器里都有类型提示、
// 不报 "Property $t does not exist on type ..."。
// 注意：本文件必须保持为模块（含 import），且不要与 '*.vue' 通配声明放在同一文件，
// 否则会使该通配声明退化为模块增强而失效。
import type { Composer } from 'vue-i18n'

type GlobalTranslate = Composer['t']

declare module 'vue' {
    interface ComponentCustomProperties {
        $t: GlobalTranslate
    }
}

declare module '@vue/runtime-core' {
    interface ComponentCustomProperties {
        $t: GlobalTranslate
    }
}

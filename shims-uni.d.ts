/// <reference types='@dcloudio/types' />
/// <reference types='vite/client' />

export {}

declare module 'vue' {
  type Hooks = App.AppInstance & Page.PageInstance
  interface ComponentCustomOptions extends Hooks {}
}

/** .env.development / .env.production 中的自定义环境变量（渐进式 TS：先给配置层上类型） */
interface ImportMetaEnv {
  /** 后端接口地址，见 src/utils/config.ts */
  readonly VITE_API_BASE_URL?: string
}

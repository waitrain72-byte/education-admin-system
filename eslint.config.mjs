import pluginVue from 'eslint-plugin-vue'
import prettier from 'eslint-config-prettier'
import globals from 'globals'

/**
 * ESLint 扁平配置（对齐 unibest 等高星 uni-app 项目的工程化实践）：
 * - vue3-essential：只保留「会导致运行时错误」的检查，风格交给 Prettier，避免大规模重排
 * - eslint-config-prettier 关闭与 Prettier 冲突的格式化规则，两者各司其职
 * - uni-app 全局对象（uni / plus / getCurrentPages）显式声明，避免 no-undef 误报
 */
export default [
  {
    ignores: ['dist/**', 'node_modules/**', 'coverage/**', 'src/libs/**'],
  },
  ...pluginVue.configs['flat/essential'],
  {
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      globals: {
        ...globals.browser,
        uni: 'readonly',
        plus: 'readonly',
        getCurrentPages: 'readonly',
      },
    },
    rules: {
      // 页面文件名即路由名（login/home/mine...），小程序路由惯例，不做组件多词名限制
      'vue/multi-word-component-names': 'off',
      'no-undef': 'error',
      'no-unused-vars': ['error', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
    },
  },
  {
    files: ['*.config.js', 'tests/**'],
    languageOptions: {
      globals: { ...globals.node },
    },
  },
  prettier,
]

/* eslint-env node */
module.exports = {
  root: true,
  env: {
    browser: true,
    es2020: true,
    node: true,
  },
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-recommended',
    'plugin:@typescript-eslint/recommended',
    'prettier',
  ],
  parser: 'vue-eslint-parser',
  parserOptions: {
    parser: '@typescript-eslint/parser',
    ecmaVersion: 'latest',
    sourceType: 'module',
  },
  plugins: ['@typescript-eslint'],
  rules: {
    // TypeScript 已由 vue-tsc 做类型检查，关闭 ESLint 的重复检查
    'no-undef': 'off',
    '@typescript-eslint/no-explicit-any': 'off',
    '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
    // Vue 3 组合式项目常见豁免
    'vue/multi-word-component-names': 'off',
    'vue/no-v-html': 'off',
    'vue/require-default-prop': 'off',
  },
  ignorePatterns: ['dist', 'node_modules', 'components.d.ts', 'coverage'],
}

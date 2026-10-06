<template>
  <div class="auth">
    <!-- 背景：一张淡淡的周课表，几门课的色块散落在格子里 -->
    <div class="auth__timetable" aria-hidden="true">
      <span
        v-for="block in blocks"
        :key="block.name"
        class="auth__block"
        :style="{ gridColumn: block.col, gridRow: block.row, '--cc': courseColor(block.name) }"
      >
        {{ block.name }}
      </span>
    </div>

    <div class="auth__corner">
      <button type="button" class="auth__toggle" :title="$t('layout.lang.switch')" @click="toggleLocale">
        {{ isZh ? 'EN' : '中' }}
      </button>
      <button type="button" class="auth__toggle" :title="$t(themeLabel)" :aria-label="$t(themeLabel)" @click="cycleTheme">
        <el-icon :size="18"><component :is="themeIcon" /></el-icon>
      </button>
    </div>

    <main class="auth__center">
      <div class="auth__brand">
        <BrandMark />
        <div>
          <div class="auth__name">{{ $t('login.systemName') }}</div>
          <div class="auth__tagline">{{ $t('login.tagline') }}</div>
        </div>
      </div>
      <section class="auth__card">
        <slot />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Monitor, Moon, Sunny } from '@element-plus/icons-vue'
import BrandMark from '@/layout/BrandMark.vue'
import { useTheme } from '@/composables/useTheme'
import { currentLocale, setLocale } from '@/composables/useLocale'
import { courseColor } from '@/utils/courseColor'

// 主题切换：浅色 → 深色 → 跟随系统 循环
const mode = useTheme()
const themeIcon = computed(() => (mode.value === 'dark' ? Moon : mode.value === 'light' ? Sunny : Monitor))
const themeLabel = computed(() => `layout.theme.current.${mode.value}`)
const cycleTheme = () => {
  mode.value = mode.value === 'light' ? 'dark' : mode.value === 'dark' ? 'auto' : 'light'
}

const isZh = computed(() => currentLocale() === 'zh-CN')
const toggleLocale = () => setLocale(isZh.value ? 'en-US' : 'zh-CN')

/** 背景课表里的几门课：列 = 星期（1~7），行 = 大节（1~5） */
const blocks = [
  { name: '高等数学', col: 1, row: 1 },
  { name: '大学英语', col: 2, row: 3 },
  { name: '数据结构', col: 3, row: 2 },
  { name: '线性代数', col: 5, row: 3 },
  { name: 'Java 程序设计', col: 3, row: 4 },
  { name: '离散数学', col: 6, row: 1 },
  { name: '中国近代史纲要', col: 7, row: 4 },
  { name: '体育（篮球）', col: 5, row: 5 },
]
</script>

<style scoped>
.auth {
  position: relative;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 72px 16px 40px;
  background: var(--xm-bg-page);
  overflow: hidden;
}

.auth__timetable {
  position: absolute;
  inset: 0;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  grid-template-rows: repeat(5, 1fr);
  gap: 10px;
  padding: 24px;
  background-image:
    linear-gradient(var(--xm-border) 1px, transparent 1px),
    linear-gradient(90deg, var(--xm-border) 1px, transparent 1px);
  background-size: calc((100% - 48px) / 7) calc((100% - 48px) / 5);
  background-position: 24px 24px;
  opacity: 0.85;
  pointer-events: none;
}

.auth__block {
  display: flex;
  align-items: flex-end;
  padding: 10px 12px;
  border-radius: 12px;
  background: var(--cc);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  opacity: 0.16;
}

:global(html.dark) .auth__block {
  opacity: 0.22;
}

.auth__corner {
  position: absolute;
  top: 18px;
  right: 20px;
  z-index: 2;
  display: flex;
  gap: 8px;
}

.auth__toggle {
  display: inline-grid;
  place-items: center;
  min-width: 36px;
  height: 36px;
  padding: 0 8px;
  border: 1px solid var(--xm-border);
  border-radius: 10px;
  background: var(--xm-bg-card);
  color: var(--xm-text-regular);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.auth__toggle:hover,
.auth__toggle:focus-visible {
  border-color: var(--xm-brand);
  color: var(--xm-brand);
}

.auth__center {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 18px;
  width: 400px;
  max-width: 100%;
}

.auth__brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.auth__brand :deep(.brand-mark) {
  width: 42px;
  height: 42px;
}

.auth__name {
  font-size: 20px;
  font-weight: 700;
  color: var(--xm-text-primary);
}

.auth__tagline {
  margin-top: 2px;
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.auth__card {
  padding: 28px 28px 24px;
  border: 1px solid var(--xm-border);
  border-radius: 18px;
  background: color-mix(in srgb, var(--xm-bg-card) 92%, transparent);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  box-shadow: var(--xm-shadow-pop);
}

@media (max-width: 640px) {
  .auth__timetable {
    gap: 6px;
    padding: 12px;
    background-size: calc((100% - 24px) / 7) calc((100% - 24px) / 5);
    background-position: 12px 12px;
  }

  .auth__block {
    font-size: 0;
  }

  .auth__card {
    padding: 22px 18px 18px;
  }
}
</style>

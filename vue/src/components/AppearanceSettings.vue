<template>
  <div class="appearance" :class="{ 'appearance--wide': wide }">
    <div class="appearance__row">
      <span class="appearance__label">{{ $t('shell.themeMode') }}</span>
      <div class="seg" role="radiogroup" :aria-label="$t('shell.themeMode')">
        <button
          v-for="m in modes"
          :key="m.value"
          type="button"
          role="radio"
          :aria-checked="mode === m.value"
          class="seg__item"
          :class="{ 'is-on': mode === m.value }"
          @click="mode = m.value"
        >
          {{ $t(m.label) }}
        </button>
      </div>
    </div>

    <div class="appearance__row">
      <span class="appearance__label">{{ $t('layout.themeColor.title') }}</span>
      <div class="swatches" role="radiogroup" :aria-label="$t('layout.themeColor.title')">
        <button
          v-for="preset in PRESET_COLORS"
          :key="preset.value || 'default'"
          type="button"
          role="radio"
          class="swatch"
          :class="{ 'is-on': themeColor === preset.value, 'swatch--default': !preset.value }"
          :aria-checked="themeColor === preset.value"
          :title="$t(preset.label)"
          :aria-label="$t(preset.label)"
          :style="preset.value ? { background: preset.value } : undefined"
          @click="setThemeColor(preset.value)"
        ></button>
        <el-color-picker :model-value="themeColor || null" size="small" :teleported="!inPopover" @change="onPickColor" />
      </div>
    </div>

    <div class="appearance__row">
      <span class="appearance__label">{{ $t('layout.lang.label') }}</span>
      <div class="seg" role="radiogroup" :aria-label="$t('layout.lang.label')">
        <button type="button" role="radio" :aria-checked="isZh" class="seg__item" :class="{ 'is-on': isZh }" @click="setLocale('zh-CN')">中文</button>
        <button type="button" role="radio" :aria-checked="!isZh" class="seg__item" :class="{ 'is-on': !isZh }" @click="setLocale('en-US')">English</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useTheme, type ThemeMode } from '@/composables/useTheme'
import { PRESET_COLORS, setThemeColor, useThemeColor } from '@/composables/useThemeColor'
import { currentLocale, setLocale } from '@/composables/useLocale'

/**
 * 外观与语言：深浅模式、主题色、界面语言。用户菜单与个人中心共用。
 * 三项都按账号保存到后端，换终端登录自动跟随。
 */
withDefaults(defineProps<{ wide?: boolean; inPopover?: boolean }>(), { wide: false, inPopover: false })

const mode = useTheme()
const modes: Array<{ value: ThemeMode; label: string }> = [
  { value: 'light', label: 'layout.theme.light' },
  { value: 'dark', label: 'layout.theme.dark' },
  { value: 'auto', label: 'layout.theme.auto' },
]

const themeColor = useThemeColor()
// el-color-picker 清空时回调 null，等价于恢复默认
const onPickColor = (value: string | null) => setThemeColor(value || '')

const isZh = computed(() => currentLocale() === 'zh-CN')
</script>

<style scoped>
.appearance {
  display: grid;
  gap: 12px;
}

.appearance__row {
  display: grid;
  gap: 6px;
}

.appearance--wide .appearance__row {
  grid-template-columns: 96px minmax(0, 1fr);
  align-items: center;
  gap: 16px;
}

.appearance__label {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.appearance--wide .appearance__label {
  font-size: 14px;
  color: var(--xm-text-regular);
}

.seg {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: 1fr;
  padding: 3px;
  border-radius: 9px;
  background: var(--xm-bg-sunken);
}

.appearance--wide .seg {
  max-width: 340px;
}

.seg__item {
  padding: 5px 0;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: var(--xm-text-secondary);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.seg__item.is-on {
  background: var(--xm-bg-card);
  color: var(--xm-text-primary);
  font-weight: 600;
  box-shadow: var(--xm-shadow-card);
}

.swatches {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 7px;
}

.swatch {
  width: 22px;
  height: 22px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 50%;
  box-shadow: 0 0 0 1px var(--xm-border);
  cursor: pointer;
  transition: transform 0.15s ease;
}

/* 「默认」色块用玉绿渐变表示，与固定色区分开 */
.swatch--default {
  background: linear-gradient(135deg, #0f7b63 0%, #3fb08f 100%);
}

.swatch:hover {
  transform: scale(1.1);
}

.swatch.is-on {
  border-color: var(--xm-bg-card);
  box-shadow: 0 0 0 2px var(--xm-text-primary);
}
</style>

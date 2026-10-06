<template>
  <section class="hero">
    <div class="hero__main">
      <p class="hero__eyebrow">{{ eyebrow }}</p>
      <h1 class="hero__title">{{ title }}</h1>
      <div v-if="$slots.default" class="hero__stats">
        <slot />
      </div>
    </div>
    <div v-if="$slots.aside" class="hero__aside">
      <slot name="aside" />
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{ title: string; eyebrow: string }>()
</script>

<style scoped>
/* 首页页头：品牌色浅底 + 课表格线，右侧放「下一节课」或快捷操作 */
.hero {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
  padding: 26px 28px;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius-lg);
  background:
    repeating-linear-gradient(0deg, transparent 0 31px, var(--xm-border) 31px 32px),
    repeating-linear-gradient(90deg, transparent 0 63px, var(--xm-border) 63px 64px),
    var(--xm-brand-soft);
  background-blend-mode: normal;
  overflow: hidden;
}

/* 格线只在左半边若隐若现：用一层渐变遮罩压淡 */
.hero::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, var(--xm-brand-soft) 35%, transparent 100%);
  opacity: 0.92;
  pointer-events: none;
}

.hero__main,
.hero__aside {
  position: relative;
}

.hero__main {
  min-width: 0;
}

.hero__eyebrow {
  font-size: 13px;
  color: var(--xm-text-secondary);
  letter-spacing: 0.02em;
}

.hero__title {
  margin-top: 6px;
  font-size: 26px;
  font-weight: 700;
  line-height: 1.3;
  color: var(--xm-text-primary);
  text-wrap: balance;
}

.hero__stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 22px;
  margin-top: 14px;
  font-size: 14px;
  color: var(--xm-text-regular);
}

.hero__aside {
  flex-shrink: 0;
}

@media (max-width: 640px) {
  .hero {
    padding: 20px 18px;
  }

  .hero__title {
    font-size: 22px;
  }

  .hero__aside {
    width: 100%;
  }
}
</style>

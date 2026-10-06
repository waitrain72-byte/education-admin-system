<template>
  <!-- 品牌标：课表的三行色条，长短不一，右下一个圆点表示「今天这一节」 -->
  <svg class="brand-mark" viewBox="0 0 32 32" aria-hidden="true">
    <rect width="32" height="32" rx="9" fill="var(--xm-brand)" />
    <rect x="8" y="8.5" width="16" height="3.2" rx="1.6" fill="#fff" />
    <rect x="8" y="14.4" width="11" height="3.2" rx="1.6" fill="#fff" fill-opacity="0.82" />
    <rect x="8" y="20.3" width="6.5" height="3.2" rx="1.6" fill="#fff" fill-opacity="0.64" />
    <circle cx="21.8" cy="21.9" r="2.6" fill="#fff" />
  </svg>
</template>

<style scoped>
.brand-mark {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  display: block;
}
</style>

<template>
  <!-- 骨架区域：先隐身 0.2 秒再淡入（.sk-region），读屏软件读到「正在加载」 -->
  <div class="sk-region" aria-busy="true">
    <span class="sr-only">{{ $t('common.loading') }}</span>
    <slot />
  </div>
</template>

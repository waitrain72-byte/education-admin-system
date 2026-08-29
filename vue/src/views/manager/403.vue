<template>
  <div>
    <div style="height: calc(100vh - 80px); overflow: hidden; display: flex; align-items: center; justify-content: center">
      <div style="font-size: 40px">{{ $t('forbidden.message') }} <router-link to="/">{{ $t('common.backHome') }}</router-link></div>
    </div>
  </div>
</template>

<script setup>
// 无需额外逻辑，保持简洁
</script>

<style scoped>
</style>

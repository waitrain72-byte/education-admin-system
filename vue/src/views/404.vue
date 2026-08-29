<template>
  <div>
    <div style="height: 100vh; overflow: hidden; display: flex; align-items: center; justify-content: center">
      <div style="font-size: 40px">{{ $t('notFound.message') }} <router-link to="/">{{ $t('common.backHome') }}</router-link></div>
    </div>
  </div>
</template>

<script setup>
// 无需额外逻辑，保持简洁
</script>

<style scoped>
</style>

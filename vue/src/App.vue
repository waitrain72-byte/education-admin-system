<template>
  <!-- el-config-provider：全局组件语言设为中文（原 app.use(ElementPlus, { locale }) 的替代） -->
  <el-config-provider :locale="zhCn">
    <div id="app">
      <router-view/>
    </div>
  </el-config-provider>
</template>

<script setup lang="ts">
import zhCn from 'element-plus/es/locale/lang/zh-cn'
</script>

<template>
  <!-- el-config-provider：Element Plus 组件语言跟随当前界面语言切换 -->
  <el-config-provider :locale="elementLocale">
    <div id="app">
      <router-view/>
    </div>
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'
import i18n from '@/i18n'
import { installThemeSync } from '@/composables/useTheme'
import { installNoticeSocket } from '@/composables/useNoticeSocket'

// Element Plus 内置语言包随 vue-i18n 当前语言联动
const elementLocale = computed(() => (i18n.global.locale.value === 'zh-CN' ? zhCn : en))

// 主题同步只需在根组件安装一次：模式变化时防抖推送到后端
installThemeSync()
// 实时通知 WebSocket：登录后自动建连接收推送
installNoticeSocket()
</script>

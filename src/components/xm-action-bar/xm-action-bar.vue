<template>
  <view class="xm-card xm-row">
    <button
      class="xm-btn xm-btn-primary"
      @click="$emit('add')"
    >
      {{ $t('common.add') }}
    </button>
    <button
      class="xm-btn xm-btn-plain"
      @click="$emit('toggle-manage')"
    >
      {{ manageMode ? $t('common.done') : $t('common.manage') }}
    </button>
    <button
      v-if="manageMode"
      class="xm-btn xm-btn-danger"
      @click="$emit('del-batch')"
    >
      {{ $t('common.batchDelete') }}
    </button>
  </view>
</template>

<script setup>
/**
 * 通用操作区（easycom 自动注册）：新增 / 批量管理切换 / 批量删除，
 * 与 useManage 组合使用。替代各管理页复制粘贴的操作区模板。
 */
defineProps({
  manageMode: { type: Boolean, default: false },
})
defineEmits(['add', 'toggle-manage', 'del-batch'])
</script>

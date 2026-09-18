<template>
  <ElDropdown
    class="tab-context-menu"
    :trigger="trigger"
    placement="bottom-start"
    popper-class="v-context-menu-popper"
    @command="command"
    @visible-change="visibleChange"
  >
    <slot></slot>
    <template #dropdown>
      <ElDropdownMenu>
        <ElDropdownItem
          v-for="(item, index) in schema"
          :key="`dropdown${index}`"
          :divided="item.divided"
          :disabled="item.disabled"
          :command="item"
        >
          <el-icon class="tab-context-menu__icon"><component :is="item.icon" /></el-icon>
          {{ item.label }}
        </ElDropdownItem>
      </ElDropdownMenu>
    </template>
  </ElDropdown>
</template>

<script setup lang="ts">
// 等价移植 vue-element-plus-admin 的 ContextMenu 组件：
// 基于 ElDropdown，支持右键/点击触发、分隔线、禁用态与自定义图标（element icons 组件对象）。
import { ElDropdown, ElDropdownMenu, ElDropdownItem } from '@/utils/element-plus'
import type { Component } from 'vue'

export interface TabContextMenuSchema {
    icon: Component
    label: string
    disabled?: boolean
    divided?: boolean
    command?: (item: TabContextMenuSchema) => void
}

const emit = defineEmits(['visibleChange'])

defineProps({
    schema: {
        type: Array as () => TabContextMenuSchema[],
        default: () => [],
    },
    trigger: {
        // 取值范围必须与 ElDropdown 的 trigger 一致：它不接受 'focus'
        type: String as () => 'click' | 'hover' | 'contextmenu',
        default: 'contextmenu',
    },
})

const command = (item: TabContextMenuSchema) => {
    item.command && item.command(item)
}

const visibleChange = (visible: boolean) => {
    emit('visibleChange', visible)
}
</script>

<style lang="scss">
/* 菜单 popper 样式对齐框架：图标与文字间距、行高 */
.v-context-menu-popper {
    .tab-context-menu__icon {
        margin-right: 6px;
        font-size: 13px;
        vertical-align: -2px;
    }
}
</style>

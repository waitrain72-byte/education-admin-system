<template>
  <view>
    <!-- 列表工具条：左侧总数 / 已选数，右侧批量管理；不再单独占一张卡片 -->
    <view class="list-toolbar">
      <text class="toolbar-count">{{
        manageMode ? $t('common.selectedCount', { n: selectedCount }) : $t('common.totalCount', { n: total })
      }}</text>
      <view class="toolbar-actions">
        <view
          v-if="manageMode"
          class="toolbar-btn is-danger"
          :class="{ 'is-disabled': !selectedCount }"
          @click="$emit('del-batch')"
        >
          <xm-icon
            name="trash"
            :size="28"
          />
          <text>{{ $t('common.batchDelete') }}</text>
        </view>
        <view
          class="toolbar-btn"
          @click="$emit('toggle-manage')"
        >
          <xm-icon
            :name="manageMode ? 'check' : 'list-check'"
            :size="28"
          />
          <text>{{ manageMode ? $t('common.done') : $t('common.manage') }}</text>
        </view>
      </view>
    </view>

    <!-- 新增：右下角悬浮按钮（批量管理时隐藏，避免误触） -->
    <view
      v-if="!manageMode"
      class="xm-fab"
      :aria-label="addText || $t('common.add')"
      aria-role="button"
      @click="$emit('add')"
    >
      <xm-icon
        name="plus"
        :size="48"
      />
    </view>
  </view>
</template>

<script setup>
/**
 * 通用列表操作（easycom 自动注册）：顶部细工具条（总数 + 批量管理）+ 右下角「新增」悬浮按钮。
 * - 替代原先搜索卡下面再叠一张「新增 / 管理」操作卡的写法，列表内容上移，一屏能多看一张卡片
 * - 悬浮按钮避让 iPhone 底部 Home 条（safe-area），宽屏下对齐居中内容区右缘（样式见 theme.scss .xm-fab）
 * - 与 useManage / useCrud 组合：total 取 useCrud 的 total，selectedCount 取 selectedIds.length
 */
defineProps({
  manageMode: { type: Boolean, default: false },
  /** 列表总条数（后端分页 total） */
  total: { type: Number, default: 0 },
  /** 批量管理模式下已勾选条数 */
  selectedCount: { type: Number, default: 0 },
  /** 新增按钮的无障碍文案，默认「新增」（如考勤页「添加考勤」） */
  addText: { type: String, default: '' },
})
defineEmits(['add', 'toggle-manage', 'del-batch'])
</script>

<style lang="scss" scoped>
.list-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 0 8rpx 20rpx;
}

.toolbar-count {
  flex: 1;
  min-width: 0;
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.toolbar-actions {
  display: flex;
  gap: 16rpx;
  flex-shrink: 0;
}

.toolbar-btn {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  height: 56rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
  color: var(--xm-brand);
  background: var(--xm-brand-soft);
}

.toolbar-btn:active {
  opacity: 0.8;
}

.toolbar-btn.is-danger {
  color: var(--xm-danger);
  background: var(--xm-danger-soft);
}

.toolbar-btn.is-disabled {
  opacity: 0.45;
}
</style>

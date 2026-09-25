<template>
  <view v-if="visible">
    <view
      class="xm-mask"
      @click="$emit('close')"
      @touchmove.stop.prevent="noop"
    ></view>
    <view class="xm-popup form-popup">
      <view class="xm-popup-title">{{ title }}</view>
      <view class="form-popup-body">
        <slot />
      </view>
      <view class="xm-row form-popup-footer">
        <button
          class="xm-btn xm-btn-plain"
          style="flex: 1"
          @click="$emit('close')"
        >
          {{ $t('common.cancel') }}
        </button>
        <button
          class="xm-btn xm-btn-primary"
          style="flex: 1"
          :loading="saving"
          :disabled="saving"
          @click="$emit('save')"
        >
          {{ confirmText || $t('common.ok') }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
/**
 * 通用底部弹层表单（easycom 自动注册）：标题 + 表单字段插槽 + 取消/确定按钮。
 * - 表单字段由页面通过默认插槽提供（各实体字段差异保留在页面侧）
 * - 字段多时只滚动中间表单区，取消/确定按钮固定在底部，不用滑到底才能保存
 * - saving：保存请求进行中时确定按钮转圈并禁用，与 useCrud 的防重复提交守卫配合
 * - 遮罩拦截 touchmove，弹层打开时背后的列表不会跟着滚动
 */
defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '' },
  saving: { type: Boolean, default: false },
  /** 确定按钮文案，默认「确定」 */
  confirmText: { type: String, default: '' },
})
defineEmits(['close', 'save'])

const noop = () => {}
</script>

<style lang="scss" scoped>
.form-popup {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.form-popup-body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
}

.form-popup-footer {
  flex-shrink: 0;
  padding-top: 16rpx;
}
</style>

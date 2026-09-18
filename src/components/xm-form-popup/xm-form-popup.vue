<template>
  <view v-if="visible">
    <view
      class="xm-mask"
      @click="$emit('close')"
    ></view>
    <view class="xm-popup">
      <view class="xm-popup-title">{{ title }}</view>
      <slot />
      <view
        class="xm-row"
        style="margin-top: 16rpx"
      >
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
          :disabled="saving"
          @click="$emit('save')"
        >
          {{ $t('common.ok') }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
/**
 * 通用底部弹层表单（easycom 自动注册）：标题 + 表单字段插槽 + 取消/保存按钮。
 * - 表单字段由页面通过默认插槽提供（各实体字段差异保留在页面侧）
 * - saving：保存请求进行中时禁用确定按钮，与 useCrud 的防重复提交守卫配合
 */
defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '' },
  saving: { type: Boolean, default: false },
})
defineEmits(['close', 'save'])
</script>

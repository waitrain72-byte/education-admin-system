<template>
  <!-- 与 xm-course-card 同尺寸的占位：封面（标签、课名、时间）+ 底栏两段字；index 只用来让每张卡的字长短不一 -->
  <view
    class="cc-sk"
    :class="{ 'is-compact': compact }"
  >
    <view class="cc-sk-cover sk sk-cover">
      <view class="sk sk-on-cover sk-tag" />
      <view
        class="sk sk-on-cover sk-h cc-sk-name"
        :style="{ width: NAME_WIDTHS[index % NAME_WIDTHS.length] }"
      />
      <view
        class="sk sk-on-cover sk-s"
        :style="{ width: TIME_WIDTHS[index % TIME_WIDTHS.length] }"
      />
    </view>
    <view class="cc-sk-foot">
      <view class="sk sk-s cc-sk-foot-item" />
      <view class="sk sk-s cc-sk-foot-item is-short" />
    </view>
  </view>
</template>

<script setup>
defineOptions({ options: { virtualHost: true } })

defineProps({
  index: { type: Number, default: 0 },
  /** 首页横向滑动列表里的窄卡片 */
  compact: { type: Boolean, default: false },
})

const NAME_WIDTHS = ['56%', '70%', '44%', '62%', '50%', '66%']
const TIME_WIDTHS = ['74%', '60%', '82%', '68%']
</script>

<style lang="scss" scoped>
.cc-sk {
  display: flex;
  flex-direction: column;
  min-width: 0;
  border: 1rpx solid var(--xm-card-border);
  border-radius: 24rpx;
  background: var(--xm-bg-card);
  box-shadow: var(--xm-shadow);
  overflow: hidden;
}

/* 封面本身是一大块占位，上面再叠几块浅色的字 */
.cc-sk-cover {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
  height: auto;
  min-height: 200rpx;
  padding: 24rpx 28rpx 26rpx;
  border-radius: 0;
  box-sizing: border-box;
}

.is-compact .cc-sk-cover {
  min-height: 180rpx;
}

.cc-sk-name {
  margin-top: auto;
}

.cc-sk-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 28rpx;
}

.cc-sk-foot-item {
  width: 120rpx;
}

.cc-sk-foot-item.is-short {
  width: 90rpx;
}
</style>

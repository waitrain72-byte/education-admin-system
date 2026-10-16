<template>
  <!-- 与 SquareCard 同尺寸的占位（与 Web 端 views/square/SquareCardSkeleton.vue 一致）：
       封面（标签、课名、时间）+ 教师、简介两行、名额条、选课按钮 -->
  <view class="sq-sk">
    <view class="sk sk-cover sq-sk-cover">
      <view class="sq-sk-tags">
        <view class="sk sk-on-cover sk-tag" />
        <view class="sk sk-on-cover sk-tag" />
      </view>
      <view
        class="sk sk-on-cover sk-h sq-sk-name"
        :style="{ width: NAME_WIDTHS[index % NAME_WIDTHS.length] }"
      />
      <view
        class="sk sk-on-cover sk-s"
        :style="{ width: TIME_WIDTHS[index % TIME_WIDTHS.length] }"
      />
    </view>
    <view class="sq-sk-body">
      <view
        class="sk"
        style="width: 22%"
      />
      <view
        class="sk sk-s"
        style="width: 92%"
      />
      <view
        class="sk sk-s"
        :style="{ width: index % 2 ? '58%' : '70%' }"
      />
      <view class="sq-sk-seats">
        <view class="sk sq-sk-bar" />
        <view class="sk sk-s sq-sk-seat" />
      </view>
      <view class="sk sk-btn sq-sk-btn" />
    </view>
  </view>
</template>

<script setup>
defineProps({
  index: { type: Number, default: 0 },
})

const NAME_WIDTHS = ['56%', '70%', '44%', '62%', '50%', '66%']
const TIME_WIDTHS = ['64%', '52%', '72%', '58%']
</script>

<style lang="scss" scoped>
.sq-sk {
  display: flex;
  flex-direction: column;
  border: 1rpx solid var(--xm-card-border);
  border-radius: 24rpx;
  background: var(--xm-bg-card);
  box-shadow: var(--xm-shadow);
  overflow: hidden;
}

/* 封面本身是一大块占位，上面再叠几块浅色的字 */
.sq-sk-cover {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  height: auto;
  padding: 24rpx 28rpx 26rpx;
  border-radius: 0;
  box-sizing: border-box;
}

.sq-sk-tags {
  display: flex;
  gap: 10rpx;
}

.sq-sk-name {
  margin-top: 12rpx;
}

.sq-sk-body {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
  padding: 20rpx 28rpx 24rpx;
}

.sq-sk-seats {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 6rpx;
}

.sq-sk-bar {
  flex: 1;
  height: 10rpx;
  border-radius: 999rpx;
}

.sq-sk-seat {
  width: 120rpx;
}

.sq-sk-btn {
  align-self: flex-end;
  margin-top: 4rpx;
}
</style>

<template>
  <!-- 列表占位（与 Web 端 components/skeleton/SkeletonList.vue 同一套）：每行「头像 / 色条 / 日期块 / 标签」
       + 一到两行字 + 末尾的标签、按钮或时间；divided 时行与行之间画分隔线，与各页列表的 row + row 一致 -->
  <view
    class="sk-list"
    :class="{ 'is-divided': divided }"
  >
    <view
      v-for="i in rows"
      :key="i"
      class="sk-list-row"
    >
      <view
        v-if="lead === 'avatar'"
        class="sk sk-avatar"
      />
      <view
        v-else-if="lead === 'bar'"
        class="sk sk-bar"
        :class="{ 'is-short': lines < 2 }"
      />
      <view
        v-else-if="lead === 'date'"
        class="sk sk-date"
      />
      <view
        v-else-if="lead === 'tag'"
        class="sk sk-tag"
      />
      <view class="sk-stack">
        <view
          class="sk"
          :style="{ width: TITLE_WIDTHS[(i - 1) % TITLE_WIDTHS.length] }"
        />
        <view
          v-if="lines > 1"
          class="sk sk-s"
          :style="{ width: META_WIDTHS[(i - 1) % META_WIDTHS.length] }"
        />
      </view>
      <view
        v-if="trail === 'tag'"
        class="sk sk-tag"
      />
      <view
        v-else-if="trail === 'btn'"
        class="sk sk-btn"
      />
      <view
        v-else-if="trail === 'text'"
        class="sk sk-s sk-list-time"
      />
    </view>
  </view>
</template>

<script setup>
defineOptions({ options: { virtualHost: true } })

defineProps({
  rows: { type: Number, default: 3 },
  /** 每行开头：none / avatar / bar / date / tag */
  lead: { type: String, default: 'none' },
  lines: { type: Number, default: 2 },
  /** 每行末尾：none / tag / btn / text */
  trail: { type: String, default: 'none' },
  divided: { type: Boolean, default: false },
})

// 每行长短不一才像真的文字；固定序列，重新渲染不会抖
const TITLE_WIDTHS = ['62%', '48%', '74%', '55%', '68%', '42%']
const META_WIDTHS = ['38%', '52%', '30%', '45%']
</script>

<style lang="scss" scoped>
.sk-list {
  display: flex;
  flex-direction: column;
  gap: 28rpx;
}

.sk-list.is-divided {
  gap: 0;
}

.sk-list-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  min-width: 0;
}

.sk-list.is-divided .sk-list-row {
  padding: 20rpx 0;
}

.sk-list.is-divided .sk-list-row + .sk-list-row {
  border-top: 1rpx solid var(--xm-border);
}

.sk-bar.is-short {
  height: 40rpx;
}

.sk-list-time {
  width: 120rpx;
}
</style>

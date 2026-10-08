<template>
  <!-- 登录 / 注册页外壳（与 Web 端 views/auth/AuthShell.vue 同一设计）：
       背景是一张淡淡的周课表，几门课的色块散落在格子里；右上角切换语言、主题；中间品牌 + 表单卡片 -->
  <view class="auth">
    <view class="auth-timetable">
      <view
        v-for="block in blocks"
        :key="block.name"
        class="auth-block"
        :style="blockStyle(block)"
        >{{ block.name }}</view
      >
    </view>

    <view class="auth-corner">
      <view
        class="auth-toggle"
        @click="toggleLocale"
        >{{ isZhLocale() ? 'EN' : '中' }}</view
      >
      <view
        class="auth-toggle"
        @click="cycleTheme"
      >
        <xm-icon
          :name="themeModeIcon"
          :size="32"
        />
      </view>
    </view>

    <view class="auth-center">
      <view class="auth-brand">
        <xm-brand-mark :size="84" />
        <view>
          <view class="auth-name">{{ $t('login.systemName') }}</view>
          <view class="auth-tagline">{{ $t('login.tagline') }}</view>
        </view>
      </view>
      <view class="auth-card">
        <slot />
      </view>
    </view>
  </view>
</template>

<script setup>
import { courseColor } from '@/utils/courseColor'
import { isZhLocale, toggleLocale } from '@/composables/useLocale'
import { cycleTheme, themeModeIcon } from '@/composables/useTheme'

/** 背景课表里的几门课：列 = 星期（1~7），行 = 大节（1~5）；与 Web 端同一组 */
const blocks = [
  { name: '高等数学', col: 1, row: 1 },
  { name: '大学英语', col: 2, row: 3 },
  { name: '数据结构', col: 3, row: 2 },
  { name: '线性代数', col: 5, row: 3 },
  { name: 'Java 程序设计', col: 3, row: 4 },
  { name: '离散数学', col: 6, row: 1 },
  { name: '中国近代史纲要', col: 7, row: 4 },
  { name: '体育（篮球）', col: 5, row: 5 },
]

const blockStyle = (block) => `grid-column:${block.col};grid-row:${block.row};background:${courseColor(block.name)};`
</script>

<style lang="scss" scoped>
.auth {
  position: relative;
  min-height: 100vh;
  padding: calc(var(--status-bar-height, 25px) + 120rpx) 40rpx calc(48rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  overflow: hidden;
  background: var(--xm-bg-page);
}

.auth-timetable {
  position: absolute;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  grid-template-rows: repeat(5, 1fr);
  gap: 12rpx;
  padding: 24rpx;
  background-image:
    linear-gradient(var(--xm-border) 1rpx, transparent 1rpx),
    linear-gradient(90deg, var(--xm-border) 1rpx, transparent 1rpx);
  background-size: calc((100% - 48rpx) / 7) calc((100% - 48rpx) / 5);
  background-position: 24rpx 24rpx;
  opacity: 0.85;
  pointer-events: none;
}

.auth-block {
  display: flex;
  align-items: flex-end;
  padding: 10rpx;
  border-radius: 16rpx;
  color: #ffffff;
  font-size: 18rpx;
  font-weight: 600;
  line-height: 1.3;
  overflow: hidden;
  /* 组件样式碰不到页面根上的 .theme-dark，深浅色的差别走主题变量 */
  opacity: var(--xm-deco-opacity);
}

.auth-corner {
  position: absolute;
  top: calc(var(--status-bar-height, 25px) + 16rpx);
  /* 右上角让出小程序胶囊按钮（约 190rpx 宽） */
  left: 32rpx;
  z-index: 2;
  display: flex;
  gap: 16rpx;
}

.auth-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 64rpx;
  height: 64rpx;
  padding: 0 16rpx;
  border: 1rpx solid var(--xm-border);
  border-radius: 18rpx;
  background: var(--xm-bg-card);
  color: var(--xm-text-2);
  font-size: 24rpx;
  font-weight: 600;
  box-sizing: border-box;
}

.auth-toggle:active {
  color: var(--xm-brand);
  border-color: var(--xm-brand);
}

.auth-center {
  position: relative;
  z-index: 1;
}

.auth-brand {
  display: flex;
  align-items: center;
  gap: 24rpx;
  margin-bottom: 36rpx;
}

.auth-name {
  font-size: 40rpx;
  font-weight: bold;
  color: var(--xm-text);
}

.auth-tagline {
  margin-top: 4rpx;
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.auth-card {
  padding: 48rpx 40rpx 40rpx;
  border: 1rpx solid var(--xm-border);
  border-radius: 32rpx;
  background: var(--xm-bg-card);
  box-shadow: var(--xm-shadow-lg);
}

/* 平板 / PC：卡片居中，内容宽 440px */
@media (min-width: 720px) {
  .auth {
    padding-left: calc((100% - 440px) / 2);
    padding-right: calc((100% - 440px) / 2);
  }
}
</style>

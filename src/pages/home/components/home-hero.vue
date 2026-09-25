<template>
  <!-- 首页头卡：头像字 + 按时段问候 + 日期角色，右侧消息铃铛（语言 / 主题切换在「我的」页） -->
  <view class="xm-hero">
    <view class="xm-row hero-row">
      <view class="hero-avatar">{{ avatarLetter }}</view>
      <view class="hero-meta">
        <view class="xm-hero-title xm-ellipsis">{{ $t(greetingKey(nowDate), { name: userName }) }}</view>
        <view class="xm-hero-sub xm-ellipsis">{{ dayText(nowDate) }} · {{ roleLabel }}</view>
      </view>
      <view
        class="hero-bell"
        aria-role="button"
        :aria-label="$t('menu.message')"
        @click="go('/pages/message/message')"
      >
        <xm-icon
          name="bell"
          :size="36"
        />
        <view
          v-if="unreadCount"
          class="bell-badge"
          >{{ unreadCount > 99 ? '99+' : unreadCount }}</view
        >
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { t } from '@/i18n'
import { enumLabel } from '@/utils/enums'
import { dayText, greetingKey } from '@/utils/dateText'

/**
 * 首页头卡：问候语用「早上好 / 下午好，姓名」短句，任何屏宽都不会被右侧按钮挤成省略号；
 * 原先头卡里的语言 / 主题快捷按钮与「我的」页重复，已收敛到「我的」页。
 */
const props = defineProps({
  /**
   * 当前时间戳（毫秒）：由首页每次 onShow 刷新，问候语与日期随之更新（跨时段回到首页不会停留在旧问候）。
   * 传数字而非 Date：小程序组件间传参走数据序列化，数字最稳妥
   */
  now: { type: Number, default: () => Date.now() },
})
const nowDate = computed(() => new Date(props.now))

const userStore = useUserStore()
const messageStore = useMessageStore()

const userName = computed(() => userStore.user.name || userStore.user.username || t('layout.guest'))
const avatarLetter = computed(() => (userName.value || '?').slice(0, 1))

/** 消息中心未读数（本地持久化的推送历史，入口在头卡铃铛与「我的」页） */
const unreadCount = computed(() => messageStore.unreadCount)

/** 角色码转展示名（统一枚举表，与「我的」页一致） */
const roleLabel = computed(() => enumLabel('role', userStore.role))

const go = (path) => uni.navigateTo({ url: path })
</script>

<style lang="scss" scoped>
/* 头像 + 文案 + 铃铛一行：文案区可收缩省略，头像与铃铛固定尺寸不被挤压 */
.hero-row {
  gap: 20rpx;
}

.hero-avatar {
  width: 88rpx;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  border-radius: 50%;
  font-size: 36rpx;
  color: #ffffff;
  background: rgba(255, 255, 255, 0.2);
  border: 2rpx solid rgba(255, 255, 255, 0.5);
  flex-shrink: 0;
}

.hero-meta {
  flex: 1;
  min-width: 0;
}

/* 消息铃铛：毛玻璃圆形按钮，右上角未读角标 */
.hero-bell {
  position: relative;
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #ffffff;
  background: rgba(255, 255, 255, 0.18);
}

.hero-bell:active {
  background: rgba(255, 255, 255, 0.3);
}

.bell-badge {
  position: absolute;
  top: -6rpx;
  right: -6rpx;
  min-width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  padding: 0 8rpx;
  box-sizing: border-box;
  border-radius: 999rpx;
  font-size: 20rpx;
  text-align: center;
  color: #ffffff;
  background: #f0555f;
  border: 2rpx solid rgba(255, 255, 255, 0.9);
}
</style>

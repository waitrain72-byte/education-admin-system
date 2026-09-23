<template>
  <!-- 欢迎 + 偏好切换：品牌渐变头卡 -->
  <view class="xm-hero">
    <view class="xm-between">
      <view class="xm-row hero-left">
        <view class="hero-avatar">{{ avatarLetter }}</view>
        <view class="hero-meta">
          <view class="xm-hero-title xm-ellipsis">{{ $t('home.welcome', { name: userName }) }}</view>
          <view class="xm-hero-sub">{{ todayText }} · {{ roleLabel }}</view>
        </view>
      </view>
      <view class="xm-row hero-prefs">
        <button
          class="hero-btn hero-bell"
          @click="go('/pages/message/message')"
        >
          🔔
          <view
            v-if="unreadCount"
            class="bell-badge"
            >{{ unreadCount > 99 ? '99+' : unreadCount }}</view
          >
        </button>
        <button
          class="hero-btn"
          @click="toggleLocale"
        >
          {{ isZhLocale() ? 'EN' : '中' }}
        </button>
        <button
          class="hero-btn"
          @click="cycleTheme"
        >
          {{ themeMode === 'light' ? '☀' : themeMode === 'dark' ? '☾' : '◐' }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { t } from '@/i18n'
import { isZhLocale, toggleLocale } from '@/composables/useLocale'
import { cycleTheme, themeMode } from '@/composables/useTheme'

/** 首页头卡：头像字 + 欢迎语 + 日期角色，右侧消息铃铛 / 语言 / 主题快捷切换 */
const userStore = useUserStore()
const messageStore = useMessageStore()

const userName = computed(() => userStore.user.name || userStore.user.username || t('layout.guest'))
const avatarLetter = computed(() => (userName.value || '?').slice(0, 1))

/** 消息中心未读数（本地持久化的推送历史，入口在头卡铃铛与「我的」页） */
const unreadCount = computed(() => messageStore.unreadCount)

/** 角色码转展示名（与「我的」页一致） */
const roleLabel = computed(() => {
  const map = {
    ADMIN: t('login.roleAdmin'),
    TEACHER: t('login.roleTeacher'),
    STUDENT: t('login.roleStudent'),
  }
  return map[userStore.role] || userStore.role || ''
})

const todayText = computed(() => {
  const d = new Date()
  return `${d.getMonth() + 1}/${d.getDate()}`
})

const go = (path) => uni.navigateTo({ url: path })
</script>

<style lang="scss" scoped>
/* 渐变头卡内部：左侧内容可收缩省略，右侧按钮固定不被挤压 */
.hero-left {
  flex: 1;
  min-width: 0;
}

.hero-avatar {
  width: 84rpx;
  height: 84rpx;
  line-height: 84rpx;
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

/* 头卡上的毛玻璃胶囊小按钮（脱离 xm-btn 体系：白字透明底融入渐变） */
.hero-btn {
  height: 60rpx;
  line-height: 60rpx;
  padding: 0 22rpx;
  margin: 0;
  font-size: 24rpx;
  color: #ffffff;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 999rpx;
  border: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.hero-btn::after {
  border: none;
}

.hero-btn:active {
  background: rgba(255, 255, 255, 0.3);
}

/* 消息铃铛：相对定位承载未读角标 */
.hero-bell {
  position: relative;
}

.bell-badge {
  position: absolute;
  top: -8rpx;
  right: -6rpx;
  min-width: 30rpx;
  height: 30rpx;
  line-height: 30rpx;
  padding: 0 6rpx;
  border-radius: 999rpx;
  font-size: 18rpx;
  text-align: center;
  color: #ffffff;
  background: #f0555f;
}

.hero-prefs {
  flex-shrink: 0;
}
</style>

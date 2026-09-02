<template>
  <view
    class="xm-page"
    :class="themeClass"
  >
    <!-- 用户卡片 -->
    <view class="xm-card user-card">
      <image
        v-if="user.avatar"
        :src="user.avatar"
        class="user-avatar"
        mode="aspectFill"
        @click="go('/pages/person/person')"
      />
      <view
        v-else
        class="user-avatar user-avatar-placeholder"
        @click="go('/pages/person/person')"
        >{{ avatarLetter }}</view
      >
      <view class="user-meta">
        <view class="xm-value user-name">{{ user.name || user.username || $t('layout.guest') }}</view>
        <view class="xm-label">{{ roleLabel }}</view>
      </view>
    </view>

    <!-- 功能入口 -->
    <view class="xm-card">
      <view
        class="mine-item"
        @click="go('/pages/person/person')"
      >
        <text class="mine-icon">🧑</text>
        <text class="xm-value">{{ $t('menu.person') }}</text>
        <text class="mine-arrow">›</text>
      </view>
      <view
        class="mine-item"
        @click="go('/pages/password/password')"
      >
        <text class="mine-icon">🔑</text>
        <text class="xm-value">{{ $t('menu.password') }}</text>
        <text class="mine-arrow">›</text>
      </view>
    </view>

    <!-- 偏好设置 -->
    <view class="xm-card">
      <view class="xm-card-title">{{ $t('home.prefs') }}</view>
      <view class="mine-item">
        <text class="mine-icon">🌐</text>
        <text class="xm-value">{{ $t('layout.lang.label') }}</text>
        <button
          class="xm-btn xm-btn-plain pref-btn"
          @click="toggleLocale"
        >
          {{ isZhLocale() ? 'EN' : '中' }}
        </button>
      </view>
      <view class="mine-item">
        <text class="mine-icon">🎨</text>
        <text class="xm-value">{{ $t('layout.theme.switch') }}</text>
        <button
          class="xm-btn xm-btn-plain pref-btn"
          @click="cycleTheme"
        >
          {{ themeMode === 'light' ? '☀' : themeMode === 'dark' ? '☾' : '◐' }}
        </button>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="xm-card">
      <button
        class="xm-btn xm-btn-danger xm-btn-block logout-btn"
        @click="logout"
      >
        {{ $t('layout.logout') }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { clearCookie } from '@/utils/request'
import { t } from '@/i18n'
import { isZhLocale, toggleLocale } from '@/composables/useLocale'
import { cycleTheme, themeMode, themeClass } from '@/composables/useTheme'

const userStore = useUserStore()
const user = computed(() => userStore.user || {})

const avatarLetter = computed(() => (user.value.name || user.value.username || '?').slice(0, 1))

/** 角色码转展示名（与登录页角色选项一致） */
const roleLabel = computed(() => {
  const map = {
    ADMIN: t('login.roleAdmin'),
    TEACHER: t('login.roleTeacher'),
    STUDENT: t('login.roleStudent'),
  }
  return map[user.value.role] || user.value.role || ''
})

const go = (path) => uni.navigateTo({ url: path })

/** 退出登录：清空用户态与验证码会话 Cookie，回到登录页 */
const logout = () => {
  uni.showModal({
    title: t('layout.logout'),
    content: t('layout.logoutConfirm'),
    success: (res) => {
      if (!res.confirm) return
      userStore.clearUser()
      clearCookie()
      uni.reLaunch({ url: '/pages/login/login' })
    },
  })
}

onShow(() => {
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  uni.setNavigationBarTitle({ title: t('menu.mine') })
})
</script>

<style lang="scss" scoped>
.user-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 36rpx 28rpx;
}

.user-avatar {
  width: 110rpx;
  height: 110rpx;
  border-radius: 50%;
  background: var(--xm-bg-hover);
  flex-shrink: 0;
}

.user-avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44rpx;
  font-weight: bold;
  color: var(--xm-brand);
}

.user-name {
  font-size: 34rpx;
  font-weight: bold;
  margin-bottom: 8rpx;
}

.mine-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 24rpx 8rpx;
  border-bottom: 1rpx solid var(--xm-border);
}

.mine-item:last-child {
  border-bottom: none;
}

.mine-icon {
  font-size: 34rpx;
}

.mine-arrow {
  margin-left: auto;
  color: var(--xm-text-2);
  font-size: 34rpx;
}

.pref-btn {
  margin-left: auto;
  height: 56rpx;
  line-height: 56rpx;
  padding: 0 22rpx;
  font-size: 24rpx;
}

.logout-btn {
  margin-top: 8rpx;
}
</style>

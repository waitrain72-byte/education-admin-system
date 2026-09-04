<template>
  <view
    class="xm-page"
    :class="themeClass"
  >
    <!-- 用户卡片：品牌渐变头卡 -->
    <view class="xm-hero user-card">
      <image
        v-if="user.avatar"
        :src="avatarUrl"
        class="xm-hero-avatar"
        mode="aspectFill"
        @click="go('/pages/person/person')"
      />
      <view
        v-else
        class="xm-hero-avatar user-avatar-placeholder"
        @click="go('/pages/person/person')"
        >{{ avatarLetter }}</view
      >
      <view class="user-meta">
        <view class="xm-hero-title xm-ellipsis">{{ user.name || user.username || $t('layout.guest') }}</view>
        <view class="user-role">{{ roleLabel }}</view>
      </view>
    </view>

    <!-- 功能入口 -->
    <view class="xm-card">
      <view
        class="xm-cell"
        @click="go('/pages/person/person')"
      >
        <text class="xm-cell-icon">🧑</text>
        <text class="xm-cell-body xm-ellipsis">{{ $t('menu.person') }}</text>
        <text class="xm-cell-arrow">›</text>
      </view>
      <view
        class="xm-cell"
        @click="go('/pages/password/password')"
      >
        <text class="xm-cell-icon">🔑</text>
        <text class="xm-cell-body xm-ellipsis">{{ $t('menu.password') }}</text>
        <text class="xm-cell-arrow">›</text>
      </view>
    </view>

    <!-- 偏好设置 -->
    <view class="xm-card">
      <view class="xm-card-title">{{ $t('home.prefs') }}</view>
      <view class="xm-cell">
        <text class="xm-cell-icon">🌐</text>
        <text class="xm-cell-body xm-ellipsis">{{ $t('layout.lang.label') }}</text>
        <button
          class="xm-btn pref-btn"
          @click="toggleLocale"
        >
          {{ isZhLocale() ? 'EN' : '中' }}
        </button>
      </view>
      <view class="xm-cell">
        <text class="xm-cell-icon">🎨</text>
        <text class="xm-cell-body xm-ellipsis">{{ $t('layout.theme.switch') }}</text>
        <button
          class="xm-btn pref-btn"
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
import { clearCookie, get, resolveFileUrl } from '@/utils/request'
import { closeWs } from '@/utils/websocket'
import { t } from '@/i18n'
import { isZhLocale, toggleLocale } from '@/composables/useLocale'
import { cycleTheme, themeMode, themeClass } from '@/composables/useTheme'

const userStore = useUserStore()
const user = computed(() => userStore.user || {})

// 头像展示地址：把老数据里的 localhost 绝对地址 / 新数据的 /api 相对路径归一成当前 baseUrl 完整地址
const avatarUrl = computed(() => resolveFileUrl(user.value.avatar))

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
      closeWs()
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

  // 跨端资料同步：Web 端等其它入口修改资料（如头像）后，进入「我的」页时
  // 静默拉取一次最新用户信息，无需重新登录。
  // selectById 返回的 token 为空，回填本地 token，防止把登录态冲掉。
  const urlByRole = { ADMIN: '/admin', TEACHER: '/teacher', STUDENT: '/student' }
  const base = urlByRole[userStore.role]
  if (base && userStore.user.id) {
    get(`${base}/selectById/${userStore.user.id}`).then((res) => {
      if (res.data.code === '200' && res.data.data) {
        userStore.patchUser({ ...res.data.data, token: userStore.token })
      }
    })
  }
})
</script>

<style lang="scss" scoped>
/* 渐变用户卡：头像 + 姓名 + 角色胶囊 */
.user-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.user-meta {
  flex: 1;
  min-width: 0;
}

.user-avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44rpx;
  font-weight: bold;
  color: #ffffff;
  background: rgba(255, 255, 255, 0.2);
}

/* 角色胶囊：白色半透明底融入渐变 */
.user-role {
  display: inline-block;
  margin-top: 10rpx;
  padding: 4rpx 20rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  color: #ffffff;
  background: rgba(255, 255, 255, 0.18);
}

/* 偏好切换按钮：主色软底胶囊 */
.pref-btn {
  height: 56rpx;
  line-height: 56rpx;
  padding: 0 24rpx;
  font-size: 24rpx;
  border-radius: 999rpx;
  border: none;
  background: var(--xm-brand-soft);
  color: var(--xm-brand);
  flex-shrink: 0;
}

.logout-btn {
  margin-top: 8rpx;
}
</style>

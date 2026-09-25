<template>
  <view
    class="xm-page xm-page-narrow"
    :class="themeClass"
    :style="themeStyle"
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
        <view class="xm-cell-icon"
          ><xm-icon
            name="user"
            :size="36"
        /></view>
        <text class="xm-cell-body xm-ellipsis">{{ $t('menu.person') }}</text>
        <view class="xm-cell-arrow"
          ><xm-icon
            name="chevron-right"
            :size="32"
        /></view>
      </view>
      <view
        class="xm-cell"
        @click="go('/pages/password/password')"
      >
        <view class="xm-cell-icon"
          ><xm-icon
            name="key"
            :size="36"
        /></view>
        <text class="xm-cell-body xm-ellipsis">{{ $t('menu.password') }}</text>
        <view class="xm-cell-arrow"
          ><xm-icon
            name="chevron-right"
            :size="32"
        /></view>
      </view>
      <view
        class="xm-cell"
        @click="go('/pages/message/message')"
      >
        <view class="xm-cell-icon"
          ><xm-icon
            name="bell"
            :size="36"
        /></view>
        <text class="xm-cell-body xm-ellipsis">{{ $t('menu.message') }}</text>
        <view
          v-if="unreadCount"
          class="cell-badge"
          >{{ unreadCount > 99 ? '99+' : unreadCount }}</view
        >
        <view class="xm-cell-arrow"
          ><xm-icon
            name="chevron-right"
            :size="32"
        /></view>
      </view>
    </view>

    <!-- 偏好设置 -->
    <view class="xm-card">
      <view class="xm-card-title">{{ $t('home.prefs') }}</view>
      <view class="xm-cell">
        <view class="xm-cell-icon"
          ><xm-icon
            name="globe"
            :size="36"
        /></view>
        <text class="xm-cell-body xm-ellipsis">{{ $t('layout.lang.label') }}</text>
        <button
          class="xm-btn pref-btn"
          @click="toggleLocale"
        >
          {{ isZhLocale() ? 'EN' : '中' }}
        </button>
      </view>
      <view class="xm-cell">
        <view class="xm-cell-icon"
          ><xm-icon
            name="moon"
            :size="36"
        /></view>
        <text class="xm-cell-body xm-ellipsis">{{ $t('layout.theme.switch') }}</text>
        <button
          class="xm-btn pref-btn"
          @click="cycleTheme"
        >
          <xm-icon
            :name="themeModeIcon"
            :size="32"
          />
        </button>
      </view>

      <!-- 主题色：预设色板点选（小程序无原生取色器；Web 端选的任意颜色同样会同步显示） -->
      <view class="xm-cell color-cell">
        <view class="xm-cell-icon"
          ><xm-icon
            name="droplet"
            :size="36"
        /></view>
        <text class="xm-cell-body xm-ellipsis">{{ $t('layout.themeColor.title') }}</text>
        <text
          v-if="isCustomOutsidePresets"
          class="color-custom-tag"
          >{{ $t('layout.themeColor.custom') }}</text
        >
      </view>
      <view class="color-swatches">
        <view
          v-for="preset in PRESET_COLORS"
          :key="preset.value || 'default'"
          class="color-swatch"
          :class="{ 'is-active': themeColor === preset.value, 'is-default': !preset.value }"
          :style="preset.value ? { background: preset.value } : {}"
          @click="setThemeColor(preset.value)"
        ></view>
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
    <xm-loader />
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { ensureLoggedIn } from '@/utils/authGuard'
import { useMessageStore } from '@/stores/message'
import { clearCookie, resolveFileUrl, SILENT } from '@/utils/request'
import { userApiOf } from '@/api'
import { closeWs } from '@/utils/websocket'
import { t } from '@/i18n'
import { confirm } from '@/utils/confirm'
import { enumLabel } from '@/utils/enums'
import { isZhLocale, toggleLocale } from '@/composables/useLocale'
import { cycleTheme, themeModeIcon } from '@/composables/useTheme'
import { PRESET_COLORS, themeColor, setThemeColor, resetThemeColorOnLogout } from '@/composables/useThemeColor'

const userStore = useUserStore()
const messageStore = useMessageStore()
const user = computed(() => userStore.user || {})

/** 消息中心未读数（本地持久化的推送历史） */
const unreadCount = computed(() => messageStore.unreadCount)

// 头像展示地址：把老数据里的 localhost 绝对地址 / 新数据的 /api 相对路径归一成当前 baseUrl 完整地址
const avatarUrl = computed(() => resolveFileUrl(user.value.avatar))

const avatarLetter = computed(() => (user.value.name || user.value.username || '?').slice(0, 1))

/** 角色码转展示名（统一枚举表，与登录页角色选项一致） */
const roleLabel = computed(() => enumLabel('role', user.value.role))

/** 当前颜色是 Web 端选的、不在预设色板里的任意颜色时，给出「自定义」提示（此时没有色块处于选中态） */
const isCustomOutsidePresets = computed(
  () => !!themeColor.value && !PRESET_COLORS.some((p) => p.value === themeColor.value),
)

const go = (path) => uni.navigateTo({ url: path })

/** 退出登录：清空用户态与验证码会话 Cookie，回到登录页 */
const logout = async () => {
  if (!(await confirm(t('layout.logoutConfirm'), { title: t('layout.logout') }))) return
  closeWs()
  userStore.clearUser()
  clearCookie()
  // 复位主题色：否则下一个在本机登录的账号会先看到上一个账号的配色
  resetThemeColorOnLogout()
  uni.reLaunch({ url: '/pages/login/login' })
}

onShow(() => {
  if (!ensureLoggedIn()) return
  uni.setNavigationBarTitle({ title: t('menu.mine') })
  // 载入当前用户的推送消息历史（消息中心入口红点）
  messageStore.loadForUser(userStore.accountKey)

  // 跨端资料同步：Web 端等其它入口修改资料（如头像）后，进入「我的」页时
  // 静默拉取一次最新用户信息，无需重新登录。
  // selectById 返回的 token 为空，回填本地 token，防止把登录态冲掉。
  const api = userApiOf(userStore.role)
  if (api && userStore.user.id) {
    // 静默同步最新资料（其他终端改过头像等）：失败不打扰用户，保留本地缓存
    api
      .selectById(userStore.user.id, SILENT)
      .then((profile) => {
        if (profile) userStore.patchUser({ ...profile, token: userStore.token })
      })
      .catch(() => {})
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

/* 消息未读数红点 */
.cell-badge {
  min-width: 34rpx;
  height: 34rpx;
  line-height: 34rpx;
  padding: 0 8rpx;
  border-radius: 999rpx;
  text-align: center;
  font-size: 20rpx;
  color: #ffffff;
  background: var(--xm-danger);
  flex-shrink: 0;
}

.logout-btn {
  margin-top: 8rpx;
}

/* 主题色：色板一行 8 个，点选即生效 */
.color-cell {
  border-bottom: none;
}

.color-custom-tag {
  font-size: 22rpx;
  padding: 2rpx 16rpx;
  border-radius: 999rpx;
  color: var(--xm-brand);
  background: var(--xm-brand-soft);
  flex-shrink: 0;
}

.color-swatches {
  display: flex;
  justify-content: space-between;
  padding: 8rpx 4rpx 12rpx;
}

.color-swatch {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  box-sizing: border-box;
  border: 4rpx solid transparent;
  box-shadow: 0 0 0 2rpx var(--xm-border);
  transition: transform 0.15s ease;
}

.color-swatch.is-active {
  border-color: var(--xm-bg-card);
  box-shadow: 0 0 0 4rpx var(--xm-text);
  transform: scale(1.08);
}

/* 「默认」色块用内置品牌渐变表示，与各自定义色区分开 */
.color-swatch.is-default {
  background: linear-gradient(135deg, #5b6cff 0%, #8b5cf6 100%);
}
</style>

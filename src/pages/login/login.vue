<template>
  <view
    class="login-page"
    :class="themeClass"
  >
    <!-- 顶部品牌区 -->
    <view class="brand">
      <view class="brand-title">{{ $t('login.systemName') }}</view>
      <view class="brand-sub">{{ $t('login.systemSub') }}</view>
      <view class="brand-slogan">{{ $t('login.slogan') }}</view>
    </view>

    <!-- 偏好切换 -->
    <view class="prefs">
      <button
        class="xm-btn xm-btn-plain pref-btn"
        @click="toggleLocale"
      >
        {{ isZhLocale() ? 'EN' : '中' }}
      </button>
      <button
        class="xm-btn xm-btn-plain pref-btn"
        @click="cycleTheme"
      >
        {{ themeMode === 'light' ? '☀' : themeMode === 'dark' ? '☾' : '◐' }}
      </button>
    </view>

    <!-- 登录卡片 -->
    <view class="xm-card login-card">
      <view class="login-title">{{ $t('login.title') }}</view>
      <view class="login-subtitle">{{ $t('login.subtitle') }}</view>

      <view class="xm-form-item">
        <input
          class="xm-input"
          v-model="form.username"
          :placeholder="$t('login.usernamePlaceholder')"
        />
      </view>
      <view class="xm-form-item">
        <input
          class="xm-input"
          v-model="form.password"
          password
          :placeholder="$t('login.passwordPlaceholder')"
        />
      </view>
      <view class="xm-form-item captcha-row">
        <input
          class="xm-input captcha-input"
          v-model="form.captcha"
          :placeholder="$t('login.captchaPlaceholder')"
        />
        <image
          v-if="captchaUrl"
          :src="captchaUrl"
          class="captcha-img"
          mode="aspectFit"
          @click="refreshCaptcha"
        />
      </view>
      <view class="xm-form-item">
        <picker
          :range="roleLabels"
          @change="onRoleChange"
        >
          <view
            class="xm-input picker-text"
            :class="{ 'picker-placeholder': !form.role }"
          >
            {{ form.role ? roleLabels[roleIndex] : $t('login.rolePlaceholder') }}
          </view>
        </picker>
      </view>

      <button
        class="xm-btn xm-btn-primary xm-btn-block login-btn"
        @click="login"
      >
        {{ $t('login.submit') }}
      </button>

      <view class="to-register">
        {{ $t('login.noAccount') }}
        <text
          class="link"
          @click="goRegister"
          >{{ $t('login.goRegister') }}</text
        >
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { request, saveCookie, clearCookie } from '@/utils/request'
import { baseUrl } from '@/utils/config'
import { t, apiMessage } from '@/i18n'
import { isZhLocale, toggleLocale } from '@/composables/useLocale'
import { cycleTheme, themeMode, themeClass, pullThemeFromServer } from '@/composables/useTheme'
import { pullLocaleFromServer } from '@/composables/useLocale'

const userStore = useUserStore()
const captchaUrl = ref('')
const form = ref({ username: '', password: '', captcha: '', role: '' })

const roleLabels = computed(() => [t('login.roleAdmin'), t('login.roleTeacher'), t('login.roleStudent')])
const roleValues = ['ADMIN', 'TEACHER', 'STUDENT']
const roleIndex = computed(() => roleValues.indexOf(form.value.role))

const onRoleChange = (e) => {
  form.value.role = roleValues[Number(e.detail.value)] || ''
}

const refreshCaptcha = async () => {
  try {
    clearCookie()
    const res = await new Promise((resolve, reject) => {
      uni.request({
        url: `${baseUrl}/captcha?t=${Date.now()}`,
        method: 'GET',
        responseType: 'arraybuffer',
        success: resolve,
        fail: reject,
      })
    })
    saveCookie(res)
    captchaUrl.value = 'data:image/gif;base64,' + uni.arrayBufferToBase64(res.data)
  } catch {
    uni.showToast({ title: t('login.captchaFailed'), icon: 'none' })
  }
}

const login = () => {
  if (!form.value.username || !form.value.password || !form.value.captcha || !form.value.role) {
    uni.showToast({ title: t('errors.4001'), icon: 'none' })
    return
  }
  request({ url: '/login', method: 'POST', data: form.value })
    .then((res) => {
      if (res.data.code === '200') {
        userStore.updateUser(res.data.data)
        pullThemeFromServer()
        pullLocaleFromServer()
        uni.reLaunch({ url: '/pages/home/home' })
      } else {
        uni.showToast({ title: apiMessage(res.data), icon: 'none' })
        refreshCaptcha()
      }
    })
    .catch(() => {
      uni.showToast({ title: t('login.loginFailed'), icon: 'none' })
      refreshCaptcha()
    })
}

const goRegister = () => {
  uni.navigateTo({ url: '/pages/register/register' })
}

refreshCaptcha()
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, var(--xm-header) 0%, var(--xm-bg-page) 45%);
  padding: 120rpx 40rpx 40rpx;
  box-sizing: border-box;
}

.brand {
  text-align: center;
  color: #ffffff;
  margin-bottom: 60rpx;
}

.brand-title {
  font-size: 44rpx;
  font-weight: bold;
}

.brand-sub {
  font-size: 24rpx;
  opacity: 0.85;
  letter-spacing: 4rpx;
  margin-top: 8rpx;
}

.brand-slogan {
  font-size: 24rpx;
  opacity: 0.75;
  margin-top: 8rpx;
}

.prefs {
  display: flex;
  justify-content: flex-end;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.pref-btn {
  height: 60rpx;
  line-height: 60rpx;
  padding: 0 24rpx;
  background: var(--xm-bg-card);
  color: var(--xm-text);
}

.login-title {
  font-size: 40rpx;
  font-weight: bold;
  text-align: center;
}

.login-subtitle {
  font-size: 26rpx;
  color: var(--xm-text-2);
  text-align: center;
  margin: 12rpx 0 32rpx;
}

.captcha-row {
  display: flex;
  gap: 16rpx;
}

.captcha-input {
  flex: 1;
}

.captcha-img {
  width: 200rpx;
  height: 76rpx;
  border-radius: 10rpx;
  border: 1rpx solid var(--xm-border);
  background: #ffffff;
}

.picker-text {
  display: flex;
  align-items: center;
  line-height: 76rpx;
}

.picker-placeholder {
  color: var(--xm-text-2);
}

.login-btn {
  margin-top: 12rpx;
}

.to-register {
  text-align: center;
  margin-top: 28rpx;
  font-size: 26rpx;
  color: var(--xm-text-2);
}

.link {
  color: var(--xm-brand);
}
</style>

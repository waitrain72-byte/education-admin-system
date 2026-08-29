<template>
  <view
    class="xm-page"
    :class="themeClass"
  >
    <view class="xm-card">
      <view class="xm-card-title">{{ $t('register.title') }}</view>
      <view
        class="xm-label"
        style="margin-bottom: 24rpx"
        >{{ $t('register.subtitle') }}</view
      >

      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('register.usernamePlaceholder') }}</view>
        <input
          class="xm-input"
          v-model="form.username"
          :placeholder="$t('register.usernamePlaceholder')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('register.passwordPlaceholder') }}</view>
        <input
          class="xm-input"
          v-model="form.password"
          password
          :placeholder="$t('register.passwordPlaceholder')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('register.confirmPlaceholder') }}</view>
        <input
          class="xm-input"
          v-model="form.confirmPass"
          password
          :placeholder="$t('register.confirmPlaceholder')"
        />
      </view>

      <button
        class="xm-btn xm-btn-primary xm-btn-block"
        @click="register"
      >
        {{ $t('register.submit') }}
      </button>
      <view class="to-login">
        {{ $t('register.hasAccount') }}
        <text
          class="link"
          @click="goLogin"
          >{{ $t('register.goLogin') }}</text
        >
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { post } from '@/utils/request'
import { t, apiMessage } from '@/i18n'

const form = ref({ username: '', password: '', confirmPass: '' })

const validate = () => {
  if (!form.value.username) return t('register.ruleUsernameRequired')
  if (form.value.username.length < 3 || form.value.username.length > 20) return t('register.ruleUsernameLength')
  if (!form.value.password) return t('register.rulePasswordRequired')
  if (form.value.password.length < 6 || form.value.password.length > 20) return t('register.rulePasswordLength')
  if (!form.value.confirmPass) return t('register.ruleConfirmRequired')
  if (form.value.confirmPass !== form.value.password) return t('register.ruleConfirmMismatch')
  return ''
}

const register = () => {
  const msg = validate()
  if (msg) {
    uni.showToast({ title: msg, icon: 'none' })
    return
  }
  post('/register', { username: form.value.username, password: form.value.password, role: 'STUDENT' })
    .then((res) => {
      if (res.data.code === '200') {
        uni.showToast({ title: t('register.success'), icon: 'success' })
        setTimeout(() => uni.navigateBack(), 800)
      } else {
        uni.showToast({ title: apiMessage(res.data), icon: 'none' })
      }
    })
    .catch(() => {
      uni.showToast({ title: t('register.failedRetry'), icon: 'none' })
    })
}

const goLogin = () => {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.to-login {
  text-align: center;
  margin-top: 28rpx;
  font-size: 26rpx;
  color: var(--xm-text-2);
}

.link {
  color: var(--xm-brand);
}
</style>

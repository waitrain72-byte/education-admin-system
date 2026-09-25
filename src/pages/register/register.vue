<template>
  <view
    class="xm-page xm-page-narrow"
    :class="themeClass"
    :style="themeStyle"
  >
    <view class="xm-card">
      <view class="xm-card-title">{{ $t('register.title') }}</view>
      <view class="xm-label register-sub">{{ $t('register.subtitle') }}</view>

      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('register.usernamePlaceholder') }}</view>
        <input
          class="xm-input"
          v-model="form.username"
          :placeholder="$t('register.usernamePlaceholder')"
          confirm-type="next"
          @confirm="focusTo('password')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('register.passwordPlaceholder') }}</view>
        <input
          class="xm-input"
          v-model="form.password"
          password
          :placeholder="$t('register.passwordPlaceholder')"
          :focus="focused === 'password'"
          confirm-type="next"
          @confirm="focusTo('confirmPass')"
          @blur="onBlur('password')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('register.confirmPlaceholder') }}</view>
        <input
          class="xm-input"
          v-model="form.confirmPass"
          password
          :placeholder="$t('register.confirmPlaceholder')"
          :focus="focused === 'confirmPass'"
          confirm-type="done"
          @confirm="register"
          @blur="onBlur('confirmPass')"
        />
      </view>

      <button
        class="xm-btn xm-btn-primary xm-btn-block"
        :loading="submitting"
        :disabled="submitting"
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
    <xm-loader />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { accountApi } from '@/api'
import { t } from '@/i18n'
import { useFocusChain } from '@/composables/useFocusChain'

const form = ref({ username: '', password: '', confirmPass: '' })
// 键盘「下一项」依次跳到密码、确认密码，最后按「完成」直接注册
const { focused, focusTo, onBlur } = useFocusChain()

const validate = () => {
  if (!form.value.username) return t('register.ruleUsernameRequired')
  if (form.value.username.length < 3 || form.value.username.length > 20) return t('register.ruleUsernameLength')
  if (!form.value.password) return t('register.rulePasswordRequired')
  if (form.value.password.length < 6 || form.value.password.length > 20) return t('register.rulePasswordLength')
  if (!form.value.confirmPass) return t('register.ruleConfirmRequired')
  if (form.value.confirmPass !== form.value.password) return t('register.ruleConfirmMismatch')
  return ''
}

// 提交中：键盘「完成」与按钮都能触发，防止连点重复注册；成功后返回登录页前保持禁用
const submitting = ref(false)

const register = () => {
  if (submitting.value) return
  const msg = validate()
  if (msg) {
    uni.showToast({ title: msg, icon: 'none' })
    return
  }
  submitting.value = true
  accountApi
    .register({ username: form.value.username, password: form.value.password, role: 'STUDENT' })
    .then(() => {
      uni.showToast({ title: t('register.success'), icon: 'success' })
      setTimeout(() => uni.navigateBack(), 800)
    })
    .catch(() => {
      // 用户名已存在等提示已由请求层统一弹出
      submitting.value = false
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

.register-sub {
  margin-bottom: 24rpx;
}
</style>

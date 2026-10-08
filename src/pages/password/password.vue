<template>
  <view
    class="xm-page xm-page-narrow"
    :class="themeClass"
    :style="themeStyle"
  >
    <view class="xm-card">
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.password.originalPassword') }}</view>
        <input
          class="xm-input"
          v-model="form.password"
          password
          :placeholder="$t('pages.password.originalPassword')"
          confirm-type="next"
          @confirm="focusTo('newPassword')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.password.newPassword') }}</view>
        <input
          class="xm-input"
          v-model="form.newPassword"
          password
          maxlength="20"
          :placeholder="$t('register.rulePasswordLength')"
          :focus="focused === 'newPassword'"
          confirm-type="next"
          @confirm="focusTo('confirmPassword')"
          @blur="onBlur('newPassword')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.password.confirmPassword') }}</view>
        <input
          class="xm-input"
          v-model="form.confirmPassword"
          password
          maxlength="20"
          :placeholder="$t('pages.password.confirmPlaceholder')"
          :focus="focused === 'confirmPassword'"
          confirm-type="done"
          @confirm="update"
          @blur="onBlur('confirmPassword')"
        />
      </view>
      <view class="note">{{ $t('profile.passwordNote') }}</view>

      <button
        class="xm-btn xm-btn-primary xm-btn-block xm-btn-lg"
        :loading="submitting"
        :disabled="submitting"
        @click="update"
      >
        {{ $t('pages.password.submit') }}
      </button>
    </view>
    <xm-loader />
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { accountApi } from '@/api'
import { closeWs } from '@/utils/websocket'
import { useUserStore } from '@/stores/user'
import { ensureLoggedIn } from '@/utils/authGuard'
import { t } from '@/i18n'
import { useFocusChain } from '@/composables/useFocusChain'

const userStore = useUserStore()
const form = reactive({ password: '', newPassword: '', confirmPassword: '' })
// 键盘「下一项」依次跳到新密码、确认密码，最后按「完成」直接提交
const { focused, focusTo, onBlur } = useFocusChain()

onShow(() => {
  if (!ensureLoggedIn()) return
  uni.setNavigationBarTitle({ title: t('profile.tabs.password') })
})

// 提交中：键盘「完成」与按钮都能触发，防止连点重复提交（第二次会因原密码已变而报错）；成功后跳登录页前保持禁用
const submitting = ref(false)

const validate = () => {
  if (!form.password) return t('pages.password.ruleOriginalRequired')
  if (!form.newPassword) return t('pages.password.ruleNewRequired')
  if (form.newPassword.length < 6 || form.newPassword.length > 20) return t('register.rulePasswordLength')
  if (!form.confirmPassword) return t('pages.password.ruleConfirmRequired')
  if (form.confirmPassword !== form.newPassword) return t('pages.password.ruleConfirmMismatch')
  return ''
}

const update = async () => {
  if (submitting.value) return
  const tip = validate()
  if (tip) {
    uni.showToast({ title: tip, icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await accountApi.updatePassword({
      username: userStore.user.username,
      role: userStore.user.role,
      password: form.password,
      newPassword: form.newPassword,
    })
    // 密码已修改，旧 token 即将失效：断开实时通知连接并重新登录
    closeWs()
    userStore.clearUser()
    uni.showToast({ title: t('pages.password.success'), icon: 'success' })
    setTimeout(() => uni.reLaunch({ url: '/pages/login/login' }), 800)
  } catch {
    // 原密码错误等提示已由请求层统一弹出
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.note {
  margin: -4rpx 0 28rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: var(--xm-text-3);
}
</style>

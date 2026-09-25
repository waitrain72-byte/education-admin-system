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
          :placeholder="$t('pages.password.newPassword')"
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
          :placeholder="$t('pages.password.confirmPlaceholder')"
          :focus="focused === 'confirmPassword'"
          confirm-type="done"
          @confirm="update"
          @blur="onBlur('confirmPassword')"
        />
      </view>

      <button
        class="xm-btn xm-btn-primary xm-btn-block"
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
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { accountApi } from '@/api'
import { closeWs } from '@/utils/websocket'
import { useUserStore } from '@/stores/user'
import { t } from '@/i18n'
import { useFocusChain } from '@/composables/useFocusChain'

const userStore = useUserStore()
const form = ref({ username: '', role: '', password: '', newPassword: '', confirmPassword: '' })
// 键盘「下一项」依次跳到新密码、确认密码，最后按「完成」直接提交
const { focused, focusTo, onBlur } = useFocusChain()

onShow(() => {
  uni.setNavigationBarTitle({ title: t('menu.password') })
  form.value.username = userStore.user.username || ''
  form.value.role = userStore.user.role || ''
})

// 提交中：键盘「完成」与按钮都能触发，防止连点重复提交（第二次会因原密码已变而报错）；成功后跳登录页前保持禁用
const submitting = ref(false)

const update = () => {
  if (submitting.value) return
  if (!form.value.password) return showToast(t('pages.password.ruleOriginalRequired'))
  if (!form.value.newPassword) return showToast(t('pages.password.ruleNewRequired'))
  if (!form.value.confirmPassword) return showToast(t('pages.password.ruleConfirmRequired'))
  if (form.value.confirmPassword !== form.value.newPassword) return showToast(t('pages.password.ruleConfirmMismatch'))

  submitting.value = true
  accountApi
    .updatePassword(form.value)
    .then(() => {
      // 密码已修改，旧 token 即将失效：断开实时通知连接并重新登录
      closeWs()
      userStore.clearUser()
      uni.showToast({ title: t('pages.password.success'), icon: 'success' })
      setTimeout(() => uni.reLaunch({ url: '/pages/login/login' }), 800)
    })
    .catch(() => {
      // 原密码错误等提示已由请求层统一弹出
      submitting.value = false
    })
}

function showToast(msg) {
  uni.showToast({ title: msg, icon: 'none' })
}
</script>

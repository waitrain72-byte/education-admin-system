<template>
  <view
    class="xm-page"
    :class="themeClass"
  >
    <view class="xm-card">
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.password.originalPassword') }}</view>
        <input
          class="xm-input"
          v-model="form.password"
          password
          :placeholder="$t('pages.password.originalPassword')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.password.newPassword') }}</view>
        <input
          class="xm-input"
          v-model="form.newPassword"
          password
          :placeholder="$t('pages.password.newPassword')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.password.confirmPassword') }}</view>
        <input
          class="xm-input"
          v-model="form.confirmPassword"
          password
          :placeholder="$t('pages.password.confirmPlaceholder')"
        />
      </view>

      <button
        class="xm-btn xm-btn-primary xm-btn-block"
        @click="update"
      >
        {{ $t('pages.password.submit') }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { put } from '@/utils/request'
import { closeWs } from '@/utils/websocket'
import { useUserStore } from '@/stores/user'
import { t, apiMessage } from '@/i18n'

const userStore = useUserStore()
const form = ref({ username: '', role: '', password: '', newPassword: '', confirmPassword: '' })

onShow(() => {
  uni.setNavigationBarTitle({ title: t('menu.password') })
  form.value.username = userStore.user.username || ''
  form.value.role = userStore.user.role || ''
})

const update = () => {
  if (!form.value.password) return showToast(t('pages.password.ruleOriginalRequired'))
  if (!form.value.newPassword) return showToast(t('pages.password.ruleNewRequired'))
  if (!form.value.confirmPassword) return showToast(t('pages.password.ruleConfirmRequired'))
  if (form.value.confirmPassword !== form.value.newPassword) return showToast(t('pages.password.ruleConfirmMismatch'))

  put('/updatePassword', form.value).then((res) => {
    if (res.data.code === '200') {
      // 密码已修改，旧 token 即将失效：断开实时通知连接并重新登录
      closeWs()
      userStore.clearUser()
      uni.showToast({ title: t('pages.password.success'), icon: 'success' })
      setTimeout(() => uni.reLaunch({ url: '/pages/login/login' }), 800)
    } else {
      showToast(apiMessage(res.data))
    }
  })
}

function showToast(msg) {
  uni.showToast({ title: msg, icon: 'none' })
}
</script>

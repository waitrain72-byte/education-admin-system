<template>
  <view
    :class="themeClass"
    :style="themeStyle"
  >
    <xm-auth-shell>
      <view class="form-title">{{ $t('register.heading') }}</view>
      <view class="form-sub">{{ $t('register.hint') }}</view>

      <!-- 键盘「下一项」依次跳到密码、确认密码，最后按「完成」直接注册 -->
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('login.account') }}</view>
        <input
          class="xm-input field"
          v-model="form.username"
          maxlength="20"
          :placeholder="$t('register.usernamePlaceholder')"
          confirm-type="next"
          @confirm="focusTo('password')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('login.password') }}</view>
        <input
          class="xm-input field"
          v-model="form.password"
          password
          maxlength="20"
          :placeholder="$t('register.passwordPlaceholder')"
          :focus="focused === 'password'"
          confirm-type="next"
          @confirm="focusTo('confirm')"
          @blur="onBlur('password')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('register.confirm') }}</view>
        <input
          class="xm-input field"
          v-model="form.confirmPass"
          password
          maxlength="20"
          :placeholder="$t('register.confirmPlaceholder')"
          :focus="focused === 'confirm'"
          confirm-type="done"
          @confirm="register"
          @blur="onBlur('confirm')"
        />
      </view>

      <button
        class="xm-btn xm-btn-primary xm-btn-block xm-btn-lg submit"
        :loading="submitting"
        :disabled="submitting"
        @click="register"
      >
        {{ $t('register.submit') }}
      </button>

      <view class="form-foot">
        {{ $t('register.hasAccount') }}
        <text
          class="xm-link"
          @click="goLogin"
          >{{ $t('register.goLogin') }}</text
        >
      </view>
    </xm-auth-shell>
    <xm-loader />
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { accountApi } from '@/api'
import { t } from '@/i18n'
import { useFocusChain } from '@/composables/useFocusChain'

const form = reactive({ username: '', password: '', confirmPass: '' })
const { focused, focusTo, onBlur } = useFocusChain()
// 提交中：键盘「完成」与按钮都能触发，防止连点重复注册；成功后回到登录页前保持禁用
const submitting = ref(false)

const validate = () => {
  const username = form.username.trim()
  if (!username) return t('register.ruleUsernameRequired')
  if (username.length < 3 || username.length > 20) return t('register.ruleUsernameLength')
  if (!form.password) return t('register.rulePasswordRequired')
  if (form.password.length < 6 || form.password.length > 20) return t('register.rulePasswordLength')
  if (!form.confirmPass) return t('register.ruleConfirmRequired')
  if (form.confirmPass !== form.password) return t('register.ruleConfirmMismatch')
  return ''
}

/** 只开放学生自助注册；教师、管理员账号由教务后台创建 */
const register = async () => {
  if (submitting.value) return
  const tip = validate()
  if (tip) {
    uni.showToast({ title: tip, icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await accountApi.register({ username: form.username.trim(), password: form.password, role: 'STUDENT' })
    uni.showToast({ title: t('register.success'), icon: 'success' })
    setTimeout(goLogin, 800)
  } catch {
    // 用户名已存在等提示已由请求层统一弹出
    submitting.value = false
  }
}

/** 从登录页进来的直接返回；直接打开注册页（如分享卡片）时没有上一页，改为打开登录页 */
const goLogin = () => {
  if (getCurrentPages().length > 1) {
    uni.navigateBack()
  } else {
    uni.reLaunch({ url: '/pages/login/login' })
  }
}
</script>

<style lang="scss" scoped>
.form-title {
  font-size: 40rpx;
  font-weight: bold;
  color: var(--xm-text);
}

.form-sub {
  margin: 8rpx 0 36rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: var(--xm-text-2);
}

.field {
  height: 88rpx;
  border-radius: 18rpx;
}

.submit {
  margin-top: 12rpx;
}

.form-foot {
  margin-top: 28rpx;
  text-align: center;
  font-size: 26rpx;
  color: var(--xm-text-2);
}
</style>

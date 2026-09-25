<template>
  <view
    class="login-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 顶部品牌区 -->
    <view class="brand">
      <view class="brand-logo">
        <xm-icon
          name="graduation"
          :size="68"
        />
      </view>
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
        <xm-icon
          :name="themeModeIcon"
          :size="32"
        />
      </button>
    </view>

    <!-- 登录卡片 -->
    <view class="xm-card login-card">
      <view class="login-title">{{ $t('login.title') }}</view>
      <view class="login-subtitle">{{ $t('login.subtitle') }}</view>

      <!-- 角色：三选一分段控件，一次点击完成（替代需要滚动确认的原生 picker） -->
      <view class="xm-form-item role-seg">
        <view
          v-for="opt in roleOptions"
          :key="opt.value"
          class="role-seg-item"
          :class="{ on: form.role === opt.value }"
          @click="form.role = opt.value"
          >{{ opt.label }}</view
        >
      </view>

      <!-- 键盘「下一项」依次跳到密码、验证码，验证码处按「完成」直接登录 -->
      <view class="xm-form-item">
        <input
          class="xm-input"
          v-model="form.username"
          :placeholder="$t('login.usernamePlaceholder')"
          confirm-type="next"
          @confirm="focusTo('password')"
        />
      </view>
      <view class="xm-form-item">
        <input
          class="xm-input"
          v-model="form.password"
          password
          :placeholder="$t('login.passwordPlaceholder')"
          :focus="focused === 'password'"
          confirm-type="next"
          @confirm="focusTo('captcha')"
          @blur="onBlur('password')"
        />
      </view>
      <view class="xm-form-item captcha-row">
        <input
          class="xm-input captcha-input"
          v-model="form.captcha"
          :placeholder="$t('login.captchaPlaceholder')"
          :focus="focused === 'captcha'"
          confirm-type="done"
          @confirm="login"
          @blur="onBlur('captcha')"
        />
        <image
          v-if="captchaUrl"
          :src="captchaUrl"
          class="captcha-img"
          mode="aspectFit"
          @click="refreshCaptcha"
        />
      </view>
      <button
        class="xm-btn xm-btn-primary xm-btn-block login-btn"
        :loading="loginBusy"
        :disabled="loginBusy"
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
    <xm-loader />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { accountApi } from '@/api'
import { connectWs } from '@/utils/websocket'
import { t } from '@/i18n'
import { enumOptions } from '@/utils/enums'
import { isZhLocale, toggleLocale, pullLocaleFromServer } from '@/composables/useLocale'
import { cycleTheme, themeModeIcon, pullThemeFromServer } from '@/composables/useTheme'
import { pullThemeColorFromServer } from '@/composables/useThemeColor'
import { usePermission } from '@/composables/usePermission'
import { useFocusChain } from '@/composables/useFocusChain'

const userStore = useUserStore()
const { pullPermissions } = usePermission()
const { focused, focusTo, onBlur } = useFocusChain()
const captchaUrl = ref('')
const form = ref({ username: '', password: '', captcha: '', role: '' })

const roleOptions = computed(() => enumOptions('role'))

// 记住上次登录的账号与角色（不存密码）：再次登录只需输入密码和验证码；退出登录不清除
const LAST_LOGIN_KEY = 'xm-last-login'
try {
  const last = uni.getStorageSync(LAST_LOGIN_KEY)
  if (last && typeof last === 'object') {
    form.value.username = last.username || ''
    form.value.role = roleOptions.value.some((o) => o.value === last.role) ? last.role : ''
  }
} catch {}

// 缺哪项提示哪项（替代笼统的「参数缺失」）
const missingFieldTip = () => {
  if (!form.value.role) return t('login.rolePlaceholder')
  if (!form.value.username) return t('login.usernamePlaceholder')
  if (!form.value.password) return t('login.passwordPlaceholder')
  if (!form.value.captcha) return t('login.captchaPlaceholder')
  return ''
}

const refreshCaptcha = async () => {
  // 验证码一码一用（后端读取即失效）：换新图时旧输入必然错误，必须清空，
  // 否则用户直接再点登录会平白多失败一次
  form.value.captcha = ''
  try {
    captchaUrl.value = await accountApi.captcha()
  } catch {
    uni.showToast({ title: t('login.captchaFailed'), icon: 'none' })
  }
}

// 登录防重复提交：请求进行中按钮转圈并禁用
const loginBusy = ref(false)

const login = () => {
  if (loginBusy.value) return
  const tip = missingFieldTip()
  if (tip) {
    uni.showToast({ title: tip, icon: 'none' })
    return
  }
  loginBusy.value = true
  accountApi
    .login(form.value)
    .then((account) => {
      try {
        uni.setStorageSync(LAST_LOGIN_KEY, { username: form.value.username, role: form.value.role })
      } catch {}
      userStore.updateUser(account)
      // 拉取当前用户 RBAC 权限码（与 Web 端一致，供首页菜单按权限过滤）
      pullPermissions()
      pullThemeFromServer()
      pullThemeColorFromServer()
      pullLocaleFromServer()
      // 登录成功后建立实时通知连接（成绩发布/作业批改/请假审批/教务通知推送）
      connectWs()
      uni.reLaunch({ url: '/pages/home/home' })
    })
    .catch(() => {
      // 失败原因（密码错误 / 验证码错误 / 账号锁定 / 断网）的提示已由请求层统一弹出；
      // 验证码一码一用，无论哪种失败都要换一张
      refreshCaptcha()
    })
    .finally(() => {
      loginBusy.value = false
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
  /* 自定义导航页：顶部间距 = 状态栏实际高度 + 50rpx，适配刘海屏/胶囊按钮区域 */
  padding: calc(var(--status-bar-height, 25px) + 50rpx) 40rpx 40rpx;
  box-sizing: border-box;
}

.brand {
  text-align: center;
  color: #ffffff;
  margin-bottom: 60rpx;
}

/* 品牌 logo：毛玻璃圆角块，在渐变底上勾勒出层次 */
.brand-logo {
  width: 128rpx;
  height: 128rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24rpx;
  border-radius: 36rpx;
  background: rgba(255, 255, 255, 0.16);
  border: 1rpx solid rgba(255, 255, 255, 0.25);
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

/* 登录卡片：悬浮于渐变底之上，投影加重突出主体 */
.login-card {
  box-shadow: 0 20rpx 60rpx rgba(31, 45, 90, 0.14);
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
  height: 80rpx;
  border-radius: 16rpx;
  border: 1rpx solid var(--xm-border);
  background: #ffffff;
}

/* 角色分段控件：输入框同款底色的槽 + 选中项主色填充 */
.role-seg {
  display: flex;
  gap: 8rpx;
  padding: 8rpx;
  border-radius: 16rpx;
  background: var(--xm-bg-input);
}

.role-seg-item {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: var(--xm-text-2);
  transition:
    background 0.2s,
    color 0.2s;
}

.role-seg-item.on {
  background: var(--xm-brand);
  color: #ffffff;
  font-weight: bold;
  box-shadow: var(--xm-shadow);
}

/* 登录按钮：胶囊大按钮 + 主色投影，与高星项目表单收尾一致 */
.login-btn {
  margin-top: 16rpx;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 999rpx;
  font-size: 32rpx;
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

/* 平板 / PC：品牌区与登录卡片居中，内容宽 440px */
@media (min-width: 720px) {
  .login-page {
    padding-left: calc((100% - 440px) / 2);
    padding-right: calc((100% - 440px) / 2);
  }
}
</style>

<template>
  <view
    :class="themeClass"
    :style="themeStyle"
  >
    <xm-auth-shell>
      <view class="form-title">{{ $t('login.heading') }}</view>
      <view class="form-sub">{{ $t('login.hint') }}</view>

      <!-- 键盘「下一项」依次跳到密码、验证码，验证码处按「完成」直接登录 -->
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('login.account') }}</view>
        <view class="field">
          <view class="field-icon">
            <xm-icon
              name="user"
              :size="32"
            />
          </view>
          <input
            class="field-input"
            v-model="form.username"
            :placeholder="$t('login.usernamePlaceholder')"
            confirm-type="next"
            @confirm="focusTo('password')"
          />
        </view>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('login.password') }}</view>
        <view class="field">
          <view class="field-icon">
            <xm-icon
              name="lock"
              :size="32"
            />
          </view>
          <input
            class="field-input"
            v-model="form.password"
            :password="!showPassword"
            :placeholder="$t('login.passwordPlaceholder')"
            :focus="focused === 'password'"
            confirm-type="next"
            @confirm="focusTo('captcha')"
            @blur="onBlur('password')"
          />
          <view
            class="field-eye"
            @click="showPassword = !showPassword"
          >
            <xm-icon
              :name="showPassword ? 'eye-off' : 'eye'"
              :size="32"
            />
          </view>
        </view>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('login.captcha') }}</view>
        <view class="captcha">
          <view class="field captcha-field">
            <input
              class="field-input"
              v-model="form.captcha"
              :placeholder="$t('login.captchaPlaceholder')"
              :focus="focused === 'captcha'"
              confirm-type="done"
              @confirm="login"
              @blur="onBlur('captcha')"
            />
          </view>
          <image
            v-if="captchaUrl"
            :src="captchaUrl"
            class="captcha-img"
            mode="aspectFit"
            :aria-label="$t('login.captchaAlt')"
            @click="refreshCaptcha"
          />
          <view
            v-else
            class="captcha-img"
            @click="refreshCaptcha"
          />
        </view>
      </view>

      <!-- 同一个账号名对应多个身份且密码相同（老数据可能出现）：后端返回 5012，这时才让用户选身份 -->
      <view
        v-if="needRole"
        class="role-pick"
      >
        <view class="role-pick-hint">{{ $t('login.pickRoleHint') }}</view>
        <view class="xm-seg">
          <view
            v-for="r in ROLES"
            :key="r"
            class="xm-seg-item"
            :class="{ on: form.role === r }"
            @click="form.role = r"
            >{{ $t('shell.roles.' + r) }}</view
          >
        </view>
      </view>

      <button
        class="xm-btn xm-btn-primary xm-btn-block xm-btn-lg submit"
        :loading="submitting"
        :disabled="submitting"
        @click="login"
      >
        {{ $t('login.submit') }}
      </button>

      <view class="form-foot">
        {{ $t('login.noAccount') }}
        <text
          class="xm-link"
          @click="goRegister"
          >{{ $t('login.goRegister') }}</text
        >
      </view>
    </xm-auth-shell>
    <xm-loader />
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { accountApi } from '@/api'
import { ApiError } from '@/utils/request'
import { connectWs } from '@/utils/websocket'
import { t } from '@/i18n'
import { pullLocaleFromServer } from '@/composables/useLocale'
import { pullThemeFromServer } from '@/composables/useTheme'
import { pullThemeColorFromServer } from '@/composables/useThemeColor'
import { usePermission } from '@/composables/usePermission'
import { useFocusChain } from '@/composables/useFocusChain'

const ROLES = ['STUDENT', 'TEACHER', 'ADMIN']

const userStore = useUserStore()
const { pullPermissions } = usePermission()
const { focused, focusTo, onBlur } = useFocusChain()

const captchaUrl = ref('')
const showPassword = ref(false)
const submitting = ref(false)
/** 默认不选身份，由后端按账号判断；只有返回 5012（一个账号名对应多个身份）时才让用户补选 */
const needRole = ref(false)
const form = reactive({ username: '', password: '', captcha: '', role: '' })

// 记住上次登录的账号（不存密码）：再次登录只需输入密码和验证码；退出登录不清除
const LAST_LOGIN_KEY = 'xm-last-login'
try {
  const last = uni.getStorageSync(LAST_LOGIN_KEY)
  if (last && typeof last === 'object') form.username = last.username || ''
} catch {
  // 存储不可用时从空白开始
}

const refreshCaptcha = async () => {
  // 验证码一码一用（后端读取即失效）：换新图时旧输入必然错误，必须清空
  form.captcha = ''
  try {
    captchaUrl.value = await accountApi.captcha()
  } catch {
    uni.showToast({ title: t('login.captchaFailed'), icon: 'none' })
  }
}

// 缺哪项提示哪项（替代笼统的「参数缺失」）
const missingFieldTip = () => {
  if (!form.username.trim()) return t('register.ruleUsernameRequired')
  if (!form.password) return t('register.rulePasswordRequired')
  if (!form.captcha) return t('login.captchaPlaceholder')
  if (needRole.value && !form.role) return t('login.pickRole')
  return ''
}

const login = async () => {
  if (submitting.value) return
  const tip = missingFieldTip()
  if (tip) {
    uni.showToast({ title: tip, icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const account = await accountApi.login({
      username: form.username.trim(),
      password: form.password,
      captcha: form.captcha,
      role: form.role || undefined,
    })
    try {
      uni.setStorageSync(LAST_LOGIN_KEY, { username: form.username.trim() })
    } catch {
      // 记不住账号不影响登录
    }
    userStore.updateUser(account)
    // 权限码、该账号保存的主题 / 主题色 / 语言、未读消息数都在后台拉取，不挡住进入首页
    pullPermissions()
    pullThemeFromServer()
    pullThemeColorFromServer()
    pullLocaleFromServer()
    useMessageStore().refresh()
    // 建立实时通知连接（成绩发布、作业批改、请假审批、教务通知、课堂签到）
    connectWs()
    const welcome = t('login.welcomeBack', { name: (account && account.name) || form.username.trim() })
    uni.reLaunch({
      url: '/pages/home/home',
      success: () => uni.showToast({ title: welcome, icon: 'none' }),
    })
  } catch (e) {
    if (e instanceof ApiError && e.code === '5012') needRole.value = true
    // 失败原因（密码错误 / 验证码错误 / 账号锁定 / 断网）的提示已由请求层统一弹出；
    // 验证码一码一用，无论哪种失败都要换一张
    refreshCaptcha()
  } finally {
    submitting.value = false
  }
}

const goRegister = () => {
  uni.navigateTo({ url: '/pages/register/register' })
}

refreshCaptcha()
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

/* 带前置图标的输入框：外框画在容器上，input 本身透明 */
.field {
  display: flex;
  align-items: center;
  gap: 16rpx;
  height: 88rpx;
  padding: 0 24rpx;
  border: 1rpx solid var(--xm-border);
  border-radius: 18rpx;
  background: var(--xm-bg-input);
  box-sizing: border-box;
}

.field-icon {
  display: flex;
  color: var(--xm-text-3);
}

.field-input {
  flex: 1;
  min-width: 0;
  height: 100%;
  font-size: 28rpx;
  color: var(--xm-text);
}

.field-eye {
  display: flex;
  padding: 12rpx 0 12rpx 12rpx;
  color: var(--xm-text-3);
}

.captcha {
  display: flex;
  gap: 16rpx;
}

.captcha-field {
  flex: 1;
}

.captcha-img {
  width: 220rpx;
  height: 88rpx;
  flex-shrink: 0;
  border: 1rpx solid var(--xm-border);
  border-radius: 18rpx;
  /* 验证码图是白底深字，深色模式下也保持白底才看得清 */
  background: #ffffff;
  box-sizing: border-box;
}

.role-pick {
  margin-bottom: 28rpx;
  padding: 20rpx;
  border-radius: 18rpx;
  background: var(--xm-warning-soft);
}

.role-pick-hint {
  margin-bottom: 16rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: var(--xm-warning);
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

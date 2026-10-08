<template>
  <view
    class="xm-page xm-page-narrow"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 基本资料（与 Web 端个人中心「基本资料」一致）：头像点一下就换、立即保存；姓名、电话、邮箱改完点保存 -->
    <view class="xm-card">
      <view
        class="avatar-row"
        @click="chooseAvatar"
      >
        <xm-user-avatar
          :name="user.name || user.username"
          :avatar="user.avatar"
          :size="128"
        />
        <view class="avatar-text">
          <view class="avatar-name">{{ user.name || user.username }}</view>
          <view class="xm-link">{{ $t('profile.changeAvatar') }}</view>
        </view>
      </view>

      <view class="xm-kv">
        <text class="xm-kv-key">{{ $t('login.account') }}</text>
        <text class="xm-kv-value xm-num">{{ user.username }}</text>
      </view>
      <view class="xm-kv">
        <text class="xm-kv-key">{{ $t('pages.person.roleLabel') }}</text>
        <text class="xm-kv-value">{{ $t('shell.roles.' + user.role) }}</text>
      </view>
      <view
        v-if="user.role === 'TEACHER'"
        class="xm-kv"
      >
        <text class="xm-kv-key">{{ $t('pages.person.titleLabel') }}</text>
        <text class="xm-kv-value">{{ user.title || '—' }}</text>
      </view>
    </view>

    <view class="xm-card">
      <view class="xm-form-item">
        <view class="xm-form-label required">{{ $t('pages.person.nameLabel') }}</view>
        <input
          class="xm-input"
          v-model="info.name"
          maxlength="20"
        />
      </view>
      <template v-if="user.role !== 'STUDENT'">
        <view class="xm-form-item">
          <view class="xm-form-label">{{ $t('pages.person.phoneLabel') }}</view>
          <input
            class="xm-input"
            v-model="info.phone"
            type="number"
            maxlength="20"
          />
        </view>
        <view class="xm-form-item">
          <view class="xm-form-label">{{ $t('pages.person.emailLabel') }}</view>
          <input
            class="xm-input"
            v-model="info.email"
            maxlength="50"
          />
        </view>
      </template>
      <view
        v-else
        class="note"
        >{{ $t('profile.studentNote') }}</view
      >
      <button
        class="xm-btn xm-btn-primary xm-btn-block xm-btn-lg"
        :loading="saving"
        :disabled="saving"
        @click="saveInfo"
      >
        {{ $t('common.save') }}
      </button>
    </view>
    <xm-loader />
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { userApiOf } from '@/api'
import { useUserStore } from '@/stores/user'
import { ensureLoggedIn } from '@/utils/authGuard'
import { chooseImage, uploadFile } from '@/utils/upload'
import { t } from '@/i18n'

const userStore = useUserStore()
const user = computed(() => userStore.user || {})
const info = reactive({ name: '', phone: '', email: '' })
const saving = ref(false)

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

onShow(() => {
  if (!ensureLoggedIn()) return
  uni.setNavigationBarTitle({ title: t('profile.tabs.info') })
  info.name = user.value.name || ''
  info.phone = user.value.phone || ''
  info.email = user.value.email || ''
})

/** 只提交本人可改的字段；账号、角色、学分等由后端白名单兜底，不会被改动 */
const saveInfo = async () => {
  if (!info.name.trim()) {
    uni.showToast({ title: t('profile.nameRequired'), icon: 'none' })
    return
  }
  if (user.value.role !== 'STUDENT' && info.email.trim() && !EMAIL_RE.test(info.email.trim())) {
    uni.showToast({ title: t('profile.emailInvalid'), icon: 'none' })
    return
  }
  const api = userApiOf(user.value.role)
  if (!api) return
  const payload = { id: user.value.id, name: info.name.trim() }
  if (user.value.role !== 'STUDENT') {
    payload.phone = info.phone.trim()
    payload.email = info.email.trim()
  }
  saving.value = true
  try {
    await api.update(payload)
    userStore.patchUser(payload)
    uni.showToast({ title: t('common.saveSuccess'), icon: 'success' })
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    saving.value = false
  }
}

/** 头像：选图（压缩、超限本地拦截）→ 上传 → 立即保存（不必再点「保存」） */
const chooseAvatar = async () => {
  const picked = await chooseImage()
  if (!picked) return
  const api = userApiOf(user.value.role)
  if (!api) return
  try {
    const url = await uploadFile(picked.path)
    await api.update({ id: user.value.id, avatar: url })
    userStore.patchUser({ avatar: url })
    uni.showToast({ title: t('profile.avatarSaved'), icon: 'success' })
  } catch {
    // 上传 / 保存失败的提示已由上传层、请求层统一弹出
  }
}
</script>

<style lang="scss" scoped>
.avatar-row {
  display: flex;
  align-items: center;
  gap: 28rpx;
  margin-bottom: 16rpx;
}

.avatar-text {
  flex: 1;
  min-width: 0;
}

.avatar-name {
  margin-bottom: 8rpx;
  font-size: 34rpx;
  font-weight: bold;
  color: var(--xm-text);
}

.note {
  margin-bottom: 28rpx;
  padding: 18rpx 20rpx;
  border-radius: 14rpx;
  background: var(--xm-bg-sunken);
  font-size: 24rpx;
  line-height: 1.6;
  color: var(--xm-text-2);
}
</style>

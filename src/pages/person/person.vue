<template>
  <view
    class="xm-page"
    :class="themeClass"
  >
    <view class="xm-card">
      <!-- 头像：点击更换（管理员/教师/学生共用，与 Web 端一致） -->
      <view
        class="avatar-row"
        @click="chooseAvatar"
      >
        <image
          v-if="user.avatar"
          :src="avatarUrl"
          class="avatar"
          mode="aspectFill"
        />
        <view
          v-else
          class="avatar avatar-placeholder"
          >{{ (user.name || user.username || '?').slice(0, 1) }}</view
        >
        <view class="xm-label">{{ $t('pages.person.avatarTip') }}</view>
      </view>

      <view class="xm-form-item">
        <view class="xm-form-label">{{
          isAdminOrTeacher ? $t('pages.person.usernameLabel') : $t('pages.person.accountLabel')
        }}</view>
        <input
          class="xm-input"
          v-model="user.username"
          disabled
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.person.nameLabel') }}</view>
        <input
          class="xm-input"
          v-model="user.name"
        />
      </view>
      <view
        class="xm-form-item"
        v-if="isStudent"
      >
        <view class="xm-form-label">{{ $t('pages.person.creditLabel') }}</view>
        <input
          class="xm-input"
          v-model="user.score"
          disabled
        />
      </view>
      <view
        class="xm-form-item"
        v-if="isStudent"
      >
        <view class="xm-form-label">{{ $t('pages.person.roleLabel') }}</view>
        <input
          class="xm-input"
          v-model="user.role"
          disabled
        />
      </view>
      <view
        class="xm-form-item"
        v-if="user.phone !== undefined"
      >
        <view class="xm-form-label">{{ $t('pages.person.phoneLabel') }}</view>
        <input
          class="xm-input"
          v-model="user.phone"
        />
      </view>
      <view
        class="xm-form-item"
        v-if="user.email !== undefined"
      >
        <view class="xm-form-label">{{ $t('pages.person.emailLabel') }}</view>
        <input
          class="xm-input"
          v-model="user.email"
        />
      </view>
      <view
        class="xm-form-item"
        v-if="user.title !== undefined"
      >
        <view class="xm-form-label">{{ $t('pages.person.titleLabel') }}</view>
        <input
          class="xm-input"
          v-model="user.title"
          disabled
        />
      </view>

      <button
        class="xm-btn xm-btn-primary xm-btn-block"
        @click="update"
      >
        {{ $t('common.save') }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { reactive, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { request, resolveFileUrl } from '@/utils/request'
import { baseUrl } from '@/utils/config'
import { useUserStore } from '@/stores/user'
import { t, apiMessage } from '@/i18n'

const userStore = useUserStore()
const user = reactive({ ...userStore.user })

// 头像展示地址归一化（老 localhost 绝对地址 / 新 /api 相对路径 → 当前 baseUrl 完整地址）。
// user.avatar 本身保持后端原始值，保存资料时原样回传，避免把本机 IP 写进数据库
const avatarUrl = computed(() => resolveFileUrl(user.avatar))

const isStudent = computed(() => user.role === 'STUDENT')
const isAdminOrTeacher = computed(() => user.role === 'ADMIN' || user.role === 'TEACHER')

onShow(() => {
  uni.setNavigationBarTitle({ title: t('menu.person') })
})

const chooseAvatar = () => {
  uni.chooseImage({
    count: 1,
    success: (res) => {
      const filePath = res.tempFilePaths[0]
      uni.uploadFile({
        url: `${baseUrl}/files/upload`,
        filePath,
        name: 'file',
        header: { token: userStore.token },
        success: (up) => {
          try {
            const data = typeof up.data === 'string' ? JSON.parse(up.data) : up.data
            if (data.code === '200') {
              user.avatar = data.data
              uni.showToast({ title: t('common.operationSuccess'), icon: 'success' })
            } else {
              uni.showToast({ title: apiMessage(data), icon: 'none' })
            }
          } catch {
            uni.showToast({ title: t('request.failed'), icon: 'none' })
          }
        },
        fail: () => {
          uni.showToast({ title: t('request.failed'), icon: 'none' })
        },
      })
    },
  })
}

const update = () => {
  const urlByRole = { ADMIN: '/admin/update', TEACHER: '/teacher/update', STUDENT: '/student/update' }
  const url = urlByRole[user.role]
  if (!url) return
  request({ url, method: 'PUT', data: user }).then((res) => {
    if (res.data.code === '200') {
      userStore.patchUser({ ...user })
      uni.showToast({ title: t('common.saveSuccess'), icon: 'success' })
    } else {
      uni.showToast({ title: apiMessage(res.data), icon: 'none' })
    }
  })
}
</script>

<style lang="scss" scoped>
.avatar-row {
  display: flex;
  align-items: center;
  gap: 24rpx;
  margin-bottom: 32rpx;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: var(--xm-bg-input);
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48rpx;
  color: var(--xm-text-2);
}
</style>

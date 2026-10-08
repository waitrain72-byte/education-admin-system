<template>
  <view
    class="xm-page xm-page-narrow"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 我的（对应 Web 端个人中心 views/profile/ProfilePage.vue 与顶栏账号菜单）：账号卡、常用入口、外观与语言、退出登录 -->
    <view
      class="xm-card profile"
      @click="go('/pages/person/person')"
    >
      <xm-user-avatar
        :name="user.name || user.username"
        :avatar="user.avatar"
        :size="120"
      />
      <view class="profile-main">
        <view class="profile-name xm-ellipsis">{{ user.name || user.username }}</view>
        <view class="profile-line">
          <text class="xm-tag xm-tag-brand">{{ $t('shell.roles.' + user.role) }}</text>
          <text class="profile-account xm-num">{{ user.username }}</text>
        </view>
        <view
          v-if="orgLine"
          class="profile-org xm-ellipsis"
          >{{ orgLine }}</view
        >
      </view>
      <view class="xm-cell-arrow">
        <xm-icon
          name="chevron-right"
          :size="32"
        />
      </view>
    </view>

    <view
      v-if="role === 'STUDENT'"
      class="facts"
    >
      <view
        class="fact"
        @click="go('/pages-course/transcript/transcript')"
      >
        <text class="fact-value xm-num">{{ detail.score == null ? user.score || 0 : detail.score }}</text>
        <text class="fact-label">{{ $t('profile.credits') }}</text>
      </view>
      <view
        class="fact"
        @click="go('/pages-course/transcript/transcript')"
      >
        <text class="fact-value xm-num">{{ gpaText }}</text>
        <text class="fact-label">{{ $t('workbench.stats.gpa') }}</text>
      </view>
    </view>

    <view class="xm-card cells">
      <view
        v-for="entry in entries"
        :key="entry.label"
        class="xm-cell"
        @click="entry.go()"
      >
        <view class="xm-cell-icon">
          <xm-icon
            :name="entry.icon"
            :size="36"
          />
        </view>
        <text class="xm-cell-body xm-ellipsis">{{ $t(entry.label) }}</text>
        <view class="xm-cell-arrow">
          <xm-icon
            name="chevron-right"
            :size="32"
          />
        </view>
      </view>
    </view>

    <!-- 外观与语言：跟随账号保存，Web 端与小程序互相同步 -->
    <view class="xm-card">
      <view class="xm-section-head">
        <text class="xm-section-title">{{ $t('profile.tabs.appearance') }}</text>
      </view>

      <view class="pref">
        <view class="pref-label">{{ $t('shell.themeMode') }}</view>
        <view class="xm-seg">
          <view
            v-for="mode in THEME_MODES"
            :key="mode.value"
            class="xm-seg-item"
            :class="{ on: themeMode === mode.value }"
            @click="setThemeMode(mode.value)"
            >{{ $t(mode.label) }}</view
          >
        </view>
      </view>

      <view class="pref">
        <view class="pref-label">
          {{ $t('layout.themeColor.title') }}
          <text
            v-if="isCustomOutsidePresets"
            class="xm-tag pref-custom"
            >{{ $t('layout.themeColor.custom') }}</text
          >
        </view>
        <view class="swatches">
          <view
            v-for="preset in PRESET_COLORS"
            :key="preset.value || 'default'"
            class="swatch"
            :class="{ 'is-active': themeColor === preset.value }"
            :style="'background:' + (preset.value || defaultBrand)"
            @click="setThemeColor(preset.value)"
          >
            <xm-icon
              v-if="themeColor === preset.value"
              name="check"
              :size="30"
              color="#ffffff"
            />
          </view>
        </view>
        <view class="swatch-name">{{ currentColorName }}</view>
      </view>

      <view class="pref">
        <view class="pref-label">{{ $t('layout.lang.label') }}</view>
        <view class="xm-seg">
          <view
            class="xm-seg-item"
            :class="{ on: isZhLocale() }"
            @click="changeLocale('zh-CN')"
            >{{ $t('layout.lang.zh') }}</view
          >
          <view
            class="xm-seg-item"
            :class="{ on: !isZhLocale() }"
            @click="changeLocale('en-US')"
            >{{ $t('layout.lang.en') }}</view
          >
        </view>
      </view>
      <view class="pref-note">{{ $t('profile.appearanceNote') }}</view>
    </view>

    <button
      class="xm-btn xm-btn-danger xm-btn-block xm-btn-lg logout"
      @click="logout"
    >
      {{ $t('layout.logout') }}
    </button>
    <xm-loader />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { ensureLoggedIn } from '@/utils/authGuard'
import { clearCookie, SILENT } from '@/utils/request'
import { scoreApi, studentApi, userApiOf } from '@/api'
import { closeWs } from '@/utils/websocket'
import { confirm } from '@/utils/confirm'
import { DEFAULT_BRAND } from '@/utils/themeColor'
import { t } from '@/i18n'
import { changeLocale, isZhLocale } from '@/composables/useLocale'
import { isDark, setThemeMode, themeMode } from '@/composables/useTheme'
import { PRESET_COLORS, resetThemeColorOnLogout, setThemeColor, themeColor } from '@/composables/useThemeColor'

const THEME_MODES = [
  { value: 'light', label: 'layout.theme.light' },
  { value: 'dark', label: 'layout.theme.dark' },
  { value: 'auto', label: 'layout.theme.auto' },
]

const userStore = useUserStore()
const user = computed(() => userStore.user || {})
const role = computed(() => userStore.role)

const go = (url) => uni.navigateTo({ url })

/** 常用入口：学生多「成绩单」「选课广场」，教师「选课广场」；资料与密码人人都有 */
const entries = computed(() => {
  const list = []
  if (role.value === 'STUDENT') {
    list.push({ label: 'transcript.entry', icon: 'award', go: () => go('/pages-course/transcript/transcript') })
  }
  if (role.value !== 'ADMIN') {
    list.push({ label: 'nav.square', icon: 'compass', go: () => go('/pages-course/square/square') })
  }
  list.push(
    { label: 'profile.tabs.info', icon: 'user', go: () => go('/pages/person/person') },
    { label: 'profile.tabs.password', icon: 'key', go: () => go('/pages/password/password') },
  )
  return list
})

// ========== 学生：学院 / 专业 / 班级、学分、绩点 ==========
const detail = ref({})
const gpa = ref(null)
const orgLine = computed(() =>
  [detail.value.collegeName, detail.value.specialityName, detail.value.className].filter(Boolean).join(' · '),
)
const gpaText = computed(() => (gpa.value == null ? '—' : gpa.value))

const loadStudentFacts = async () => {
  try {
    // selectById 不带关联名称，按 id 查一行列表数据（与 Web 端个人中心同一做法）
    const rows = await studentApi.selectAll({ id: userStore.user.id }, SILENT)
    detail.value = (rows && rows[0]) || {}
  } catch {
    // 拉不到就只显示角色与账号
  }
  try {
    const transcript = await scoreApi.transcript(SILENT)
    gpa.value = transcript && transcript.summary ? transcript.summary.gpa : null
  } catch {
    // 绩点显示占位
  }
}

// ========== 主题色 ==========
const defaultBrand = computed(() => (isDark.value ? DEFAULT_BRAND.dark : DEFAULT_BRAND.light))

/** 当前颜色是 Web 端选的、不在预设色板里的任意颜色时，给出「自定义」提示（此时没有色块处于选中态） */
const isCustomOutsidePresets = computed(
  () => !!themeColor.value && !PRESET_COLORS.some((p) => p.value === themeColor.value),
)

const currentColorName = computed(() => {
  const preset = PRESET_COLORS.find((p) => p.value === themeColor.value)
  return preset ? t(preset.label) : t('layout.themeColor.custom')
})

// ========== 退出登录 ==========
/** 清空用户态与验证码会话 Cookie，断开实时通知，回到登录页 */
const logout = async () => {
  if (!(await confirm(t('layout.logoutConfirm'), { title: t('layout.logout') }))) return
  closeWs()
  userStore.clearUser()
  clearCookie()
  // 复位主题色：否则下一个在本机登录的账号会先看到上一个账号的配色
  resetThemeColorOnLogout()
  uni.reLaunch({ url: '/pages/login/login' })
}

onShow(() => {
  if (!ensureLoggedIn()) return
  uni.setNavigationBarTitle({ title: t('tab.mine') })
  // 跨端资料同步：Web 端改过头像、姓名后，进入「我的」静默拉一次最新资料，无需重新登录。
  // selectById 返回的 token 为空，回填本地 token，防止把登录态冲掉
  const api = userApiOf(userStore.role)
  if (api && userStore.user.id) {
    api
      .selectById(userStore.user.id, SILENT)
      .then((profile) => {
        if (profile) userStore.patchUser({ ...profile, token: userStore.token })
      })
      .catch(() => {})
  }
  if (userStore.role === 'STUDENT') loadStudentFacts()
})
</script>

<style lang="scss" scoped>
.profile {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.profile-main {
  flex: 1;
  min-width: 0;
}

.profile-name {
  font-size: 36rpx;
  font-weight: bold;
  color: var(--xm-text);
}

.profile-line {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 10rpx;
}

.profile-account {
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.profile-org {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.fact {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
  padding: 22rpx 24rpx;
  border: 1rpx solid var(--xm-card-border);
  border-radius: 20rpx;
  background: var(--xm-bg-card);
  box-shadow: var(--xm-shadow);
}

.fact-value {
  font-size: 44rpx;
  font-weight: bold;
  color: var(--xm-text);
}

.fact-label {
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.cells {
  padding-top: 4rpx;
  padding-bottom: 4rpx;
}

.pref + .pref {
  margin-top: 28rpx;
}

.pref-label {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 14rpx;
  font-size: 26rpx;
  color: var(--xm-text-2);
}

/* 八个色块一行放下：56 × 8 + 16 × 7 = 560rpx */
.swatches {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.swatch {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  box-sizing: border-box;
}

.swatch.is-active {
  box-shadow:
    0 0 0 4rpx var(--xm-bg-card),
    0 0 0 8rpx var(--xm-text-3);
}

.swatch-name {
  margin-top: 12rpx;
  font-size: 22rpx;
  color: var(--xm-text-3);
}

.pref-note {
  margin-top: 28rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: var(--xm-text-3);
}

.logout {
  margin-top: 8rpx;
}
</style>

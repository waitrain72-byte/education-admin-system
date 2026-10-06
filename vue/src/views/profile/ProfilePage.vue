<template>
  <div class="profile">
    <aside class="panel profile-card">
      <el-upload
        class="profile-card__avatar"
        action="/api/files/upload"
        :show-file-list="false"
        :headers="{ token: user.token }"
        accept="image/*"
        :on-success="onAvatarUploaded"
        :on-error="() => ElMessage.error($t('profile.avatarFailed'))"
      >
        <UserAvatar :name="user.name" :avatar="user.avatar" :size="96" />
        <span class="profile-card__avatar-tip">{{ $t('profile.changeAvatar') }}</span>
      </el-upload>
      <div class="profile-card__name">{{ user.name }}</div>
      <span class="pill pill--brand">{{ $t('shell.roles.' + user.role) }}</span>
      <dl class="profile-card__facts">
        <div>
          <dt>{{ $t('login.account') }}</dt>
          <dd>{{ user.username }}</dd>
        </div>
        <template v-if="user.role === 'STUDENT'">
          <div>
            <dt>{{ $t('pages.student.college') }}</dt>
            <dd>{{ detail.collegeName || '—' }}</dd>
          </div>
          <div>
            <dt>{{ $t('pages.student.speciality') }}</dt>
            <dd>{{ detail.specialityName || '—' }}</dd>
          </div>
          <div>
            <dt>{{ $t('pages.student.classes') }}</dt>
            <dd>{{ detail.className || '—' }}</dd>
          </div>
          <div>
            <dt>{{ $t('profile.credits') }}</dt>
            <dd class="num">{{ detail.score ?? user.score ?? 0 }}</dd>
          </div>
        </template>
        <div v-if="user.role === 'TEACHER'">
          <dt>{{ $t('pages.person.titleLabel') }}</dt>
          <dd>{{ user.title || '—' }}</dd>
        </div>
      </dl>
    </aside>

    <section class="panel profile-main">
      <el-tabs v-model="tab" class="profile-tabs" @tab-change="onTabChange">
        <el-tab-pane name="info" :label="$t('profile.tabs.info')">
          <el-form ref="infoRef" :model="info" :rules="infoRules" label-position="top" class="profile-form">
            <el-form-item prop="name" :label="$t('pages.person.nameLabel')">
              <el-input id="profile-name" v-model="info.name" maxlength="20" />
            </el-form-item>
            <template v-if="user.role !== 'STUDENT'">
              <el-form-item prop="phone" :label="$t('pages.person.phoneLabel')">
                <el-input id="profile-phone" v-model="info.phone" maxlength="20" />
              </el-form-item>
              <el-form-item prop="email" :label="$t('pages.person.emailLabel')">
                <el-input id="profile-email" v-model="info.email" maxlength="50" />
              </el-form-item>
            </template>
            <p v-else class="profile-note">{{ $t('profile.studentNote') }}</p>
            <el-button type="primary" :loading="savingInfo" @click="saveInfo">{{ $t('common.save') }}</el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane name="password" :label="$t('profile.tabs.password')">
          <el-form ref="pwdRef" :model="pwd" :rules="pwdRules" label-position="top" class="profile-form">
            <el-form-item prop="password" :label="$t('pages.password.originalPassword')">
              <el-input id="profile-old-password" v-model="pwd.password" show-password autocomplete="current-password" />
            </el-form-item>
            <el-form-item prop="newPassword" :label="$t('pages.password.newPassword')">
              <el-input id="profile-new-password" v-model="pwd.newPassword" show-password autocomplete="new-password" />
            </el-form-item>
            <el-form-item prop="confirmPassword" :label="$t('pages.password.confirmPassword')">
              <el-input id="profile-confirm-password" v-model="pwd.confirmPassword" show-password autocomplete="new-password" />
            </el-form-item>
            <p class="profile-note">{{ $t('profile.passwordNote') }}</p>
            <el-button type="primary" :loading="savingPwd" @click="savePassword">{{ $t('pages.password.submit') }}</el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane name="appearance" :label="$t('profile.tabs.appearance')">
          <div class="profile-form">
            <AppearanceSettings wide />
            <p class="profile-note">{{ $t('profile.appearanceNote') }}</p>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from '@/utils/element-plus'
import request from '@/utils/request'
import { useUser } from '@/components/useUser'
import UserAvatar from '@/components/UserAvatar.vue'
import AppearanceSettings from '@/components/AppearanceSettings.vue'
import { t } from '@/i18n'

defineOptions({ name: 'ProfilePage' })

const route = useRoute()
const router = useRouter()
const { user, patchUser, clearUser } = useUser()

const TABS = ['info', 'password', 'appearance']
const tab = ref(TABS.includes(String(route.query.tab)) ? String(route.query.tab) : 'info')
watch(
  () => route.query.tab,
  (value) => {
    if (TABS.includes(String(value))) tab.value = String(value)
  },
)
const onTabChange = (name: string | number) => router.replace({ query: name === 'info' ? {} : { tab: String(name) } })

const roleBase = computed(() => '/' + String(user.value.role || '').toLowerCase())

// ========== 学生的学院 / 专业 / 班级（selectById 不带关联名称，按 id 查一行列表数据） ==========
const detail = ref<Record<string, any>>({})
onMounted(async () => {
  if (user.value.role !== 'STUDENT') return
  try {
    const rows = await request.get<any[]>('/student/selectAll', { params: { id: user.value.id } })
    detail.value = rows?.[0] || {}
  } catch {
    // 拉不到就只显示占位
  }
})

// ========== 基本资料 ==========
const infoRef = ref<FormInstance>()
const info = reactive({ name: user.value.name || '', phone: user.value.phone || '', email: user.value.email || '' })
const infoRules = computed<FormRules>(() => ({
  name: [{ required: true, message: t('profile.nameRequired'), trigger: 'blur' }],
  email: [{ type: 'email', message: t('profile.emailInvalid'), trigger: 'blur' }],
}))
const savingInfo = ref(false)

const saveInfo = async () => {
  const valid = await infoRef.value?.validate().catch(() => false)
  if (!valid) return
  savingInfo.value = true
  // 只提交本人可改的字段；账号、角色、学分等由后端白名单兜底，不会被改动
  const payload: Record<string, any> = { id: user.value.id, name: info.name.trim() }
  if (user.value.role !== 'STUDENT') {
    payload.phone = info.phone.trim()
    payload.email = info.email.trim()
  }
  try {
    await request.put(`${roleBase.value}/update`, payload)
    patchUser(payload)
    ElMessage.success(t('common.saveSuccess'))
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    savingInfo.value = false
  }
}

/** 头像上传成功后立即保存（不必再点「保存」），顶栏头像随之更新 */
const onAvatarUploaded = async (response: any) => {
  const url = response?.data
  if (response?.code !== '200' || !url) {
    ElMessage.error(response?.msg || t('profile.avatarFailed'))
    return
  }
  try {
    await request.put(`${roleBase.value}/update`, { id: user.value.id, avatar: url })
    patchUser({ avatar: url })
    ElMessage.success(t('profile.avatarSaved'))
  } catch {
    // 错误提示已由拦截器统一处理
  }
}

// ========== 修改密码 ==========
const pwdRef = ref<FormInstance>()
const pwd = reactive({ password: '', newPassword: '', confirmPassword: '' })
const savingPwd = ref(false)

const pwdRules = computed<FormRules>(() => ({
  password: [{ required: true, message: t('pages.password.ruleOriginalRequired'), trigger: 'blur' }],
  newPassword: [
    { required: true, message: t('pages.password.ruleNewRequired'), trigger: 'blur' },
    { min: 6, max: 20, message: t('register.rulePasswordLength'), trigger: 'blur' },
  ],
  confirmPassword: [
    {
      required: true,
      trigger: 'blur',
      validator: (_rule: any, value: string, callback: (e?: Error) => void) => {
        if (!value) callback(new Error(t('pages.password.ruleConfirmRequired')))
        else if (value !== pwd.newPassword) callback(new Error(t('pages.password.ruleConfirmMismatch')))
        else callback()
      },
    },
  ],
}))

const savePassword = async () => {
  const valid = await pwdRef.value?.validate().catch(() => false)
  if (!valid) return
  savingPwd.value = true
  try {
    await request.put('/updatePassword', {
      username: user.value.username,
      role: user.value.role,
      password: pwd.password,
      newPassword: pwd.newPassword,
    })
    ElMessage.success(t('pages.password.success'))
    clearUser()
    router.push('/login')
  } catch {
    // 错误提示（如原密码错误）已由拦截器统一处理
  } finally {
    savingPwd.value = false
  }
}
</script>

<style scoped>
.profile {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.profile-card {
  display: grid;
  justify-items: center;
  gap: 8px;
  padding: 28px 20px 22px;
  text-align: center;
}

.profile-card__avatar :deep(.el-upload) {
  position: relative;
  display: block;
  border-radius: 50%;
  cursor: pointer;
}

.profile-card__avatar-tip {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 13px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.profile-card__avatar :deep(.el-upload:hover) .profile-card__avatar-tip,
.profile-card__avatar :deep(.el-upload:focus-visible) .profile-card__avatar-tip {
  opacity: 1;
}

.profile-card__name {
  margin-top: 8px;
  font-size: 20px;
  font-weight: 700;
  color: var(--xm-text-primary);
}

.profile-card__facts {
  display: grid;
  gap: 10px;
  width: 100%;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--xm-border);
  text-align: left;
}

.profile-card__facts div {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 13px;
}

.profile-card__facts dt {
  color: var(--xm-text-secondary);
}

.profile-card__facts dd {
  color: var(--xm-text-primary);
  text-align: right;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.profile-main {
  padding: 6px 24px 24px;
}

.profile-form {
  max-width: 440px;
  padding-top: 6px;
}

.profile-note {
  margin: 4px 0 16px;
  font-size: 13px;
  color: var(--xm-text-secondary);
}

@media (max-width: 860px) {
  .profile {
    grid-template-columns: minmax(0, 1fr);
  }

  .profile-main {
    padding: 4px 16px 20px;
  }
}
</style>

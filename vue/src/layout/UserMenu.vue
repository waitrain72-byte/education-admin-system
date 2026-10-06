<template>
  <el-popover
    v-model:visible="open"
    trigger="click"
    placement="bottom-end"
    :width="296"
    popper-class="user-menu-popper"
    :show-arrow="false"
  >
    <template #reference>
      <button type="button" class="user-trigger" :aria-label="$t('shell.userMenu')">
        <UserAvatar :name="user.name" :avatar="user.avatar" :size="32" />
        <span class="user-trigger__name">{{ user.name || $t('layout.guest') }}</span>
        <el-icon class="user-trigger__caret"><ArrowDown /></el-icon>
      </button>
    </template>

    <div class="user-menu">
      <div class="user-menu__head">
        <UserAvatar :name="user.name" :avatar="user.avatar" :size="44" />
        <div class="user-menu__who">
          <div class="user-menu__name">{{ user.name || $t('layout.guest') }}</div>
          <div class="user-menu__meta">
            <span class="role-pill">{{ $t('shell.roles.' + (user.role || 'STUDENT')) }}</span>
            <span class="user-menu__account">{{ user.username }}</span>
          </div>
        </div>
      </div>

      <div class="user-menu__links">
        <button type="button" class="menu-row" @click="go('/profile')">
          <el-icon><User /></el-icon><span>{{ $t('shell.profile') }}</span>
        </button>
        <button type="button" class="menu-row" @click="go('/profile?tab=password')">
          <el-icon><Lock /></el-icon><span>{{ $t('shell.password') }}</span>
        </button>
      </div>

      <div class="user-menu__section">
        <AppearanceSettings in-popover />
      </div>

      <button type="button" class="menu-row menu-row--danger" @click="logout">
        <el-icon><SwitchButton /></el-icon><span>{{ $t('shell.logout') }}</span>
      </button>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown, Lock, SwitchButton, User } from '@element-plus/icons-vue'
import { ElMessage } from '@/utils/element-plus'
import { useUser } from '@/components/useUser'
import UserAvatar from '@/components/UserAvatar.vue'
import AppearanceSettings from '@/components/AppearanceSettings.vue'
import { resetThemeColorOnLogout } from '@/composables/useThemeColor'
import { t } from '@/i18n'

const router = useRouter()
const { user, clearUser } = useUser()
const open = ref(false)

const go = (path: string) => {
  open.value = false
  router.push(path)
}

const logout = () => {
  open.value = false
  clearUser()
  // 复位主题色：否则下一个在本机登录的账号会先看到上一个账号的配色
  resetThemeColorOnLogout()
  ElMessage.success(t('shell.loggedOut'))
  router.push('/login')
}
</script>

<style scoped>
.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 6px 0 4px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: var(--xm-text-primary);
  cursor: pointer;
  font: inherit;
}

.user-trigger:hover,
.user-trigger:focus-visible {
  background: var(--xm-bg-hover);
}

.user-trigger__name {
  max-width: 7em;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 14px;
}

.user-trigger__caret {
  color: var(--xm-text-secondary);
  font-size: 12px;
}

.user-menu {
  display: grid;
  gap: 6px;
}

.user-menu__head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 4px 12px;
  border-bottom: 1px solid var(--xm-border);
}

.user-menu__who {
  min-width: 0;
}

.user-menu__name {
  font-size: 15px;
  font-weight: 600;
  color: var(--xm-text-primary);
}

.user-menu__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.user-menu__account {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.role-pill {
  flex-shrink: 0;
  padding: 1px 8px;
  border-radius: 999px;
  background: var(--xm-brand-soft);
  color: var(--xm-brand);
  font-weight: 600;
}

.user-menu__links {
  display: grid;
  gap: 2px;
  padding-bottom: 6px;
  border-bottom: 1px solid var(--xm-border);
}

.menu-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 8px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--xm-text-regular);
  font: inherit;
  font-size: 14px;
  text-align: left;
  cursor: pointer;
}

.menu-row:hover,
.menu-row:focus-visible {
  background: var(--xm-bg-hover);
  color: var(--xm-text-primary);
}

.menu-row--danger:hover {
  color: var(--xm-bad);
}

.user-menu__section {
  padding: 8px 4px 12px;
  border-bottom: 1px solid var(--xm-border);
}

@media (max-width: 640px) {
  .user-trigger__name,
  .user-trigger__caret {
    display: none;
  }
}
</style>

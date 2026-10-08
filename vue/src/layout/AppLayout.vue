<template>
  <div class="app-shell">
    <a class="skip-link" href="#main">{{ $t('shell.skipToContent') }}</a>
    <TopNav @open-search="searchOpen = true" />
    <main id="main" class="app-main">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <div :key="pageKey" class="app-page">
            <component :is="Component" />
          </div>
        </transition>
      </router-view>
    </main>
    <CommandPalette v-model="searchOpen" />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TopNav from '@/layout/TopNav.vue'
import CommandPalette from '@/layout/CommandPalette.vue'
import request from '@/utils/request'
import { useUser } from '@/components/useUser'
import { usePermission } from '@/composables/usePermission'
import { useMessageStore } from '@/stores/messages'
import { onSocketMessage, setNotificationClickHandler, type SocketMessage } from '@/composables/useNoticeSocket'

const route = useRoute()
const router = useRouter()
const { user, updateUser } = useUser()
const { pullPermissions } = usePermission()
const messageStore = useMessageStore()

const searchOpen = ref(false)

/**
 * 切页动画的 key：按「布局下第一级路由」计算，而不是完整路径——
 * 教务后台、课程空间内部切换子页面时，外层（侧栏、课程页头）不跟着整体重绘。
 */
const pageKey = computed(() => {
  const record = route.matched[1]
  if (!record) return route.path
  return record.path.replace(/:(\w+)(\([^)]*\))?/g, (_, key: string) => String(route.params[key] ?? ''))
})

// ========== 全局快捷键：Ctrl/Cmd + K 打开搜索；不在输入框里时按 / 也能打开 ==========
const isTyping = (target: EventTarget | null) => {
  const el = target as HTMLElement | null
  if (!el) return false
  return el.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(el.tagName)
}

const onKeydown = (e: KeyboardEvent) => {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    searchOpen.value = !searchOpen.value
  } else if (e.key === '/' && !searchOpen.value && !isTyping(e.target)) {
    e.preventDefault()
    searchOpen.value = true
  }
}

// ========== 跨端资料同步：别的终端改了资料（如头像），进入系统 / 切换页面时静默拉一次 ==========
let lastUserRefreshAt = 0
async function refreshCurrentUser() {
  if (!user.value?.id || !user.value?.role) return
  const now = Date.now()
  if (now - lastUserRefreshAt < 10000) return
  lastUserRefreshAt = now
  try {
    const profile = await request.get<any>(`/${String(user.value.role).toLowerCase()}/selectById/${user.value.id}`)
    if (profile) {
      // selectById 返回的 token 为空，回填本地 token 与权限码，防止把登录态冲掉
      updateUser({ ...profile, token: user.value.token, permissions: user.value.permissions })
    }
  } catch {
    // 静默失败：拉取不到时保留本地缓存
  }
}

watch(
  () => route.path,
  () => refreshCurrentUser(),
)

// 收到带标题的推送（成绩发布、作业批改……）时未读数 +1；静默事件不影响未读数
let offSocket: (() => void) | null = null

onMounted(() => {
  if (!user.value.id) {
    router.push('/login')
    return
  }
  // 进入布局时拉取当前用户权限码，保证按钮级权限与后端一致
  pullPermissions()
  refreshCurrentUser()
  messageStore.refresh()
  offSocket = onSocketMessage((message: SocketMessage) => {
    if (message.title && message.type !== 'notice') {
      messageStore.refresh()
    }
  })
  setNotificationClickHandler((message: SocketMessage) => {
    router.push(message.link || '/messages')
  })
  window.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  offSocket?.()
  setNotificationClickHandler(null)
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: var(--xm-bg-page);
}

.app-main {
  max-width: var(--xm-content-width);
  margin: 0 auto;
  padding: 24px 24px 56px;
}

.skip-link {
  position: absolute;
  left: 12px;
  top: -48px;
  z-index: 100;
  padding: 8px 14px;
  border-radius: 8px;
  background: var(--xm-brand);
  color: #fff;
}

.skip-link:focus {
  top: 12px;
}

@media (max-width: 640px) {
  .app-main {
    padding: 14px 12px 40px;
  }
}
</style>

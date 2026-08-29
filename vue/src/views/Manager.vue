<template>
  <div class="manager-container">
    <!--  头部  -->
    <div class="manager-header">
      <div class="manager-header-left">
        <img src="@/assets/imgs/教务系统.png" />
        <!-- 英文标题较长，切换小字号避免撑破 60px 头部 -->
        <div class="title" :class="{ 'title-en': !isZh }">{{ $t('layout.title') }}</div>
      </div>

      <div class="manager-header-center">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/' }">{{ $t('layout.breadcrumbHome') }}</el-breadcrumb-item>
          <el-breadcrumb-item :to="{ path: route.path }">{{ route.meta?.name ? $t(route.meta.name as string) : $t('common.page') }}</el-breadcrumb-item>
        </el-breadcrumb>

        <!-- 多标签页：紧跟在面包屑后面，可快速切换/关闭 -->
        <div class="manager-tabs">
          <div
              v-for="tab in tabs"
              :key="tab.path"
              class="manager-tab"
              :class="{ active: route.path === tab.path }"
              @click="router.push(tab.path)"
          >
            <span>{{ $t(tab.name) }}</span>
            <el-icon v-if="tab.path !== '/home'" class="tab-close" @click.stop="closeTab(tab.path)">
              <CircleClose />
            </el-icon>
          </div>
        </div>
      </div>

      <div class="manager-header-right">
        <!-- 语言切换：中文 / English -->
        <el-dropdown placement="bottom" @command="setLocale">
          <span class="theme-switch-trigger lang-trigger" :title="$t('layout.lang.switch')">
            {{ isZh ? '中' : 'EN' }}
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="zh-CN" :data-active="isZh">中文</el-dropdown-item>
              <el-dropdown-item command="en-US" :data-active="!isZh">English</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <!-- 主题切换：浅色 / 深色 / 跟随系统 -->
        <el-dropdown placement="bottom" class="theme-switch" @command="setTheme">
          <span class="theme-switch-trigger" :title="$t(themeLabel)">
            <el-icon :size="20"><component :is="themeIcon" /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="light" :data-active="mode === 'light'">
                <el-icon><Sunny /></el-icon>{{ $t('layout.theme.light') }}
              </el-dropdown-item>
              <el-dropdown-item command="dark" :data-active="mode === 'dark'">
                <el-icon><Moon /></el-icon>{{ $t('layout.theme.dark') }}
              </el-dropdown-item>
              <el-dropdown-item command="auto" :data-active="mode === 'auto'">
                <el-icon><Monitor /></el-icon>{{ $t('layout.theme.auto') }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <el-dropdown placement="bottom" @command="handleCommand">
          <div class="avatar">
            <img :src="user.avatar || defaultAvatar" />
            <div>{{ user.name || $t('layout.guest') }}</div>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="person">{{ $t('layout.profile') }}</el-dropdown-item>
              <el-dropdown-item command="password">{{ $t('layout.changePassword') }}</el-dropdown-item>
              <el-dropdown-item command="logout" divided>{{ $t('layout.logout') }}</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!--  主体  -->
    <div class="manager-main">
      <!--  侧边栏  -->
      <div class="manager-main-left">
        <el-menu
            :default-openeds="openeds"
            router
            style="border: none; height: 100%"
            :default-active="route.path"
        >
          <el-menu-item index="/home">
            <el-icon><HomeFilled /></el-icon>
            <span>{{ $t('menu.home') }}</span>
          </el-menu-item>

          <el-sub-menu v-for="group in menuGroups" :key="group.key" :index="group.key">
            <template #title>
              <el-icon><component :is="group.icon" /></el-icon>
              <span>{{ $t(group.title) }}</span>
            </template>
            <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
              {{ $t(item.name) }}
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </div>

      <!--  数据表格  -->
      <div class="manager-main-right">
        <router-view @update:user="refreshUser" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, provide } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Sunny, Moon, Monitor } from '@element-plus/icons-vue'
import { useUser } from '@/components/useUser.ts'
import { useTheme } from '@/composables/useTheme'
import { currentLocale, setLocale } from '@/composables/useLocale'
import { t } from '@/i18n'

const router = useRouter()
const route = useRoute()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

// 使用全局用户状态
const { user, refreshUser, clearUser } = useUser()

// 菜单分组配置：顺序即展示顺序（标题为 i18n 键）
const menuGroupConfig: Record<string, { title: string; icon: string }> = {
  info: { title: 'layout.groupInfo', icon: 'ChatRound' },
  admin: { title: 'layout.groupAdmin', icon: 'Message' },
  teach: { title: 'layout.groupTeach', icon: 'Opportunity' },
  edu: { title: 'layout.groupEdu', icon: 'Stamp' },
  user: { title: 'layout.groupUser', icon: 'User' },
  system: { title: 'layout.groupSystem', icon: 'Setting' },
}

// 根据当前角色从路由配置动态生成侧边菜单，与路由 meta.roles 保持单一数据源
const menuGroups = computed(() => {
  const layoutRoute = router.options.routes.find((r) => r.path === '/')
  const children = layoutRoute?.children || []
  const grouped: Record<string, { path: string; name: string }[]> = {}

  for (const child of children) {
    const meta = child.meta as { group?: string; roles?: string[]; name?: string } | undefined
    if (!meta?.group) continue
    if (meta.roles && !meta.roles.includes(user.value.role)) continue
    if (!grouped[meta.group]) grouped[meta.group] = []
    const routeName = typeof child.name === 'string' ? child.name : ''
    grouped[meta.group].push({ path: '/' + child.path, name: meta.name || routeName })
  }

  return Object.entries(menuGroupConfig)
      .filter(([key]) => grouped[key]?.length)
      .map(([key, cfg]) => ({ key, title: cfg.title, icon: cfg.icon, items: grouped[key] || [] }))
})

const openeds = ref<string[]>(Object.keys(menuGroupConfig))

// 主题模式（light/dark/auto），切换后由 useTheme 负责持久化与后端同步
const mode = useTheme()
const themeIcon = computed(() => (mode.value === 'dark' ? Moon : mode.value === 'light' ? Sunny : Monitor))
const themeLabel = computed(() => `layout.theme.current.${mode.value}`)
const setTheme = (command: string) => {
    mode.value = command as 'light' | 'dark' | 'auto'
}

// 语言偏好（zh-CN/en-US），切换后持久化并同步后端
const isZh = computed(() => currentLocale() === 'zh-CN')

// ========== 多标签页 ==========
interface TabItem {
  path: string
  name: string
}

const tabs = ref<TabItem[]>([{ path: '/home', name: 'menu.home' }])

// 路由变化时把新页面加入标签（首页固定不可关闭）
watch(
    () => route.path,
    (path) => {
      if (tabs.value.some((tab) => tab.path === path)) return
      const name = typeof route.meta?.name === 'string' ? route.meta.name : path
      tabs.value.push({ path, name })
    }
)

const closeTab = (path: string) => {
  const index = tabs.value.findIndex((tab) => tab.path === path)
  if (index === -1) return
  tabs.value.splice(index, 1)
  // 关闭的是当前页时，跳到最后一个标签
  if (route.path === path) {
    router.push(tabs.value[tabs.value.length - 1].path)
  }
}

// 提供 refreshUser 方法给子组件
provide('refreshUser', refreshUser)

onMounted(() => {
  if (!user.value.id) {
    router.push('/login')
  }
})

const handleCommand = (command: string) => {
  switch (command) {
    case 'person':
      goToPerson()
      break
    case 'password':
      router.push('/password')
      break
    case 'logout':
      logout()
      break
  }
}

const goToPerson = () => {
  const roleMap: Record<string, string> = {
    'ADMIN': '/adminPerson',
    'TEACHER': '/teacherPerson',
    'STUDENT': '/studentPerson'
  }
  const path = roleMap[user.value.role || ''] || '/'
  router.push(path)
}

const logout = () => {
  clearUser()
  ElMessage.success(t('layout.loggedOut'))
  router.push('/login')
}
</script>

<style >
@import "@/assets/css/manager.css";
</style>

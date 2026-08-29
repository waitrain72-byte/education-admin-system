<template>
  <div class="manager-container">
    <!--  头部  -->
    <div class="manager-header">
      <div class="manager-header-left">
        <img src="@/assets/imgs/教务系统.png" />
        <div class="title">教务管理系统</div>
      </div>

      <div class="manager-header-center">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item :to="{ path: route.path }">{{ route.meta?.name || '页面' }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <div class="manager-header-right">
        <!-- 主题切换：浅色 / 深色 / 跟随系统 -->
        <el-dropdown placement="bottom" class="theme-switch" @command="setTheme">
          <span class="theme-switch-trigger" :title="themeLabel">
            <el-icon :size="20"><component :is="themeIcon" /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="light" :data-active="mode === 'light'">
                <el-icon><Sunny /></el-icon>浅色模式
              </el-dropdown-item>
              <el-dropdown-item command="dark" :data-active="mode === 'dark'">
                <el-icon><Moon /></el-icon>深色模式
              </el-dropdown-item>
              <el-dropdown-item command="auto" :data-active="mode === 'auto'">
                <el-icon><Monitor /></el-icon>跟随系统
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <el-dropdown placement="bottom" @command="handleCommand">
          <div class="avatar">
            <img :src="user.avatar || defaultAvatar" />
            <div>{{ user.name || '游客' }}</div>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="person">个人信息</el-dropdown-item>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
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
            <span>系统首页</span>
          </el-menu-item>

          <el-sub-menu v-for="group in menuGroups" :key="group.key" :index="group.key">
            <template #title>
              <el-icon><component :is="group.icon" /></el-icon>
              <span>{{ group.title }}</span>
            </template>
            <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
              {{ item.name }}
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
import { ref, computed, onMounted, provide } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Sunny, Moon, Monitor } from '@element-plus/icons-vue'
import { useUser } from '@/components/useUser.ts'
import { useTheme } from '@/composables/useTheme'

const router = useRouter()
const route = useRoute()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

// 使用全局用户状态
const { user, refreshUser, clearUser } = useUser()

// 菜单分组配置：顺序即展示顺序
const menuGroupConfig: Record<string, { title: string; icon: string }> = {
  info: { title: '信息公告', icon: 'ChatRound' },
  admin: { title: '行政管理', icon: 'Message' },
  teach: { title: '教学管理', icon: 'Opportunity' },
  edu: { title: '教务管理', icon: 'Stamp' },
  user: { title: '用户管理', icon: 'User' },
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
const themeLabel = computed(() =>
    mode.value === 'dark' ? '深色模式' : mode.value === 'light' ? '浅色模式' : '跟随系统'
)
const setTheme = (command: string) => {
    mode.value = command as 'light' | 'dark' | 'auto'
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
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style >
@import "@/assets/css/manager.css";
</style>

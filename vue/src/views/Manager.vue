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
            @select="handleSelect"
        >
          <el-menu-item index="/home">
            <el-icon><HomeFilled /></el-icon>
            <span>系统首页</span>
          </el-menu-item>

          <el-sub-menu index="info">
            <template #title>
              <el-icon><ChatRound /></el-icon>
              <span>信息公告</span>
            </template>
            <el-menu-item index="/notice">教务通知</el-menu-item>
            <el-menu-item index="/examplan">考试安排</el-menu-item>
            <el-menu-item index="/roomplan">教室安排</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="administration">
            <template #title>
              <el-icon><Message /></el-icon>
              <span>行政管理</span>
            </template>
            <el-menu-item index="/college">学院信息</el-menu-item>
            <el-menu-item index="/speciality">专业信息</el-menu-item>
            <el-menu-item index="/classes">班级信息</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="teach">
            <template #title>
              <el-icon><Opportunity /></el-icon>
              <span>教学管理</span>
            </template>
            <el-menu-item index="/course">课程信息</el-menu-item>
            <el-menu-item index="/choice">我的选课</el-menu-item>
            <el-menu-item index="/curriculum" v-if="user.role === 'STUDENT'">我的课表</el-menu-item>
            <el-menu-item index="/score">我的成绩</el-menu-item>
            <el-menu-item index="/comment">网上评教</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="educational">
            <template #title>
              <el-icon><Stamp /></el-icon>
              <span>教务管理</span>
            </template>
            <el-menu-item index="/apply">请假申请</el-menu-item>
            <el-menu-item index="/homework">作业提交</el-menu-item>
            <el-menu-item index="/attendance">考勤信息</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="user">
            <template #title>
              <el-icon><User /></el-icon>
              <span>用户管理</span>
            </template>
            <el-menu-item index="/admin">管理员信息</el-menu-item>
            <el-menu-item index="/teacher">教师信息</el-menu-item>
            <el-menu-item index="/student">学生信息</el-menu-item>
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
import { ref, onMounted, provide } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUser } from '@/components/useUser.ts'

const router = useRouter()
const route = useRoute()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

// 使用全局用户状态
const { user, refreshUser, clearUser } = useUser()

const openeds = ref<string[]>(['info', 'administration', 'teach', 'educational', 'user'])

// 提供 refreshUser 方法给子组件
provide('refreshUser', refreshUser)

onMounted(() => {
  if (!user.value.id) {
    router.push('/login')
  }

  if (user.value.role === 'STUDENT') {
    openeds.value = ['teach']
  }
})

const handleSelect = (index: string) => {
  console.log('选中的菜单:', index)
}

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

<template>
    <div class="manager-container" :class="{ 'sidebar-open': sidebarOpen }">
        <!--  头部  -->
        <div class="manager-header">
            <!-- 窄屏侧边栏开关（桌面端由 CSS 隐藏） -->
            <button class="sidebar-toggle" type="button" @click="sidebarOpen = !sidebarOpen">
                <el-icon><Fold /></el-icon>
            </button>
            <div class="manager-header-left">
                <img src="@/assets/imgs/教务系统.png" />
                <!-- 英文标题较长，切换小字号避免撑破 60px 头部 -->
                <div class="title" :class="{ 'title-en': !isZh }">{{ $t('layout.title') }}</div>
            </div>

            <div class="manager-header-center">
                <el-breadcrumb separator="/">
                    <el-breadcrumb-item :to="{ path: '/' }">{{ $t('layout.breadcrumbHome') }}</el-breadcrumb-item>
                    <el-breadcrumb-item :to="{ path: route.path }">{{
                        route.meta?.name ? $t(route.meta.name as string) : $t('common.page')
                    }}</el-breadcrumb-item>
                </el-breadcrumb>
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

                <!-- 主题切换开关：浅色 / 深色 -->
                <el-tooltip :content="isDark ? $t('layout.theme.light') : $t('layout.theme.dark')" placement="bottom">
                    <el-switch
                        v-model="darkSwitch"
                        class="theme-switch"
                        size="large"
                        inline-prompt
                        :active-icon="Moon"
                        :inactive-icon="Sunny"
                    />
                </el-tooltip>

                <el-dropdown placement="bottom" @command="handleCommand">
                    <div class="avatar">
                        <img :src="resolveFileUrl(user.avatar) || defaultAvatar" />
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
            <!--  侧边栏（窄屏下由 CSS 变为抽屉，sidebar-open 控制展开）  -->
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

            <!-- 抽屉遮罩：仅窄屏出现 -->
            <div class="sidebar-mask" @click="sidebarOpen = false"></div>

            <!--  数据表格（keep-alive 缓存已打开标签页对应的页面，关闭标签即释放缓存）  -->
            <div class="manager-main-right">
                <!-- 多标签页条：结构/交互/样式照搬 vue-element-plus-admin 的 TagsView
                     （左右滚动箭头 + 标签列表（右键菜单）+ 刷新 + 更多操作下拉） -->
                <div class="tags-view">
                    <span class="tags-view__tool tags-view__tool--first" @click="move(-200)">
                        <el-icon><DArrowLeft /></el-icon>
                    </span>
                    <div class="tags-view__list">
                        <div class="tags-view__list-inner">
                            <TabContextMenu
                                v-for="tab in tabs"
                                :key="tab.path"
                                :schema="contextMenuSchema(tab)"
                                @visible-change="(v: boolean) => visibleChange(v, tab)"
                            >
                                <div
                                    class="tags-view__item"
                                    :class="{ 'is-active': selectedPath === tab.path }"
                                    @click="router.push(tab.path)"
                                >
                                    <span>{{ $t(tab.name) }}</span>
                                    <el-icon
                                        v-if="tab.path !== AFFIX_TAB"
                                        class="tags-view__item-close"
                                        @click.prevent.stop="closeSelectedTag(tab)"
                                    >
                                        <Close />
                                    </el-icon>
                                </div>
                            </TabContextMenu>
                        </div>
                    </div>
                    <span class="tags-view__tool" @click="move(200)">
                        <el-icon><DArrowRight /></el-icon>
                    </span>
                    <span class="tags-view__tool" @click="refreshSelectedTag()">
                        <el-icon><RefreshRight /></el-icon>
                    </span>
                    <TabContextMenu trigger="click" :schema="contextMenuSchema()">
                        <span class="tags-view__tool">
                            <el-icon><Setting /></el-icon>
                        </span>
                    </TabContextMenu>
                </div>

                <router-view v-slot="{ Component }">
                    <!-- 路由切换过渡：淡入淡出 + 轻微上移；keep-alive 缓存保留每页已加载数据，避免重复请求。
                         reloadKey 供标签页右键"重新加载"使用：key 变化强制重建当前组件实例（其余标签缓存保留） -->
                    <transition name="route-fade" mode="out-in">
                        <keep-alive :include="cachedViews">
                            <component
                                :is="Component"
                                :key="route.path + '-' + reloadKey"
                                @update:user="refreshUser"
                            />
                        </keep-alive>
                    </transition>
                </router-view>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, provide } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
    Sunny, Moon, Fold, RefreshRight, Close, Back, Right, CircleClose, SemiSelect,
    DArrowLeft, DArrowRight, Setting,
} from '@element-plus/icons-vue'
import { useUser } from '@/components/useUser.ts'
import TabContextMenu from '@/components/TabContextMenu.vue'
import { resolveFileUrl } from '@/utils/file'
import request from '@/utils/request'
import { usePermission } from '@/composables/usePermission'
import { isDark, setThemeMode } from '@/composables/useTheme'
import { currentLocale, setLocale } from '@/composables/useLocale'
import { t } from '@/i18n'

const router = useRouter()
const route = useRoute()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

// 使用全局用户状态
const { user, updateUser, refreshUser, clearUser } = useUser()
const { pullPermissions } = usePermission()

// 窄屏侧边栏抽屉开关（≤1024px 生效，桌面端由 CSS 忽略）
const sidebarOpen = ref(false)

// 跨端资料同步：手机端等其它入口修改资料（如头像）后，Web 端在进入系统/切换页面时
// 静默拉取一次最新用户信息，右上角头像等无需重新登录即可更新。
// selectById 返回的 token 为空，回填本地 token 与权限码，防止把登录态冲掉。
let lastUserRefreshAt = 0

async function refreshCurrentUser() {
    if (!user.value?.id || !user.value?.role) return
    const now = Date.now()
    if (now - lastUserRefreshAt < 10000) return
    lastUserRefreshAt = now
    try {
        const res: any = await request.get(`/${String(user.value.role).toLowerCase()}/selectById/${user.value.id}`)
        if (res.data?.code === '200' && res.data.data) {
            updateUser({ ...res.data.data, token: user.value.token, permissions: user.value.permissions })
        }
    } catch {
        // 静默失败：拉取不到时保留本地缓存
    }
}

// 路由切换后自动收起抽屉（窄屏点击菜单跳转后回到内容区），并顺带同步最新用户资料
watch(
    () => route.path,
    () => {
        sidebarOpen.value = false
        refreshCurrentUser()
    },
)

onMounted(() => {
    refreshCurrentUser()
})

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

// 主题切换开关（浅色 / 深色），偏好持久化并同步后端
const darkSwitch = computed({
    get: () => isDark.value,
    set: (value: boolean) => setThemeMode(value ? 'dark' : 'light'),
})

// 语言偏好（zh-CN/en-US），切换后持久化并同步后端
const isZh = computed(() => currentLocale() === 'zh-CN')

// ========== 多标签页 ==========
interface TabItem {
    path: string
    name: string
    routeName: string
}

const tabs = ref<TabItem[]>([{ path: '/home', name: 'menu.home', routeName: 'Home' }])

const cachedViews = computed(() => tabs.value.map((tab) => tab.routeName))

// 路由变化时把新页面加入标签（首页固定不可关闭）；immediate 保证整页刷新时当前页也入列
watch(
    () => route.path,
    (path) => {
        if (tabs.value.some((tab) => tab.path === path)) return
        const name = typeof route.meta?.name === 'string' ? route.meta.name : path
        const routeName = typeof route.name === 'string' ? route.name : path
        tabs.value.push({ path, name, routeName })
    },
    { immediate: true },
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

// ========== 标签页（逻辑/交互照搬 vue-element-plus-admin 的 TagsView） ==========
// 首页为固定标签（affix，不可关闭），其余均可关闭
const AFFIX_TAB = '/home'

// reloadKey 自增用于强制重建当前页组件（keep-alive 按 key 区分实例，其余标签缓存保留）
const reloadKey = ref(0)

// 选中的标签：右键其它标签会把其设为选中（菜单动作作用于选中标签），与框架 selectedTag 一致
const selectedPath = ref(route.path)

const setSelected = (path: string) => {
    selectedPath.value = path
}

// 路由变化：选中的标签跟随当前路由
watch(
    () => route.path,
    (path) => {
        setSelected(path)
    },
)

/** 右键菜单 / 齿轮下拉的菜单 schema（禁用逻辑与框架 TagsView 完全一致） */
const contextMenuSchema = (tab?: TabItem) => {
    const path = tab?.path ?? selectedPath.value
    const isSelected = selectedPath.value === path
    const firstPath = tabs.value[0]?.path
    const lastPath = tabs.value[tabs.value.length - 1]?.path
    return [
        {
            icon: RefreshRight,
            label: t('layout.tabs.reload'),
            disabled: !isSelected,
            command: () => refreshSelectedTag(path),
        },
        {
            icon: Close,
            label: t('layout.tabs.close'),
            divided: true,
            disabled: path === AFFIX_TAB,
            command: () => closeSelectedTag({ path } as TabItem),
        },
        {
            icon: Back,
            label: t('layout.tabs.closeLeft'),
            disabled: path === firstPath || !isSelected,
            command: () => closeLeftTags(path),
        },
        {
            icon: Right,
            label: t('layout.tabs.closeRight'),
            disabled: path === lastPath || !isSelected,
            command: () => closeRightTags(path),
        },
        {
            icon: CircleClose,
            label: t('layout.tabs.closeOther'),
            divided: true,
            disabled: !isSelected,
            command: () => closeOtherTags(path),
        },
        {
            icon: SemiSelect,
            label: t('layout.tabs.closeAll'),
            command: () => closeAllTags(),
        },
    ]
}

/** 右键菜单打开时：右键的标签自动设为选中（框架 visibleChange 行为） */
const visibleChange = (visible: boolean, tab: TabItem) => {
    if (visible) {
        setSelected(tab.path)
    }
}

/** 重新加载指定标签：先跳转到该页，再强制重建组件 */
const refreshSelectedTag = async (path?: string) => {
    const target = path ?? selectedPath.value
    if (route.path !== target) {
        await router.push(target)
    }
    await nextTick()
    reloadKey.value++
}

/** 关闭单个标签；关闭的是当前页时跳到最后一个剩余标签 */
const closeSelectedTag = (tab: TabItem) => {
    const index = tabs.value.findIndex((t) => t.path === tab.path)
    if (index === -1) return
    tabs.value.splice(index, 1)
    if (route.path === tab.path) {
        toLastView()
    }
    if (selectedPath.value === tab.path) {
        setSelected(route.path)
    }
}

/** 跳到最后一个剩余标签（没有则回首页） */
const toLastView = () => {
    const latest = tabs.value[tabs.value.length - 1]
    if (latest) {
        router.push(latest.path)
    } else {
        router.push(AFFIX_TAB)
    }
}

const closeLeftTags = (path: string) => {
    const index = tabs.value.findIndex((t) => t.path === path)
    const closed = tabs.value
        .slice(0, index)
        .filter((t) => t.path !== AFFIX_TAB)
        .map((t) => t.path)
    const pathSet = new Set(closed)
    tabs.value = tabs.value.filter((t) => !pathSet.has(t.path))
    if (pathSet.has(route.path)) {
        router.push(path)
    }
}

const closeRightTags = (path: string) => {
    const index = tabs.value.findIndex((t) => t.path === path)
    const closed = tabs.value
        .slice(index + 1)
        .filter((t) => t.path !== AFFIX_TAB)
        .map((t) => t.path)
    const pathSet = new Set(closed)
    tabs.value = tabs.value.filter((t) => !pathSet.has(t.path))
    if (pathSet.has(route.path)) {
        router.push(path)
    }
}

const closeOtherTags = (path: string) => {
    const closed = tabs.value
        .filter((t) => t.path !== path && t.path !== AFFIX_TAB)
        .map((t) => t.path)
    const pathSet = new Set(closed)
    tabs.value = tabs.value.filter((t) => !pathSet.has(t.path))
    if (pathSet.has(route.path)) {
        router.push(path)
    }
}

const closeAllTags = () => {
    const closed = tabs.value
        .filter((t) => t.path !== AFFIX_TAB)
        .map((t) => t.path)
    const pathSet = new Set(closed)
    tabs.value = tabs.value.filter((t) => !pathSet.has(t.path))
    if (pathSet.has(route.path)) {
        toLastView()
    }
}

/** 标签条左右滚动（照框架 move ±200px，平滑滚动） */
const move = (to: number) => {
    const list = document.querySelector('.tags-view__list') as HTMLElement | null
    list?.scrollTo({ left: list.scrollLeft + to, behavior: 'smooth' })
}

// 提供 refreshUser 方法给子组件
provide('refreshUser', refreshUser)

onMounted(() => {
    if (!user.value.id) {
        router.push('/login')
    } else {
        // 进入主布局时拉取当前用户权限码，保证按钮级权限与后端一致
        pullPermissions()
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
        ADMIN: '/adminPerson',
        TEACHER: '/teacherPerson',
        STUDENT: '/studentPerson',
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

<style scoped>
/* 标签页条：样式照搬 vue-element-plus-admin 的 TagsView（工具栏分隔线 + 边框卡片式标签） */
.tags-view {
    display: flex;
    width: 100%;
    height: 35px;
    position: relative;
    background: var(--xm-bg-card, #fff);
    border-radius: 10px;
    margin-bottom: 14px;
    overflow: hidden;
}

.tags-view__tool {
    width: 35px;
    height: 35px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    position: relative;
    color: var(--el-text-color-placeholder);
    transition: color 0.2s ease;
}

.tags-view__tool::before {
    content: '';
    position: absolute;
    top: 1px;
    left: 0;
    width: 100%;
    height: calc(100% - 1px);
    border-left: 1px solid var(--xm-border, #d9d9d9);
}

.tags-view__tool--first::before {
    border-right: 1px solid var(--xm-border, #d9d9d9);
    border-left: none;
}

.tags-view__tool:hover {
    color: var(--el-color-primary);
}

.tags-view__list {
    flex: 1;
    overflow: hidden;
}

.tags-view__list-inner {
    display: flex;
    height: 100%;
}

/* 单个标签：矩形边框卡片，hover 显示关闭按钮，选中蓝底白字 */
.tags-view__item {
    position: relative;
    top: 3px;
    height: calc(100% - 6px);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 25px 0 15px;
    margin-left: 4px;
    font-size: 12px;
    cursor: pointer;
    border: 1px solid #d9d9d9;
    border-radius: 2px;
    white-space: nowrap;
    user-select: none;
    transition: all 0.2s ease;
}

.tags-view__item-close {
    position: absolute;
    top: 50%;
    right: 5px;
    display: none;
    transform: translate(0, -50%);
    font-size: 12px;
    color: #333;
}

.tags-view__item:hover .tags-view__item-close {
    display: block;
}

.tags-view__item:not(.is-active):hover {
    color: var(--el-color-primary);
}

.tags-view__item.is-active {
    color: #ffffff;
    background-color: var(--el-color-primary);
    border: 1px solid var(--el-color-primary);
}

.tags-view__item.is-active .tags-view__item-close {
    color: #ffffff !important;
}

/* 深色模式适配（旧系统 dark 类挂在 html 上） */
:global(html.dark) .tags-view {
    background: var(--xm-bg-card, #141414);
}

:global(html.dark) .tags-view__item {
    border-color: var(--el-border-color);
}

:global(html.dark) .tags-view__tool::before,
:global(html.dark) .tags-view__tool--first::before {
    border-color: var(--el-border-color);
}
</style>

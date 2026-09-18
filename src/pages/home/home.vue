<template>
  <view
    class="xm-page"
    :class="themeClass"
  >
    <!-- 欢迎 + 偏好切换：品牌渐变头卡 -->
    <view class="xm-hero">
      <view class="xm-between">
        <view class="xm-row hero-left">
          <view class="hero-avatar">{{ avatarLetter }}</view>
          <view class="hero-meta">
            <view class="xm-hero-title xm-ellipsis">{{ $t('home.welcome', { name: userName }) }}</view>
            <view class="xm-hero-sub">{{ todayText }} · {{ roleLabel }}</view>
          </view>
        </view>
        <view class="xm-row hero-prefs">
          <button
            class="hero-btn hero-bell"
            @click="go('/pages/message/message')"
          >
            🔔
            <view
              v-if="unreadCount"
              class="bell-badge"
              >{{ unreadCount > 99 ? '99+' : unreadCount }}</view
            >
          </button>
          <button
            class="hero-btn"
            @click="toggleLocale"
          >
            {{ isZhLocale() ? 'EN' : '中' }}
          </button>
          <button
            class="hero-btn"
            @click="cycleTheme"
          >
            {{ themeMode === 'light' ? '☀' : themeMode === 'dark' ? '☾' : '◐' }}
          </button>
        </view>
      </view>
    </view>

    <!-- 功能入口（按角色过滤，与 Web 端路由权限一致） -->
    <view class="xm-card">
      <view class="xm-card-title">{{ $t('home.quickEntry') }}</view>
      <view class="xm-grid">
        <view
          v-for="item in menuItems"
          :key="item.path"
          class="xm-grid-item"
          @click="go(item.path)"
        >
          <text class="xm-grid-item-emoji">{{ item.icon }}</text>
          <view class="xm-grid-item-label">{{ $t(item.name) }}</view>
        </view>
      </view>
    </view>

    <!-- 骨架屏：无缓存首次加载时的占位（有缓存则秒开，不经过此分支） -->
    <view
      v-if="firstLoading"
      class="xm-card"
    >
      <view class="skeleton skeleton-title"></view>
      <view class="skeleton skeleton-line"></view>
      <view class="skeleton skeleton-line"></view>
      <view class="skeleton skeleton-line short"></view>
    </view>

    <template v-else>
      <!-- 今日课程（学生=选课课表今日列；教师=本人授课课程取今天星期）：严格按方案 A 演示稿样式 -->
      <view
        v-if="userStore.role === 'STUDENT' || userStore.role === 'TEACHER'"
        class="xm-card"
      >
        <view class="xm-card-title">
          {{ $t('home.todayTitle') }}
          <text class="today-date">{{ todayTitleText }}</text>
        </view>
        <xm-empty
          v-if="!todayCourses.length"
          icon="🎉"
          :text="$t('home.noClassToday')"
        />
        <template v-if="todayCourses.length">
          <!-- 收起状态：一行摘要，点击展开完整时间轴 -->
          <view
            v-if="!todayExpanded"
            class="today-summary"
            @click="todayExpanded = true"
          >
            <view class="summary-main">
              <view class="summary-line1">
                {{ todayCountText
                }}<template v-if="doingCourse"> · {{ doingCourse.name }} {{ todayStatusText.doing }}</template>
              </view>
              <view
                v-if="nextCourse"
                class="summary-line2"
              >
                {{ $t('home.nextClass') }} {{ nextCourse.start }} {{ nextCourse.name
                }}<template v-if="nextCourse.sub"> · {{ nextCourse.sub }}</template>
              </view>
            </view>
            <text class="summary-arrow">{{ $t('home.expand') }} ▾</text>
          </view>
          <!-- 展开：完整时间轴（与方案 A 演示稿一致） -->
          <template v-else>
            <view
              v-for="c in todayCourses"
              :key="c.key"
              class="today-item"
            >
              <view class="today-time">
                <text class="today-start">{{ c.start || '--:--' }}</text>
                <text class="today-end">{{ c.end }}</text>
              </view>
              <view class="today-rail">
                <view
                  class="today-dot"
                  :class="c.status ? 'is-' + c.status : ''"
                ></view>
              </view>
              <view
                class="today-block"
                :class="{ 'is-done': c.status === 'done' }"
                :style="courseBlockStyle(c.name)"
              >
                <view class="today-name">
                  <text class="today-course">{{ c.name }}</text>
                  <!-- 严格按演示稿：进行中=绿色胶囊，已结束=弱化文字，未开始不挂标签 -->
                  <text
                    v-if="c.status === 'doing'"
                    class="today-status status-doing"
                    >{{ todayStatusText.doing }}</text
                  >
                  <text
                    v-else-if="c.status === 'done'"
                    class="today-status status-done"
                    >{{ todayStatusText.done }}</text
                  >
                </view>
                <text
                  v-if="c.sub"
                  class="today-sub"
                  >{{ c.sub }}</text
                >
              </view>
            </view>
            <view
              class="today-collapse"
              @click="todayExpanded = false"
            >
              <text class="summary-arrow">{{ $t('home.collapse') }} ▴</text>
            </view>
          </template>
        </template>
      </view>

      <!-- 分组标签页：动态（推荐/通知/考试）与 统计（考勤/成绩） -->
      <view class="home-tabs">
        <view
          class="home-tab"
          :class="{ on: homeTab === 'feed' }"
          @click="homeTab = 'feed'"
          >{{ $t('home.tabFeed') }}</view
        >
        <view
          class="home-tab"
          :class="{ on: homeTab === 'stats' }"
          @click="homeTab = 'stats'"
          >{{ $t('home.tabStats') }}</view
        >
      </view>
      <template v-if="homeTab === 'feed'">
        <!-- 为你推荐（协同过滤课程推荐，仅学生角色；点击进入选课页） -->
        <view
          v-if="userStore.role === 'STUDENT' && recommends.length"
          class="xm-card"
        >
          <view class="xm-card-title">{{ $t('home.recommend') }}</view>
          <view
            v-for="c in recommends"
            :key="c.id"
            class="notice-item"
            @click="go('/pages/choice/choice')"
          >
            <view class="xm-between">
              <view class="xm-value">{{ c.name }}</view>
              <view class="xm-label">{{ c.teacherName }}</view>
            </view>
            <view class="recommend-reason">{{ c.reason }}</view>
          </view>
        </view>

        <!-- 教务通知 / 考试安排（首页仅展示最新 3 条，避免数据多时页面过长） -->
        <view class="xm-card">
          <view class="xm-card-title">{{ $t('home.notice') }}</view>
          <xm-empty
            v-if="!notices.length"
            icon="📢"
          />
          <view
            v-for="item in noticeList"
            :key="item.id"
            class="notice-item"
          >
            <view class="xm-value">{{ item.title }}</view>
            <view class="xm-label">{{ item.time }}</view>
          </view>
          <view
            v-if="notices.length"
            class="xm-label card-more"
            @click="go('/pages/notice/notice')"
            >{{ $t('home.viewAll') }}</view
          >
        </view>

        <view class="xm-card">
          <view class="xm-card-title">{{ $t('home.examplan') }}</view>
          <xm-empty
            v-if="!examplans.length"
            icon="📝"
          />
          <view
            v-for="item in examplanList"
            :key="item.id"
            class="notice-item"
          >
            <view class="xm-value">{{ item.name }}</view>
            <view
              class="xm-label"
              v-if="item.time"
              >{{ item.time }}</view
            >
          </view>
          <view
            v-if="examplans.length"
            class="xm-label card-more"
            @click="go('/pages/examplan/examplan')"
            >{{ $t('home.viewAll') }}</view
          >
        </view>
      </template>

      <template v-else>
        <!-- 考勤统计（数据来自 /attendance/getPie，纯 CSS 环形图 + 图例） -->
        <view class="xm-card">
          <view class="xm-card-title">{{ $t('home.attendanceStats') }}</view>
          <xm-empty
            v-if="!attendanceTotal"
            icon="🕐"
          />
          <template v-else>
            <view class="donut-wrap">
              <view
                class="donut"
                :style="{ background: donutGradient }"
              >
                <view class="donut-hole">
                  <text class="donut-num">{{ attendanceTotal }}</text>
                  <text class="donut-label">{{ $t('home.donutTotal') }}</text>
                </view>
              </view>
            </view>
            <view
              v-for="r in attendanceRows"
              :key="r.label"
              class="stat-row"
            >
              <view
                class="stat-dot"
                :style="{ background: r.color }"
              ></view>
              <text class="stat-label">{{ r.label }}</text>
              <view class="stat-track">
                <view
                  class="stat-fill"
                  :style="{ width: r.percent + '%', background: r.color }"
                ></view>
              </view>
              <text class="stat-num">{{ r.value }}</text>
            </view>
          </template>
        </view>

        <!-- 成绩统计（数据来自 /score/getLine，纯 CSS 柱状图） -->
        <view class="xm-card">
          <view class="xm-card-title">{{ $t('home.scoreStats') }}</view>
          <xm-empty
            v-if="!scoreRows.length"
            icon="💯"
          />
          <view
            v-else
            class="vbar-chart"
          >
            <view
              v-for="(r, i) in scoreRows"
              :key="r.label"
              class="vbar-col"
            >
              <text class="vbar-num">{{ r.value }}</text>
              <view class="vbar-track">
                <view
                  class="vbar-bar"
                  :style="{ height: vbarHeight(r), background: bandColors[i % bandColors.length] }"
                ></view>
              </view>
              <text class="vbar-label">{{ r.label }}</text>
            </view>
          </view>
        </view>
      </template>
    </template>
    <xm-loader />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onHide } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { usePermission } from '@/composables/usePermission'
import { get } from '@/utils/request'
import { resetWsUnread } from '@/utils/websocket'
import { t, isZh } from '@/i18n'
import { isZhLocale, toggleLocale } from '@/composables/useLocale'
import { cycleTheme, themeMode, themeClass } from '@/composables/useTheme'
import { courseBlockStyle } from '@/utils/courseColor'

const userStore = useUserStore()
const messageStore = useMessageStore()
const { pullPermissions } = usePermission()
const userName = computed(() => userStore.user.name || userStore.user.username || t('layout.guest'))

/** 消息中心未读数（本地持久化的推送历史，入口在头卡铃铛与「我的」页） */
const unreadCount = computed(() => messageStore.unreadCount)

/** 角色码转展示名（与「我的」页一致） */
const roleLabel = computed(() => {
  const map = {
    ADMIN: t('login.roleAdmin'),
    TEACHER: t('login.roleTeacher'),
    STUDENT: t('login.roleStudent'),
  }
  return map[userStore.role] || userStore.role || ''
})

const avatarLetter = computed(() => (userName.value || '?').slice(0, 1))
const todayText = computed(() => {
  const d = new Date()
  return `${d.getMonth() + 1}/${d.getDate()}`
})

// 功能入口：与 Web 端路由 meta.roles / meta.permission 保持一致
// perm 为 RBAC 权限码：权限已拉取时按码过滤；未拉取到（空）时退化为仅按 roles 过滤
// 管理类页面位于 pages-admin 分包；个人中心/修改密码入口移至「我的」Tab
const allMenus = [
  { path: '/pages/notice/notice', name: 'menu.notice', icon: '📢', perm: 'notice:view' },
  { path: '/pages/examplan/examplan', name: 'menu.examplan', icon: '📝', perm: 'examplan:view' },
  { path: '/pages/roomplan/roomplan', name: 'menu.roomplan', icon: '🏫', perm: 'roomplan:view' },
  { path: '/pages-admin/college/college', name: 'menu.college', icon: '🏛', roles: ['ADMIN'], perm: 'college:view' },
  {
    path: '/pages-admin/speciality/speciality',
    name: 'menu.speciality',
    icon: '📚',
    roles: ['ADMIN'],
    perm: 'speciality:view',
  },
  { path: '/pages-admin/classes/classes', name: 'menu.classes', icon: '👨‍👩‍👧', roles: ['ADMIN'], perm: 'classes:view' },
  { path: '/pages/course/course', name: 'menu.course', icon: '📖', perm: 'course:view' },
  { path: '/pages/choice/choice', name: 'menu.choice', icon: '🧾', perm: 'choice:view' },
  {
    path: '/pages/curriculum/curriculum',
    name: 'menu.curriculum',
    icon: '🗓',
    roles: ['STUDENT'],
    perm: 'curriculum:view',
  },
  { path: '/pages/score/score', name: 'menu.score', icon: '💯', perm: 'score:view' },
  { path: '/pages/comment/comment', name: 'menu.comment', icon: '⭐', perm: 'comment:view' },
  { path: '/pages/apply/apply', name: 'menu.apply', icon: '📮', perm: 'apply:view' },
  { path: '/pages/homework/homework', name: 'menu.homework', icon: '📒', perm: 'homework:view' },
  { path: '/pages/attendance/attendance', name: 'menu.attendance', icon: '🕐', perm: 'attendance:view' },
  { path: '/pages-admin/admin/admin', name: 'menu.admin', icon: '👤', roles: ['ADMIN'], perm: 'admin:view' },
  { path: '/pages-admin/teacher/teacher', name: 'menu.teacher', icon: '👨‍🏫', roles: ['ADMIN'], perm: 'teacher:view' },
  { path: '/pages-admin/student/student', name: 'menu.student', icon: '🎓', roles: ['ADMIN'], perm: 'student:view' },
]

const menuItems = computed(() => {
  const role = userStore.role
  const perms = userStore.permissions
  const isAdmin = role === 'ADMIN'
  return allMenus.filter((m) => {
    if (m.roles && !m.roles.includes(role)) return false
    // RBAC：权限码已拉取且非管理员时，隐藏无权限的入口（ADMIN 固定放行）
    if (m.perm && !isAdmin && perms.length && !perms.includes(m.perm)) return false
    return true
  })
})

const go = (path) => uni.navigateTo({ url: path })

const notices = ref([])
const examplans = ref([])
// 课程推荐（基于物品的协同过滤）：仅学生角色请求与展示
const recommends = ref([])

// ===== 今日课程（学生：选课课表取今日列；教师：本人授课课程取今天星期） =====
const todayCourses = ref([])
const WEEKDAY_FIELDS = ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday']
const WEEKDAY_ZH = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']

/** 从「第一大节（08:30 ~ 10:10）」解析起止时间 */
const parseSegmentTime = (segment) => {
  const m = /(\d{1,2}:\d{2})\s*~\s*(\d{1,2}:\d{2})/.exec(segment || '')
  return m ? [m[1], m[2]] : ['', '']
}

const toMinutes = (hhmm) => {
  const [h, m] = String(hhmm).split(':').map(Number)
  return (h || 0) * 60 + (m || 0)
}

const classStatus = (segment) => {
  const [start, end] = parseSegmentTime(segment)
  if (!start || !end) return ''
  const now = new Date().getHours() * 60 + new Date().getMinutes()
  if (now < toMinutes(start)) return 'todo'
  if (now > toMinutes(end)) return 'done'
  return 'doing'
}

const loadTodayCourses = async () => {
  if (userStore.role === 'STUDENT') {
    // 学生：课表接口按当前登录学生返回整周课表，取今天一列
    const res = await get('/choice/getCurriculum')
    const rows = (res.data && res.data.data) || []
    const field = WEEKDAY_FIELDS[new Date().getDay()]
    todayCourses.value = rows
      .filter((r) => r[field])
      .map((r) => {
        const text = String(r[field]).trim()
        const first = text.split('\n')[0].trim()
        // 单元格若为「课程名（教师）」格式：拆成 课程名 + 第二行教师，与演示稿「名称/信息」两行结构一致
        const m = /^(.*?)[（(]([^）(]*)[）)]\s*$/.exec(first)
        const name = m ? m[1].trim() : first
        const subParts = [m && m[2].trim(), text.split('\n').slice(1).join(' ')].filter(Boolean)
        return {
          key: r.segment + text,
          segment: r.segment,
          name,
          sub: subParts.join(' · '),
        }
      })
    // 单元格不含教室：按「课程名 + 教师」反查课程表补齐 room，第二行组成「教室 · 老师」（与演示稿一致）
    try {
      const res2 = await get('/course/selectAll')
      const courses = (res2.data && res2.data.data) || []
      todayCourses.value = todayCourses.value.map((c) => {
        const hit = courses.find((k) => k.name === c.name && (!c.sub || k.teacherName === c.sub))
        return { ...c, sub: [hit && hit.room, c.sub].filter(Boolean).join(' · ') }
      })
    } catch {
      // 反查失败时保留「老师」作为第二行，不影响课表展示
    }
  } else if (userStore.role === 'TEACHER') {
    // 教师：按本人 teacherId 过滤授课课程，再取今天星期几的课
    const res = await get('/course/selectPage', { pageNum: 1, pageSize: 100, teacherId: userStore.user.id })
    const rows = ((res.data && res.data.data && res.data.data.list) || []).filter(
      (c) => c.teacherId === userStore.user.id,
    )
    const todayZh = WEEKDAY_ZH[new Date().getDay()]
    todayCourses.value = rows
      .filter((c) => c.week === todayZh)
      .map((c) => ({ key: c.segment + c.name, segment: c.segment, name: c.name, sub: c.room || '' }))
  } else {
    todayCourses.value = []
  }
  todayCourses.value.forEach((c) => {
    const [start, end] = parseSegmentTime(c.segment)
    c.start = start
    c.end = end
    c.status = classStatus(c.segment)
  })
  // 按开始时间排序兜底：不依赖后端返回顺序，保证「下一节」判定正确
  todayCourses.value.sort((a, b) => toMinutes(a.start || '23:59') - toMinutes(b.start || '23:59'))
}

const todayStatusText = computed(() => ({
  doing: t('home.classDoing'),
  todo: t('home.classTodo'),
  done: t('home.classDone'),
}))

// 首页压缩（方案 B）：今日课程默认展开（严格按演示稿），可收起为摘要条 + 动态/统计分组标签页
const homeTab = ref('feed')
const todayExpanded = ref(true)

/** 标题右侧日期：中文「9月18日 周四」/ 英文「9/18 Thu」 */
const todayTitleText = computed(() => {
  const d = new Date()
  if (isZh()) {
    return `${d.getMonth() + 1}月${d.getDate()}日 周${'日一二三四五六'[d.getDay()]}`
  }
  return `${d.getMonth() + 1}/${d.getDate()} ${['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'][d.getDay()]}`
})
const todayCountText = computed(() => t('home.todayCount', { n: todayCourses.value.length }))
/** 当前正在上的课（进行中） */
const doingCourse = computed(() => todayCourses.value.find((c) => c.status === 'doing') || null)
/** 下一节未开始的课（课表按节次顺序返回，取第一个未开始） */
const nextCourse = computed(() => todayCourses.value.find((c) => c.status === 'todo') || null)

// 教务通知拉取：进入首页与收到 WebSocket 推送（新教务通知）时都会调用（返回 Promise 供缓存写回时机使用）
const loadNotices = () =>
  get('/notice/selectAll').then((res) => {
    notices.value = (res.data && res.data.data) || []
  })
const onWsPush = () => loadNotices()

// 首页仅展示最新 3 条，完整列表在对应页面分页浏览
const HOME_LIST_LIMIT = 3
const noticeList = computed(() => notices.value.slice(0, HOME_LIST_LIMIT))
const examplanList = computed(() => examplans.value.slice(0, HOME_LIST_LIMIT))

// 考勤统计
const attendanceStats = ref({ late: 0, absent: 0, earlyLeave: 0, normal: 0 })
const attendanceTotal = computed(() => {
  const s = attendanceStats.value
  return s.late + s.absent + s.earlyLeave + s.normal
})
const attendanceRows = computed(() => {
  const s = attendanceStats.value
  const total = attendanceTotal.value
  if (!total) return []
  const mk = (label, value, color) => ({
    label,
    value,
    color,
    percent: Math.round((value / total) * 100),
  })
  return [
    mk(t('home.statusNormal'), s.normal, '#22b866'),
    mk(t('home.statusLate'), s.late, '#f59e0b'),
    mk(t('home.statusEarlyLeave'), s.earlyLeave, '#3b82f6'),
    mk(t('home.statusAbsent'), s.absent, '#f0555f'),
  ]
})

// 成绩统计
const scoreStats = ref({ excellent: 0, good: 0, fail: 0 })
const scoreRows = computed(() => {
  const s = scoreStats.value
  const total = s.excellent + s.good + s.fail
  if (!total) return []
  const mk = (label, value) => ({ label, value, percent: Math.round((value / total) * 100) })
  return [mk(t('home.bandExcellent'), s.excellent), mk(t('home.bandGood'), s.good), mk(t('home.bandFail'), s.fail)]
})

// 图表实现：纯 CSS（conic-gradient 环形图 + 柱状图），零依赖、小程序 100% 渲染、深浅主题自适应
const bandColors = ['#22b866', '#5b6cff', '#f0555f']

/** 环形图锥形渐变：按各状态精确占比分段（用原始值计算，避免四舍五入留下缝隙） */
const donutGradient = computed(() => {
  const total = attendanceTotal.value
  if (!total) return 'transparent'
  let acc = 0
  const stops = attendanceRows.value
    .filter((r) => r.value > 0)
    .map((r) => {
      const start = (acc / total) * 100
      acc += r.value
      const end = (acc / total) * 100
      return `${r.color} ${start}% ${end}%`
    })
  return `conic-gradient(${stops.join(', ')})`
})

const vbarMax = computed(() => Math.max(...scoreRows.value.map((r) => r.value), 1))

const vbarHeight = (r) => Math.max(12, Math.round((r.value / vbarMax.value) * 100)) + '%'

// 首页数据本地缓存（按用户 ID 隔离，防止切换账号闪现他人数据）：
// 进入首页先渲染缓存（秒开不白屏），静默刷新完成后写回；无缓存时显示骨架屏
const HOME_CACHE_KEY = 'xm-home-cache-'
const firstLoading = ref(true)

const applyCache = () => {
  try {
    const cached = uni.getStorageSync(HOME_CACHE_KEY + userStore.user.id)
    if (cached && typeof cached === 'object' && Array.isArray(cached.notices)) {
      notices.value = cached.notices || []
      examplans.value = cached.examplans || []
      if (cached.attendanceStats) attendanceStats.value = cached.attendanceStats
      if (cached.scoreStats) scoreStats.value = cached.scoreStats
      if (Array.isArray(cached.recommends)) recommends.value = cached.recommends
      return true
    }
  } catch {}
  return false
}

const saveCache = () => {
  try {
    uni.setStorageSync(HOME_CACHE_KEY + userStore.user.id, {
      notices: notices.value,
      examplans: examplans.value,
      attendanceStats: attendanceStats.value,
      scoreStats: scoreStats.value,
      recommends: recommends.value,
    })
  } catch {}
}

onShow(() => {
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  // 动态设置导航栏标题，跟随语言切换
  uni.setNavigationBarTitle({ title: t('menu.home') })

  // 载入当前用户的推送消息历史（铃铛未读角标）
  messageStore.loadForUser(userStore.user.id)
  // 回到首页即视为已读：清掉推送未读角标
  resetWsUnread()
  // 收到 WebSocket 推送时实时刷新首页通知列表（先解绑再绑定，防止页面反复进出后重复触发）
  uni.$off('ws:push', onWsPush)
  uni.$on('ws:push', onWsPush)

  // 权限码缺失（如旧版本登录留下的缓存）时补拉，保证菜单过滤与 Web 端授权一致
  if (userStore.role !== 'ADMIN' && !userStore.permissions.length) {
    pullPermissions()
  }

  // 首页数据：先渲染缓存（秒开），再静默刷新；无缓存时显示骨架屏。
  // 每个请求单独 catch（网络失败不应打断其它请求），全部结束后写缓存并撤骨架屏
  firstLoading.value = !applyCache()
  const tasks = [
    get('/examplan/selectAll').then((res) => {
      examplans.value = (res.data && res.data.data) || []
    }),
    get('/attendance/getPie').then((res) => {
      if (res.data.code !== '200') return
      const data = (res.data.data && res.data.data.data) || []
      const stats = { late: 0, absent: 0, earlyLeave: 0, normal: 0 }
      // 后端按中文状态分组统计，这里按中文键匹配（数据库存储值为中文）
      data.forEach((item) => {
        if (item.name === '迟到') stats.late = item.value || 0
        else if (item.name === '缺勤') stats.absent = item.value || 0
        else if (item.name === '早退') stats.earlyLeave = item.value || 0
        else if (item.name === '正常') stats.normal = item.value || 0
      })
      attendanceStats.value = stats
    }),
    get('/score/getLine').then((res) => {
      if (res.data.code !== '200') return
      const yAxis = (res.data.data && res.data.data.yAxis) || []
      if (yAxis.length >= 5) {
        scoreStats.value = {
          excellent: yAxis[0] || 0,
          good: yAxis[1] || 0,
          fail: yAxis[yAxis.length - 1] || 0,
        }
      }
    }),
  ]
  // 学生角色追加课程推荐（协同过滤）
  if (userStore.role === 'STUDENT') {
    tasks.push(
      get('/course/recommend?limit=4').then((res) => {
        recommends.value = (res.data && res.data.data) || []
      }),
    )
  }
  Promise.all(
    [...tasks, loadNotices().catch(() => {}), loadTodayCourses().catch(() => {})].map((p) => p.catch(() => {})),
  ).then(() => {
    saveCache()
    firstLoading.value = false
  })
})

onHide(() => {
  uni.$off('ws:push', onWsPush)
})
</script>

<style lang="scss" scoped>
/* 渐变头卡内部：左侧内容可收缩省略，右侧按钮固定不被挤压 */
.hero-left {
  flex: 1;
  min-width: 0;
}

.hero-avatar {
  width: 84rpx;
  height: 84rpx;
  line-height: 84rpx;
  text-align: center;
  border-radius: 50%;
  font-size: 36rpx;
  color: #ffffff;
  background: rgba(255, 255, 255, 0.2);
  border: 2rpx solid rgba(255, 255, 255, 0.5);
  flex-shrink: 0;
}

.hero-meta {
  flex: 1;
  min-width: 0;
}

/* 头卡上的毛玻璃胶囊小按钮（脱离 xm-btn 体系：白字透明底融入渐变） */
.hero-btn {
  height: 60rpx;
  line-height: 60rpx;
  padding: 0 22rpx;
  margin: 0;
  font-size: 24rpx;
  color: #ffffff;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 999rpx;
  border: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.hero-btn::after {
  border: none;
}

.hero-btn:active {
  background: rgba(255, 255, 255, 0.3);
}

/* 消息铃铛：相对定位承载未读角标 */
.hero-bell {
  position: relative;
}

.bell-badge {
  position: absolute;
  top: -8rpx;
  right: -6rpx;
  min-width: 30rpx;
  height: 30rpx;
  line-height: 30rpx;
  padding: 0 6rpx;
  border-radius: 999rpx;
  font-size: 18rpx;
  text-align: center;
  color: #ffffff;
  background: #f0555f;
}

.hero-prefs {
  flex-shrink: 0;
}

.notice-item {
  padding: 16rpx 0;
  border-bottom: 1rpx solid var(--xm-border);
}

.notice-item:last-child {
  border-bottom: none;
}

/* 推荐理由：品牌色弱化小字 */
.recommend-reason {
  font-size: 24rpx;
  color: var(--xm-brand);
  margin-top: 4rpx;
}

/* 「查看全部」入口：右对齐，主题色 */
.card-more {
  text-align: right;
  padding-top: 12rpx;
  color: var(--xm-brand);
}

/* ===== 今日课程：时间轴彩色块 ===== */

/* 标题右侧日期（与演示稿一致：9月18日 周四） */
.today-date {
  font-weight: normal;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

/* 今日课程摘要条（收起态，点击展开完整时间轴） */
.today-summary {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.summary-main {
  flex: 1;
  min-width: 0;
}

.summary-line1 {
  font-size: 26rpx;
  color: var(--xm-text);
}

.summary-line2 {
  font-size: 22rpx;
  color: var(--xm-text-2);
  margin-top: 4rpx;
}

.summary-arrow {
  font-size: 22rpx;
  color: var(--xm-text-2);
  flex-shrink: 0;
}

/* 展开态底部的「收起」行 */
.today-collapse {
  display: flex;
  justify-content: flex-end;
  padding-top: 8rpx;
}

/* 动态 / 统计 分组标签页 */
.home-tabs {
  display: flex;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.home-tab {
  flex: 1;
  text-align: center;
  padding: 14rpx 0;
  border-radius: 14rpx;
  font-size: 26rpx;
  color: var(--xm-text-2);
  background: var(--xm-bg-card);
  border: 1rpx solid var(--xm-border);
}

.home-tab.on {
  background: var(--xm-brand);
  color: #ffffff;
  font-weight: bold;
  border-color: var(--xm-brand);
}

.today-item {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.today-item:last-child {
  margin-bottom: 4rpx;
}

.today-time {
  width: 96rpx;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-end;
  gap: 2rpx;
}

.today-start {
  font-size: 26rpx;
  font-weight: bold;
  color: var(--xm-text);
}

.today-end {
  font-size: 20rpx;
  color: var(--xm-text-2);
}

.today-rail {
  width: 24rpx;
  flex-shrink: 0;
  display: flex;
  justify-content: center;
}

.today-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: var(--xm-brand);
  margin-top: 22rpx;
}

.today-dot.is-done {
  background: var(--xm-border);
}

.today-dot.is-doing {
  background: var(--xm-success);
  animation: today-pulse 2s infinite;
}

@keyframes today-pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(34, 184, 102, 0.3);
  }

  70% {
    box-shadow: 0 0 0 12rpx rgba(34, 184, 102, 0);
  }

  100% {
    box-shadow: 0 0 0 0 rgba(34, 184, 102, 0);
  }
}

.today-block {
  flex: 1;
  border-radius: 16rpx;
  padding: 18rpx 22rpx;
  border: 1rpx solid transparent;
  /* 单节课（只有名称一行）也保持与演示稿相近的块高度 */
  min-height: 92rpx;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.today-block.is-done {
  opacity: 0.55;
}

.today-name {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.today-course {
  font-size: 28rpx;
  font-weight: bold;
}

.today-sub {
  display: block;
  font-size: 22rpx;
  margin-top: 6rpx;
  opacity: 0.85;
}

.today-status {
  font-size: 20rpx;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
  font-weight: normal;
  flex-shrink: 0;
}

.status-doing {
  background: var(--xm-success);
  color: #ffffff;
}

.status-done {
  color: var(--xm-text-2);
}

/* ===== 统计图表：纯 CSS 环形图 + 柱状图 ===== */
.donut-wrap {
  display: flex;
  justify-content: center;
  padding: 8rpx 0 24rpx;
}

.donut {
  width: 280rpx;
  height: 280rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.donut-hole {
  width: 176rpx;
  height: 176rpx;
  border-radius: 50%;
  background: var(--xm-bg-card);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.donut-num {
  font-size: 40rpx;
  font-weight: bold;
  color: var(--xm-text);
}

.donut-label {
  font-size: 20rpx;
  color: var(--xm-text-2);
  margin-top: 2rpx;
}

.stat-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 14rpx;
}

.stat-dot {
  width: 18rpx;
  height: 18rpx;
  border-radius: 50%;
  flex-shrink: 0;
}

.stat-label {
  width: 110rpx;
  font-size: 26rpx;
  color: var(--xm-text-2);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-shrink: 0;
}

.stat-track {
  flex: 1;
  height: 14rpx;
  border-radius: 999rpx;
  background: var(--xm-bg-input);
  overflow: hidden;
}

.stat-fill {
  height: 100%;
  border-radius: 999rpx;
}

.stat-num {
  width: 60rpx;
  text-align: right;
  font-size: 26rpx;
  flex-shrink: 0;
}

.vbar-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  padding: 12rpx 8rpx 4rpx;
}

.vbar-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
  width: 140rpx;
}

.vbar-num {
  font-size: 24rpx;
  color: var(--xm-text);
}

.vbar-track {
  width: 64rpx;
  height: 240rpx;
  border-radius: 999rpx;
  background: var(--xm-bg-input);
  display: flex;
  align-items: flex-end;
  overflow: hidden;
}

.vbar-bar {
  width: 100%;
  border-radius: 999rpx;
  transition: height 0.3s ease;
}

.vbar-label {
  font-size: 24rpx;
  color: var(--xm-text-2);
}
</style>

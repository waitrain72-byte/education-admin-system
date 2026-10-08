<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 课程（与 Web 端 views/courses/CoursesPage.vue 一致）：学生 = 已选、教师 = 所授、管理员 = 全部；点进去就是课程空间 -->
    <view class="head">
      <view class="head-sub">{{ $t('courses.sub.' + roleKey) }}</view>
      <view class="head-actions">
        <button
          v-if="role === 'STUDENT'"
          class="xm-btn xm-btn-plain xm-btn-sm"
          @click="go('/pages-course/transcript/transcript')"
        >
          {{ $t('transcript.entry') }}
        </button>
        <button
          class="xm-btn xm-btn-sm"
          :class="role === 'STUDENT' ? 'xm-btn-primary' : 'xm-btn-plain'"
          @click="go('/pages-course/square/square')"
        >
          {{ role === 'STUDENT' ? $t('courses.goSelect') : $t('nav.square') }}
        </button>
        <button
          v-if="role === 'ADMIN'"
          class="xm-btn xm-btn-primary xm-btn-sm"
          @click="go('/pages-admin/course-form/course-form')"
        >
          {{ $t('courses.openCourse') }}
        </button>
      </view>
    </view>

    <view class="toolbar">
      <xm-search
        v-model="keyword"
        :placeholder="$t('courses.searchPlaceholder')"
      />
      <xm-chips
        v-model="filter"
        :options="filterOptions"
      />
    </view>

    <view
      v-if="loading && !courses.length"
      class="xm-list"
    >
      <view
        v-for="i in 3"
        :key="i"
        class="xm-card"
      >
        <view class="skeleton skeleton-title" />
        <view class="skeleton skeleton-line" />
        <view class="skeleton skeleton-line short" />
      </view>
    </view>
    <view
      v-else-if="shown.length"
      class="xm-list"
    >
      <view
        v-for="c in shown"
        :key="c.id"
        class="list-item"
      >
        <xm-course-card
          :course="c"
          :url="'/pages-course/space/space?id=' + c.id"
        >
          <!-- 小程序不支持按条件提供插槽：插槽总是提供，里面按角色区分 -->
          <template #foot>
            <template v-if="role === 'STUDENT'">
              <text class="xm-ellipsis">{{ c.teacherName || '—' }}</text>
              <text
                v-if="c.credit != null"
                class="xm-num"
                >{{ $t('courses.credits', { n: c.credit }) }}</text
              >
            </template>
            <template v-else>
              <text class="xm-num">{{ $t('workbench.students', { n: c.studentCount || 0 }) }}</text>
              <text
                v-if="c.ungraded"
                class="xm-tag xm-tag-warning"
                >{{ $t('workbench.toGrade', { n: c.ungraded }) }}</text
              >
              <text
                v-else-if="role === 'ADMIN'"
                class="xm-ellipsis"
                >{{ c.teacherName || '—' }}</text
              >
            </template>
          </template>
        </xm-course-card>
      </view>
    </view>
    <xm-empty
      v-else
      icon="book"
      :text="courses.length ? $t('courses.noMatch') : $t('courses.empty.' + roleKey)"
    />
    <xm-loader />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { courseApi } from '@/api'
import { SILENT } from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { ensureLoggedIn } from '@/utils/authGuard'
import { readUserCache, writeUserCache } from '@/utils/userCache'
import { t } from '@/i18n'

const userStore = useUserStore()
const role = computed(() => userStore.role)
const roleKey = computed(() => String(role.value || 'STUDENT').toLowerCase())

const courses = ref([])
const loading = ref(false)
const keyword = ref('')
const filter = ref('all')
let cachedFor = ''

const STATUS_OF = { active: '已开课', upcoming: '未开课', finished: '已结课' }

const filterOptions = computed(() => [
  { value: 'all', label: t('courses.filter.all'), count: courses.value.length },
  ...Object.keys(STATUS_OF).map((key) => ({
    value: key,
    label: t('courses.filter.' + key),
    count: courses.value.filter((c) => c.status === STATUS_OF[key]).length,
  })),
])

const shown = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  return courses.value.filter((c) => {
    if (filter.value !== 'all' && c.status !== STATUS_OF[filter.value]) return false
    if (!q) return true
    return [c.name, c.teacherName, c.room].some((v) =>
      String(v || '')
        .toLowerCase()
        .includes(q),
    )
  })
})

const load = async () => {
  loading.value = true
  try {
    courses.value = (await courseApi.mine(SILENT)) || []
    writeUserCache('courses', userStore.accountKey, courses.value)
  } catch {
    // 提示已由请求层统一弹出，保留缓存里的列表
  } finally {
    loading.value = false
  }
}

const go = (url) => uni.navigateTo({ url })

onShow(() => {
  if (!ensureLoggedIn()) return
  uni.setNavigationBarTitle({ title: t('courses.title') })
  // 先渲染本账号上次的列表（秒开），再静默刷新；从课程空间回来时人数、待批数也跟着更新
  if (cachedFor !== userStore.accountKey) {
    cachedFor = userStore.accountKey
    courses.value = readUserCache('courses', cachedFor) || []
  }
  load()
})

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
.head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.head-sub {
  flex: 1;
  min-width: 300rpx;
  font-size: 24rpx;
  line-height: 1.5;
  color: var(--xm-text-2);
}

.head-actions {
  display: flex;
  gap: 12rpx;
}

.toolbar {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.list-item {
  margin-bottom: 24rpx;
}

@media (min-width: 720px) {
  .list-item {
    margin-bottom: 0;
  }
}
</style>

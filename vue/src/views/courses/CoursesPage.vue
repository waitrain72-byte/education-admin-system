<template>
  <div class="courses">
    <div class="page-head">
      <div>
        <h1 class="page-title">{{ $t('courses.title') }}</h1>
        <p class="page-sub">{{ $t('courses.sub.' + roleKey) }}</p>
      </div>
      <div class="page-actions">
        <el-button v-if="role === 'STUDENT'" type="primary" @click="router.push('/legacy/course')">
          {{ $t('courses.goSelect') }}
        </el-button>
        <el-button v-if="role === 'ADMIN'" type="primary" @click="router.push('/admin/courses')">
          {{ $t('courses.openCourse') }}
        </el-button>
      </div>
    </div>

    <div class="toolbar">
      <div class="chips" role="tablist">
        <button
          v-for="f in filters"
          :key="f.key"
          type="button"
          role="tab"
          class="chip"
          :class="{ 'is-on': filter === f.key }"
          :aria-selected="filter === f.key"
          @click="filter = f.key"
        >
          {{ $t(f.label) }}<span class="chip__count num">{{ counts[f.key] }}</span>
        </button>
      </div>
      <el-input
        v-model="keyword"
        class="toolbar__search"
        :placeholder="$t('courses.searchPlaceholder')"
        clearable
        :prefix-icon="Search"
      />
    </div>

    <div v-if="loading && !courses.length" class="course-grid">
      <el-skeleton v-for="i in 6" :key="i" animated class="panel panel--pad" :rows="3" />
    </div>
    <div v-else-if="shown.length" class="course-grid">
      <CourseCard v-for="c in shown" :key="c.id" :course="c" :to="`/course/${c.id}`">
        <template v-if="role !== 'STUDENT'" #foot>
          <span class="num">{{ $t('workbench.students', { n: c.studentCount ?? 0 }) }}</span>
          <span v-if="c.ungraded" class="pill pill--warn">{{ $t('workbench.toGrade', { n: c.ungraded }) }}</span>
          <span v-else-if="role === 'ADMIN'" class="foot-teacher">{{ c.teacherName || '—' }}</span>
        </template>
      </CourseCard>
    </div>
    <div v-else class="panel empty-note">
      {{ courses.length ? $t('courses.noMatch') : $t('courses.empty.' + roleKey) }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUser } from '@/components/useUser'
import CourseCard from '@/components/CourseCard.vue'

defineOptions({ name: 'CoursesPage' })

const router = useRouter()
const { user } = useUser()
const role = computed(() => user.value.role)
const roleKey = computed(() => String(role.value || 'STUDENT').toLowerCase())

const courses = ref<any[]>([])
const loading = ref(false)
const keyword = ref('')
const filter = ref<'all' | 'active' | 'upcoming' | 'finished'>('all')

const filters = [
  { key: 'all', label: 'courses.filter.all' },
  { key: 'active', label: 'courses.filter.active' },
  { key: 'upcoming', label: 'courses.filter.upcoming' },
  { key: 'finished', label: 'courses.filter.finished' },
] as const

const STATUS_OF: Record<string, string> = { active: '已开课', upcoming: '未开课', finished: '已结课' }

const counts = computed(() => ({
  all: courses.value.length,
  active: courses.value.filter((c) => c.status === '已开课').length,
  upcoming: courses.value.filter((c) => c.status === '未开课').length,
  finished: courses.value.filter((c) => c.status === '已结课').length,
}))

const shown = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  return courses.value.filter((c) => {
    if (filter.value !== 'all' && c.status !== STATUS_OF[filter.value]) return false
    if (!q) return true
    return [c.name, c.teacherName, c.room].some((v) => String(v || '').toLowerCase().includes(q))
  })
})

onMounted(async () => {
  loading.value = true
  try {
    courses.value = (await request.get<any[]>('/course/mine')) || []
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 14px;
  border: 1px solid var(--xm-border);
  border-radius: 999px;
  background: var(--xm-bg-card);
  color: var(--xm-text-regular);
  font: inherit;
  font-size: 14px;
  cursor: pointer;
}

.chip:hover {
  border-color: var(--xm-brand);
}

.chip.is-on {
  border-color: var(--xm-brand);
  background: var(--xm-brand);
  color: var(--xm-on-brand);
}

.chip__count {
  font-size: 12px;
  opacity: 0.75;
}

.toolbar__search {
  width: 260px;
}

.foot-teacher {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

@media (max-width: 640px) {
  .toolbar__search {
    width: 100%;
  }
}
</style>

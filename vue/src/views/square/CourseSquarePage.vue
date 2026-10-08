<template>
  <div class="square">
    <div class="page-head">
      <div>
        <h1 class="page-title">{{ $t('square.title') }}</h1>
        <p class="page-sub">{{ canEnroll ? $t('square.sub.student') : $t('square.sub.other') }}</p>
      </div>
      <span class="square__count num">{{ $t('square.count', { n: courses.length }) }}</span>
    </div>

    <div class="square-filters">
      <el-input
        v-model="keyword"
        class="square-filters__search"
        :placeholder="$t('square.search')"
        clearable
        :prefix-icon="Search"
        @input="debouncedLoad"
        @clear="load"
      />
      <div class="chips" role="tablist" :aria-label="$t('square.allTypes')">
        <button
          v-for="opt in typeOptions"
          :key="opt.value"
          type="button"
          role="tab"
          class="chip"
          :class="{ 'is-on': type === opt.value }"
          :aria-selected="type === opt.value"
          @click="setType(opt.value)"
        >
          {{ opt.label }}
        </button>
      </div>
      <el-select
        v-model="week"
        class="square-filters__select"
        clearable
        :placeholder="$t('square.allWeekdays')"
        :aria-label="$t('square.allWeekdays')"
        @change="load"
      >
        <el-option v-for="w in WEEKDAYS" :key="w" :label="w" :value="w" />
      </el-select>
      <el-select
        v-model="status"
        class="square-filters__select"
        clearable
        :placeholder="$t('square.allStatus')"
        :aria-label="$t('square.allStatus')"
        @change="load"
      >
        <el-option :label="$t('courses.filter.active')" value="已开课" />
        <el-option :label="$t('courses.filter.upcoming')" value="未开课" />
      </el-select>
      <el-checkbox v-model="available" @change="load">{{ $t('square.onlyAvailable') }}</el-checkbox>
      <el-checkbox v-model="includeEnded" @change="load">{{ $t('square.includeEnded') }}</el-checkbox>
    </div>

    <section v-if="recommendations.length" class="square-rec">
      <div class="square-rec__head">
        <h2 class="square-rec__title">{{ $t('square.recommend') }}</h2>
        <span class="square-rec__hint">{{ $t('square.recommendHint') }}</span>
      </div>
      <div class="square-grid">
        <SquareCard
          v-for="c in recommendations"
          :key="'rec' + c.id"
          :course="c"
          :can-enroll="canEnroll"
          :busy="busyId === c.id"
          @enroll="enroll"
          @drop="drop"
        />
      </div>
    </section>

    <div v-if="loading && !courses.length" class="square-grid">
      <el-skeleton v-for="i in 6" :key="i" animated class="panel panel--pad" :rows="4" />
    </div>
    <div v-else-if="courses.length" class="square-grid">
      <SquareCard
        v-for="c in courses"
        :key="c.id"
        :course="c"
        :can-enroll="canEnroll"
        :busy="busyId === c.id"
        @enroll="enroll"
        @drop="drop"
      />
    </div>
    <div v-else class="panel empty-note">{{ $t('square.empty') }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import SquareCard from './SquareCard.vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from '@/utils/element-plus'
import { WEEKDAYS } from '@/utils/schedule'
import { t } from '@/i18n'

defineOptions({ name: 'CourseSquarePage' })

const courses = ref<Record<string, any>[]>([])
const recommendations = ref<Record<string, any>[]>([])
const types = ref<string[]>([])
const canEnroll = ref(false)
const loading = ref(false)
const busyId = ref<number | null>(null)

const keyword = ref('')
const type = ref('')
const week = ref('')
const status = ref('')
const available = ref(false)
const includeEnded = ref(false)

const typeOptions = computed(() => [
  { value: '', label: t('square.allTypes') },
  ...types.value.map((value) => ({ value, label: value })),
])

let seq = 0
const load = async () => {
  const mySeq = ++seq
  loading.value = true
  try {
    const data = await request.get<Record<string, any>>('/course/square', {
      params: {
        keyword: keyword.value.trim() || undefined,
        type: type.value || undefined,
        week: week.value || undefined,
        status: status.value || undefined,
        available: available.value || undefined,
        includeEnded: includeEnded.value || undefined,
      },
    })
    // 连续输入时只认最后一次请求的结果
    if (mySeq !== seq) return
    courses.value = data.courses || []
    recommendations.value = data.recommendations || []
    if (data.types?.length) types.value = data.types
    canEnroll.value = !!data.canEnroll
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    if (mySeq === seq) loading.value = false
  }
}

let timer: ReturnType<typeof setTimeout> | null = null
const debouncedLoad = () => {
  if (timer) clearTimeout(timer)
  timer = setTimeout(load, 300)
}
onBeforeUnmount(() => {
  if (timer) clearTimeout(timer)
})

const setType = (value: string) => {
  type.value = value
  load()
}

const enroll = async (course: Record<string, any>) => {
  busyId.value = course.id
  try {
    await request.post(`/course/${course.id}/enroll`)
    ElMessage.success(t('square.enrolledMsg', { name: course.name }))
    await load()
  } catch {
    // 满员、冲突等提示已由拦截器统一处理
  } finally {
    busyId.value = null
  }
}

const drop = async (course: Record<string, any>) => {
  try {
    await ElMessageBox.confirm(t('square.dropConfirm', { name: course.name }), t('square.drop'), { type: 'warning' })
  } catch {
    return
  }
  busyId.value = course.id
  try {
    await request.delete(`/course/${course.id}/enroll`)
    ElMessage.success(t('square.droppedMsg', { name: course.name }))
    await load()
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    busyId.value = null
  }
}

onMounted(load)
</script>

<style scoped>
.square__count {
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.square-filters {
  display: flex;
  align-items: center;
  gap: 10px 14px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.square-filters__search {
  width: 240px;
}

.square-filters__select {
  width: 128px;
}

.square-rec {
  margin-bottom: 22px;
  padding: 16px;
  border-radius: var(--xm-radius-lg);
  background: var(--xm-brand-soft);
}

.square-rec__head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.square-rec__title {
  font-size: 15px;
  font-weight: 700;
  color: var(--xm-text-primary);
}

.square-rec__hint {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.square-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
}

@media (max-width: 640px) {
  .square-filters__search {
    width: 100%;
  }

  .square-filters__select {
    width: calc(50% - 7px);
  }
}
</style>

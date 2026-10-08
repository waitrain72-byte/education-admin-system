<template>
  <div class="mb">
    <div v-if="!data && loading" class="panel panel--pad">
      <el-skeleton animated :rows="5" />
    </div>

    <template v-else-if="data">
      <section v-if="data.teacher" class="panel mb-teacher">
        <UserAvatar :name="data.teacher.name" :avatar="data.teacher.avatar" :size="44" />
        <div class="mb-teacher__info">
          <div class="mb-teacher__label">{{ $t('space.members.teacher') }}</div>
          <div class="mb-teacher__name">
            {{ data.teacher.name }}
            <span v-if="data.teacher.title" class="mb-teacher__title">{{ data.teacher.title }}</span>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel__head mb-head">
          <span class="panel__title">
            {{ $t('space.members.students') }}
            <span class="mb-count num">{{ $t('space.members.count', { n: students.length }) }}</span>
          </span>
          <el-input v-model="keyword" :placeholder="$t('space.members.search')" clearable class="mb-search" />
        </div>
        <div class="panel__body">
          <div v-if="!students.length" class="empty-note">{{ $t('space.members.empty') }}</div>
          <div v-else-if="!visible.length" class="empty-note">{{ $t('space.members.noMatch') }}</div>

          <!-- 老师：学情表（学号、出勤率、作业提交） -->
          <el-table v-else-if="teaching" :data="visible" row-key="id" class="mb-table">
            <el-table-column :label="$t('space.grades.student')" min-width="160">
              <template #default="{ row }">
                <div class="mb-who">
                  <UserAvatar :name="row.name" :avatar="row.avatar" :size="28" />
                  <span>{{ row.name }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="username" :label="$t('space.members.username')" min-width="120">
              <template #default="{ row }"><span class="num">{{ row.username }}</span></template>
            </el-table-column>
            <el-table-column prop="className" :label="$t('space.members.className')" min-width="120" />
            <el-table-column :label="$t('space.members.attendanceRate')" min-width="150" sortable :sort-method="byRate">
              <template #default="{ row }">
                <div v-if="row.attendanceRate != null" class="mb-rate">
                  <span class="mb-rate__bar"><span :class="rateTone(row.attendanceRate)" :style="{ width: row.attendanceRate + '%' }"></span></span>
                  <span class="num">{{ row.attendanceRate }}%</span>
                </div>
                <span v-else class="mb-none">—</span>
              </template>
            </el-table-column>
            <el-table-column :label="$t('space.members.submitted')" min-width="110" align="center">
              <template #default="{ row }">
                <span class="num">{{ row.submitted }}<span class="mb-none"> / {{ data.assignmentCount }}</span></span>
              </template>
            </el-table-column>
          </el-table>

          <!-- 学生：同学名单 -->
          <ul v-else class="mb-grid">
            <li v-for="s in visible" :key="s.id" class="mb-card">
              <UserAvatar :name="s.name" :avatar="s.avatar" :size="36" />
              <div class="mb-card__text">
                <div class="mb-card__name">{{ s.name }}</div>
                <div class="mb-card__sub">{{ s.className || '—' }}</div>
              </div>
            </li>
          </ul>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import UserAvatar from '@/components/UserAvatar.vue'
import request from '@/utils/request'
import { matchesPerson } from '@/utils/courseSpace'

const props = defineProps<{ overview: Record<string, any> }>()

const route = useRoute()
const courseId = computed(() => Number(route.params.id))
const teaching = computed(() => ['teacher', 'admin'].includes(props.overview.relation))

const data = ref<Record<string, any> | null>(null)
const loading = ref(false)
const keyword = ref('')

const students = computed<Record<string, any>[]>(() => data.value?.students || [])
const visible = computed(() => students.value.filter((s) => matchesPerson(s, keyword.value)))

const load = async () => {
  loading.value = true
  try {
    data.value = await request.get<Record<string, any>>(`/course/${courseId.value}/members`)
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    loading.value = false
  }
}

watch(courseId, load, { immediate: true })

const byRate = (a: Record<string, any>, b: Record<string, any>) => (a.attendanceRate ?? -1) - (b.attendanceRate ?? -1)
const rateTone = (rate: number) => (rate >= 90 ? 'is-ok' : rate >= 75 ? 'is-warn' : 'is-bad')
</script>

<style scoped>
.mb {
  display: grid;
  /* 列宽可以缩到 0：里面的宽表格自己横向滚动，不把整页撑出屏幕 */
  grid-template-columns: minmax(0, 1fr);
  gap: 20px;
}

.mb-teacher {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
}

.mb-teacher__label {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.mb-teacher__name {
  font-size: 16px;
  font-weight: 600;
  color: var(--xm-text-primary);
}

.mb-teacher__title {
  margin-left: 8px;
  font-size: 13px;
  font-weight: 400;
  color: var(--xm-text-secondary);
}

.mb-head {
  align-items: center;
  flex-wrap: wrap;
}

.mb-count {
  margin-left: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--xm-text-secondary);
}

.mb-search {
  width: 220px;
}

.mb-table {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: transparent;
}

.mb-who {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--xm-text-primary);
}

.mb-rate {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mb-rate__bar {
  width: 72px;
  height: 6px;
  border-radius: 999px;
  background: var(--xm-bg-sunken);
  overflow: hidden;
}

.mb-rate__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
}

.mb-rate__bar .is-ok {
  background: var(--xm-ok);
}

.mb-rate__bar .is-warn {
  background: var(--xm-warn);
}

.mb-rate__bar .is-bad {
  background: var(--xm-bad);
}

.mb-none {
  color: var(--xm-text-secondary);
}

.mb-grid {
  list-style: none;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: 8px;
}

.mb-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius);
}

.mb-card__text {
  min-width: 0;
}

.mb-card__name {
  font-size: 14px;
  color: var(--xm-text-primary);
}

.mb-card__sub {
  font-size: 12px;
  color: var(--xm-text-secondary);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

@media (max-width: 560px) {
  .mb-search {
    width: 100%;
  }
}
</style>

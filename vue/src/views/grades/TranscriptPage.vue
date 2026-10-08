<template>
  <div class="tp">
    <div class="page-head">
      <div>
        <h1 class="page-title">{{ $t('transcript.title') }}</h1>
        <p class="page-sub">{{ $t('transcript.sub') }}</p>
      </div>
    </div>

    <el-skeleton v-if="loading && !data" animated :rows="6" />

    <template v-else-if="data">
      <div class="tp-kpis">
        <div class="tp-kpi">
          <span class="tp-kpi__label">{{ $t('transcript.credits') }}</span>
          <b class="tp-kpi__value num">{{ summary.credits }}</b>
          <span v-if="summary.requiredCredits" class="tp-kpi__sub">{{ $t('transcript.ofRequired', { n: summary.requiredCredits }) }}</span>
          <span v-if="summary.requiredCredits" class="tp-kpi__bar"><span :style="{ width: creditFill + '%' }"></span></span>
        </div>
        <div class="tp-kpi">
          <span class="tp-kpi__label">{{ $t('transcript.gpa') }}</span>
          <b class="tp-kpi__value num">{{ summary.gpa ?? '—' }}</b>
        </div>
        <div class="tp-kpi">
          <span class="tp-kpi__label">{{ $t('transcript.average') }}</span>
          <b class="tp-kpi__value num">{{ summary.average ?? '—' }}</b>
        </div>
        <div class="tp-kpi">
          <span class="tp-kpi__label">{{ $t('transcript.results') }}</span>
          <span class="tp-kpi__counts">
            <span class="pill pill--ok">{{ $t('transcript.passed', { n: summary.passed }) }}</span>
            <span v-if="summary.failed" class="pill pill--bad">{{ $t('transcript.failed', { n: summary.failed }) }}</span>
            <span v-if="summary.pending" class="pill">{{ $t('transcript.pending', { n: summary.pending }) }}</span>
          </span>
        </div>
      </div>

      <section class="panel">
        <el-table :data="rows" row-key="courseId" class="tp-table" :empty-text="$t('transcript.empty')">
          <el-table-column :label="$t('transcript.course')" min-width="190" fixed="left">
            <template #default="{ row }">
              <div class="tp-course">
                <span class="tp-course__dot" :style="{ background: courseColor(row.courseName) }"></span>
                <div class="tp-course__main">
                  <router-link :to="`/course/${row.courseId}/grades`" class="tp-course__name">{{ row.courseName }}</router-link>
                  <span class="tp-sub">
                    {{ row.type || '—' }} · {{ $t('courses.credits', { n: row.credit ?? 0 }) }}<template v-if="row.teacherName"> · {{ row.teacherName }}</template>
                  </span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column v-for="col in PARTS" :key="col.prop" :label="$t(col.label)" width="76" align="center">
            <template #default="{ row }">
              <span class="num tp-part">{{ row.published ? fmt(row[col.prop]) : '' }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('transcript.total')" width="80" align="center">
            <template #default="{ row }">
              <b v-if="row.published" class="num" :class="{ 'tp-fail': !row.passed }">{{ fmt(row.total) }}</b>
            </template>
          </el-table-column>
          <el-table-column :label="$t('transcript.gradePoint')" width="80" align="center">
            <template #default="{ row }">
              <span v-if="row.published" class="num">{{ row.gradePoint.toFixed(1) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('transcript.result')" width="100" align="center">
            <template #default="{ row }">
              <span v-if="!row.published" class="pill">{{ $t('transcript.notOut') }}</span>
              <span v-else-if="row.passed" class="pill pill--ok">{{ $t('transcript.pass') }}</span>
              <span v-else class="pill pill--bad">{{ $t('transcript.fail') }}</span>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <p class="tp-hint">{{ $t('transcript.gpaHint') }}</p>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import request from '@/utils/request'
import { courseColor } from '@/utils/courseColor'

defineOptions({ name: 'TranscriptPage' })

/** 四个分项：考勤、作业由记录折算，平时、期末由老师录入（权重在成绩册里设） */
const PARTS = [
  { prop: 'attendanceScore', label: 'transcript.attendance' },
  { prop: 'homeworkScore', label: 'transcript.homework' },
  { prop: 'ordinaryScore', label: 'transcript.ordinary' },
  { prop: 'examScore', label: 'transcript.exam' },
]

const data = ref<Record<string, any> | null>(null)
const loading = ref(false)

const summary = computed(() => data.value?.summary || {})
const rows = computed<Record<string, any>[]>(() => data.value?.rows || [])
const creditFill = computed(() => {
  const required = Number(summary.value.requiredCredits) || 0
  return required ? Math.min(100, Math.round(((Number(summary.value.credits) || 0) / required) * 100)) : 0
})

/** 分数保留到一位小数，整数不带 .0；没有这一项（权重为 0 或没录）显示 — */
const fmt = (value: number | null | undefined) => (value == null ? '—' : String(Math.round(value * 10) / 10))

onMounted(async () => {
  loading.value = true
  try {
    data.value = await request.get<Record<string, any>>('/score/transcript')
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.tp {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
}

.tp .page-head {
  margin-bottom: 0;
}

.tp-kpis {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.tp-kpi {
  display: grid;
  align-content: start;
  gap: 4px;
  padding: 14px 16px;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius-lg);
  background: var(--xm-bg-card);
}

.tp-kpi__label,
.tp-kpi__sub {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.tp-kpi__value {
  font-size: 26px;
  line-height: 1.2;
  color: var(--xm-text-primary);
}

.tp-kpi__bar {
  height: 6px;
  margin-top: 4px;
  border-radius: 999px;
  background: var(--xm-bg-sunken);
  overflow: hidden;
}

.tp-kpi__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--xm-brand);
}

.tp-kpi__counts {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
}

/* 表格底色要不透明：课程列固定在左侧，横向滚动时其他列从它下面滑过 */
.tp-table {
  --el-table-bg-color: var(--xm-bg-card);
  --el-table-tr-bg-color: var(--xm-bg-card);
  --el-table-header-bg-color: var(--xm-bg-card);
}

.tp-course {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.tp-course__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.tp-course__main {
  display: grid;
  min-width: 0;
}

.tp-course__name {
  overflow: hidden;
  font-weight: 600;
  color: var(--xm-text-primary);
  white-space: nowrap;
  text-overflow: ellipsis;
}

.tp-course__name:hover {
  color: var(--xm-brand);
}

.tp-sub {
  overflow: hidden;
  font-size: 12px;
  color: var(--xm-text-secondary);
  white-space: nowrap;
  text-overflow: ellipsis;
}

.tp-part {
  color: var(--xm-text-regular);
}

.tp-fail {
  color: var(--xm-bad);
}

.tp-hint {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

@media (max-width: 760px) {
  .tp-kpis {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>

<template>
  <div class="screen-viewport">
    <div class="screen" :style="screenStyle">
      <!-- 顶栏：是谁的数据、第几教学周、投屏开关、时间 -->
      <header class="hd">
        <div class="hd__side">
          <div v-if="isTeacher" class="hd__avatar">{{ initial }}</div>
          <div v-else class="hd__mark">
            <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M2 7l10-4 10 4-10 4z" />
              <path d="M6 9v5c0 1.7 2.7 3 6 3s6-1.3 6-3V9" />
              <path d="M22 7v6" />
            </svg>
          </div>
          <div class="hd__titles">
            <h1 class="hd__title">{{ isTeacher ? $t('pages.dashboard.teacherTitle') : $t('pages.dashboard.adminTitle') }}</h1>
            <p class="hd__sub">
              <span v-if="skeleton && isTeacher" class="sk-region"><SkBlock :w="320" :h="16" /></span>
              <template v-else>{{ subtitle }}</template>
            </p>
          </div>
        </div>
        <div class="hd__term">
          <div class="hd__term-line">
            <span v-if="skeleton" class="sk-region"><SkBlock :w="300" :h="20" /></span>
            <template v-else>
              <span>{{ semester.name }}</span>
              <b class="num">{{ weekText }}</b>
              <span v-if="semester.weeks" class="muted">{{ $t('pages.dashboard.weeks', { n: semester.weeks }) }}</span>
            </template>
          </div>
          <div class="hd__progress"><span :style="{ width: termProgress + '%' }"></span></div>
        </div>
        <div class="hd__side hd__side--end">
          <button type="button" class="btn" :class="{ 'btn--on': projection }" :aria-pressed="projection" @click="toggleProjection">
            {{ projection ? $t('pages.dashboard.projectionOn') : $t('pages.dashboard.projectionOff') }}
          </button>
          <div class="hd__clock">
            <span class="num">{{ clock.hhmm }}</span>
            <span>{{ dateLine }} · {{ $t('pages.dashboard.refresh') }}</span>
          </div>
          <button type="button" class="btn" @click="goBack">{{ $t('pages.dashboard.exit') }}</button>
        </div>
      </header>

      <p v-if="loadError" class="load-error" role="status">{{ $t('pages.dashboard.loadFailed') }}</p>

      <!-- 骨架：第一次取数据时，面板标题照常显示，指标、表格、图表、名单位置画占位（不先露出「暂无」） -->
      <template v-if="skeleton">
        <section class="kpis sk-region" aria-busy="true">
          <span class="sr-only">{{ $t('common.loading') }}</span>
          <div v-for="i in 5" :key="i" class="kpi" aria-hidden="true">
            <SkBlock :w="110" :h="18" />
            <SkBlock :w="i % 2 ? 120 : 96" :h="44" />
            <SkBlock :w="170" :h="16" />
          </div>
        </section>
        <main class="grid sk-region" aria-hidden="true">
          <div class="col">
            <section class="panel">
              <div class="panel__head">
                <h2>{{ isTeacher ? $t('pages.dashboard.today.teacherTitle') : $t('pages.dashboard.today.title') }}</h2>
                <SkBlock :w="220" :h="16" />
              </div>
              <SkeletonList :rows="isTeacher ? 3 : 5" :lead="isTeacher ? 'date' : 'none'" :lines="isTeacher ? 2 : 1" trail="text" />
            </section>
            <section class="panel panel--grow">
              <div class="panel__head">
                <h2>{{ isTeacher ? $t('pages.dashboard.week.teacherTitle') : $t('pages.dashboard.week.title') }}</h2>
              </div>
              <SkBlock class="dash-sk__fill" w="100%" h="auto" />
            </section>
          </div>
          <div class="col">
            <section class="panel">
              <div class="panel__head">
                <h2>{{ isTeacher ? $t('pages.dashboard.trend.teacherTitle') : $t('pages.dashboard.trend.title') }}</h2>
              </div>
              <SkBlock w="100%" :h="300" />
            </section>
            <section class="panel panel--grow">
              <div class="panel__head">
                <h2>{{ isTeacher ? $t('pages.dashboard.myCourses.title') : $t('pages.dashboard.ranking.title') }}</h2>
              </div>
              <SkeletonList :rows="5" :lines="isTeacher ? 2 : 1" trail="text" />
            </section>
          </div>
          <div class="col">
            <section class="panel">
              <div class="panel__head">
                <h2>{{ isTeacher ? $t('pages.dashboard.warnings.teacherTitle') : $t('pages.dashboard.warnings.title') }}</h2>
              </div>
              <SkBlock w="100%" :h="14" shape="pill" />
              <SkeletonList class="dash-sk__gap" :rows="3" :lines="1" trail="text" />
            </section>
            <section class="panel panel--grow">
              <div class="panel__head">
                <h2 v-if="isTeacher">{{ $t('pages.dashboard.evaluation.title') }}</h2>
                <SkBlock v-else :w="200" :h="24" />
              </div>
              <SkeletonList :rows="isTeacher ? 5 : 3" :lead="isTeacher ? 'none' : 'date'" :lines="isTeacher ? 1 : 2" :trail="isTeacher ? 'text' : 'none'" />
            </section>
          </div>
        </main>
        <footer class="ft sk-region" aria-hidden="true">
          <div class="ft__strip">
            <SkBlock :w="96" :h="28" shape="pill" />
            <SkBlock :w="420" :h="18" />
          </div>
          <div class="ft__todos">
            <SkBlock v-for="i in 3" :key="i" :w="130" :h="30" shape="pill" />
          </div>
        </footer>
      </template>

      <!-- 指标：都用比例或进度，人多人少都看得出好坏 -->
      <section v-if="!skeleton" class="kpis">
        <div v-for="k in kpis" :key="k.key" class="kpi" :class="{ 'kpi--warn': k.warn }">
          <span class="kpi__label">{{ k.label }}</span>
          <span class="kpi__value">
            <b class="num" :class="{ accent: k.accent }">{{ k.value }}</b>
            <small v-if="k.unit">{{ k.unit }}</small>
            <em v-if="k.delta" class="num" :class="{ accent: k.accent }">{{ k.delta }}</em>
          </span>
          <span class="kpi__sub">{{ k.sub }}</span>
        </div>
      </section>

      <main v-if="!skeleton" class="grid">
        <!-- 左：今天与本周 -->
        <div class="col">
          <section v-if="!isTeacher" class="panel">
            <div class="panel__head">
              <h2>{{ $t('pages.dashboard.today.title') }}</h2>
              <span>{{ $t('pages.dashboard.today.summary', { weekday: weekdayText, courses: today.courses ?? 0, students: today.students ?? 0, used: today.roomsUsed ?? 0, total: today.roomsTotal ?? 0 }) }}</span>
            </div>
            <div class="periods">
              <div class="periods__row periods__row--head">
                <span>{{ $t('pages.dashboard.today.colPeriod') }}</span>
                <span>{{ $t('pages.dashboard.today.colCourses') }}</span>
                <span>{{ $t('pages.dashboard.today.colStudents') }}</span>
                <span>{{ $t('pages.dashboard.today.colRooms') }}</span>
                <span class="end">{{ $t('pages.dashboard.today.colState') }}</span>
              </div>
              <div
                v-for="p in today.periods || []"
                :key="p.index"
                class="periods__row"
                :class="{ 'is-now': p.state === 'ongoing' && p.courses > 0, 'is-empty': !p.courses }"
              >
                <div>
                  <div class="periods__name">{{ periodLabel(p.index) }}</div>
                  <div class="num periods__time">{{ p.start }}–{{ p.end }}</div>
                </div>
                <span class="num periods__value">{{ p.courses }}<small v-if="p.courses">{{ $t('pages.dashboard.today.unitCourses') }}</small></span>
                <span class="num periods__value">{{ p.students }}<small v-if="p.courses">{{ $t('pages.dashboard.today.unitPeople') }}</small></span>
                <span class="num periods__value">{{ p.rooms }}<small v-if="p.courses">{{ $t('pages.dashboard.today.unitRooms') }}</small></span>
                <span class="end"><span :class="{ pill: p.state === 'ongoing' && p.courses > 0 }">{{ periodStateText(p) }}</span></span>
              </div>
            </div>
            <div class="notice-box" :class="{ 'notice-box--warn': today.attentionCount || today.conflicts }">
              <template v-if="today.attentionCount">
                <span class="notice-box__title">{{ $t('pages.dashboard.today.attention', { n: today.attentionCount }) }}</span>
                <span v-for="item in today.attention" :key="item.courseId">
                  {{ $t('pages.dashboard.today.attentionItem', { course: item.courseName, room: item.room || '—', teacher: item.teacherName || '—', minutes: item.minutes }) }}
                </span>
                <span v-if="today.attentionCount > today.attention.length" class="muted">
                  {{ $t('pages.dashboard.today.attentionMore', { n: today.attentionCount - today.attention.length }) }}
                </span>
              </template>
              <span v-else>{{ $t('pages.dashboard.today.allGood') }}</span>
              <span class="muted">{{ today.conflicts ? $t('pages.dashboard.today.conflicts', { n: today.conflicts }) : $t('pages.dashboard.today.noConflicts') }}</span>
            </div>
          </section>

          <section v-else class="panel">
            <div class="panel__head">
              <h2>{{ $t('pages.dashboard.today.teacherTitle') }}</h2>
              <span>{{ $t('pages.dashboard.today.teacherSummary', { weekday: weekdayText, courses: today.courses ?? 0, students: today.students ?? 0 }) }}</span>
            </div>
            <div v-if="(today.list || []).length" class="classes">
              <div v-for="c in today.list" :key="c.id" class="class-card" :class="{ 'is-now': c.state === 'ongoing' }">
                <div class="class-card__time">
                  <span>{{ periodLabel(c.period) }}</span>
                  <span class="num">{{ c.start }}–{{ c.end }}</span>
                </div>
                <div class="class-card__body">
                  <div class="class-card__top">
                    <b>{{ c.name }}</b>
                    <span class="pill" :class="{ 'pill--ghost': c.state !== 'ongoing' }">{{ classBadge(c) }}</span>
                  </div>
                  <span class="muted">
                    {{ $t('pages.dashboard.today.classMeta', { room: c.room || '—', n: c.students }) }}
                    <template v-if="c.state === 'ongoing'">
                      · {{ c.checkedIn ? $t('pages.dashboard.today.checkedIn') : $t('pages.dashboard.today.noCheckIn') }}
                    </template>
                  </span>
                  <div v-if="c.state === 'ongoing'" class="track"><span :style="{ width: classProgress(c) + '%' }"></span></div>
                </div>
              </div>
            </div>
            <p v-else class="empty">{{ $t('pages.dashboard.today.noClasses') }}</p>
          </section>

          <section class="panel panel--grow">
            <div class="panel__head">
              <h2>{{ isTeacher ? $t('pages.dashboard.week.teacherTitle') : $t('pages.dashboard.week.title') }}</h2>
              <span v-if="isTeacher">{{ $t('pages.dashboard.week.total', { n: weekTotal }) }}</span>
              <span v-else class="legend">
                {{ $t('pages.dashboard.week.legend') }}
                <i class="heat-0"></i>0 <i class="heat-1"></i>1 <i class="heat-2"></i>2–4 <i class="heat-3"></i>5+
              </span>
            </div>
            <div class="week" :style="{ gridTemplateColumns: `72px repeat(${weekDays.length}, minmax(0, 1fr))` }">
              <span></span>
              <span v-for="d in weekDays" :key="'h' + d.index" class="week__day" :class="{ 'is-today': d.index === todayIndex }">
                {{ dayLabel(d.index) }}<template v-if="d.index === todayIndex"> · {{ $t('pages.dashboard.week.today') }}</template>
                <span v-if="!isTeacher" class="num week__count">{{ d.total }}</span>
              </span>
              <template v-for="period in 5" :key="'p' + period">
                <span class="week__period">{{ periodLabel(period) }}</span>
                <button
                  v-for="d in weekDays"
                  :key="d.index + '-' + period"
                  type="button"
                  class="week__cell"
                  :class="[`heat-${heatLevel(d.cells[period - 1].count)}`, { 'is-active': isActiveCell(d.index, period) }]"
                  :aria-label="cellLabel(d.index, period)"
                  @click="selectCell(d.index, period)"
                >
                  <template v-if="d.cells[period - 1].count">
                    {{ isTeacher ? d.cells[period - 1].courses.map((c: any) => c.name).join(' / ') : d.cells[period - 1].count }}
                  </template>
                </button>
              </template>
            </div>
            <p class="foot">{{ cellDetail }}</p>
          </section>
        </div>

        <!-- 中：出勤 -->
        <div class="col">
          <section class="panel">
            <div class="panel__head">
              <h2>{{ isTeacher ? $t('pages.dashboard.trend.teacherTitle') : $t('pages.dashboard.trend.title') }}</h2>
              <span>{{ $t('pages.dashboard.trend.hint') }}</span>
            </div>
            <div v-show="weekly.length" ref="trendEl" class="trend" role="img" :aria-label="trendLabel"></div>
            <p v-if="!weekly.length" class="empty empty--tall">{{ $t('pages.dashboard.trend.empty') }}</p>
          </section>

          <section v-if="!isTeacher" class="panel panel--grow">
            <div class="panel__head">
              <h2>{{ $t('pages.dashboard.ranking.title') }}</h2>
              <span>{{ $t('pages.dashboard.ranking.hint', { n: ranking.limit ?? 0 }) }}</span>
            </div>
            <div class="chips">
              <span class="chip">{{ $t('pages.dashboard.ranking.good') }} <b class="num">{{ ranking.bands?.good ?? 0 }}</b> {{ $t('pages.dashboard.ranking.unit') }}</span>
              <span class="chip">{{ $t('pages.dashboard.ranking.fair') }} <b class="num">{{ ranking.bands?.fair ?? 0 }}</b> {{ $t('pages.dashboard.ranking.unit') }}</span>
              <span class="chip" :class="{ 'chip--warn': ranking.bands?.low }">{{ $t('pages.dashboard.ranking.low') }} <b class="num">{{ ranking.bands?.low ?? 0 }}</b> {{ $t('pages.dashboard.ranking.unit') }}</span>
            </div>
            <div v-if="(ranking.lowest || []).length" class="bars">
              <div v-for="c in ranking.lowest" :key="c.id" class="bars__row">
                <span class="bars__name" :title="c.name">{{ c.name }}</span>
                <div class="track track--thick"><span :class="{ warn: c.rate < 90 }" :style="{ width: c.rate + '%' }"></span></div>
                <b class="num bars__value">{{ c.rate }}%</b>
                <span class="num bars__count">{{ c.present }}/{{ c.counted }}</span>
              </div>
            </div>
            <p v-else class="empty">{{ $t('pages.dashboard.ranking.empty') }}</p>
            <p v-if="ranking.smallCount" class="foot">
              {{ $t('pages.dashboard.ranking.small', { min: ranking.minStudents, names: names(ranking.smallNames, ranking.smallCount) }) }}
            </p>
            <p v-if="ranking.noRecordCount" class="foot">
              {{ $t('pages.dashboard.ranking.noRecord', { n: ranking.noRecordCount, names: names(ranking.noRecordNames, ranking.noRecordCount) }) }}
            </p>
          </section>

          <section v-else class="panel panel--grow">
            <div class="panel__head">
              <h2>{{ $t('pages.dashboard.myCourses.title') }}</h2>
              <span>{{ $t('pages.dashboard.myCourses.hint') }}</span>
            </div>
            <div v-if="myCourses.length" class="bars">
              <div v-for="c in myCourses" :key="c.id" class="course-row">
                <div class="bars__row">
                  <span class="bars__name" :title="c.name">{{ c.name }}</span>
                  <template v-if="c.rate != null">
                    <div class="track track--thick"><span :class="{ warn: c.rate < 90 }" :style="{ width: c.rate + '%' }"></span></div>
                    <b class="num bars__value">{{ c.rate }}%</b>
                    <span class="num bars__count">{{ c.present }}/{{ c.counted }}</span>
                  </template>
                  <span v-else class="muted bars__none">{{ $t('pages.dashboard.myCourses.noRecord', { n: c.students }) }}</span>
                </div>
                <span class="course-row__task" :class="c.assignment ? assignmentTone(c.assignment) : 'muted'">
                  {{ c.assignment ? assignmentText(c.assignment) : $t('pages.dashboard.myCourses.noAssignment') }}
                </span>
              </div>
            </div>
            <p v-else class="empty">{{ $t('pages.dashboard.myCourses.empty') }}</p>
          </section>
        </div>

        <!-- 右：学生与考试 / 评教 -->
        <div class="col">
          <section class="panel">
            <div class="panel__head">
              <h2>{{ isTeacher ? $t('pages.dashboard.warnings.teacherTitle') : $t('pages.dashboard.warnings.title') }}</h2>
              <span>{{ isTeacher ? $t('pages.dashboard.warnings.teacherScope') : $t('pages.dashboard.warnings.students', { n: warnings.students ?? 0 }) }}</span>
            </div>
            <div class="stack" role="img" :aria-label="stackLabel">
              <span v-for="s in warnStack" :key="s.key" :class="'stack__' + s.key" :style="{ width: s.width + '%' }"></span>
            </div>
            <div class="legend legend--levels">
              <span v-for="s in warnLegend" :key="s.key"><i :class="'stack__' + s.key"></i>{{ s.label }} <b class="num">{{ s.value }}</b></span>
            </div>
            <template v-if="!isTeacher && (warnings.byCollege || []).length">
              <p class="sub-title">{{ $t('pages.dashboard.warnings.byCollege') }}</p>
              <div v-for="c in warnings.byCollege" :key="c.name" class="kv">
                <span>{{ c.name || $t('pages.dashboard.warnings.noCollege') }}</span>
                <span class="num">{{ c.warned }} <span class="muted">/ {{ c.total }}</span></span>
              </div>
            </template>
            <template v-if="(warnings.top || []).length">
              <p class="sub-title">{{ projection ? $t('pages.dashboard.warnings.topMasked') : $t('pages.dashboard.warnings.top') }}</p>
              <div v-for="s in warnings.top" :key="s.studentId" class="risk">
                <div class="risk__line">
                  <span>{{ displayName(s.name) }}<template v-if="s.className"> · {{ s.className }}</template></span>
                  <span class="badge" :class="'badge--' + levelKey(s.level)">{{ levelText(s.level) }} <b class="num">{{ s.riskIndex }}</b></span>
                </div>
                <span v-if="isTeacher" class="muted risk__detail">
                  {{ $t('pages.dashboard.warnings.detail', { avg: s.avgScore, failed: s.failedCount, absent: s.absentRate }) }}
                </span>
              </div>
            </template>
            <p v-else class="empty">{{ $t('pages.dashboard.warnings.empty') }}</p>
            <router-link v-if="!isTeacher" to="/admin/warnings" class="link">{{ $t('pages.dashboard.warnings.viewAll') }} →</router-link>
            <p v-else class="foot">{{ $t('pages.dashboard.warnings.maskHint') }}</p>
          </section>

          <section v-if="!isTeacher" class="panel panel--grow">
            <div class="panel__head">
              <h2>{{ $t('pages.dashboard.exams.title', { days: exams.windowDays ?? 0 }) }}</h2>
              <span>{{ $t('pages.dashboard.exams.hint', { n: exams.limit ?? 0 }) }}</span>
            </div>
            <div v-for="e in exams.soon || []" :key="e.id" class="exam">
              <div class="exam__days" :class="{ warn: e.days != null && e.days <= 7 }">
                <template v-if="e.days === 0">{{ $t('pages.dashboard.exams.today') }}</template>
                <template v-else><b class="num">{{ e.days }}</b><small>{{ $t('pages.dashboard.exams.day') }}</small></template>
              </div>
              <div class="exam__info">
                <b>{{ e.name }}</b>
                <span class="num muted">{{ examDate(e.examTime) }}</span>
              </div>
            </div>
            <p v-if="!(exams.soon || []).length" class="empty">{{ $t('pages.dashboard.exams.empty', { days: exams.windowDays ?? 0 }) }}</p>
            <p v-if="exams.remaining && exams.next" class="foot">
              {{ $t('pages.dashboard.exams.later', { n: exams.remaining, name: exams.next.name, date: examDate(exams.next.examTime) }) }}
            </p>
          </section>

          <section v-else class="panel panel--grow">
            <div class="panel__head">
              <h2>{{ $t('pages.dashboard.evaluation.title') }}</h2>
              <span>{{ $t('pages.dashboard.evaluation.hint', { n: evaluation.total ?? 0 }) }}</span>
            </div>
            <template v-if="dims.length">
              <div v-for="d in dims" :key="d.key" class="bars__row bars__row--dim">
                <span class="bars__name">{{ $t('space.evaluation.dimensions.' + d.key) }}</span>
                <div class="track"><span :style="{ width: (d.value / 5) * 100 + '%' }"></span></div>
                <b class="num bars__value">{{ d.value.toFixed(2) }}</b>
              </div>
              <p class="foot">{{ evaluationFrom }}</p>
            </template>
            <p v-else class="empty">{{ $t('pages.dashboard.evaluation.empty') }}</p>
          </section>
        </div>
      </main>

      <!-- 底栏：全校通知（教师为近期考试）+ 待办 -->
      <footer v-if="!skeleton" class="ft">
        <div class="ft__strip">
          <template v-if="!isTeacher">
            <span class="ft__badge">{{ $t('pages.dashboard.notices.title') }}</span>
            <template v-if="notices.length">
              <span v-for="(n, i) in notices" :key="n.id" class="ft__item" :class="{ muted: i > 0 }">
                {{ n.title }} <span class="num muted">{{ String(n.time || '').slice(5, 10) }}</span>
              </span>
            </template>
            <span v-else class="muted">{{ $t('pages.dashboard.notices.empty') }}</span>
          </template>
          <template v-else>
            <span class="ft__badge">{{ $t('pages.dashboard.exams.teacherTitle', { days: exams.windowDays ?? 0 }) }}</span>
            <template v-if="(exams.soon || []).length">
              <span v-for="(e, i) in exams.soon" :key="e.id" class="ft__item" :class="{ muted: i > 0 }">
                {{ e.name }}
                <span class="num" :class="e.days != null && e.days <= 7 ? 'warn' : 'muted'">
                  {{ e.days === 0 ? $t('pages.dashboard.exams.today') : $t('pages.dashboard.exams.inDays', { n: e.days }) }}
                </span>
              </span>
            </template>
            <span v-else class="muted">{{ $t('pages.dashboard.exams.empty', { days: exams.windowDays ?? 0 }) }}</span>
          </template>
        </div>
        <div class="ft__todos">
          <span class="muted">{{ isTeacher ? $t('pages.dashboard.todos.teacherTitle') : $t('pages.dashboard.todos.title') }}</span>
          <span v-for="todo in todos" :key="todo.key" class="chip" :class="{ 'chip--warn': todo.warn }">{{ todo.text }}</span>
          <span v-if="!todos.length" class="chip">{{ $t('pages.dashboard.todos.none') }}</span>
        </div>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { SVGRenderer } from 'echarts/renderers'
import request from '@/utils/request'
import { t } from '@/i18n'
import { useUser } from '@/components/useUser'
import SkBlock from '@/components/skeleton/SkBlock.vue'
import SkeletonList from '@/components/skeleton/SkeletonList.vue'
import { currentLocale } from '@/composables/useLocale'
import { syncServerClock, useServerClock } from '@/composables/useServerClock'
import { useSkeleton } from '@/composables/useSkeleton'
import { addDays, formatDay, toMinutes, weekdayIndex } from '@/utils/schedule'
import { deadlineState, heatLevel, latestWeek, maskName, nameList } from '@/utils/dashboard'

// SVG 渲染：大屏整体按比例缩放，矢量图在任何缩放下都清晰
echarts.use([LineChart, GridComponent, TooltipComponent, SVGRenderer])

/** 1920 × 1080 设计稿等比缩放；每分钟刷新一次（跨天、新数据都靠它） */
const DESIGN_W = 1920
const DESIGN_H = 1080
const REFRESH_MS = 60 * 1000
const PROJECTION_KEY = 'xm-dashboard-projection'
const ACCENT = '#3fb08f'
const WARN = '#e4ad52'

const router = useRouter()
const clock = useServerClock()
const { user } = useUser()
const data = ref<Record<string, any> | null>(null)
const loadError = ref(false)
// 第一次取数据时出骨架；之后每分钟刷新保留上一次的数据，不再出骨架
const skeleton = useSkeleton(() => !data.value && !loadError.value)

// ========== 投屏模式：投到公共屏幕时隐藏学生姓名，选择记在本机 ==========
const readProjection = () => {
  try {
    return localStorage.getItem(PROJECTION_KEY) === '1'
  } catch {
    return false
  }
}
const projection = ref(readProjection())
const toggleProjection = () => {
  projection.value = !projection.value
  try {
    localStorage.setItem(PROJECTION_KEY, projection.value ? '1' : '0')
  } catch {
    // 本机存储不可用时只在这次打开里生效
  }
}
const displayName = (name: string) => (projection.value ? maskName(name) : name)

// ========== 缩放 ==========
const scale = ref(1)
const screenStyle = computed(() => ({ transform: `translate(-50%, -50%) scale(${scale.value})` }))
const fit = () => {
  scale.value = Math.min(window.innerWidth / DESIGN_W, window.innerHeight / DESIGN_H)
}

const goBack = () => {
  window.history.length > 1 ? router.back() : router.push('/home')
}

// ========== 公共 ==========
// 数据回来之前按登录身份判断，骨架就是教师版或管理员版，不会先闪一下管理员版
const isTeacher = computed(() => (data.value?.role ?? user.value.role) === 'TEACHER')
const semester = computed<Record<string, any>>(() => data.value?.semester || {})
const locale = computed(() => currentLocale())
const separator = computed(() => (locale.value === 'zh-CN' ? '、' : ', '))
const nowText = computed(() => `${clock.value.date} ${clock.value.hhmm}`)
const periodLabel = (index: number) => t(`pages.dashboard.period${index}`)
const names = (list: string[], total: number) =>
  nameList(list, total, separator.value, (n) => t('pages.dashboard.ranking.etc', { n }))

// ========== 顶栏 ==========
const subtitle = computed(() => {
  if (!isTeacher.value) return t('pages.dashboard.adminSubtitle')
  const teacher = data.value?.teacher || {}
  return t('pages.dashboard.teacherSubtitle', {
    name: teacher.name || '',
    title: teacher.title || '',
    courses: teacher.courseCount ?? 0,
    students: teacher.studentCount ?? 0,
  })
})
const initial = computed(() => String(data.value?.teacher?.name || '').slice(0, 1))
const weekText = computed(() => {
  const { currentWeek, weeks } = semester.value
  if (currentWeek == null) return ''
  if (currentWeek <= 0) return t('pages.dashboard.beforeTerm')
  if (currentWeek > (weeks || 0)) return t('pages.dashboard.afterTerm')
  return t('pages.dashboard.weekNo', { n: currentWeek })
})
const termProgress = computed(() => {
  const { currentWeek, weeks } = semester.value
  if (!weeks || currentWeek == null) return 0
  return Math.max(0, Math.min(100, Math.round((currentWeek / weeks) * 100)))
})
const weekdayText = computed(() => formatDay(clock.value.date, locale.value, { weekday: 'long' }))
const dateLine = computed(
  () => `${formatDay(clock.value.date, locale.value, { month: 'long', day: 'numeric' })} ${weekdayText.value}`,
)

// ========== 指标 ==========
interface Kpi {
  key: string
  label: string
  value: string
  unit?: string
  delta?: string
  sub: string
  warn?: boolean
  accent?: boolean
}

const kpis = computed<Kpi[]>(() => {
  const d = data.value
  if (!d) return []
  const list: Kpi[] = []

  const latest = latestWeek(d.attendance?.weekly)
  let weekSub = t('pages.dashboard.kpi.noAttendance')
  if (latest) {
    if (latest.delta === null) weekSub = t('pages.dashboard.kpi.weekRateOnly', { week: latest.week })
    else if (latest.delta > 0) weekSub = t('pages.dashboard.kpi.weekRateUp', { week: latest.week, n: latest.delta })
    else if (latest.delta < 0) weekSub = t('pages.dashboard.kpi.weekRateDown', { week: latest.week, n: -latest.delta })
    else weekSub = t('pages.dashboard.kpi.weekRateFlat', { week: latest.week })
  }
  list.push({
    key: 'week',
    label: t('pages.dashboard.kpi.weekRate'),
    value: latest ? `${latest.rate}%` : '—',
    delta: latest?.delta ? `${latest.delta > 0 ? '↑' : '↓'} ${Math.abs(latest.delta)}` : '',
    sub: weekSub,
    accent: true,
  })

  const a = d.assignments || {}
  list.push({
    key: 'submit',
    label: t('pages.dashboard.kpi.submitRate'),
    value: a.due > 0 ? `${Math.round((a.submitted / a.due) * 100)}%` : '—',
    sub: a.due > 0 ? t('pages.dashboard.kpi.submitRateSub', { due: a.due, submitted: a.submitted }) : t('pages.dashboard.kpi.noDue'),
  })
  if (isTeacher.value) {
    list.push({
      key: 'ungraded',
      label: t('pages.dashboard.kpi.myUngraded'),
      value: String(a.ungraded ?? 0),
      unit: t('pages.dashboard.kpi.papers'),
      sub: t('pages.dashboard.kpi.myUngradedSub', { n: a.overdue ?? 0, days: a.overdueDays ?? 0 }),
      warn: a.ungraded > 0,
    })
  } else {
    list.push({
      key: 'overdue',
      label: t('pages.dashboard.kpi.overdue', { days: a.overdueDays ?? 0 }),
      value: String(a.overdue ?? 0),
      unit: t('pages.dashboard.kpi.papers'),
      sub: t('pages.dashboard.kpi.overdueSub', { teachers: a.overdueTeachers ?? 0, total: a.ungraded ?? 0 }),
      warn: a.overdue > 0,
    })
  }

  const e = d.evaluation || {}
  list.push({
    key: 'evaluation',
    label: isTeacher.value ? t('pages.dashboard.kpi.myEvaluation') : t('pages.dashboard.kpi.evaluation'),
    value: e.average != null ? Number(e.average).toFixed(2) : '—',
    unit: e.average != null ? '/ 5' : '',
    sub: e.total ? t('pages.dashboard.kpi.evaluationSub', { total: e.total, courses: e.courses }) : t('pages.dashboard.kpi.noEvaluation'),
  })

  const w = d.warnings || {}
  const warned = (w.high || 0) + (w.middle || 0) + (w.low || 0)
  list.push({
    key: 'warnings',
    label: isTeacher.value ? t('pages.dashboard.kpi.myWarnings') : t('pages.dashboard.kpi.warnings'),
    value: String(warned),
    unit: t('pages.dashboard.kpi.people'),
    sub: t('pages.dashboard.kpi.warningsSub', { high: w.high || 0, middle: w.middle || 0, low: w.low || 0 }),
    warn: warned > 0,
  })
  return list
})

// ========== 今天 ==========
const today = computed<Record<string, any>>(() => data.value?.today || {})

const periodStateText = (p: Record<string, any>) => {
  if (!p.courses) return p.state === 'finished' ? t('pages.dashboard.today.finished') : t('pages.dashboard.today.none')
  if (p.state === 'ongoing') return t('pages.dashboard.today.ongoing')
  if (p.state === 'finished') return t('pages.dashboard.today.finished')
  return t('pages.dashboard.today.startsAt', { time: p.start })
}

const minutesLeft = (end: string) => Math.max(0, toMinutes(end) - toMinutes(clock.value.hhmm))
const classBadge = (c: Record<string, any>) => {
  if (c.state === 'ongoing') return t('pages.dashboard.today.remaining', { n: minutesLeft(c.end) })
  if (c.state === 'finished') return t('pages.dashboard.today.classDone')
  return t('pages.dashboard.today.startsAt', { time: c.start })
}
const classProgress = (c: Record<string, any>) => {
  const total = toMinutes(c.end) - toMinutes(c.start)
  if (total <= 0) return 0
  return Math.max(0, Math.min(100, Math.round(((total - minutesLeft(c.end)) / total) * 100)))
}

// ========== 本周 ==========
const weekDays = computed<any[]>(() => (data.value?.week || []).filter((d: any) => d.index <= 5 || d.total > 0))
const weekTotal = computed(() => (data.value?.week || []).reduce((sum: number, d: any) => sum + (d.total || 0), 0))
const todayIndex = computed(() => weekdayIndex(semester.value.weekday) + 1)
// 2024-01-01 是星期一：借它按界面语言格式化「周一 / Mon」
const dayLabel = (index: number) => formatDay(addDays('2024-01-01', index - 1), locale.value, { weekday: 'short' })

const selected = ref<{ day: number; period: number } | null>(null)
const defaultCell = computed(() => {
  const ongoing = (today.value.periods || []).find((p: any) => p.state === 'ongoing' && p.courses > 0)
  return ongoing ? { day: todayIndex.value, period: ongoing.index } : null
})
const activeCell = computed(() => selected.value || defaultCell.value)
const isActiveCell = (day: number, period: number) => activeCell.value?.day === day && activeCell.value?.period === period
const selectCell = (day: number, period: number) => {
  selected.value = isActiveCell(day, period) && selected.value ? null : { day, period }
}
const cellOf = (day: number, period: number) =>
  (data.value?.week || []).find((d: any) => d.index === day)?.cells?.[period - 1]
const cellLabel = (day: number, period: number) =>
  `${dayLabel(day)} ${periodLabel(period)} · ${cellOf(day, period)?.count ?? 0}`
const cellDetail = computed(() => {
  const at = activeCell.value
  if (!at) return t('pages.dashboard.week.hint')
  const cell = cellOf(at.day, at.period)
  const params = { day: dayLabel(at.day), period: periodLabel(at.period) }
  if (!cell?.count) return t('pages.dashboard.week.detailEmpty', params)
  const list = cell.courses.map((c: any) => (isTeacher.value ? `${c.name} · ${c.room || '—'}` : c.name))
  return t('pages.dashboard.week.detail', {
    ...params,
    n: cell.count,
    list: nameList(list.slice(0, 3), cell.count, separator.value, (n) => t('pages.dashboard.week.more', { n })),
  })
})

// ========== 出勤 ==========
const weekly = computed<any[]>(() => data.value?.attendance?.weekly || [])
const ranking = computed<Record<string, any>>(() => data.value?.attendance?.ranking || {})
const myCourses = computed<any[]>(() => data.value?.attendance?.courses || [])
const trendLabel = computed(() =>
  weekly.value.map((p) => `${t('pages.dashboard.trend.weekLabel', { n: p.week })} ${p.rate}%`).join(separator.value),
)

const trendEl = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

const renderTrend = () => {
  if (!trendEl.value || !weekly.value.length) return
  if (!chart) chart = echarts.init(trendEl.value)
  const points = weekly.value
  const rates = points.map((p) => p.rate as number)
  const min = Math.min(...rates)
  const lowest = points.length > 1 ? rates.indexOf(min) : -1
  // 纵轴按整刻度从最低点下面一点开始：出勤率多在 90% 以上，从 0 画起就看不出起伏
  const interval = min >= 75 ? 5 : min >= 50 ? 10 : 20
  const yMin = Math.max(0, Math.floor((min - interval / 2) / interval) * interval)
  chart.setOption(
    {
      grid: { left: 56, right: 28, top: 40, bottom: 36 },
      tooltip: {
        trigger: 'axis',
        backgroundColor: '#121a17',
        borderColor: '#2f3d37',
        textStyle: { color: '#e6ece9' },
        formatter: (params: any) => {
          const p = points[params[0].dataIndex]
          return `${t('pages.dashboard.trend.weekLabel', { n: p.week })}<br/>${p.rate}%（${p.present}/${p.counted}）`
        },
      },
      xAxis: {
        type: 'category',
        data: points.map((p) => t('pages.dashboard.trend.weekLabel', { n: p.week })),
        axisLine: { lineStyle: { color: '#2f3d37' } },
        axisTick: { show: false },
        axisLabel: { color: '#8d9a94', fontSize: 14 },
      },
      yAxis: {
        type: 'value',
        min: yMin,
        max: 100,
        interval,
        axisLabel: { color: '#8d9a94', fontSize: 13, formatter: '{value}%' },
        splitLine: { lineStyle: { color: '#22302b' } },
      },
      series: [
        {
          type: 'line',
          data: points.map((p, i) =>
            i === lowest
              ? {
                  value: p.rate,
                  itemStyle: { borderColor: WARN },
                  label: { position: 'bottom', color: WARN, formatter: `{c}%\n${t('pages.dashboard.trend.lowest')}` },
                }
              : p.rate,
          ),
          symbol: 'circle',
          symbolSize: 11,
          lineStyle: { color: ACCENT, width: 3 },
          itemStyle: { color: '#0b110f', borderColor: ACCENT, borderWidth: 3 },
          label: { show: true, position: 'top', color: '#e6ece9', fontSize: 15, fontWeight: 600, formatter: '{c}%' },
          areaStyle: { color: 'rgba(63, 176, 143, 0.12)' },
        },
      ],
    },
    true,
  )
}

const assignmentText = (a: Record<string, any>) => {
  const state = deadlineState(a.deadline, nowText.value)
  const key = state.kind === 'closed' ? 'closed' : state.kind === 'today' ? 'dueToday' : 'open'
  return t(`pages.dashboard.myCourses.${key}`, { title: a.title, submitted: a.submitted, students: a.students, days: state.days })
}
/** 已截止还没交齐标红；两天内截止标黄 */
const assignmentTone = (a: Record<string, any>) => {
  const state = deadlineState(a.deadline, nowText.value)
  if (state.kind === 'closed') return a.submitted < a.students ? 'bad' : 'muted'
  return state.kind === 'today' || state.days <= 2 ? 'warn' : 'muted'
}

// ========== 预警 ==========
const warnings = computed<Record<string, any>>(() => data.value?.warnings || {})
const LEVELS: Record<string, string> = { 高风险: 'high', 中风险: 'middle', 低风险: 'low' }
const levelKey = (level: string) => LEVELS[level] || 'low'
const levelText = (level: string) => t(`pages.dashboard.warnings.${levelKey(level)}`)
const warnLegend = computed(() => {
  const w = warnings.value
  return ['high', 'middle', 'low', 'normal'].map((key) => ({
    key,
    label: t(`pages.dashboard.warnings.${key}`),
    value: w[key] ?? 0,
  }))
})
const warnStack = computed(() => {
  const total = warnings.value.students || 0
  return warnLegend.value
    .filter((s) => s.key !== 'normal')
    .map((s) => ({ key: s.key, width: total > 0 ? (s.value / total) * 100 : 0 }))
})
const stackLabel = computed(() => warnLegend.value.map((s) => `${s.label} ${s.value}`).join(separator.value))

// ========== 考试、评教、通知、待办 ==========
const exams = computed<Record<string, any>>(() => data.value?.exams || {})
const examDate = (time: string) => {
  const text = String(time || '')
  return `${formatDay(text.slice(0, 10), locale.value, { month: 'long', day: 'numeric' })} ${text.slice(11, 16)}`.trim()
}

const evaluation = computed<Record<string, any>>(() => data.value?.evaluation || {})
const dims = computed(() =>
  Object.entries(evaluation.value.dimensions || {})
    .filter(([, value]) => value != null)
    .map(([key, value]) => ({ key, value: Number(value) }))
    .sort((a, b) => b.value - a.value),
)
const evaluationFrom = computed(() =>
  t('pages.dashboard.evaluation.from', {
    list: (evaluation.value.byCourse || [])
      .map((c: any) => t('pages.dashboard.evaluation.item', { name: c.name, n: c.total }))
      .join(separator.value),
  }),
)

const notices = computed<any[]>(() => data.value?.notices || [])

const todos = computed(() => {
  const d = data.value?.todos || {}
  const list: Array<{ key: string; text: string; warn: boolean }> = []
  if (d.pendingApply > 0) list.push({ key: 'apply', text: t('pages.dashboard.todos.pendingApply', { n: d.pendingApply }), warn: true })
  if (d.ungraded > 0) list.push({ key: 'ungraded', text: t('pages.dashboard.todos.ungraded', { n: d.ungraded }), warn: true })
  if (d.draftGradebooks > 0) list.push({ key: 'draft', text: t('pages.dashboard.todos.draftGradebooks', { n: d.draftGradebooks }), warn: false })
  if (d.unscheduled > 0) list.push({ key: 'unscheduled', text: t('pages.dashboard.todos.unscheduled', { n: d.unscheduled }), warn: false })
  if (isTeacher.value) {
    const n = (today.value.list || []).filter((c: any) => c.state !== 'finished' && !c.checkedIn).length
    if (n > 0) list.push({ key: 'checkIn', text: t('pages.dashboard.todos.checkIn', { n }), warn: false })
  }
  return list
})

// ========== 加载与刷新 ==========
let refreshTimer: ReturnType<typeof setInterval> | null = null

const load = async () => {
  try {
    const result = await request.get<Record<string, any>>('/dashboard/stats')
    data.value = result || {}
    syncServerClock(result?.now)
    loadError.value = false
  } catch {
    // 错误提示已由 axios 拦截器统一处理；保留上一次的数据，下次定时刷新时自动重试
    loadError.value = true
  }
}

watch([weekly, locale], () => nextTick(renderTrend))
// 数据到了但骨架还在停留时图表容器不在页面上，骨架撤掉后补画一次
watch(skeleton, (show) => {
  if (!show) nextTick(renderTrend)
})

onMounted(() => {
  fit()
  window.addEventListener('resize', fit)
  load()
  refreshTimer = setInterval(load, REFRESH_MS)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', fit)
  if (refreshTimer) clearInterval(refreshTimer)
  chart?.dispose()
  chart = null
})
</script>

<style lang="scss" scoped>
/* 大屏固定深色（投到大屏幕上更清楚），配色取系统深色模式的玉绿 */
.screen-viewport {
  width: 100vw;
  height: 100vh;
  background: #050807;
  overflow: hidden;
  position: relative;
}

.screen {
  --bg: #0b110f;
  --panel: #121a17;
  --line: #22302b;
  --line-strong: #2f3d37;
  --track: #1d2622;
  --cell: #161f1c;
  --text: #e6ece9;
  --text-2: #b4c0bb;
  --text-3: #8d9a94;
  --accent: #3fb08f;
  --accent-soft: #182a24;
  --warn: #e4ad52;
  --warn-soft: #36290f;
  --warn-line: #5a4318;
  --bad: #f26b5f;
  --bad-soft: #3b1b18;
  /* 大屏固定深色：骨架占位也用深色（不随系统浅色走） */
  --xm-sk-base: #1d2622;
  --xm-sk-shine: rgba(150, 225, 195, 0.1);

  position: absolute;
  left: 50%;
  top: 50%;
  width: 1920px;
  height: 1080px;
  transform-origin: center;
  box-sizing: border-box;
  padding: 28px 36px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  background: var(--bg);
  color: var(--text);
  font-size: 16px;
}

.num {
  font-family: var(--xm-font-num);
  font-variant-numeric: tabular-nums;
}

.muted {
  color: var(--text-3);
}

.accent {
  color: var(--accent);
}

.warn {
  color: var(--warn);
}

.bad {
  color: var(--bad);
}

h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

/* ---------- 顶栏 ---------- */
.hd {
  height: 64px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.hd__side {
  width: 600px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.hd__side--end {
  justify-content: flex-end;
  gap: 20px;
}

.hd__mark,
.hd__avatar {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--accent);
  color: var(--bg);
}

.hd__mark {
  border-radius: 12px;
}

.hd__avatar {
  border-radius: 50%;
  font-size: 22px;
  font-weight: 700;
}

.hd__titles {
  min-width: 0;
}

.hd__title {
  margin: 0;
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 2px;
}

.hd__sub {
  margin: 2px 0 0;
  font-size: 15px;
  color: var(--text-3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.hd__term {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.hd__term-line {
  display: flex;
  align-items: baseline;
  gap: 14px;
  font-size: 17px;
  color: var(--text-2);

  b {
    font-size: 30px;
    font-weight: 600;
    color: var(--text);
  }
}

.hd__progress {
  width: 420px;
  height: 6px;
  border-radius: 3px;
  background: var(--track);

  span {
    display: block;
    height: 100%;
    border-radius: 3px;
    background: var(--accent);
  }
}

.hd__clock {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  font-size: 13px;
  color: var(--text-3);

  .num {
    font-size: 36px;
    font-weight: 600;
    line-height: 1;
    color: var(--text);
  }
}

.btn {
  height: 44px;
  padding: 0 18px;
  border-radius: 10px;
  border: 1px solid var(--line-strong);
  background: transparent;
  color: var(--text-2);
  font: inherit;
  font-size: 15px;
  cursor: pointer;

  &:hover {
    border-color: var(--accent);
    color: var(--text);
  }

  &:focus-visible {
    outline: 2px solid var(--accent);
    outline-offset: 2px;
  }
}

.btn--on {
  border-color: var(--accent);
  background: var(--accent-soft);
  color: var(--accent);
}

.load-error {
  position: absolute;
  top: 98px;
  left: 50%;
  transform: translateX(-50%);
  margin: 0;
  padding: 6px 16px;
  border-radius: 8px;
  background: var(--bad-soft);
  color: var(--bad);
  font-size: 14px;
  z-index: 1;
}

/* ---------- 指标 ---------- */
.kpis {
  height: 132px;
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 20px;
}

.kpi {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 18px 24px;
  border-radius: 14px;
  border: 1px solid var(--line);
  background: var(--panel);
  min-width: 0;
}

.kpi__label {
  font-size: 16px;
  color: var(--text-2);
}

.kpi__value {
  display: flex;
  align-items: baseline;
  gap: 8px;

  b {
    font-size: 48px;
    font-weight: 600;
    line-height: 1;
  }

  small {
    font-size: 20px;
    color: var(--text-3);
  }

  em {
    font-style: normal;
    font-size: 18px;
  }
}

.kpi__sub {
  font-size: 14px;
  color: var(--text-3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kpi--warn {
  border-color: var(--warn-line);
  background: var(--warn-soft);

  .kpi__label,
  .kpi__value b,
  .kpi__value small {
    color: var(--warn);
  }

  .kpi__sub {
    color: #d9c3a0;
  }
}

/* ---------- 主体三栏 ---------- */
.grid {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 560px minmax(0, 1fr) 560px;
  gap: 20px;
}

.col {
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 18px 24px;
  border-radius: 14px;
  border: 1px solid var(--line);
  background: var(--panel);
  min-height: 0;
}

.panel--grow {
  flex: 1;
  overflow: hidden;
}

.panel__head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;

  h2 {
    flex-shrink: 0;
    white-space: nowrap;
  }

  > span {
    font-size: 14px;
    color: var(--text-3);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.empty {
  margin: 0;
  padding: 18px 0;
  font-size: 15px;
  color: var(--text-3);
  text-align: center;
}

.empty--tall {
  height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.foot {
  margin: 0;
  font-size: 13px;
  color: var(--text-3);
}

.sub-title {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--text-3);
}

.link {
  font-size: 14px;
  color: var(--accent);
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

.pill {
  font-size: 13px;
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--accent);
  color: var(--bg);
  font-weight: 600;
  white-space: nowrap;
}

.pill--ghost {
  background: transparent;
  border: 1px solid var(--line-strong);
  color: var(--text-2);
  font-weight: 400;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.chip {
  font-size: 14px;
  padding: 4px 12px;
  border-radius: 8px;
  background: var(--accent-soft);
  color: var(--text-2);
  white-space: nowrap;

  b {
    color: var(--text);
    font-weight: 600;
  }
}

.chip--warn {
  background: var(--warn-soft);
  color: var(--warn);

  b {
    color: var(--warn);
  }
}

.track {
  flex: 1;
  height: 8px;
  border-radius: 4px;
  background: var(--track);
  overflow: hidden;

  span {
    display: block;
    height: 100%;
    border-radius: 4px;
    background: var(--accent);
  }

  span.warn {
    background: var(--warn);
  }
}

.track--thick {
  height: 10px;
}

/* ---------- 今日（管理员按节次汇总） ---------- */
.periods {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.periods__row {
  display: grid;
  grid-template-columns: 104px repeat(3, minmax(0, 1fr)) 120px;
  gap: 0 10px;
  align-items: center;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid transparent;

  .end {
    text-align: right;
    font-size: 13px;
    color: var(--text-2);
  }
}

.periods__row--head {
  min-height: 0;
  font-size: 13px;
  color: var(--text-3);
}

.periods__row.is-now {
  background: var(--accent-soft);
  border-color: var(--accent);

  .periods__name {
    color: var(--accent);
    font-weight: 600;
  }
}

.periods__row.is-empty {
  color: var(--text-3);
}

.periods__name {
  font-size: 15px;
}

.periods__time {
  font-size: 12px;
  color: var(--text-3);
}

.periods__value {
  font-size: 21px;
  font-weight: 600;

  small {
    margin-left: 3px;
    font-size: 13px;
    font-weight: 400;
    color: var(--text-3);
  }
}

.is-empty .periods__value {
  font-weight: 400;
}

.notice-box {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 14px;
  border-radius: 10px;
  background: var(--accent-soft);
  font-size: 14px;
}

.notice-box--warn {
  background: var(--warn-soft);
  color: #f0dcb8;

  .muted {
    color: #b9a684;
  }
}

.notice-box__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--warn);
}

/* ---------- 今日（教师逐节） ---------- */
.classes {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.class-card {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid var(--line);
}

.class-card.is-now {
  background: var(--accent-soft);
  border-color: var(--accent);
}

.class-card__time {
  width: 104px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  font-size: 15px;
  color: var(--text-2);

  .num {
    font-size: 13px;
    color: var(--text-3);
  }
}

.class-card__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 14px;
}

.class-card__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;

  b {
    font-size: 20px;
  }
}

/* ---------- 本周热力格 ---------- */
.legend {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-3);

  i {
    width: 14px;
    height: 14px;
    border-radius: 3px;
    margin-left: 4px;
  }
}

.week {
  display: grid;
  gap: 6px;
  font-size: 13px;
}

.week__day {
  text-align: center;
  color: var(--text-3);
  border-radius: 6px;
  white-space: nowrap;
}

.week__day.is-today {
  background: var(--accent);
  color: var(--bg);
  font-weight: 700;
}

.week__count {
  margin-left: 6px;
  opacity: 0.85;
}

.week__period {
  display: flex;
  align-items: center;
  color: var(--text-3);
}

.week__cell {
  height: 26px;
  padding: 0 4px;
  border: 0;
  border-radius: 6px;
  color: var(--text);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  &:focus-visible {
    outline: 2px solid var(--accent);
    outline-offset: 1px;
  }
}

.week__cell.is-active {
  box-shadow: inset 0 0 0 2px var(--accent);
  font-weight: 700;
}

.heat-0 {
  background: var(--cell);
}

.heat-1 {
  background: #1f3a31;
}

.heat-2 {
  background: #2a6150;
}

.heat-3 {
  background: var(--accent);
  color: var(--bg);
}

/* ---------- 出勤 ---------- */
.trend {
  width: 100%;
  height: 300px;
}

/* 骨架：排课热力格那一块撑满面板，预警条下面留点距离 */
.dash-sk__fill {
  flex: 1;
  min-height: 200px;
}

.dash-sk__gap {
  margin-top: 14px;
}

.bars {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.bars__row {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.bars__row--dim {
  gap: 14px;

  .bars__name {
    width: 80px;
    font-size: 15px;
  }

  .bars__value {
    width: 44px;
    font-size: 17px;
  }
}

.bars__name {
  width: 140px;
  flex-shrink: 0;
  font-size: 16px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bars__value {
  width: 68px;
  text-align: right;
  font-size: 18px;
  font-weight: 600;
}

.bars__count {
  width: 64px;
  text-align: right;
  font-size: 13px;
  color: var(--text-3);
}

.bars__none {
  flex: 1;
  font-size: 14px;
}

.course-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.course-row__task {
  padding-left: 156px;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ---------- 预警 ---------- */
.stack {
  display: flex;
  height: 14px;
  border-radius: 7px;
  overflow: hidden;
  background: #2a3430;
}

.stack__high {
  background: var(--bad);
}

.stack__middle {
  background: var(--warn);
}

.stack__low {
  background: #8d9a94;
}

.stack__normal {
  background: #2a3430;
}

.legend--levels {
  gap: 18px;
  font-size: 14px;
  color: var(--text-2);

  span {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  i {
    width: 10px;
    height: 10px;
    border-radius: 2px;
    margin: 0;
  }
}

.kv {
  display: flex;
  justify-content: space-between;
  font-size: 15px;
}

.risk {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.risk__line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 15px;
}

.risk__detail {
  font-size: 13px;
}

.badge {
  font-size: 13px;
  padding: 2px 10px;
  border-radius: 6px;
}

.badge--high {
  background: var(--bad-soft);
  color: var(--bad);
}

.badge--middle {
  background: var(--warn-soft);
  color: var(--warn);
}

.badge--low {
  background: var(--track);
  color: var(--text-2);
}

/* ---------- 考试 ---------- */
.exam {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 10px 0;
  border-bottom: 1px solid var(--track);
}

.exam__days {
  width: 84px;
  display: flex;
  align-items: baseline;
  gap: 4px;
  font-size: 18px;
  color: var(--text-2);

  b {
    font-size: 44px;
    font-weight: 600;
    line-height: 1;
    color: var(--text);
  }

  small {
    font-size: 15px;
  }
}

.exam__days.warn,
.exam__days.warn b {
  color: var(--warn);
}

.exam__info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 14px;

  b {
    font-size: 17px;
  }
}

/* ---------- 底栏 ---------- */
.ft {
  height: 52px;
  flex-shrink: 0;
  display: flex;
  gap: 20px;
}

.ft__strip,
.ft__todos {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 20px;
  border-radius: 12px;
  border: 1px solid var(--line);
  background: var(--panel);
}

.ft__strip {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.ft__todos {
  flex-shrink: 0;
  gap: 12px;
}

.ft__badge {
  flex-shrink: 0;
  font-size: 14px;
  padding: 3px 12px;
  border-radius: 6px;
  background: var(--accent-soft);
  color: var(--accent);
}

.ft__item {
  font-size: 16px;
  white-space: nowrap;

  & + .ft__item {
    padding-left: 16px;
    border-left: 1px solid var(--line-strong);
  }
}
</style>

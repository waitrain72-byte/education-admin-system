<template>
  <div class="ev">
    <div v-if="!data && loading" class="panel panel--pad">
      <el-skeleton animated :rows="5" />
    </div>

    <!-- ==================== 学生 ==================== -->
    <template v-else-if="data && !canTeach">
      <section v-if="data.mine" class="panel">
        <div class="panel__head">
          <span class="panel__title">{{ $t('space.evaluation.mine') }}</span>
          <span class="panel__hint">{{ $t('space.evaluation.submittedAt', { time: data.mine.createTime || '—' }) }}</span>
        </div>
        <div class="panel__body">
          <ul class="dims">
            <li v-for="key in DIMENSIONS" :key="key" class="dims__row">
              <span class="dims__name">{{ $t('space.evaluation.dimensions.' + key) }}</span>
              <el-rate :model-value="data.mine[key]" disabled />
            </li>
          </ul>
          <p v-if="data.mine.comment" class="ev-comment">{{ data.mine.comment }}</p>
        </div>
      </section>

      <section v-else-if="data.open" class="panel">
        <div class="panel__head">
          <span class="panel__title">{{ $t('space.evaluation.form') }}</span>
          <span class="panel__hint">{{ $t('space.evaluation.formHint') }}</span>
        </div>
        <div class="panel__body">
          <ul class="dims">
            <li v-for="key in DIMENSIONS" :key="key" class="dims__row">
              <span :id="`dim-${key}`" class="dims__name">{{ $t('space.evaluation.dimensions.' + key) }}</span>
              <el-rate v-model="form[key]" :aria-labelledby="`dim-${key}`" />
            </li>
          </ul>
          <label class="ev-label" for="ev-comment">{{ $t('space.evaluation.commentLabel') }}</label>
          <el-input
            id="ev-comment"
            v-model="form.comment"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            :placeholder="$t('space.evaluation.commentPlaceholder')"
          />
          <div class="ev-actions">
            <el-button type="primary" :loading="submitting" @click="submit">{{ $t('space.evaluation.submit') }}</el-button>
          </div>
        </div>
      </section>

      <section v-else class="panel ev-closed">
        <div class="ev-closed__title">{{ $t('space.evaluation.notOpen') }}</div>
        <p class="ev-closed__hint">{{ $t('space.evaluation.notOpenHint') }}</p>
      </section>
    </template>

    <!-- ==================== 老师 / 管理员 ==================== -->
    <template v-else-if="data">
      <section v-if="!data.count" class="panel ev-closed">
        <div class="ev-closed__title">{{ $t('space.evaluation.empty') }}</div>
        <p v-if="!data.open" class="ev-closed__hint">{{ $t('space.evaluation.emptyHint') }}</p>
      </section>

      <template v-else>
        <div class="ev-top">
          <section class="panel ev-overall">
            <div class="ev-overall__label">{{ $t('space.evaluation.overall') }}</div>
            <div class="ev-overall__value num">{{ data.overall?.toFixed(2) }}</div>
            <el-rate :model-value="data.overall" disabled allow-half />
            <div class="ev-overall__count">{{ $t('space.evaluation.count', { n: data.count, total: data.students }) }}</div>
            <ul class="stars">
              <li v-for="star in [5, 4, 3, 2, 1]" :key="star" class="stars__row">
                <span class="stars__label num">{{ star }}★</span>
                <span class="stars__bar"><span :style="{ width: starPercent(star) + '%' }"></span></span>
                <span class="stars__count num">{{ data.stars[star - 1] }}</span>
              </li>
            </ul>
          </section>

          <section class="panel">
            <div class="panel__head">
              <span class="panel__title">{{ $t('space.evaluation.summary') }}</span>
            </div>
            <div class="panel__body">
              <ul class="bars">
                <li v-for="key in DIMENSIONS" :key="key" class="bars__row">
                  <span class="bars__name">{{ $t('space.evaluation.dimensions.' + key) }}</span>
                  <span class="bars__track"><span :style="{ width: ((data.averages[key] || 0) / 5) * 100 + '%' }"></span></span>
                  <span class="bars__value num">{{ data.averages[key]?.toFixed(2) ?? '—' }}</span>
                </li>
              </ul>
            </div>
          </section>
        </div>

        <section class="panel">
          <div class="panel__head">
            <span class="panel__title">{{ $t('space.evaluation.comments') }}</span>
            <span class="panel__hint num">{{ data.comments.length }}</span>
          </div>
          <div class="panel__body">
            <div v-if="!data.comments.length" class="empty-note">{{ $t('space.evaluation.noComments') }}</div>
            <ul v-else class="comments">
              <li v-for="c in data.comments" :key="c.id" class="comments__item">
                <div class="comments__head">
                  <el-rate :model-value="c.overall" disabled allow-half size="small" />
                  <span class="comments__time num">{{ c.createTime }}</span>
                </div>
                <p class="ev-comment">{{ c.comment }}</p>
              </li>
            </ul>
          </div>
        </section>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from '@/utils/element-plus'
import { t } from '@/i18n'

const DIMENSIONS = ['attitude', 'contentScore', 'method', 'effect', 'support'] as const
type Dimension = (typeof DIMENSIONS)[number]

const props = defineProps<{ overview: Record<string, any> }>()

const route = useRoute()
const courseId = computed(() => Number(route.params.id))
const relation = computed<string>(() => props.overview.relation || 'visitor')
const canTeach = computed(() => relation.value === 'teacher' || relation.value === 'admin')

const data = ref<Record<string, any> | null>(null)
const loading = ref(false)
const submitting = ref(false)
const form = reactive<Record<Dimension, number> & { comment: string }>({
  attitude: 0,
  contentScore: 0,
  method: 0,
  effect: 0,
  support: 0,
  comment: '',
})

const load = async () => {
  loading.value = true
  try {
    data.value = await request.get<Record<string, any>>(`/course/${courseId.value}/evaluations`)
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    loading.value = false
  }
}

watch(courseId, load, { immediate: true })

const starPercent = (star: number) => {
  const count = Number(data.value?.stars?.[star - 1]) || 0
  return data.value?.count ? Math.round((count / data.value.count) * 100) : 0
}

const submit = async () => {
  if (DIMENSIONS.some((key) => !form[key])) {
    ElMessage.warning(t('space.evaluation.incomplete'))
    return
  }
  try {
    await ElMessageBox.confirm(t('space.evaluation.submitConfirm'), t('space.evaluation.submit'), { type: 'info' })
  } catch {
    return
  }
  submitting.value = true
  try {
    await request.post(`/course/${courseId.value}/evaluations`, { ...form })
    ElMessage.success(t('space.evaluation.submitted'))
    await load()
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.ev {
  display: grid;
  /* 列宽可以缩到 0：里面的宽表格自己横向滚动，不把整页撑出屏幕 */
  grid-template-columns: minmax(0, 1fr);
  gap: 20px;
}

.dims {
  list-style: none;
  display: grid;
  gap: 10px;
  margin-bottom: 18px;
}

.dims__row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.dims__name {
  width: 96px;
  font-size: 14px;
  color: var(--xm-text-regular);
}

.ev-label {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.ev-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.ev-comment {
  font-size: 14px;
  line-height: 1.7;
  color: var(--xm-text-regular);
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.ev-closed {
  display: grid;
  justify-items: center;
  gap: 6px;
  padding: 44px 24px;
  text-align: center;
}

.ev-closed__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--xm-text-primary);
}

.ev-closed__hint {
  max-width: 420px;
  font-size: 13px;
  color: var(--xm-text-secondary);
}

/* ---------- 汇总 ---------- */
.ev-top {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 20px;
}

.ev-overall {
  display: grid;
  justify-items: center;
  gap: 6px;
  padding: 22px 20px;
}

.ev-overall__label,
.ev-overall__count {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.ev-overall__value {
  font-size: 48px;
  font-weight: 700;
  line-height: 1.1;
  color: var(--xm-text-primary);
}

.stars {
  list-style: none;
  display: grid;
  gap: 4px;
  width: 100%;
  margin-top: 10px;
}

.stars__row {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr) 24px;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.stars__bar {
  height: 6px;
  border-radius: 999px;
  background: var(--xm-bg-sunken);
  overflow: hidden;
}

.stars__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #f7ba2a;
}

.stars__count {
  text-align: right;
}

.bars {
  list-style: none;
  display: grid;
  gap: 14px;
}

.bars__row {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr) 40px;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}

.bars__name {
  color: var(--xm-text-regular);
}

.bars__track {
  height: 10px;
  border-radius: 999px;
  background: var(--xm-bg-sunken);
  overflow: hidden;
}

.bars__track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--xm-brand);
}

.bars__value {
  font-weight: 700;
  color: var(--xm-text-primary);
  text-align: right;
}

.comments {
  list-style: none;
  display: grid;
}

.comments__item {
  padding: 12px 0;
  border-bottom: 1px solid var(--xm-border);
}

.comments__item:first-child {
  padding-top: 0;
}

.comments__item:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.comments__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 4px;
}

.comments__time {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

@media (max-width: 860px) {
  .ev-top {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 480px) {
  .dims__row {
    flex-wrap: wrap;
    gap: 4px 16px;
  }

  .bars__row {
    grid-template-columns: 80px minmax(0, 1fr) 36px;
  }
}
</style>

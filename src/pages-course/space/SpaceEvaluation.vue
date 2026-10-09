<template>
  <!-- 课程空间 · 评价（与 Web 端 views/course/CourseEvaluation.vue 一致）：
       学生在结课后从五个方面打分、留言（只能评一次）；老师 / 管理员看匿名汇总 -->
  <view>
    <view
      v-if="!data && loading"
      class="xm-card"
    >
      <view class="skeleton skeleton-title" />
      <view class="skeleton skeleton-line" />
    </view>

    <!-- ==================== 学生 ==================== -->
    <template v-else-if="data && !canTeach">
      <view
        v-if="data.mine"
        class="xm-card"
      >
        <view class="xm-section-head">
          <text class="xm-section-title">{{ $t('space.evaluation.mine') }}</text>
          <text class="xm-label">{{ $t('space.evaluation.submittedAt', { time: data.mine.createTime || '—' }) }}</text>
        </view>
        <view
          v-for="key in DIMENSIONS"
          :key="key"
          class="dim"
        >
          <text class="dim-name">{{ $t('space.evaluation.dimensions.' + key) }}</text>
          <xm-rate
            :model-value="data.mine[key]"
            readonly
          />
        </view>
        <view
          v-if="data.mine.comment"
          class="xm-quote"
          >{{ data.mine.comment }}</view
        >
      </view>

      <view
        v-else-if="data.open"
        class="xm-card"
      >
        <view class="xm-section-head">
          <text class="xm-section-title">{{ $t('space.evaluation.form') }}</text>
        </view>
        <view class="hint">{{ $t('space.evaluation.formHint') }}</view>
        <view
          v-for="key in DIMENSIONS"
          :key="key"
          class="dim"
        >
          <text class="dim-name">{{ $t('space.evaluation.dimensions.' + key) }}</text>
          <xm-rate
            v-model="form[key]"
            :size="52"
          />
        </view>
        <view class="xm-form-item comment">
          <view class="xm-form-label">{{ $t('space.evaluation.commentLabel') }}</view>
          <textarea
            class="xm-textarea"
            v-model="form.comment"
            maxlength="500"
            :placeholder="$t('space.evaluation.commentPlaceholder')"
            :show-confirm-bar="false"
          />
        </view>
        <button
          class="xm-btn xm-btn-primary xm-btn-block xm-btn-lg"
          :loading="submitting"
          :disabled="submitting"
          @click="submit"
        >
          {{ $t('space.evaluation.submit') }}
        </button>
      </view>

      <view
        v-else
        class="xm-card closed"
      >
        <view class="closed-title">{{ $t('space.evaluation.notOpen') }}</view>
        <view class="closed-hint">{{ $t('space.evaluation.notOpenHint') }}</view>
      </view>
    </template>

    <!-- ==================== 老师 / 管理员 ==================== -->
    <template v-else-if="data">
      <view
        v-if="!data.count"
        class="xm-card closed"
      >
        <view class="closed-title">{{ $t('space.evaluation.empty') }}</view>
        <view
          v-if="!data.open"
          class="closed-hint"
          >{{ $t('space.evaluation.emptyHint') }}</view
        >
      </view>

      <template v-else>
        <view class="xm-card overall">
          <view class="overall-main">
            <text class="overall-label">{{ $t('space.evaluation.overall') }}</text>
            <text class="overall-value xm-num">{{ fixed(data.overall) }}</text>
            <xm-rate
              :model-value="data.overall || 0"
              readonly
              :size="36"
            />
            <text class="overall-count">{{
              $t('space.evaluation.count', { n: data.count, total: data.students })
            }}</text>
          </view>
          <view class="stars">
            <view
              v-for="star in [5, 4, 3, 2, 1]"
              :key="star"
              class="stars-row"
            >
              <text class="stars-label xm-num">{{ star }}★</text>
              <view class="stars-track">
                <view
                  class="stars-fill"
                  :style="'width:' + starPercent(star) + '%'"
                />
              </view>
              <text class="stars-count xm-num">{{ (data.stars || [])[star - 1] || 0 }}</text>
            </view>
          </view>
        </view>

        <view class="xm-card">
          <view class="xm-section-head">
            <text class="xm-section-title">{{ $t('space.evaluation.summary') }}</text>
          </view>
          <view
            v-for="key in DIMENSIONS"
            :key="key"
            class="bar"
          >
            <text class="bar-name">{{ $t('space.evaluation.dimensions.' + key) }}</text>
            <view class="bar-track">
              <view
                class="bar-fill"
                :style="'width:' + ((data.averages[key] || 0) / 5) * 100 + '%'"
              />
            </view>
            <text class="bar-value xm-num">{{ fixed(data.averages[key]) }}</text>
          </view>
        </view>

        <view class="xm-card">
          <view class="xm-section-head">
            <text class="xm-section-title">{{ $t('space.evaluation.comments') }}</text>
            <text class="xm-label xm-num">{{ data.comments.length }}</text>
          </view>
          <view
            v-if="!data.comments.length"
            class="muted"
            >{{ $t('space.evaluation.noComments') }}</view
          >
          <view
            v-for="c in data.comments"
            :key="c.id"
            class="comment-item"
          >
            <view class="comment-head">
              <xm-rate
                :model-value="c.overall || 0"
                readonly
                :size="28"
              />
              <text class="comment-time xm-num">{{ c.createTime }}</text>
            </view>
            <view class="comment-text">{{ c.comment }}</view>
          </view>
        </view>
      </template>
    </template>
  </view>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { evaluationApi } from '@/api'
import { SILENT } from '@/utils/request'
import { confirm } from '@/utils/confirm'
import { t } from '@/i18n'

const DIMENSIONS = ['attitude', 'contentScore', 'method', 'effect', 'support']

const props = defineProps({
  overview: { type: Object, required: true },
  courseId: { type: Number, required: true },
})

const relation = computed(() => props.overview.relation || 'visitor')
const canTeach = computed(() => relation.value === 'teacher' || relation.value === 'admin')

const data = ref(null)
const loading = ref(false)
const submitting = ref(false)
const form = reactive({ attitude: 0, contentScore: 0, method: 0, effect: 0, support: 0, comment: '' })

const fixed = (n) => (n == null ? '—' : Number(n).toFixed(2))

const load = async () => {
  loading.value = true
  try {
    data.value = await evaluationApi.view(props.courseId, SILENT)
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    loading.value = false
  }
}

watch(() => props.courseId, load, { immediate: true })

const starPercent = (star) => {
  const count = Number(data.value && data.value.stars && data.value.stars[star - 1]) || 0
  return data.value && data.value.count ? Math.round((count / data.value.count) * 100) : 0
}

const submit = async () => {
  if (DIMENSIONS.some((key) => !form[key])) {
    uni.showToast({ title: t('space.evaluation.incomplete'), icon: 'none' })
    return
  }
  if (!(await confirm(t('space.evaluation.submitConfirm'), { title: t('space.evaluation.submit') }))) return
  submitting.value = true
  try {
    await evaluationApi.submit(props.courseId, { ...form })
    uni.showToast({ title: t('space.evaluation.submitted'), icon: 'success' })
    await load()
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    submitting.value = false
  }
}

defineExpose({ reload: load })
</script>

<style lang="scss" scoped>
.hint {
  margin: -8rpx 0 12rpx;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.dim {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 14rpx 0;
}

.dim-name {
  font-size: 28rpx;
  color: var(--xm-text);
}

.comment {
  margin-top: 16rpx;
}

.closed {
  padding: 48rpx 28rpx;
  text-align: center;
}

.closed-title {
  font-size: 30rpx;
  font-weight: 600;
  color: var(--xm-text);
}

.closed-hint {
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: var(--xm-text-2);
}

.overall {
  display: flex;
  align-items: center;
  gap: 28rpx;
}

.overall-main {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
  flex-shrink: 0;
}

.overall-label {
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.overall-value {
  font-size: 72rpx;
  font-weight: bold;
  line-height: 1.1;
  color: var(--xm-text);
}

.overall-count {
  font-size: 20rpx;
  color: var(--xm-text-2);
}

.stars {
  flex: 1;
  min-width: 0;
}

.stars-row,
.bar {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 6rpx 0;
}

.stars-label {
  width: 52rpx;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.stars-track,
.bar-track {
  flex: 1;
  height: 12rpx;
  border-radius: 999rpx;
  background: var(--xm-bg-sunken);
  overflow: hidden;
}

.stars-fill {
  height: 100%;
  background: #f5a623;
}

.bar-fill {
  height: 100%;
  background: var(--xm-brand);
}

.stars-count {
  width: 40rpx;
  font-size: 22rpx;
  text-align: right;
  color: var(--xm-text-2);
}

.bar {
  padding: 12rpx 0;
}

.bar-name {
  width: 150rpx;
  flex-shrink: 0;
  font-size: 26rpx;
  color: var(--xm-text);
}

.bar-value {
  width: 76rpx;
  font-size: 26rpx;
  text-align: right;
  color: var(--xm-text);
}

.muted {
  font-size: 26rpx;
  color: var(--xm-text-2);
}

.comment-item {
  padding: 18rpx 0;
}

.comment-item + .comment-item {
  border-top: 1rpx solid var(--xm-border);
}

.comment-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.comment-time {
  font-size: 22rpx;
  color: var(--xm-text-3);
}

.comment-text {
  margin-top: 8rpx;
  font-size: 26rpx;
  line-height: 1.7;
  color: var(--xm-text);
  white-space: pre-wrap;
  word-break: break-all;
}
</style>

<template>
  <view
    class="xm-page xm-page-narrow"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 学期设置（与 Web 端 views/admin/SemesterSettings.vue 一致）：首页的「第几周」、日程的教学周都按这里推算 -->
    <view class="sub">{{ $t('admin.semester.sub') }}</view>

    <view class="xm-card preview">
      <view class="preview-label">{{ $t('admin.semester.preview') }}</view>
      <view class="preview-week">{{ previewWeek }}</view>
      <view class="preview-sub xm-num">{{ current.today }} {{ weekLabel(current.weekday, true) }}</view>
      <view class="bar">
        <view
          v-for="w in weeksCount"
          :key="w"
          class="bar-week"
          :class="{ 'is-past': week > 0 && w < week, 'is-now': w === week }"
        />
      </view>
    </view>

    <view class="xm-card">
      <view class="xm-form-item">
        <view class="xm-form-label required">{{ $t('admin.semester.name') }}</view>
        <input
          class="xm-input"
          v-model="form.name"
          maxlength="50"
          :placeholder="$t('admin.semester.namePlaceholder')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label required">{{ $t('admin.semester.startDate') }}</view>
        <picker
          mode="date"
          :value="form.startDate"
          @change="onDatePicked"
        >
          <view
            class="xm-select"
            :class="{ placeholder: !form.startDate }"
          >
            <text class="xm-num">{{ form.startDate || $t('admin.semester.startPlaceholder') }}</text>
            <xm-icon
              name="calendar"
              :size="28"
            />
          </view>
        </picker>
        <view
          v-if="notMonday"
          class="warn"
          >{{ $t('admin.semester.notMonday') }}</view
        >
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('admin.semester.weeks') }}</view>
        <input
          class="xm-input"
          v-model="form.weeks"
          type="number"
          maxlength="2"
        />
      </view>
      <button
        class="xm-btn xm-btn-primary xm-btn-block xm-btn-lg"
        :loading="saving"
        :disabled="saving"
        @click="save"
      >
        {{ $t('common.save') }}
      </button>
    </view>
    <xm-loader />
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { configApi } from '@/api'
import { SILENT } from '@/utils/request'
import { ensureRole } from '@/utils/authGuard'
import { daysBetween, isoWeekday } from '@/utils/schedule'
import { weekLabel } from '@/utils/courseText'
import { t } from '@/i18n'

const saving = ref(false)
const form = reactive({ name: '', startDate: '', weeks: '18' })
const current = ref({})

const weeksCount = computed(() => {
  const n = Math.round(Number(form.weeks))
  return Number.isFinite(n) && n >= 1 && n <= 30 ? n : 0
})

/** 按表单里的开学日期，预览「今天」是第几周（与后端同一算法：开学当天第 1 周，满 7 天加 1） */
const week = computed(() => {
  if (!form.startDate || !current.value.today) return 0
  const days = daysBetween(form.startDate, current.value.today)
  if (days < 0) return 0
  return Math.floor(days / 7) + 1
})

const previewWeek = computed(() => {
  if (!form.startDate) return '—'
  if (week.value === 0) return t('workbench.beforeSemester')
  if (week.value > weeksCount.value) return t('workbench.afterSemester')
  return t('workbench.weekN', { n: week.value })
})

/** 开学日期不是周一时提醒：教学周按 7 天一周切分，从周一开始最直观 */
const notMonday = computed(() => !!form.startDate && isoWeekday(form.startDate) !== 0)

const onDatePicked = (e) => {
  form.startDate = e.detail.value
}

const load = async () => {
  try {
    const data = (await configApi.semester(SILENT)) || {}
    current.value = data
    form.name = data.name || ''
    form.startDate = data.startDate || ''
    form.weeks = String(data.weeks || 18)
  } catch {
    // 提示已由请求层统一弹出
  }
}

const save = async () => {
  if (!form.name.trim()) {
    uni.showToast({ title: t('admin.semester.nameRequired'), icon: 'none' })
    return
  }
  if (!form.startDate) {
    uni.showToast({ title: t('admin.semester.startRequired'), icon: 'none' })
    return
  }
  saving.value = true
  try {
    current.value =
      (await configApi.saveSemester({
        name: form.name.trim(),
        startDate: form.startDate,
        weeks: weeksCount.value || 18,
      })) || current.value
    uni.showToast({ title: t('common.saveSuccess'), icon: 'success' })
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    saving.value = false
  }
}

onShow(() => {
  if (!ensureRole(['ADMIN'])) return
  uni.setNavigationBarTitle({ title: t('admin.semester.title') })
  load()
})
</script>

<style lang="scss" scoped>
.sub {
  margin-bottom: 20rpx;
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.preview {
  text-align: center;
}

.preview-label {
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.preview-week {
  margin: 6rpx 0;
  font-size: 48rpx;
  font-weight: bold;
  color: var(--xm-brand);
}

.preview-sub {
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.bar {
  display: flex;
  gap: 4rpx;
  margin-top: 20rpx;
}

.bar-week {
  flex: 1;
  height: 16rpx;
  border-radius: 4rpx;
  background: var(--xm-bg-sunken);
}

.bar-week.is-past {
  background: var(--xm-brand-soft);
}

.bar-week.is-now {
  background: var(--xm-brand);
}

.warn {
  margin-top: 10rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: var(--xm-warning);
}
</style>

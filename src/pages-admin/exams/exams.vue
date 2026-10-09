<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 考试安排（与 Web 端教务后台「考试安排」一致）：按标题搜索，点一条编辑，右下角新建；
         考试时间旁边是倒计时标签（今天 / 明天 / 还有 N 天 / 已结束） -->
    <view class="filters">
      <xm-search
        v-model="keyword"
        :placeholder="$t('pages.examplan.searchPlaceholder')"
        @confirm="exams.reload()"
        @clear="exams.reload()"
      />
    </view>

    <xm-empty
      v-if="exams.loaded.value && !exams.loading.value && !exams.list.value.length"
      icon="calendar"
    />
    <view
      v-else
      class="xm-list"
    >
      <view
        v-for="e in exams.list.value"
        :key="e.id"
        class="xm-card exam"
        @click="openForm(e)"
      >
        <view class="exam-head">
          <text class="exam-name">{{ e.name }}</text>
          <text
            v-if="countdownTag(e.examTime)"
            class="xm-tag"
            :class="countdownTag(e.examTime).cls"
            >{{ countdownTag(e.examTime).text }}</text
          >
        </view>
        <view class="exam-time xm-num">
          <xm-icon
            name="clock"
            :size="26"
          />
          {{ e.examTime || '—' }}
        </view>
        <view
          v-if="e.content"
          class="exam-content xm-clamp-2"
          >{{ e.content }}</view
        >
      </view>
    </view>
    <xm-list-footer
      :visible="exams.list.value.length > 0"
      :loading="exams.loading.value"
      :finished="exams.finished.value"
      @load-more="exams.loadNext()"
    />

    <view
      class="xm-fab"
      @click="openForm(null)"
    >
      <xm-icon
        name="plus"
        :size="48"
      />
    </view>

    <xm-form-popup
      :visible="formOpen"
      :title="$t(editing ? 'common.editTitle' : 'common.addTitle', { name: $t('menu.examplan') })"
      :saving="saving"
      :confirm-text="$t('common.save')"
      @close="formOpen = false"
      @save="save"
    >
      <view class="xm-form-item">
        <view class="xm-form-label required">{{ $t('pages.examplan.title') }}</view>
        <input
          class="xm-input"
          v-model="form.name"
          maxlength="100"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label required">{{ $t('pages.examplan.examTime') }}</view>
        <xm-datetime
          v-model="form.examTime"
          :date-placeholder="$t('pages.examplan.examTimePlaceholder')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label required">{{ $t('pages.examplan.content') }}</view>
        <textarea
          class="xm-textarea content"
          v-model="form.content"
          maxlength="500"
          :show-confirm-bar="false"
        />
      </view>
      <button
        v-if="editing"
        class="xm-btn xm-btn-danger xm-btn-block"
        @click="remove"
      >
        {{ $t('common.delete') }}
      </button>
    </xm-form-popup>
    <xm-loader />
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { examplanApi } from '@/api'
import { SILENT } from '@/utils/request'
import { ensureRole } from '@/utils/authGuard'
import { countdownTag, isCompleteExamTime } from '@/utils/examCountdown'
import { usePager } from '@/composables/usePager'
import { confirm } from '@/utils/confirm'
import { t } from '@/i18n'

const keyword = ref('')
const exams = usePager((page) => {
  const params = { ...page }
  if (keyword.value.trim()) params.name = keyword.value.trim()
  return examplanApi.selectPage(params, SILENT)
})

const formOpen = ref(false)
const editing = ref(null)
const saving = ref(false)
const form = reactive({ name: '', content: '', examTime: '' })

const openForm = (exam) => {
  editing.value = exam
  form.name = (exam && exam.name) || ''
  form.content = (exam && exam.content) || ''
  form.examTime = (exam && exam.examTime) || ''
  formOpen.value = true
}

const save = async () => {
  if (!form.name.trim()) {
    uni.showToast({ title: t('pages.examplan.ruleTitleRequired'), icon: 'none' })
    return
  }
  if (!form.examTime) {
    uni.showToast({ title: t('pages.examplan.ruleExamTimeRequired'), icon: 'none' })
    return
  }
  if (!isCompleteExamTime(form.examTime)) {
    uni.showToast({ title: t('mobile.datetime.incomplete'), icon: 'none' })
    return
  }
  if (!form.content.trim()) {
    uni.showToast({ title: t('pages.examplan.ruleContentRequired'), icon: 'none' })
    return
  }
  const body = { name: form.name.trim(), content: form.content.trim(), examTime: form.examTime }
  saving.value = true
  try {
    if (editing.value) await examplanApi.update({ ...body, id: editing.value.id })
    else await examplanApi.add(body)
    uni.showToast({ title: t('common.saveSuccess'), icon: 'success' })
    formOpen.value = false
    exams.reload()
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    saving.value = false
  }
}

const remove = async () => {
  if (!(await confirm(t('common.deleteConfirm')))) return
  try {
    await examplanApi.delete(editing.value.id)
    uni.showToast({ title: t('common.operationSuccess'), icon: 'none' })
    formOpen.value = false
    exams.reload()
  } catch {
    // 提示已由请求层统一弹出
  }
}

onShow(() => {
  if (!ensureRole(['ADMIN'])) return
  uni.setNavigationBarTitle({ title: t('menu.examplan') })
  if (!exams.loaded.value) exams.reload()
})

onPullDownRefresh(async () => {
  await exams.reload()
  uni.stopPullDownRefresh()
})

onReachBottom(() => exams.loadNext())
</script>

<style lang="scss" scoped>
.filters {
  margin-bottom: 24rpx;
}

.exam-head {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.exam-name {
  flex: 1;
  min-width: 0;
  font-size: 30rpx;
  font-weight: 600;
  color: var(--xm-text);
}

.exam-time {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.exam-content {
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: var(--xm-text-2);
}

.content {
  min-height: 200rpx;
}
</style>

<template>
  <!-- 请假申请（与 Web 端 views/schedule/LeaveDialog.vue 一致）：先列出这几天要上的课，再填理由提交（教务处在后台审批） -->
  <xm-form-popup
    :visible="visible"
    :title="$t('leave.dialogTitle')"
    :saving="submitting"
    :confirm-text="$t('leave.submit')"
    @close="$emit('close')"
    @save="submit"
  >
    <view class="xm-kv">
      <text class="xm-kv-key">{{ $t('leave.range') }}</text>
      <text class="xm-kv-value">{{ rangeText }}</text>
    </view>

    <view class="affected">
      <view class="xm-form-label">{{ $t('leave.affected', { n: affected.length }) }}</view>
      <view
        v-if="loading"
        class="skeleton skeleton-line"
      />
      <view
        v-else-if="!affected.length"
        class="affected-empty"
        >{{ $t('leave.affectedEmpty') }}</view
      >
      <view v-else>
        <view
          v-for="c in affected"
          :key="c.date + '-' + c.courseId + '-' + c.start"
          class="affected-row"
        >
          <view
            class="affected-bar"
            :style="'background:' + courseColor(c.courseName)"
          />
          <text class="affected-when xm-num">{{ dayText(c.date) }} {{ c.start }}</text>
          <text class="affected-course">{{ c.courseName }}</text>
          <text class="affected-teacher">{{ c.teacherName }}</text>
        </view>
      </view>
    </view>

    <view class="xm-form-item">
      <view class="xm-form-label required">{{ $t('leave.reason') }}</view>
      <textarea
        class="xm-textarea reason"
        v-model="content"
        maxlength="500"
        :placeholder="$t('leave.reasonPlaceholder')"
        :show-confirm-bar="false"
      />
    </view>
  </xm-form-popup>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { applyApi } from '@/api'
import { SILENT } from '@/utils/request'
import { courseColor } from '@/utils/courseColor'
import { daysBetween, formatDay } from '@/utils/schedule'
import { isZh, t } from '@/i18n'

const props = defineProps({
  visible: { type: Boolean, default: false },
  from: { type: String, default: '' },
  to: { type: String, default: '' },
})

const emit = defineEmits(['close', 'submitted'])

const content = ref('')
const affected = ref([])
const loading = ref(false)
const submitting = ref(false)

const days = computed(() => (props.from && props.to ? daysBetween(props.from, props.to) + 1 : 0))
const dayText = (iso) => formatDay(iso, isZh(), true)
const rangeText = computed(() =>
  t('leave.selected', { from: dayText(props.from), to: dayText(props.to), n: days.value }),
)

watch(
  () => props.visible,
  async (open) => {
    if (!open) return
    content.value = ''
    affected.value = []
    loading.value = true
    try {
      affected.value = (await applyApi.preview({ from: props.from, days: days.value }, SILENT)) || []
    } catch {
      // 日期不合法等提示已由请求层统一弹出
    } finally {
      loading.value = false
    }
  },
  { immediate: true },
)

const submit = async () => {
  if (!content.value.trim()) {
    uni.showToast({ title: t('leave.reasonRequired'), icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await applyApi.add({ content: content.value.trim(), time: props.from, day: days.value })
    uni.showToast({ title: t('leave.submitted'), icon: 'none' })
    emit('submitted')
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.affected {
  margin: 16rpx 0 24rpx;
}

.affected-empty {
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.affected-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 10rpx 0;
  font-size: 24rpx;
}

.affected-bar {
  width: 8rpx;
  height: 32rpx;
  border-radius: 4rpx;
  flex-shrink: 0;
}

.affected-when {
  flex-shrink: 0;
  color: var(--xm-text-2);
}

.affected-course {
  flex: 1;
  min-width: 0;
  color: var(--xm-text);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.affected-teacher {
  flex-shrink: 0;
  color: var(--xm-text-2);
}

.reason {
  min-height: 200rpx;
}
</style>

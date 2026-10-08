<template>
  <!-- 需要关注的学生（教师首页，与 Web 端 views/home/AttentionPanel.vue 一致）：
       自己课上学业风险不是「正常」的学生，风险高的在前，可以直接发提醒 -->
  <view class="xm-card">
    <view class="xm-section-head">
      <text class="xm-section-title">{{ $t('workbench.attention.title') }}</text>
    </view>
    <view
      v-if="loading"
      class="skeleton skeleton-line"
    />
    <template v-else-if="students.length">
      <view class="att-hint">{{ $t('workbench.attention.hint') }}</view>
      <view
        v-for="s in shown"
        :key="s.studentId"
        class="att-row"
      >
        <xm-user-avatar
          :name="s.studentName"
          :size="64"
        />
        <view class="att-main">
          <view class="att-name">
            <text class="xm-ellipsis">{{ s.studentName }}</text>
            <text
              class="xm-tag"
              :class="s.level === '高风险' ? 'xm-tag-danger' : 'xm-tag-warning'"
              >{{ levelLabel(s.level) }}</text
            >
          </view>
          <view class="att-meta xm-num">
            {{ $t('workbench.attention.meta', { index: s.riskIndex, failed: s.failedCount, absent: s.absentRate }) }}
          </view>
        </view>
        <button
          class="xm-btn xm-btn-soft xm-btn-sm"
          :disabled="sending === s.studentId || sent.includes(s.studentId)"
          @click="remind(s)"
        >
          {{ $t('workbench.attention.notify') }}
        </button>
      </view>
      <view
        v-if="students.length > LIMIT && !expanded"
        class="att-more"
        @click="expanded = true"
        >{{ $t('workbench.attention.more', { n: students.length - LIMIT }) }}</view
      >
    </template>
    <view
      v-else
      class="att-empty"
      >{{ $t('workbench.attention.empty') }}</view
    >
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { warningApi } from '@/api'
import { SILENT } from '@/utils/request'
import { t } from '@/i18n'

const LIMIT = 5

const students = ref([])
const loading = ref(false)
const expanded = ref(false)
const sending = ref(null)
const sent = ref([])

const shown = computed(() => (expanded.value ? students.value : students.value.slice(0, LIMIT)))

/** 后端返回的等级是中文原值，显示时翻译 */
const LEVEL_KEY = { 高风险: 'high', 中风险: 'medium', 低风险: 'low' }
const levelLabel = (level) => (LEVEL_KEY[level] ? t(`workbench.attention.levels.${LEVEL_KEY[level]}`) : level)

const load = async () => {
  loading.value = !students.value.length
  try {
    const list = (await warningApi.list(SILENT)) || []
    students.value = list.filter((s) => s.level !== '正常')
  } catch {
    // 拿不到就显示空，首页其他部分不受影响
  } finally {
    loading.value = false
  }
}

const remind = async (s) => {
  sending.value = s.studentId
  try {
    await warningApi.notify(s.studentId)
    sent.value.push(s.studentId)
    uni.showToast({ title: t('workbench.attention.notified', { name: s.studentName }), icon: 'none' })
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    sending.value = null
  }
}

load()

defineExpose({ load })
</script>

<style lang="scss" scoped>
.att-hint {
  margin: -8rpx 0 12rpx;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.att-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 16rpx 0;
}

.att-row + .att-row {
  border-top: 1rpx solid var(--xm-border);
}

.att-main {
  flex: 1;
  min-width: 0;
}

.att-name {
  display: flex;
  align-items: center;
  gap: 12rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: var(--xm-text);
}

.att-meta {
  margin-top: 4rpx;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.att-more {
  padding-top: 16rpx;
  text-align: center;
  font-size: 26rpx;
  color: var(--xm-brand);
}

.att-empty {
  padding: 16rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: var(--xm-text-2);
}
</style>

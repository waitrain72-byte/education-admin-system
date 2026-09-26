<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 搜索区 -->
    <xm-search-card
      v-model="query.name"
      :placeholder="$t('pages.examplan.searchPlaceholder')"
      @search="search"
      @reset="resetQuery"
    />

    <!-- 操作区：仅管理员可新增/批量管理（与 Web 端 user.role === 'ADMIN' 一致） -->
    <xm-action-bar
      v-if="canManage"
      :manage-mode="manageMode"
      :total="total"
      :selected-count="selectedIds.length"
      @add="handleAdd()"
      @toggle-manage="toggleManage"
      @del-batch="delBatch"
    />

    <!-- 列表 -->
    <xm-empty
      v-if="!list.length && !loading"
      :action-text="$t('common.reload')"
      @action="load(true)"
    />

    <!-- 列表：手机单列，平板 ≥720px 两列、≥1248px 三列（theme.scss .xm-list） -->
    <view class="xm-list">
      <view
        v-for="item in list"
        :key="item.id"
        class="xm-card"
        @click="toggleExpand(item.id)"
      >
        <view class="xm-between">
          <view class="xm-row xm-card-head">
            <!-- 批量管理模式下显示勾选框 -->
            <checkbox
              v-if="manageMode"
              :checked="selectedIds.includes(item.id)"
              style="transform: scale(0.8)"
              @click.stop="toggleSelect(item.id)"
            />
            <text class="xm-card-name xm-ellipsis">{{ item.name }}</text>
          </view>
          <text class="xm-card-no">#{{ item._index }}</text>
        </view>
        <!-- 考试时间 + 倒计时（今天 / 明天 / 还有 N 天 / 已结束）；创建时间弱化显示 -->
        <view class="xm-meta">
          <view class="xm-meta-item full">
            <xm-icon
              name="clock"
              :size="28"
            />
            <text class="xm-meta-text">{{ item.examTime || $t('pages.examplan.examTimeUnset') }}</text>
            <text
              v-if="countdowns[item.id]"
              class="xm-tag exam-countdown"
              :class="countdowns[item.id].cls"
              >{{ countdowns[item.id].text }}</text
            >
          </view>
          <view class="xm-meta-item full">
            <xm-icon
              name="calendar"
              :size="28"
            />
            <text class="xm-meta-text">{{ $t('pages.examplan.time') }} {{ item.time || '-' }}</text>
          </view>
        </view>
        <!-- 说明默认两行，点卡片展开全文 -->
        <view
          v-if="item.content"
          class="xm-card-desc"
          :class="{ 'xm-clamp-2': !expandedIds.includes(item.id) }"
          >{{ item.content }}</view
        >
        <view
          class="xm-actions"
          v-if="!manageMode && canManage"
        >
          <button
            class="xm-btn xm-btn-plain"
            @click.stop="handleEdit(item)"
          >
            {{ $t('common.edit') }}
          </button>
          <button
            class="xm-btn xm-btn-danger"
            @click.stop="del(item.id)"
          >
            {{ $t('common.delete') }}
          </button>
        </view>
      </view>
    </view>

    <xm-list-footer
      :visible="!!list.length"
      :loading="loading"
      :finished="finished()"
      @load-more="loadNext"
    />

    <!-- 新增/编辑表单（底部弹层） -->
    <xm-form-popup
      :visible="formVisible"
      :saving="saving"
      :title="$t(form.id ? 'common.editTitle' : 'common.addTitle', { name: $t('pages.examplan.entity') })"
      @close="closeForm"
      @save="save"
    >
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.examplan.title') }}</view>
        <input
          class="xm-input"
          v-model="form.name"
          :placeholder="$t('pages.examplan.ruleTitleRequired')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.examplan.content') }}</view>
        <textarea
          class="xm-textarea"
          v-model="form.content"
          :placeholder="$t('pages.examplan.ruleContentRequired')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.examplan.examTime') }}</view>
        <!-- 小程序没有日期时间合一的原生选择器：日期 + 开考时间两个选择器拼成 yyyy-MM-dd HH:mm -->
        <view class="exam-time-row">
          <view class="exam-date">
            <xm-picker
              v-model="examDate"
              mode="date"
              :placeholder="$t('pages.examplan.examDatePlaceholder')"
            />
          </view>
          <view class="exam-clock">
            <xm-picker
              v-model="examClock"
              mode="time"
              :placeholder="$t('pages.examplan.examClockPlaceholder')"
            />
          </view>
        </view>
      </view>
    </xm-form-popup>
    <xm-loader />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { useListPage } from '@/composables/useListPage'
import { examplanApi } from '@/api'
import { t } from '@/i18n'
import { countdownTag, splitExamTime, joinExamTime, isCompleteExamTime } from '@/utils/examCountdown'

const userStore = useUserStore()
// 所有角色可看，仅管理员可管理（与 Web 端逻辑一致）
const canManage = computed(() => userStore.role === 'ADMIN')

const {
  list,
  loading,
  finished,
  total,
  form,
  formVisible,
  saving,
  selectedIds,
  query,
  manageMode,
  load,
  loadNext,
  search,
  resetQuery,
  toggleManage,
  toggleSelect,
  handleAdd,
  handleEdit,
  closeForm,
  save,
  del,
  delBatch,
} = useListPage({
  api: examplanApi,
  title: 'menu.examplan',
  query: { name: '' },
  validate: (f) => {
    if (!f.name) return t('pages.examplan.ruleTitleRequired')
    if (!f.content) return t('pages.examplan.ruleContentRequired')
    if (!isCompleteExamTime(f.examTime)) return t('pages.examplan.ruleExamTimeRequired')
    return ''
  },
})

// 表单里的考试时间：日期、开考时间两个选择器分别读写 form.examTime 的两半（发布时间由后端生成，表单不再提供）
const examParts = computed(() => splitExamTime(form.value.examTime))
const examDate = computed({
  get: () => examParts.value.date,
  set: (date) => (form.value.examTime = joinExamTime(date, examParts.value.time)),
})
const examClock = computed({
  get: () => examParts.value.time,
  set: (time) => (form.value.examTime = joinExamTime(examParts.value.date, time)),
})

// 倒计时以「今天」为基准：每次显示页面刷新一次，跨天后回到页面也准确
const now = ref(new Date())
onShow(() => {
  now.value = new Date()
})
const countdowns = computed(() =>
  Object.fromEntries(list.value.map((row) => [row.id, countdownTag(row.examTime, now.value)])),
)

// 说明默认两行，点卡片展开 / 收起全文
const expandedIds = ref([])
const toggleExpand = (id) => {
  const idx = expandedIds.value.indexOf(id)
  if (idx >= 0) expandedIds.value.splice(idx, 1)
  else expandedIds.value.push(id)
}
</script>

<style lang="scss" scoped>
/* 倒计时标签跟在考试时间后，不被挤压 */
.exam-countdown {
  flex-shrink: 0;
}

/* 考试时间：日期 + 开考时间并排 */
.exam-time-row {
  display: flex;
  gap: 16rpx;
}

.exam-date {
  flex: 3;
  min-width: 0;
}

.exam-clock {
  flex: 2;
  min-width: 0;
}
</style>

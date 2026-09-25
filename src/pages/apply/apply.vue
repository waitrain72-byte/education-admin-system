<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 搜索区 -->
    <xm-search-card
      v-model="query.content"
      :placeholder="$t('pages.apply.contentPlaceholder')"
      @search="search"
      @reset="resetQuery"
    />

    <!-- 状态快捷筛选 Tab：全部/待审核/审核通过/审核不通过（点击即筛选） -->
    <view class="xm-card xm-tabs">
      <view
        v-for="tab in statusTabs"
        :key="tab.value"
        class="xm-tab"
        :class="{ active: query.status === tab.value }"
        @click="onTabChange(tab.value)"
      >
        {{ tab.label }}
      </view>
    </view>

    <!-- 新增：右下角悬浮按钮（与各列表页一致，避让 Home 条，样式见 theme.scss .xm-fab） -->
    <view
      v-if="userStore.role === 'STUDENT'"
      class="xm-fab"
      :aria-label="$t('pages.apply.applyLeave')"
      aria-role="button"
      @click="onAdd"
    >
      <xm-icon
        name="plus"
        :size="48"
      />
    </view>

    <!-- 列表 -->
    <xm-empty
      v-if="!list.length && !loading"
      :action-text="$t('common.reload')"
      @action="load(true)"
    />

    <!-- 列表：手机单列，平板 ≥720px 两列、≥1248px 三列（theme.scss .xm-list） -->
    <view class="xm-list">
      <view
        v-for="row in list"
        :key="row.id"
        class="xm-card"
      >
        <view class="xm-between">
          <!-- 主标题：请假缘由；右侧审核状态语义化标签 -->
          <view class="xm-value xm-ellipsis item-title">{{ row.content }}</view>
          <view
            class="xm-tag status-tag"
            :class="enumTag('applyStatus', row.status)"
            >{{ enumLabel('applyStatus', row.status) }}</view
          >
        </view>
        <view class="item-meta">
          <text v-if="userStore.role !== 'STUDENT'">{{ row.studentName }} · </text>
          <text>{{ row.time }} · {{ $t('pages.apply.dayLabel') }} {{ row.day }}</text>
        </view>
        <view
          v-if="row.descr"
          class="item-descr"
          >{{ row.descr }}</view
        >

        <view class="xm-actions">
          <button
            v-if="userStore.role === 'STUDENT' && row.status !== '审核通过'"
            class="xm-btn xm-btn-plain"
            @click="onEdit(row)"
          >
            {{ $t('common.edit') }}
          </button>
          <button
            v-if="userStore.role === 'ADMIN' && row.status === '待审核'"
            class="xm-btn xm-btn-primary"
            @click="handleCheck(row)"
          >
            {{ $t('pages.apply.review') }}
          </button>
          <button
            v-if="userStore.role === 'STUDENT' && row.status === '待审核'"
            class="xm-btn xm-btn-danger"
            @click="del(row.id)"
          >
            {{ $t('pages.apply.withdraw') }}
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

    <!-- 请假申请/编辑表单（底部弹层，学生） -->
    <xm-form-popup
      :visible="formVisible"
      :saving="saving"
      :title="$t(form.id ? 'common.editTitle' : 'common.addTitle', { name: $t('pages.apply.entity') })"
      @close="closeForm"
      @save="save"
    >
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.apply.contentLabel') }}</view>
        <textarea
          class="xm-textarea"
          v-model="form.content"
          :placeholder="$t('pages.apply.contentPlaceholder')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.apply.timeLabel') }}</view>
        <xm-picker
          v-model="form.time"
          mode="date"
          :placeholder="$t('pages.apply.datePlaceholder')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.apply.dayLabel') }}</view>
        <input
          class="xm-input"
          v-model="form.day"
          type="number"
        />
      </view>
    </xm-form-popup>

    <!-- 审核弹层（管理员） -->
    <xm-form-popup
      :visible="checkVisible"
      :saving="checking"
      :title="$t('pages.apply.checkDialogTitle')"
      @close="checkVisible = false"
      @save="check"
    >
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.apply.statusLabel') }}</view>
        <xm-picker
          v-model="form.status"
          :options="enumOptions('applyStatus')"
          :placeholder="$t('pages.apply.statusPlaceholder')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.apply.descrLabel') }}</view>
        <textarea
          class="xm-textarea"
          v-model="form.descr"
        />
      </view>
    </xm-form-popup>
    <xm-loader />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { useListPage } from '@/composables/useListPage'
import { applyApi } from '@/api'
import { t } from '@/i18n'
import { enumLabel, enumTag, enumOptions } from '@/utils/enums'

const userStore = useUserStore()
const checkVisible = ref(false)
// 审核提交中：确定按钮转圈禁用，防止连点重复提交
const checking = ref(false)

// 撤销申请即删除该条请假记录，确认文案与 Web 端一致
const {
  list,
  loading,
  finished,
  form,
  formVisible,
  saving,
  query,
  load,
  loadNext,
  search,
  resetQuery,
  handleAdd,
  handleEdit,
  save,
  closeForm,
  del,
} = useListPage({
  api: applyApi,
  title: 'menu.apply',
  query: { status: '', content: '' },
  deleteConfirm: 'pages.apply.deleteConfirm',
  validate: (f) => {
    if (!f.time) return t('pages.apply.ruleTimeRequired')
    if (!f.content) return t('pages.apply.ruleContentRequired')
    if (!f.day) return t('pages.apply.ruleDayRequired')
    return ''
  },
})

// 顶部状态筛选 Tab：'' = 全部（空参数不传给后端，不加 status 条件）
const statusTabs = computed(() => [{ label: t('common.all'), value: '' }, ...enumOptions('applyStatus')])
const onTabChange = (value) => {
  if (query.status === value) return
  query.status = value
  search()
}

// 学生新增 / 编辑：编辑时重置回待审核并清空审核说明（与 Web 端 handleEdit 一致）
const onAdd = () => handleAdd({ studentId: userStore.user.id, status: '待审核' })
const onEdit = (row) => handleEdit(row, { status: '待审核', descr: '' })

const handleCheck = (row) => {
  form.value = JSON.parse(JSON.stringify(row))
  checkVisible.value = true
}

// 审核提交：与 Web 端 check 一致，直接走 /apply/update
const check = () => {
  if (checking.value) return
  checking.value = true
  applyApi
    .update(form.value)
    .then(() => {
      uni.showToast({ title: t('common.operationSuccess'), icon: 'success' })
      load(true)
      checkVisible.value = false
    })
    .catch(() => {
      // 提示已由请求层统一弹出
    })
    .finally(() => {
      checking.value = false
    })
}
</script>

<style lang="scss" scoped>
/* 主标题：请假缘由单行省略，右侧状态标签不被挤压 */
.item-title {
  font-weight: bold;
  flex: 1;
  min-width: 0;
}

/* 状态标签：禁止收缩换行 */
.status-tag {
  flex-shrink: 0;
  white-space: nowrap;
}

/* 次要信息行：学生/时间/天数弱化小字 */
.item-meta {
  font-size: 24rpx;
  color: var(--xm-text-2);
  margin-top: 12rpx;
}

/* 审核说明气泡 */
.item-descr {
  font-size: 26rpx;
  color: var(--xm-text);
  background: var(--xm-bg-input);
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
  margin-top: 12rpx;
}
</style>

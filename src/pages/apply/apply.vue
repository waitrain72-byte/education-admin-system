<template>
  <view
    class="xm-page"
    :class="themeClass"
  >
    <!-- 搜索区 -->
    <view class="xm-card">
      <view
        class="xm-form-item"
        style="margin-bottom: 16rpx"
      >
        <input
          class="xm-input"
          v-model="content"
          :placeholder="$t('pages.apply.contentPlaceholder')"
        />
      </view>
      <view class="xm-row">
        <button
          class="xm-btn xm-btn-primary"
          style="flex: 1"
          @click="search"
        >
          {{ $t('common.search') }}
        </button>
        <button
          class="xm-btn xm-btn-plain"
          style="flex: 1; margin-left: 16rpx"
          @click="onReset"
        >
          {{ $t('common.reset') }}
        </button>
      </view>
    </view>

    <!-- 状态快捷筛选 Tab：全部/待审核/审核通过/审核不通过（点击即筛选） -->
    <view class="xm-card status-tabs">
      <view
        v-for="tab in statusTabs"
        :key="tab.value"
        class="status-tab"
        :class="{ active: status === tab.value }"
        @click="onTabChange(tab.value)"
      >
        {{ tab.label }}
      </view>
    </view>

    <!-- 操作区：学生可提交请假申请 -->
    <view
      class="xm-card"
      v-if="userStore.role === 'STUDENT'"
    >
      <button
        class="xm-btn xm-btn-primary xm-btn-block"
        @click="onAdd"
      >
        {{ $t('pages.apply.applyLeave') }}
      </button>
    </view>

    <!-- 列表 -->
    <view
      v-if="!list.length && !loading"
      class="xm-empty"
      >{{ $t('common.empty') }}</view
    >

    <view
      v-for="row in list"
      :key="row.id"
      class="xm-card"
    >
      <view class="xm-between">
        <view class="xm-row">
          <view
            class="xm-value"
            style="font-weight: bold"
            >{{ row.studentName }}</view
          >
          <view
            class="xm-tag"
            :class="statusTagClass(row.status)"
            >{{ statusLabelOf(row.status) }}</view
          >
        </view>
        <view class="xm-label">{{ $t('pages.apply.id') }}: {{ row._index }}</view>
      </view>
      <view
        class="xm-row"
        style="flex-wrap: wrap; margin-top: 12rpx"
      >
        <view class="xm-label field">{{ $t('pages.apply.timeLabel') }}: {{ row.time }}</view>
        <view class="xm-label field">{{ $t('pages.apply.dayLabel') }}: {{ row.day }}</view>
      </view>
      <view style="margin-top: 8rpx">
        <view class="xm-label">{{ $t('pages.apply.contentLabel') }}</view>
        <view
          class="xm-value"
          style="margin-top: 4rpx"
          >{{ row.content }}</view
        >
      </view>
      <view
        v-if="row.descr"
        style="margin-top: 8rpx"
      >
        <view class="xm-label">{{ $t('pages.apply.descrLabel') }}</view>
        <view
          class="xm-value"
          style="margin-top: 4rpx"
          >{{ row.descr }}</view
        >
      </view>

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
          @click="onWithdraw(row.id)"
        >
          {{ $t('pages.apply.withdraw') }}
        </button>
      </view>
    </view>

    <xm-list-footer :visible="!!list.length" :loading="loading" :finished="finished()" @load-more="loadNext" />

    <!-- 请假申请/编辑表单（底部弹层，学生） -->
    <view
      v-if="formVisible"
      class="xm-mask"
      @click="closeForm"
    ></view>

    <view
      v-if="formVisible"
      class="xm-popup"
    >
      <view class="xm-popup-title">{{ $t('pages.apply.dialogTitle') }}</view>
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
        <input
          class="xm-input"
          v-model="form.time"
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
      <view
        class="xm-row"
        style="margin-top: 16rpx"
      >
        <button
          class="xm-btn xm-btn-plain"
          style="flex: 1"
          @click="closeForm"
        >
          {{ $t('common.cancel') }}
        </button>
        <button
          class="xm-btn xm-btn-primary"
          style="flex: 1"
          @click="save"
        >
          {{ $t('common.ok') }}
        </button>
      </view>
    </view>

    <!-- 审核弹层（管理员） -->
    <view
      v-if="checkVisible"
      class="xm-mask"
      @click="checkVisible = false"
    ></view>

    <view
      v-if="checkVisible"
      class="xm-popup"
    >
      <view class="xm-popup-title">{{ $t('pages.apply.checkDialogTitle') }}</view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.apply.statusLabel') }}</view>
        <picker
          :range="statusLabels"
          :value="checkStatusIndex"
          @change="onCheckStatusChange"
        >
          <view
            class="xm-input picker-display"
            :class="{ 'picker-placeholder': !form.status }"
          >
            {{ form.status ? statusLabelOf(form.status) : $t('pages.apply.statusPlaceholder') }}
          </view>
        </picker>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.apply.descrLabel') }}</view>
        <textarea
          class="xm-textarea"
          v-model="form.descr"
        />
      </view>
      <view
        class="xm-row"
        style="margin-top: 16rpx"
      >
        <button
          class="xm-btn xm-btn-plain"
          style="flex: 1"
          @click="checkVisible = false"
        >
          {{ $t('common.cancel') }}
        </button>
        <button
          class="xm-btn xm-btn-primary"
          style="flex: 1"
          @click="check"
        >
          {{ $t('common.ok') }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { useCrud } from '@/composables/useCrud'
import { put, del as delRequest } from '@/utils/request'
import { t, apiMessage } from '@/i18n'

const userStore = useUserStore()
const content = ref('')
const status = ref('')
const checkVisible = ref(false)

const { list, loading, finished, form, formVisible, load, loadNext, search, save, closeForm } = useCrud({
  url: '/apply',
  getParams: () => ({ status: status.value, content: content.value }),
  validate: (f) => {
    if (!f.time) return t('pages.apply.ruleTimeRequired')
    if (!f.content) return t('pages.apply.ruleContentRequired')
    if (!f.day) return t('pages.apply.ruleDayRequired')
    return ''
  },
})

// 状态枚举值保持中文入库，仅翻译显示文案（与 Web 端一致）
const statusOptions = computed(() => [
  { label: t('pages.apply.statusPending'), value: '待审核' },
  { label: t('pages.apply.statusApproved'), value: '审核通过' },
  { label: t('pages.apply.statusRejected'), value: '审核不通过' },
])
const statusLabels = computed(() => statusOptions.value.map((o) => o.label))
const statusLabelOf = (value) => {
  const opt = statusOptions.value.find((o) => o.value === value)
  return opt ? opt.label : value
}
const statusTagClass = (value) => {
  if (value === '审核通过') return 'xm-tag-success'
  if (value === '审核不通过') return 'xm-tag-danger'
  return 'xm-tag-warning'
}

const searchStatusIndex = computed(() => statusOptions.value.findIndex((o) => o.value === status.value))
const checkStatusIndex = computed(() => statusOptions.value.findIndex((o) => o.value === form.value.status))

// 顶部状态筛选 Tab：'' = 全部（useCrud 会剔除空参数，后端不加 status 条件）
const statusTabs = computed(() => [{ label: t('common.all'), value: '' }, ...statusOptions.value])
const onTabChange = (value) => {
  if (status.value === value) return
  status.value = value
  search()
}

const onCheckStatusChange = (e) => {
  const opt = statusOptions.value[e.detail.value]
  if (opt) form.value.status = opt.value
}

const onAdd = () => {
  form.value = { studentId: userStore.user.id, status: '待审核' }
  formVisible.value = true
}

// 学生编辑：重置回待审核并清空审核说明（与 Web 端 handleEdit 一致）
const onEdit = (row) => {
  form.value = JSON.parse(JSON.stringify(row))
  form.value.status = '待审核'
  form.value.descr = ''
  formVisible.value = true
}

const handleCheck = (row) => {
  form.value = JSON.parse(JSON.stringify(row))
  checkVisible.value = true
}

// 审核提交：与 Web 端 check 一致，直接走 /apply/update
const check = () => {
  put('/apply/update', form.value).then((res) => {
    if (res.data && res.data.code === '200') {
      uni.showToast({ title: t('common.operationSuccess'), icon: 'success' })
      load(true)
      checkVisible.value = false
    } else {
      uni.showToast({ title: apiMessage(res.data), icon: 'none' })
    }
  })
}

// 撤销申请确认文案与 Web 端一致
const onWithdraw = (id) => {
  uni.showModal({
    title: t('common.confirmDeleteTitle'),
    content: t('pages.apply.deleteConfirm'),
    success: async (res) => {
      if (!res.confirm) return
      try {
        const r = await delRequest(`/apply/delete/${id}`)
        if (r.data && r.data.code === '200') {
          uni.showToast({ title: t('common.operationSuccess'), icon: 'success' })
          load(true)
        } else {
          uni.showToast({ title: apiMessage(r.data), icon: 'none' })
        }
      } catch {
        // 请求层已统一提示
      }
    },
  })
}

const onReset = () => {
  status.value = ''
  content.value = ''
  search()
}

onShow(() => {
  uni.setNavigationBarTitle({ title: t('menu.apply') })
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  load(true)
})

onReachBottom(() => loadNext())
</script>

<style lang="scss" scoped>
.field {
  width: 50%;
  margin-bottom: 8rpx;
}

.picker-display {
  display: flex;
  align-items: center;
}

.picker-placeholder {
  color: var(--xm-text-2);
}

/* 顶部状态筛选 Tab */
.status-tabs {
  display: flex;
  gap: 12rpx;
  padding: 16rpx 20rpx;
}

.status-tab {
  flex: 1;
  text-align: center;
  padding: 12rpx 0;
  border-radius: 10rpx;
  font-size: 26rpx;
  color: var(--xm-text-2);
  background: var(--xm-bg-hover);
}

.status-tab.active {
  background: var(--xm-brand);
  color: #ffffff;
  font-weight: 600;
}
</style>

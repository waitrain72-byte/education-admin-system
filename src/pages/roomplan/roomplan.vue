<template>
  <view
    class="xm-page"
    :class="themeClass"
  >
    <!-- 搜索区 -->
    <view
      class="xm-card xm-row"
      style="flex-wrap: wrap"
    >
      <input
        class="xm-input"
        style="flex: 1"
        v-model="name"
        :placeholder="$t('pages.roomplan.searchPlaceholder')"
      />
      <picker
        style="flex: 1"
        :range="statusLabels"
        @change="onSearchStatusChange"
      >
        <view class="xm-input">{{ status ? statusLabel(status) : $t('pages.roomplan.statusPlaceholder') }}</view>
      </picker>
      <button
        class="xm-btn xm-btn-primary"
        @click="search"
      >
        {{ $t('common.search') }}
      </button>
      <button
        class="xm-btn xm-btn-plain"
        @click="onReset"
      >
        {{ $t('common.reset') }}
      </button>
    </view>

    <!-- 操作区：仅管理员可新增/批量管理（与 Web 端 user.role === 'ADMIN' 一致） -->
    <view
      class="xm-card xm-row"
      v-if="canManage"
    >
      <button
        class="xm-btn xm-btn-primary"
        @click="onAdd"
      >
        {{ $t('common.add') }}
      </button>
      <button
        class="xm-btn xm-btn-plain"
        @click="toggleManage"
      >
        {{ manageMode ? $t('common.done') : $t('common.manage') }}
      </button>
      <button
        v-if="manageMode"
        class="xm-btn xm-btn-danger"
        @click="delBatch"
      >
        {{ $t('common.batchDelete') }}
      </button>
    </view>

    <!-- 列表 -->
    <view
      v-if="!list.length && !loading"
      class="xm-empty"
      >{{ $t('common.empty') }}</view
    >

    <view
      v-for="item in list"
      :key="item.id"
      class="xm-card"
    >
      <view class="xm-between">
        <view class="xm-row">
          <!-- 批量管理模式下显示勾选框 -->
          <checkbox
            v-if="manageMode"
            :checked="selectedIds.includes(item.id)"
            style="transform: scale(0.8)"
            @click.stop="toggleSelect(item.id)"
          />
          <view
            class="xm-value"
            style="font-weight: bold"
            >{{ item.name }}</view
          >
        </view>
        <view class="xm-label">ID: {{ item.id }}</view>
      </view>
      <view
        class="xm-row"
        style="margin-top: 8rpx"
      >
        <view class="xm-label">{{ $t('pages.roomplan.status') }}: {{ statusLabel(item.status) }}</view>
        <view class="xm-label">{{ $t('pages.roomplan.num') }}: {{ item.num }}</view>
      </view>
      <view
        class="xm-label"
        style="margin-top: 8rpx"
        >{{ $t('pages.roomplan.description') }}: {{ item.content }}</view
      >
      <view
        class="xm-actions"
        v-if="!manageMode && canManage"
      >
        <button
          class="xm-btn xm-btn-plain"
          @click="onEdit(item)"
        >
          {{ $t('common.edit') }}
        </button>
        <button
          class="xm-btn xm-btn-danger"
          @click="del(item.id)"
        >
          {{ $t('common.delete') }}
        </button>
      </view>
    </view>

    <view
      v-if="list.length"
      class="xm-empty"
      @click="loadNext"
    >
      {{ finished() ? $t('common.noMore') : $t('common.loadMore') }}
    </view>

    <!-- 新增/编辑表单（底部弹层） -->
    <view
      v-if="formVisible"
      class="xm-mask"
      @click="closeForm"
    ></view>

    <view
      v-if="formVisible"
      class="xm-popup"
    >
      <view class="xm-popup-title"
        >{{ form.id ? $t('common.edit') : $t('common.add') }} - {{ $t('pages.roomplan.dialogTitle') }}</view
      >
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.roomplan.name') }}</view>
        <input
          class="xm-input"
          v-model="form.name"
          :placeholder="$t('pages.roomplan.ruleNameRequired')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.roomplan.status') }}</view>
        <picker
          :range="statusLabels"
          :value="formStatusIndex"
          @change="onFormStatusChange"
        >
          <view class="xm-input">{{
            form.status ? statusLabel(form.status) : $t('pages.roomplan.statusPlaceholder')
          }}</view>
        </picker>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.roomplan.num') }}</view>
        <input
          class="xm-input"
          v-model="form.num"
          :placeholder="$t('pages.roomplan.ruleNumRequired')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.roomplan.contentLabel') }}</view>
        <textarea
          class="xm-textarea"
          v-model="form.content"
          :placeholder="$t('pages.roomplan.ruleContentRequired')"
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
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { useCrud } from '@/composables/useCrud'
import { t } from '@/i18n'

const userStore = useUserStore()
const name = ref('')
const status = ref('')
const manageMode = ref(false)
const canManage = computed(() => userStore.role === 'ADMIN')

// 数据库存的是中文枚举值（空闲/占用），只翻译显示 label
const statusValues = ['空闲', '占用']
const statusLabels = computed(() => [t('pages.roomplan.free'), t('pages.roomplan.occupied')])
const statusLabel = (value) => {
  const idx = statusValues.indexOf(value)
  return idx >= 0 ? statusLabels.value[idx] : value
}

const {
  list,
  total,
  loading,
  finished,
  form,
  formVisible,
  selectedIds,
  load,
  loadNext,
  search,
  handleAdd,
  handleEdit,
  closeForm,
  save,
  del,
  delBatch,
} = useCrud({
  url: '/roomplan',
  getParams: () => ({ name: name.value, status: status.value }),
  validate: (f) => {
    if (!f.name) return t('pages.roomplan.ruleNameRequired')
    if (!f.status) return t('pages.roomplan.ruleStatusRequired')
    if (!f.num) return t('pages.roomplan.ruleNumRequired')
    if (!f.content) return t('pages.roomplan.ruleContentRequired')
    return ''
  },
})

const onSearchStatusChange = (e) => {
  status.value = statusValues[Number(e.detail.value)] || ''
}
const formStatusIndex = computed(() =>
  statusValues.indexOf(form.value.status) >= 0 ? statusValues.indexOf(form.value.status) : 0,
)
const onFormStatusChange = (e) => {
  form.value.status = statusValues[Number(e.detail.value)] || ''
}

const toggleManage = () => {
  manageMode.value = !manageMode.value
  if (!manageMode.value) selectedIds.value = []
}

const toggleSelect = (id) => {
  const idx = selectedIds.value.indexOf(id)
  if (idx >= 0) selectedIds.value.splice(idx, 1)
  else selectedIds.value.push(id)
}

const onAdd = () => handleAdd({})
const onEdit = (row) => handleEdit(row)
const onReset = () => {
  name.value = ''
  status.value = ''
  search()
}

// 页面入口：所有角色可见，仅管理员可管理（与 Web 端逻辑一致）
onShow(() => {
  uni.setNavigationBarTitle({ title: t('menu.roomplan') })
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  load(true)
})

onReachBottom(() => loadNext())
</script>

<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 搜索区 -->
    <view class="xm-card xm-row">
      <input
        class="xm-input"
        style="flex: 1"
        v-model="name"
        :placeholder="$t('pages.examplan.searchPlaceholder')"
      />
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
    <xm-empty
      v-if="!list.length && !loading"
      :action-text="$t('common.reload')"
      @action="load(true)"
    />

    <view
      v-for="item in list"
      :key="item.id"
      class="xm-card"
      @click="toggleExpand(item.id)"
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
        <view class="xm-label">{{ $t('pages.examplan.id') }}: {{ item._index }}</view>
      </view>
      <view
        v-if="item.time"
        class="xm-label"
        style="margin-top: 8rpx"
        >{{ $t('pages.examplan.time') }}: {{ item.time }}</view
      >
      <view
        class="xm-label"
        style="margin-top: 8rpx"
        :style="
          expandedIds.includes(item.id)
            ? ''
            : 'display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden'
        "
        >{{ item.content }}</view
      >
      <view
        class="xm-actions"
        v-if="!manageMode && canManage"
      >
        <button
          class="xm-btn xm-btn-plain"
          @click.stop="onEdit(item)"
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

    <xm-list-footer
      :visible="!!list.length"
      :loading="loading"
      :finished="finished()"
      @load-more="loadNext"
    />

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
        >{{ form.id ? $t('common.edit') : $t('common.add') }} - {{ $t('pages.examplan.dialogTitle') }}</view
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
        <view class="xm-form-label">{{ $t('pages.examplan.time') }}</view>
        <picker
          mode="date"
          :value="form.time"
          @change="onTimeChange"
        >
          <view
            class="xm-input picker-display"
            :class="{ 'picker-placeholder': !form.time }"
          >
            {{ form.time || $t('pages.examplan.timePlaceholder') }}
          </view>
        </picker>
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
    <xm-loader />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { ensureLoggedIn } from '@/utils/authGuard'
import { useCrud } from '@/composables/useCrud'
import { t } from '@/i18n'

const userStore = useUserStore()
const name = ref('')
const manageMode = ref(false)
const expandedIds = ref([])
const canManage = computed(() => userStore.role === 'ADMIN')

const {
  list,
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
  url: '/examplan',
  getParams: () => ({ name: name.value }),
  validate: (f) => {
    if (!f.name) return t('pages.examplan.ruleTitleRequired')
    if (!f.content) return t('pages.examplan.ruleContentRequired')
    return ''
  },
})

const toggleManage = () => {
  manageMode.value = !manageMode.value
  if (!manageMode.value) selectedIds.value = []
}

const toggleSelect = (id) => {
  const idx = selectedIds.value.indexOf(id)
  if (idx >= 0) selectedIds.value.splice(idx, 1)
  else selectedIds.value.push(id)
}

const toggleExpand = (id) => {
  const idx = expandedIds.value.indexOf(id)
  if (idx >= 0) expandedIds.value.splice(idx, 1)
  else expandedIds.value.push(id)
}

// 考试日期：日期选择器（YYYY-MM-DD，后端仍按字符串存储）
const onTimeChange = (e) => {
  form.value.time = e.detail.value
}

const onAdd = () => handleAdd({})
const onEdit = (row) => handleEdit(row)
const onReset = () => {
  name.value = ''
  search()
}

// 页面入口：所有角色可见，仅管理员可管理（与 Web 端逻辑一致）
onShow(() => {
  uni.setNavigationBarTitle({ title: t('menu.examplan') })
  if (!ensureLoggedIn()) return
  load(true)
})

onReachBottom(() => loadNext())
</script>

<style lang="scss" scoped>
.picker-display {
  display: flex;
  align-items: center;
}

.picker-placeholder {
  color: var(--xm-text-2);
}
</style>

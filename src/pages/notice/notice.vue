<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 搜索区 -->
    <xm-search-card
      v-model="query.title"
      :placeholder="$t('pages.notice.searchPlaceholder')"
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
            <text class="xm-card-name xm-ellipsis">{{ item.title }}</text>
          </view>
          <text class="xm-card-no">#{{ item._index }}</text>
        </view>
        <view class="xm-meta">
          <view class="xm-meta-item">
            <xm-icon
              name="clock"
              :size="28"
            />
            <text class="xm-meta-text">{{ item.time || '-' }}</text>
          </view>
          <view class="xm-meta-item">
            <xm-icon
              name="user"
              :size="28"
            />
            <text class="xm-meta-text">{{ item.user || '-' }}</text>
          </view>
        </view>
        <!-- 正文默认两行，点卡片展开全文 -->
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
      :title="$t(form.id ? 'common.editTitle' : 'common.addTitle', { name: $t('pages.notice.entity') })"
      @close="closeForm"
      @save="save"
    >
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.notice.title') }}</view>
        <input
          class="xm-input"
          v-model="form.title"
          :placeholder="$t('pages.notice.ruleTitleRequired')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.notice.content') }}</view>
        <textarea
          class="xm-textarea"
          v-model="form.content"
          :placeholder="$t('pages.notice.ruleContentRequired')"
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
import { noticeApi } from '@/api'
import { t } from '@/i18n'

const userStore = useUserStore()
// 所有角色可看，仅管理员可管理（与 Web 端 user.role === 'ADMIN' 一致）
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
  api: noticeApi,
  title: 'menu.notice',
  query: { title: '' },
  validate: (f) => {
    if (!f.title) return t('pages.notice.ruleTitleRequired')
    if (!f.content) return t('pages.notice.ruleContentRequired')
    return ''
  },
})

// 正文默认两行，点卡片展开 / 收起全文
const expandedIds = ref([])
const toggleExpand = (id) => {
  const idx = expandedIds.value.indexOf(id)
  if (idx >= 0) expandedIds.value.splice(idx, 1)
  else expandedIds.value.push(id)
}
</script>

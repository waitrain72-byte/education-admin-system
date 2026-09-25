<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 搜索区：第一行 教室名 + 查询/重置；第二行 状态筛选独占整行（不再挤压换行） -->
    <view
      class="xm-card xm-row"
      style="flex-wrap: wrap"
    >
      <input
        class="xm-input"
        style="flex: 1 1 0; min-width: 0"
        v-model="query.name"
        :placeholder="$t('pages.roomplan.searchPlaceholder')"
        confirm-type="search"
        @confirm="search"
      />
      <button
        class="xm-btn xm-btn-primary"
        style="flex-shrink: 0"
        @click="search"
      >
        {{ $t('common.search') }}
      </button>
      <button
        class="xm-btn xm-btn-plain"
        style="flex-shrink: 0"
        @click="resetQuery"
      >
        {{ $t('common.reset') }}
      </button>
      <view class="search-status">
        <xm-picker
          v-model="query.status"
          :options="enumOptions('roomStatus')"
          :placeholder="$t('pages.roomplan.statusPlaceholder')"
        />
      </view>
    </view>

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
      >
        <!-- 标题行：名称 + 空闲/占用；标签行：编号 / 类型 / 容量；说明最多两行 -->
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
          <text
            v-if="item.status"
            class="xm-tag"
            :class="enumTag('roomStatus', item.status)"
            >{{ enumLabel('roomStatus', item.status) }}</text
          >
        </view>
        <view class="xm-tags">
          <text
            v-if="item.code"
            class="xm-tag xm-tag-brand"
            >{{ item.code }}</text
          >
          <text
            v-if="item.type"
            class="xm-tag"
            >{{ enumLabel('roomType', item.type) }}</text
          >
          <text
            v-if="item.num"
            class="xm-tag"
            >{{ $t('pages.course.capacity', { n: item.num }) }}</text
          >
          <text class="xm-tags-end">#{{ item._index }}</text>
        </view>
        <view
          v-if="item.content"
          class="xm-card-desc xm-clamp-2"
          >{{ item.content }}</view
        >
        <view
          class="xm-actions"
          v-if="!manageMode && canManage"
        >
          <button
            class="xm-btn xm-btn-plain"
            @click="handleEdit(item)"
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
      :title="$t(form.id ? 'common.editTitle' : 'common.addTitle', { name: $t('pages.roomplan.entity') })"
      @close="closeForm"
      @save="save"
    >
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.roomplan.code') }}</view>
        <input
          class="xm-input"
          v-model="form.code"
          :placeholder="$t('pages.roomplan.codePlaceholder')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.roomplan.type') }}</view>
        <xm-picker
          v-model="form.type"
          :options="enumOptions('roomType')"
          :placeholder="$t('pages.roomplan.typePlaceholder')"
        />
      </view>
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
        <xm-picker
          v-model="form.status"
          :options="enumOptions('roomStatus')"
          :placeholder="$t('pages.roomplan.statusPlaceholder')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.roomplan.num') }}</view>
        <input
          class="xm-input"
          v-model="form.num"
          type="number"
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
    </xm-form-popup>
    <xm-loader />
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { useListPage } from '@/composables/useListPage'
import { roomplanApi } from '@/api'
import { t } from '@/i18n'
import { enumLabel, enumTag, enumOptions } from '@/utils/enums'

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
  api: roomplanApi,
  title: 'menu.roomplan',
  query: { name: '', status: '' },
  validate: (f) => {
    if (!f.code) return t('pages.roomplan.ruleCodeRequired')
    if (!f.name) return t('pages.roomplan.ruleNameRequired')
    if (!f.type) return t('pages.roomplan.ruleTypeRequired')
    if (!f.status) return t('pages.roomplan.ruleStatusRequired')
    if (!f.num) return t('pages.roomplan.ruleNumRequired')
    if (!f.content) return t('pages.roomplan.ruleContentRequired')
    return ''
  },
})
</script>

<style lang="scss" scoped>
/* 状态筛选在搜索卡第二行独占整行（第一行是教室名 + 查询 / 重置） */
.search-status {
  flex: 1 1 100%;
}
</style>

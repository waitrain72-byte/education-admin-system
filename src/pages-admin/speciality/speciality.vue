<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 搜索区 -->
    <xm-search-card
      v-model="query.name"
      :placeholder="$t('pages.speciality.searchPlaceholder')"
      @search="search"
      @reset="resetQuery"
    />

    <!-- 操作区 -->
    <xm-action-bar
      :manage-mode="manageMode"
      :total="total"
      :selected-count="selectedIds.length"
      @add="onAdd"
      @toggle-manage="toggleManage"
      @del-batch="delBatch"
    />

    <!-- 列表 -->
    <xm-empty
      v-if="!list.length && !loading"
      :action-text="$t('common.reload')"
      @action="search"
    />

    <!-- 列表：手机单列，平板 ≥720px 两列、≥1248px 三列（theme.scss .xm-list） -->
    <view class="xm-list">
      <view
        v-for="item in list"
        :key="item.id"
        class="xm-card"
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
        <view class="xm-meta">
          <view class="xm-meta-item">
            <xm-icon
              name="landmark"
              :size="28"
            />
            <text class="xm-meta-text">{{ item.collegeName || '-' }}</text>
          </view>
          <view
            v-if="item.score != null && item.score !== ''"
            class="xm-meta-item"
          >
            <xm-icon
              name="award"
              :size="28"
            />
            <text class="xm-meta-text">{{ $t('pages.course.credit', { n: item.score }) }}</text>
          </view>
        </view>
        <view
          v-if="item.content"
          class="xm-card-desc xm-clamp-2"
          >{{ item.content }}</view
        >
        <view
          class="xm-actions"
          v-if="!manageMode"
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
      :title="$t(form.id ? 'common.editTitle' : 'common.addTitle', { name: $t('pages.speciality.entity') })"
      @close="closeForm"
      @save="save"
    >
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.speciality.name') }}</view>
        <input
          class="xm-input"
          v-model="form.name"
          :placeholder="$t('pages.speciality.ruleNameRequired')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.speciality.content') }}</view>
        <textarea
          class="xm-textarea"
          v-model="form.content"
          :placeholder="$t('pages.speciality.content')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.speciality.college') }}</view>
        <xm-picker
          v-model="form.collegeId"
          :options="collegeData"
          label-key="name"
          value-key="id"
          :placeholder="$t('pages.speciality.collegePlaceholder')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.speciality.score') }}</view>
        <input
          class="xm-input"
          v-model="form.score"
          type="digit"
          :placeholder="$t('pages.speciality.score')"
        />
      </view>
    </xm-form-popup>

    <xm-loader />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { useListPage } from '@/composables/useListPage'
import { specialityApi, collegeApi } from '@/api'
import { orNull } from '@/utils/request'
import { t } from '@/i18n'

// 上级学院下拉（接口与 Web 端一致），打开表单时加载
const collegeData = ref([])

// 仅管理员可见（与 Web 端路由 meta.roles 一致）
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
  api: specialityApi,
  title: 'menu.speciality',
  roles: ['ADMIN'],
  query: { name: '' },
  validate: (f) => (f.name ? '' : t('pages.speciality.ruleNameRequired')),
})

const loadCollege = async () => {
  collegeData.value = (await orNull(collegeApi.selectAll())) || []
}

const onAdd = () => {
  loadCollege()
  handleAdd()
}

const onEdit = (row) => {
  loadCollege()
  handleEdit(row)
}
</script>

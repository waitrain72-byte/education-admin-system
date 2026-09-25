<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 搜索区 -->
    <xm-search-card
      v-model="query.username"
      :placeholder="$t('pages.admin.searchPlaceholder')"
      @search="search"
      @reset="resetQuery"
    />

    <!-- 操作区：新增 / 批量管理 -->
    <xm-action-bar
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
            <image
              v-if="item.avatar"
              lazy-load
              :src="resolveFileUrl(item.avatar)"
              class="xm-avatar"
              mode="aspectFill"
            />
            <view
              v-else
              class="xm-avatar xm-avatar-text"
              >{{ (item.name || item.username || '?').slice(0, 1) }}</view
            >
            <view class="xm-card-head">
              <view class="xm-card-name xm-ellipsis">{{ item.name || item.username }}</view>
              <view class="xm-card-sub xm-ellipsis">{{ item.username }}</view>
            </view>
          </view>
          <text class="xm-card-no">#{{ item._index }}</text>
        </view>
        <view class="xm-meta">
          <view class="xm-meta-item">
            <xm-icon
              name="phone"
              :size="28"
            />
            <text class="xm-meta-text">{{ item.phone || '-' }}</text>
          </view>
          <view class="xm-meta-item">
            <xm-icon
              name="mail"
              :size="28"
            />
            <text class="xm-meta-text">{{ item.email || '-' }}</text>
          </view>
        </view>
        <view
          class="xm-actions"
          v-if="!manageMode"
        >
          <button
            class="xm-btn xm-btn-plain"
            @click="handleEdit(item)"
          >
            {{ $t('common.edit') }}
          </button>
          <button
            class="xm-btn xm-btn-plain"
            :disabled="item.id === userStore.user.id"
            @click="resetPassword(item)"
          >
            {{ $t('common.resetPassword') }}
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
      :title="$t(form.id ? 'common.editTitle' : 'common.addTitle', { name: $t('pages.admin.entity') })"
      @close="closeForm"
      @save="save"
    >
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.admin.username') }}</view>
        <input
          class="xm-input"
          v-model="form.username"
          :placeholder="$t('pages.admin.username')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.admin.name') }}</view>
        <input
          class="xm-input"
          v-model="form.name"
          :placeholder="$t('pages.admin.name')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.admin.phone') }}</view>
        <input
          class="xm-input"
          v-model="form.phone"
          type="number"
          maxlength="11"
          :placeholder="$t('pages.admin.phone')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.admin.email') }}</view>
        <input
          class="xm-input"
          v-model="form.email"
          :placeholder="$t('pages.admin.email')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.admin.avatar') }}</view>
        <view
          class="xm-row"
          v-if="form.avatar"
        >
          <image
            :src="resolveFileUrl(form.avatar)"
            class="xm-avatar"
            mode="aspectFill"
          />
        </view>
        <button
          class="xm-btn xm-btn-primary"
          @click="uploadAvatar"
        >
          {{ $t('pages.admin.uploadAvatar') }}
        </button>
      </view>
    </xm-form-popup>

    <xm-loader />
  </view>
</template>

<script setup>
import { useUserStore } from '@/stores/user'
import { useListPage } from '@/composables/useListPage'
import { adminApi } from '@/api'
import { useResetPassword } from '@/composables/useResetPassword'
import { useAvatarUpload, syncCurrentUser } from '@/composables/useUserForm'
import { resolveFileUrl } from '@/utils/request'
import { t } from '@/i18n'

const userStore = useUserStore()

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
  api: adminApi,
  title: 'menu.admin',
  roles: ['ADMIN'],
  query: { username: '' },
  validate: (f) => (f.username ? '' : t('pages.admin.ruleUsernameRequired')),
  // 改的是当前登录账号自己时，同步头像 / 姓名 / 联系方式
  afterSave: syncCurrentUser('ADMIN', ['avatar', 'name', 'phone', 'email']),
})

// 重置为默认密码（自己不可重置，按钮已禁用，与 Web 端一致）
const resetPassword = useResetPassword(adminApi, 'admin')
const uploadAvatar = useAvatarUpload(form)
</script>

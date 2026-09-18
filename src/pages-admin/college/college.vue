<template>
  <view
    class="xm-page"
    :class="themeClass"
  >
    <!-- 搜索区 -->
    <xm-search-card
      v-model="keyword"
      :placeholder="$t('pages.college.namePlaceholder')"
      @search="search"
      @reset="onReset"
    />

    <!-- 操作区：新增 / 批量管理 -->
    <xm-action-bar
      :manage-mode="manageMode"
      @add="onAdd"
      @toggle-manage="toggleManage"
      @del-batch="delBatch"
    />

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
        <view class="xm-label">{{ $t('pages.college.id') }}: {{ item._index }}</view>
      </view>
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
      :title="(form.id ? $t('common.edit') : $t('common.add')) + ' - ' + $t('pages.college.dialogTitle')"
      @close="closeForm"
      @save="save"
    >
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.college.nameLabel') }}</view>
        <input
          class="xm-input"
          v-model="form.name"
          :placeholder="$t('pages.college.namePlaceholder')"
        />
      </view>
    </xm-form-popup>

    <xm-loader />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { useCrud } from '@/composables/useCrud'
import { useManage } from '@/composables/useManage'
import { t } from '@/i18n'

const userStore = useUserStore()
const keyword = ref('')

const {
  list,
  loading,
  saving,
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
  url: '/college',
  getParams: () => ({ name: keyword.value }),
  validate: (f) => {
    if (!f.name) return t('pages.college.ruleNameRequired')
    return ''
  },
})

const { manageMode, toggleManage, toggleSelect } = useManage(selectedIds)

const onAdd = () => handleAdd({})
const onEdit = (row) => handleEdit(row)
const onReset = () => {
  keyword.value = ''
  search()
}

// 页面入口：仅管理员可见（与 Web 端路由 meta.roles 一致）
onShow(() => {
  uni.setNavigationBarTitle({ title: t('menu.college') })
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  if (!['ADMIN'].includes(userStore.role)) {
    uni.showToast({ title: t('forbidden.message'), icon: 'none' })
    setTimeout(() => uni.navigateBack(), 800)
    return
  }
  load(true)
})

onReachBottom(() => loadNext())
</script>

<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 搜索区 -->
    <xm-search-card
      v-model="name"
      :placeholder="$t('pages.speciality.searchPlaceholder')"
      @search="search"
      @reset="onReset"
    />

    <!-- 操作区 -->
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
        <view class="xm-label">{{ $t('pages.speciality.id') }}: {{ item._index }}</view>
      </view>
      <view
        class="xm-label"
        style="margin-top: 8rpx"
        >{{ $t('pages.speciality.content') }}: {{ item.content }}</view
      >
      <view
        class="xm-row"
        style="margin-top: 8rpx"
      >
        <view class="xm-label">{{ $t('pages.speciality.college') }}: {{ item.collegeName }}</view>
        <view class="xm-label">{{ $t('pages.speciality.score') }}: {{ item.score }}</view>
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
      :title="(form.id ? $t('common.edit') : $t('common.add')) + ' - ' + $t('pages.speciality.dialogTitle')"
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
        <picker
          :range="collegeLabels"
          :value="collegeIndex"
          @change="onCollegeChange"
        >
          <view class="xm-input">{{ collegeName(form.collegeId) || $t('pages.speciality.collegePlaceholder') }}</view>
        </picker>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.speciality.score') }}</view>
        <input
          class="xm-input"
          v-model="form.score"
          :placeholder="$t('pages.speciality.score')"
        />
      </view>
    </xm-form-popup>

    <xm-loader />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { ensureLoggedIn } from '@/utils/authGuard'
import { useCrud } from '@/composables/useCrud'
import { useManage } from '@/composables/useManage'
import { get } from '@/utils/request'
import { t } from '@/i18n'

const userStore = useUserStore()
const name = ref('')
const collegeData = ref([])

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
  url: '/speciality',
  getParams: () => ({ name: name.value }),
  validate: (f) => {
    if (!f.name) return t('pages.speciality.ruleNameRequired')
    return ''
  },
})

const { manageMode, toggleManage, toggleSelect } = useManage(selectedIds)

// 级联下拉：上级学院列表（接口与 Web 端一致），表单打开时加载
const loadCollege = async () => {
  try {
    collegeData.value = (await get('/college/selectAll')) || []
  } catch {
    // 请求层已统一提示
  }
}

const collegeLabels = computed(() => collegeData.value.map((item) => item.name))
const collegeIndex = computed(() => {
  const idx = collegeData.value.findIndex((item) => item.id === form.value.collegeId)
  return idx >= 0 ? idx : 0
})
const collegeName = (id) => {
  const item = collegeData.value.find((c) => c.id === id)
  return item ? item.name : ''
}
const onCollegeChange = (e) => {
  const item = collegeData.value[Number(e.detail.value)]
  if (item) form.value.collegeId = item.id
}

const onAdd = () => {
  loadCollege()
  handleAdd({})
}
const onEdit = (row) => {
  loadCollege()
  handleEdit(row)
}
const onReset = () => {
  name.value = ''
  search()
}

// 页面入口：仅管理员可见（与 Web 端路由 meta.roles 一致）
onShow(() => {
  uni.setNavigationBarTitle({ title: t('menu.speciality') })
  if (!ensureLoggedIn()) return
  if (!['ADMIN'].includes(userStore.role)) {
    uni.showToast({ title: t('errors.403'), icon: 'none' })
    setTimeout(() => uni.navigateBack(), 800)
    return
  }
  load(true)
})

onReachBottom(() => loadNext())
</script>

<template>
  <view
    class="xm-page"
    :class="themeClass"
  >
    <!-- 搜索区 -->
    <view class="xm-card xm-row">
      <input
        class="xm-input"
        style="flex: 1"
        v-model="name"
        :placeholder="$t('pages.speciality.searchPlaceholder')"
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

    <!-- 操作区 -->
    <view class="xm-card xm-row">
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
        >{{ form.id ? $t('common.edit') : $t('common.add') }} - {{ $t('pages.speciality.dialogTitle') }}</view
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
import { get } from '@/utils/request'
import { apiMessage, t } from '@/i18n'

const userStore = useUserStore()
const name = ref('')
const manageMode = ref(false)
const collegeData = ref([])

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
  url: '/speciality',
  getParams: () => ({ name: name.value }),
  validate: (f) => {
    if (!f.name) return t('pages.speciality.ruleNameRequired')
    return ''
  },
})

// 级联下拉：上级学院列表（接口与 Web 端一致），表单打开时加载
const loadCollege = async () => {
  try {
    const res = await get('/college/selectAll')
    if (res.data && res.data.code === '200') {
      collegeData.value = res.data.data || []
    } else {
      uni.showToast({ title: apiMessage(res.data), icon: 'none' })
    }
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

const toggleManage = () => {
  manageMode.value = !manageMode.value
  if (!manageMode.value) selectedIds.value = []
}

const toggleSelect = (id) => {
  const idx = selectedIds.value.indexOf(id)
  if (idx >= 0) selectedIds.value.splice(idx, 1)
  else selectedIds.value.push(id)
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
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  if (!['ADMIN'].includes(userStore.role)) {
    uni.showToast({ title: t('errors.403'), icon: 'none' })
    setTimeout(() => uni.navigateBack(), 800)
    return
  }
  load(true)
})

onReachBottom(() => loadNext())
</script>

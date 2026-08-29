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
        v-model="keyword"
        :placeholder="$t('pages.student.searchPlaceholder')"
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

    <!-- 操作区：新增 / 批量管理 -->
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
          <image
            v-if="item.avatar"
            :src="item.avatar"
            class="xm-avatar"
            mode="aspectFill"
          />
          <view
            class="xm-value"
            style="font-weight: bold"
            >{{ item.username }}</view
          >
        </view>
        <view class="xm-label">ID: {{ item.id }}</view>
      </view>
      <view class="xm-label">{{ $t('pages.student.name') }}: {{ item.name }}</view>
      <view class="xm-label">{{ $t('pages.student.role') }}: {{ item.role }}</view>
      <view class="xm-label">{{ $t('pages.student.college') }}: {{ item.collegeName }}</view>
      <view class="xm-label">{{ $t('pages.student.speciality') }}: {{ item.specialityName }}</view>
      <view class="xm-label">{{ $t('pages.student.classes') }}: {{ item.className }}</view>
      <view class="xm-label">{{ $t('pages.student.score') }}: {{ item.score }}</view>
      <view
        class="xm-actions"
        v-if="!manageMode"
      >
        <button
          v-if="isAdminOrTeacher"
          class="xm-btn xm-btn-plain"
          @click="onEdit(item)"
        >
          {{ $t('common.edit') }}
        </button>
        <button
          v-if="isAdmin"
          class="xm-btn xm-btn-plain"
          @click="resetPassword(item)"
        >
          {{ $t('common.resetPassword') }}
        </button>
        <button
          v-if="isAdminOrTeacher"
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
        >{{ form.id ? $t('common.edit') : $t('common.add') }} - {{ $t('pages.student.dialogTitle') }}</view
      >
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.student.avatar') }}</view>
        <view
          class="xm-row"
          v-if="form.avatar"
        >
          <image
            :src="form.avatar"
            class="xm-avatar"
            mode="aspectFill"
          />
        </view>
        <button
          class="xm-btn xm-btn-primary"
          @click="uploadAvatar"
        >
          {{ $t('pages.student.uploadAvatar') }}
        </button>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.student.username') }}</view>
        <input
          class="xm-input"
          v-model="form.username"
          :placeholder="$t('pages.student.username')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.student.name') }}</view>
        <input
          class="xm-input"
          v-model="form.name"
          :placeholder="$t('pages.student.name')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.student.college') }}</view>
        <picker
          :range="collegeLabels"
          @change="onCollegeChange"
        >
          <view
            class="xm-input picker-text"
            :class="{ 'picker-placeholder': form.collegeId == null }"
          >
            {{ form.collegeId != null ? collegeLabels[collegeIndex] : $t('pages.student.collegePlaceholder') }}
          </view>
        </picker>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.student.speciality') }}</view>
        <picker
          :range="specialityLabels"
          @change="onSpecialityChange"
        >
          <view
            class="xm-input picker-text"
            :class="{ 'picker-placeholder': form.specialityId == null }"
          >
            {{
              form.specialityId != null ? specialityLabels[specialityIndex] : $t('pages.student.specialityPlaceholder')
            }}
          </view>
        </picker>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.student.classes') }}</view>
        <picker
          :range="classesLabels"
          @change="onClassesChange"
        >
          <view
            class="xm-input picker-text"
            :class="{ 'picker-placeholder': form.classId == null }"
          >
            {{ form.classId != null ? classesLabels[classesIndex] : $t('pages.student.classesPlaceholder') }}
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
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { useCrud } from '@/composables/useCrud'
import { get, put } from '@/utils/request'
import { baseUrl } from '@/utils/config'
import { t, apiMessage } from '@/i18n'

const userStore = useUserStore()
const keyword = ref('')
const manageMode = ref(false)
const collegeData = ref([])
const specialityData = ref([])
const classesData = ref([])

const isAdmin = computed(() => userStore.role === 'ADMIN')
// Web 端 Student.vue 的编辑/删除按钮为 v-permission="['ADMIN', 'TEACHER']"
const isAdminOrTeacher = computed(() => userStore.role === 'ADMIN' || userStore.role === 'TEACHER')

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
  url: '/student',
  getParams: () => ({ username: keyword.value }),
  validate: (f) => {
    if (!f.username) return t('pages.student.ruleUsernameRequired')
    return ''
  },
  afterSave: (formData) => {
    // 如果修改的是当前登录学生自己的信息，同步全局状态
    if (formData.id === userStore.user.id) {
      userStore.patchUser({
        avatar: formData.avatar,
        name: formData.name,
      })
    }
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

const onAdd = () => handleAdd({})
const onEdit = (row) => handleEdit(row)
const onReset = () => {
  keyword.value = ''
  search()
}

// 学院/专业/班级下拉数据
const loadCollege = () => {
  get('/college/selectAll').then((res) => {
    if (res.data && res.data.code === '200') {
      collegeData.value = res.data.data || []
    } else {
      uni.showToast({ title: apiMessage(res.data), icon: 'none' })
    }
  })
}

const loadSpeciality = () => {
  get('/speciality/selectAll').then((res) => {
    if (res.data && res.data.code === '200') {
      specialityData.value = res.data.data || []
    } else {
      uni.showToast({ title: apiMessage(res.data), icon: 'none' })
    }
  })
}

const loadClasses = () => {
  get('/classes/selectAll').then((res) => {
    if (res.data && res.data.code === '200') {
      classesData.value = res.data.data || []
    } else {
      uni.showToast({ title: apiMessage(res.data), icon: 'none' })
    }
  })
}

const collegeLabels = computed(() => collegeData.value.map((i) => i.name))
const collegeIndex = computed(() => collegeData.value.findIndex((i) => i.id === form.value.collegeId))
const onCollegeChange = (e) => {
  const item = collegeData.value[Number(e.detail.value)]
  if (item) form.value.collegeId = item.id
}

const specialityLabels = computed(() => specialityData.value.map((i) => i.name))
const specialityIndex = computed(() => specialityData.value.findIndex((i) => i.id === form.value.specialityId))
const onSpecialityChange = (e) => {
  const item = specialityData.value[Number(e.detail.value)]
  if (item) form.value.specialityId = item.id
}

const classesLabels = computed(() => classesData.value.map((i) => i.name))
const classesIndex = computed(() => classesData.value.findIndex((i) => i.id === form.value.classId))
const onClassesChange = (e) => {
  const item = classesData.value[Number(e.detail.value)]
  if (item) form.value.classId = item.id
}

// 重置密码为 123456
const resetPassword = (row) => {
  uni.showModal({
    title: t('common.resetPassword'),
    content: t('pages.student.resetConfirm', { username: row.username }),
    success: async (res) => {
      if (!res.confirm) return
      try {
        const r = await put('/student/resetPassword/' + row.id)
        if (r.data && r.data.code === '200') {
          uni.showToast({ title: t('pages.student.resetSuccess'), icon: 'none' })
        } else {
          uni.showToast({ title: apiMessage(r.data), icon: 'none' })
        }
      } catch {
        // 请求层已统一提示
      }
    },
  })
}

// 上传头像（与 Web 端 el-upload + /files/upload 一致）
const uploadAvatar = () => {
  uni.chooseImage({
    count: 1,
    success: (res) => {
      uni.uploadFile({
        url: `${baseUrl}/files/upload`,
        filePath: res.tempFilePaths[0],
        name: 'file',
        header: { token: userStore.token },
        success: (up) => {
          try {
            const data = typeof up.data === 'string' ? JSON.parse(up.data) : up.data
            if (data.code === '200') {
              form.value.avatar = data.data
            } else {
              uni.showToast({ title: apiMessage(data), icon: 'none' })
            }
          } catch {
            uni.showToast({ title: t('request.failed'), icon: 'none' })
          }
        },
        fail: () => {
          uni.showToast({ title: t('request.failed'), icon: 'none' })
        },
      })
    },
  })
}

// 页面入口：仅管理员可见（与 Web 端路由 meta.roles 一致）
onShow(() => {
  uni.setNavigationBarTitle({ title: t('menu.student') })
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
  loadCollege()
  loadSpeciality()
  loadClasses()
})

onReachBottom(() => loadNext())
</script>

<style lang="scss" scoped>
.xm-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: var(--xm-bg-input);
  flex-shrink: 0;
}

.picker-text {
  display: flex;
  align-items: center;
  line-height: 76rpx;
}

.picker-placeholder {
  color: var(--xm-text-2);
}
</style>

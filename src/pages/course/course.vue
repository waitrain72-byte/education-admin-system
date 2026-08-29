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
        :placeholder="$t('pages.course.searchPlaceholder')"
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

    <!-- 操作区：新增 / 批量管理（仅管理员） -->
    <view
      class="xm-card xm-row"
      v-if="isAdmin"
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
            v-if="isAdmin && manageMode"
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
      <view class="xm-label">{{ $t('pages.course.type') }}: {{ item.type }}</view>
      <view class="xm-label">{{ $t('pages.course.teacher') }}: {{ item.teacherName }}</view>
      <view class="xm-label">{{ $t('pages.course.score') }}: {{ item.score }}</view>
      <view class="xm-label">{{ $t('pages.course.num') }}: {{ item.num }}</view>
      <view class="xm-label">{{ $t('pages.course.room') }}: {{ item.room }}</view>
      <view class="xm-label">{{ $t('pages.course.week') }}: {{ item.week }}</view>
      <view class="xm-label">{{ $t('pages.course.segment') }}: {{ item.segment }}</view>
      <view class="xm-label">{{ $t('pages.course.status') }}: {{ item.status }}</view>
      <view
        class="xm-actions"
        v-if="!manageMode"
      >
        <button
          v-if="userStore.role !== 'STUDENT'"
          class="xm-btn xm-btn-plain"
          @click="onEdit(item)"
        >
          {{ $t('common.edit') }}
        </button>
        <button
          v-if="userStore.role === 'STUDENT'"
          class="xm-btn xm-btn-primary"
          :disabled="item.status !== '未开课'"
          @click="choiceCourse(item)"
        >
          {{ $t('pages.course.choice') }}
        </button>
        <button
          v-if="isAdmin"
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
        >{{ form.id ? $t('common.edit') : $t('common.add') }} - {{ $t('pages.course.dialogTitle') }}</view
      >
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.course.name') }}</view>
        <input
          class="xm-input"
          v-model="form.name"
          :disabled="!isAdmin"
          :placeholder="$t('pages.course.name')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.course.type') }}</view>
        <picker
          :range="typeLabels"
          :disabled="!isAdmin"
          @change="onTypeChange"
        >
          <view
            class="xm-input picker-text"
            :class="{ 'picker-placeholder': !form.type }"
          >
            {{ form.type ? typeLabels[typeIndex] : $t('pages.course.typePlaceholder') }}
          </view>
        </picker>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.course.teacher') }}</view>
        <picker
          :range="teacherLabels"
          :disabled="!isAdmin"
          @change="onTeacherChange"
        >
          <view
            class="xm-input picker-text"
            :class="{ 'picker-placeholder': form.teacherId == null }"
          >
            {{ form.teacherId != null ? teacherLabels[teacherIndex] : $t('pages.course.teacherPlaceholder') }}
          </view>
        </picker>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.course.score') }}</view>
        <input
          class="xm-input"
          v-model="form.score"
          :disabled="!isAdmin"
          :placeholder="$t('pages.course.score')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.course.num') }}</view>
        <input
          class="xm-input"
          v-model="form.num"
          :disabled="!isAdmin"
          :placeholder="$t('pages.course.num')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.course.room') }}</view>
        <input
          class="xm-input"
          v-model="form.room"
          :disabled="!isAdmin"
          :placeholder="$t('pages.course.room')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.course.week') }}</view>
        <picker
          :range="weekLabels"
          :disabled="!isAdmin"
          @change="onWeekChange"
        >
          <view
            class="xm-input picker-text"
            :class="{ 'picker-placeholder': !form.week }"
          >
            {{ form.week ? weekLabels[weekIndex] : $t('pages.course.selectPlaceholder') }}
          </view>
        </picker>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.course.segment') }}</view>
        <picker
          :range="segmentLabels"
          :disabled="!isAdmin"
          @change="onSegmentChange"
        >
          <view
            class="xm-input picker-text"
            :class="{ 'picker-placeholder': !form.segment }"
          >
            {{ form.segment ? segmentLabels[segmentIndex] : $t('pages.course.selectPlaceholder') }}
          </view>
        </picker>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.course.status') }}</view>
        <picker
          :range="statusLabels"
          @change="onStatusChange"
        >
          <view
            class="xm-input picker-text"
            :class="{ 'picker-placeholder': !form.status }"
          >
            {{ form.status ? statusLabels[statusIndex] : $t('pages.course.selectPlaceholder') }}
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
import { get, post } from '@/utils/request'
import { t, apiMessage } from '@/i18n'

const userStore = useUserStore()
const keyword = ref('')
const manageMode = ref(false)
const teacherData = ref([])

const isAdmin = computed(() => userStore.role === 'ADMIN')

// 星期/大节/类型/状态下拉选项：label 走翻译，value 保持中文（数据库按中文存储）
const typeValues = ['必修', '选修']
const typeLabels = computed(() => [t('pages.course.required'), t('pages.course.elective')])
const typeIndex = computed(() => typeValues.indexOf(form.value.type))
const onTypeChange = (e) => {
  form.value.type = typeValues[Number(e.detail.value)] || ''
}

const weekValues = ['星期一', '星期二', '星期三', '星期四', '星期五', '星期六', '星期日']
const weekLabels = computed(() => [
  t('pages.course.monday'),
  t('pages.course.tuesday'),
  t('pages.course.wednesday'),
  t('pages.course.thursday'),
  t('pages.course.friday'),
  t('pages.course.saturday'),
  t('pages.course.sunday'),
])
const weekIndex = computed(() => weekValues.indexOf(form.value.week))
const onWeekChange = (e) => {
  form.value.week = weekValues[Number(e.detail.value)] || ''
}

const segmentValues = [
  '第一大节（08:30 ~ 10:10）',
  '第二大节（10:30 ~ 12:10）',
  '第三大节（14:00 ~ 15:40）',
  '第四大节（16:00 ~ 17:40）',
  '第五大节（19:00 ~ 20:40）',
]
const segmentLabels = computed(() => [
  t('pages.course.segment1'),
  t('pages.course.segment2'),
  t('pages.course.segment3'),
  t('pages.course.segment4'),
  t('pages.course.segment5'),
])
const segmentIndex = computed(() => segmentValues.indexOf(form.value.segment))
const onSegmentChange = (e) => {
  form.value.segment = segmentValues[Number(e.detail.value)] || ''
}

const statusValues = ['未开课', '已开课', '已结课']
const statusLabels = computed(() => [
  t('pages.course.notStarted'),
  t('pages.course.started'),
  t('pages.course.finished'),
])
const statusIndex = computed(() => statusValues.indexOf(form.value.status))
const onStatusChange = (e) => {
  form.value.status = statusValues[Number(e.detail.value)] || ''
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
  url: '/course',
  getParams: () => ({ name: keyword.value }),
  validate: (f) => {
    if (!f.name) return t('pages.course.ruleNameRequired')
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

const onAdd = () => handleAdd({})
const onEdit = (row) => handleEdit(row)
const onReset = () => {
  keyword.value = ''
  search()
}

const teacherLabels = computed(() => teacherData.value.map((i) => i.name))
const teacherIndex = computed(() => teacherData.value.findIndex((i) => i.id === form.value.teacherId))
const onTeacherChange = (e) => {
  const item = teacherData.value[Number(e.detail.value)]
  if (item) form.value.teacherId = item.id
}

// 学生选课（与 Web 端 choiceCourse 接口一致）
const choiceCourse = (row) => {
  post('/choice/add', { studentId: userStore.user.id, teacherId: row.teacherId, courseId: row.id }).then((res) => {
    if (res.data && res.data.code === '200') {
      uni.showToast({ title: t('pages.course.choiceSuccess'), icon: 'success' })
    } else {
      uni.showToast({ title: apiMessage(res.data), icon: 'none' })
    }
  })
}

const loadTeacher = () => {
  get('/teacher/selectAll').then((res) => {
    if (res.data && res.data.code === '200') {
      teacherData.value = res.data.data || []
    } else {
      uni.showToast({ title: apiMessage(res.data), icon: 'none' })
    }
  })
}

// 页面入口：所有登录角色可见（与 Web 端路由一致，课程页不限角色）
onShow(() => {
  uni.setNavigationBarTitle({ title: t('menu.course') })
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  load(true)
  loadTeacher()
})

onReachBottom(() => loadNext())
</script>

<style lang="scss" scoped>
.picker-text {
  display: flex;
  align-items: center;
  line-height: 76rpx;
}

.picker-placeholder {
  color: var(--xm-text-2);
}
</style>

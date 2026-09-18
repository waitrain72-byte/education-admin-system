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
    <xm-empty
      v-if="!list.length && !loading"
      :action-text="$t('common.reload')"
      @action="load(true)"
    />

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
        <view class="xm-label">{{ $t('pages.course.id') }}: {{ item._index }}</view>
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
        <picker
          :range="roomLabels"
          @change="onRoomChange"
        >
          <view
            class="xm-input picker-text"
            :class="{ 'picker-placeholder': !form.room }"
          >
            {{ form.room ? roomDisplay(form.room) : $t('pages.course.roomPlaceholder') }}
          </view>
        </picker>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.course.week') }}</view>
        <picker
          :range="weekLabels"
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
    <xm-loader />
  </view>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
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

// ===== 教室：按当前星期/大节/人数拉取空闲列表（授课教室+运动场馆），支持自动分配 =====
const freeRooms = ref([])
// 选项描述：运动场馆用名称；教室优先用「使用说明」里的用途（多媒体教室/机房等），缺省回退名称
const roomDesc = (r) => {
  if (r.type === '运动场馆') return r.name
  return (
    String(r.content || '')
      .split('（')[0]
      .split('\n')[0]
      .trim() || r.name
  )
}
const roomLabels = computed(() => [
  t('pages.course.autoAssignOption'),
  ...freeRooms.value.map((r) => `${r.code} ${roomDesc(r)}（${r.num}人）`),
])
const roomDisplay = (code) => {
  const hit = freeRooms.value.find((r) => r.code === code)
  return hit ? `${hit.code} ${roomDesc(hit)}（${hit.num}人）` : code
}
const loadFreeRooms = async () => {
  if (!form.value.week || !form.value.segment) {
    freeRooms.value = []
    return
  }
  freeRooms.value =
    (await get(
      '/course/roomFree',
      {
        week: form.value.week,
        segment: form.value.segment,
        num: form.value.num || undefined,
        excludeId: form.value.id || undefined,
      },
      { loading: false },
    ).then((res) => res.data.data)) || []
}
const onRoomChange = (e) => {
  const idx = Number(e.detail.value)
  if (idx === 0) {
    // 自动分配：取容量最贴近人数的第一间空闲教室/场地
    if (!freeRooms.value.length) {
      uni.showToast({ title: t('pages.course.noFreeRoom'), icon: 'none' })
      return
    }
    form.value.room = freeRooms.value[0].code
    uni.showToast({
      title: t('pages.course.autoAssigned', {
        code: freeRooms.value[0].code,
        name: roomDesc(freeRooms.value[0]),
      }),
      icon: 'none',
    })
    return
  }
  const hit = freeRooms.value[idx - 1]
  if (hit) form.value.room = hit.code
}

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
  url: '/course',
  getParams: () => ({ name: keyword.value }),
  validate: (f) => {
    if (!f.name) return t('pages.course.ruleNameRequired')
    // 已选教室则必须同时有上课时段，否则无法进行占用校验，会产生无排期课程
    if (f.room && (!f.week || !f.segment)) return t('pages.course.ruleRoomNeedTime')
    return ''
  },
  beforeSave: async (f) => {
    // 教室留空 = 系统自动分配：按容量最贴近人数取第一间空闲教室/场地并提示
    if (!f.room && f.week && f.segment) {
      const res = await get('/course/roomFree', {
        week: f.week,
        segment: f.segment,
        num: f.num || undefined,
        excludeId: f.id || undefined,
      })
      const list = (res.data && res.data.data) || []
      if (!list.length) return t('pages.course.noFreeRoom')
      f.room = list[0].code
      uni.showToast({
        title: t('pages.course.autoAssigned', { code: list[0].code, name: roomDesc(list[0]) }),
        icon: 'none',
      })
    }
    if (!f.room) return t('pages.course.ruleRoomRequired')
    return ''
  },
})

// 弹层打开或星期/大节/人数变化时刷新空闲列表（静默加载）
// 注意：必须放在 useCrud 解构之后，formVisible 在解构中声明（否则触发 TDZ 引用错误导致页面白屏）
watch([formVisible, () => form.value.week, () => form.value.segment, () => form.value.num], () => {
  if (formVisible.value) loadFreeRooms()
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
  // 教师下拉仅表单使用且对非管理员禁用：学生无需加载，避免 teacher:view 403
  if (userStore.role === 'ADMIN') {
    loadTeacher()
  }
})

onReachBottom(() => loadNext())
</script>

<style lang="scss" scoped>
.picker-text {
  display: flex;
  align-items: center;
  line-height: 78rpx;
}

.picker-placeholder {
  color: var(--xm-text-2);
}
</style>

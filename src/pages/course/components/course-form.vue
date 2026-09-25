<template>
  <view>
    <xm-form-popup
      :visible="visible"
      :saving="saving"
      :title="$t(form.id ? 'common.editTitle' : 'common.addTitle', { name: $t('pages.course.entity') })"
      @close="$emit('close')"
      @save="$emit('save')"
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
        <xm-picker
          v-model="form.type"
          :options="enumOptions('courseType')"
          :disabled="!isAdmin"
          :placeholder="$t('pages.course.typePlaceholder')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.course.teacher') }}</view>
        <!-- 教师：管理员可搜索选择；教师本人编辑时只读（教师列表仅管理员加载，回落显示课程自带的教师名） -->
        <view
          class="xm-input picker-text"
          :class="{ 'picker-placeholder': form.teacherId == null, 'is-readonly': !isAdmin }"
          @click="openTeacherPicker"
        >
          {{ form.teacherId != null ? teacherNameOf(form.teacherId) : $t('pages.course.teacherPlaceholder') }}
        </view>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.course.score') }}</view>
        <input
          class="xm-input"
          v-model="form.score"
          type="digit"
          :disabled="!isAdmin"
          :placeholder="$t('pages.course.score')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.course.num') }}</view>
        <input
          class="xm-input"
          v-model="form.num"
          type="number"
          :disabled="!isAdmin"
          :placeholder="$t('pages.course.num')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.course.week') }}</view>
        <xm-picker
          v-model="form.week"
          :options="enumOptions('week')"
          :placeholder="$t('pages.course.selectPlaceholder')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.course.segment') }}</view>
        <xm-picker
          v-model="form.segment"
          :options="enumOptions('segment')"
          :placeholder="$t('pages.course.selectPlaceholder')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.course.room') }}</view>
        <!-- 教室：可搜索选择器（空闲教室可达数百间，原生滚轮难以查找） -->
        <view
          class="xm-input picker-text"
          :class="{ 'picker-placeholder': !form.room }"
          @click="openRoomPicker"
        >
          {{ form.room ? roomDisplay(form.room) : $t('pages.course.roomPlaceholder') }}
        </view>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.course.status') }}</view>
        <xm-picker
          v-model="form.status"
          :options="enumOptions('courseStatus')"
          :placeholder="$t('pages.course.selectPlaceholder')"
        />
      </view>
    </xm-form-popup>

    <!-- 两个选择器与表单弹层并列（不能放进表单里：弹层入场动画会让内部的 fixed 定位失效），层级更高叠在表单之上 -->
    <xm-select-popup
      :visible="roomPickerVisible"
      :title="$t('pages.course.room')"
      :options="roomOptions"
      :value="form.room || ''"
      :placeholder="$t('pages.course.roomSearchPlaceholder')"
      @close="roomPickerVisible = false"
      @select="onRoomSelect"
    />
    <xm-select-popup
      :visible="teacherPickerVisible"
      :title="$t('pages.course.teacher')"
      :options="teacherOptions"
      :value="form.teacherId == null ? '' : form.teacherId"
      :placeholder="$t('pages.course.teacherSearchPlaceholder')"
      @close="teacherPickerVisible = false"
      @select="onTeacherSelect"
    />
  </view>
</template>

<script setup>
/**
 * 课程新增 / 编辑表单（底部弹层）：基本字段 + 教室、授课教师两个可搜索选择器。
 * form 由列表页的 useListPage 持有，这里通过 v-model:form 直接编辑它的字段；保存 / 关闭交回页面。
 */
import { ref, computed, watch } from 'vue'
import { courseApi } from '@/api'
import { orNull, SILENT } from '@/utils/request'
import { t } from '@/i18n'
import { enumLabel, enumOptions } from '@/utils/enums'
import { AUTO_ROOM, roomDesc, freeRoomQuery } from '../room'

const form = defineModel('form', { type: Object, required: true })
const props = defineProps({
  visible: { type: Boolean, default: false },
  saving: { type: Boolean, default: false },
  /** 管理员可改全部字段；教师本人只能改时间 / 教室 / 状态 */
  isAdmin: { type: Boolean, default: false },
  /** 教师下拉数据（仅管理员由页面加载） */
  teachers: { type: Array, default: () => [] },
})
defineEmits(['close', 'save'])

// ===== 教室：按当前星期 / 大节 / 人数拉取空闲列表（授课教室 + 运动场馆），支持自动分配 =====
const freeRooms = ref([])
const roomText = (r) => `${r.code} ${roomDesc(r)}（${t('pages.course.capacity', { n: r.num })}）`
const roomDisplay = (code) => {
  const hit = freeRooms.value.find((r) => r.code === code)
  return hit ? roomText(hit) : code
}
const loadFreeRooms = async () => {
  if (!form.value.week || !form.value.segment) {
    freeRooms.value = []
    return
  }
  freeRooms.value = (await orNull(courseApi.roomFree(freeRoomQuery(form.value), SILENT))) || []
}
// 弹层打开或星期 / 大节 / 人数变化时刷新空闲列表（静默加载）
watch([() => props.visible, () => form.value.week, () => form.value.segment, () => form.value.num], () => {
  if (props.visible) loadFreeRooms()
})

// 选择器选项：首项「自动分配」固定置顶（搜索时也保留），其余为该时段空闲教室
const roomOptions = computed(() => [
  { value: AUTO_ROOM, label: t('pages.course.autoAssignOption'), desc: t('pages.course.autoAssignDesc'), pinned: true },
  ...freeRooms.value.map((r) => ({
    value: r.code,
    label: `${r.code} ${roomDesc(r)}`,
    desc: [enumLabel('roomType', r.type), t('pages.course.capacity', { n: r.num })].filter(Boolean).join(' · '),
  })),
])
const roomPickerVisible = ref(false)
const openRoomPicker = () => {
  // 空闲教室按「周几 + 大节」计算：时段没选时列表必然为空，先提示
  if (!form.value.week || !form.value.segment) {
    uni.showToast({ title: t('pages.course.pickTimeFirst'), icon: 'none' })
    return
  }
  roomPickerVisible.value = true
}
const onRoomSelect = (opt) => {
  roomPickerVisible.value = false
  if (opt.value !== AUTO_ROOM) {
    form.value.room = opt.value
    return
  }
  // 自动分配：取容量最贴近人数的第一间空闲教室 / 场地
  if (!freeRooms.value.length) {
    uni.showToast({ title: t('pages.course.noFreeRoom'), icon: 'none' })
    return
  }
  form.value.room = freeRooms.value[0].code
  uni.showToast({
    title: t('pages.course.autoAssigned', { code: freeRooms.value[0].code, name: roomDesc(freeRooms.value[0]) }),
    icon: 'none',
  })
}

// ===== 授课教师：只有管理员能改；教师本人编辑自己的课程时只读 =====
const teacherPickerVisible = ref(false)
const openTeacherPicker = () => {
  if (props.isAdmin) teacherPickerVisible.value = true
}
const teacherOptions = computed(() => props.teachers.map((i) => ({ value: i.id, label: i.name, desc: i.title || '' })))
const teacherNameOf = (id) => {
  const hit = props.teachers.find((i) => i.id === id)
  return hit ? hit.name : form.value.teacherName || ''
}
const onTeacherSelect = (opt) => {
  teacherPickerVisible.value = false
  form.value.teacherId = opt.value
}
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

/* 只读字段（教师本人编辑课程时不可改授课教师） */
.is-readonly {
  color: var(--xm-text-2);
}
</style>

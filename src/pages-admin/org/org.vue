<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 组织架构（与 Web 端 views/admin/OrgAdmin.vue 一致）：学院 → 专业 → 班级，各级的学生人数；
         手机上一层一层点进去，顶部面包屑可以退回上一层 -->
    <view class="sub">{{ $t('admin.org.sub') }}</view>

    <scroll-view
      scroll-x
      class="crumbs"
      :show-scrollbar="false"
    >
      <text
        class="crumb"
        :class="{ on: !path.length }"
        @click="goLevel(0)"
        >{{ $t('admin.org.title') }}</text
      >
      <template
        v-for="(node, i) in pathNodes"
        :key="node.key"
      >
        <text class="crumb-sep">›</text>
        <text
          class="crumb"
          :class="{ on: i === pathNodes.length - 1 }"
          @click="goLevel(i + 1)"
          >{{ node.name }}</text
        >
      </template>
    </scroll-view>

    <view
      v-if="loading && !raw.length"
      class="xm-card"
    >
      <view class="skeleton skeleton-line" />
      <view class="skeleton skeleton-line short" />
    </view>

    <!-- 当前节点详情（学院 / 专业 / 班级） -->
    <view
      v-if="current"
      class="xm-card node"
    >
      <view class="node-head">
        <text class="xm-tag xm-tag-brand">{{ $t('admin.org.types.' + current.type) }}</text>
        <text class="node-name">{{ current.name }}</text>
      </view>
      <view
        v-if="current.content"
        class="node-content"
        >{{ current.content }}</view
      >
      <view class="xm-kv">
        <text class="xm-kv-key">{{ $t('admin.org.studentsLabel') }}</text>
        <text class="xm-kv-value xm-num">{{ current.students }}</text>
      </view>
      <view
        v-if="current.type === 'speciality'"
        class="xm-kv"
      >
        <text class="xm-kv-key">{{ $t('admin.org.requiredCredits') }}</text>
        <text class="xm-kv-value xm-num">{{ current.score == null ? '—' : current.score }}</text>
      </view>
      <view
        v-if="current.type === 'class'"
        class="xm-kv"
      >
        <text class="xm-kv-key">{{ $t('admin.org.headTeacher') }}</text>
        <text class="xm-kv-value">{{ current.teacherName || $t('admin.org.noHeadTeacher') }}</text>
      </view>
      <view class="xm-actions">
        <button
          class="xm-btn xm-btn-danger xm-btn-sm"
          @click="remove(current)"
        >
          {{ $t('admin.org.delete') }}
        </button>
        <button
          class="xm-btn xm-btn-plain xm-btn-sm"
          @click="openEdit(current)"
        >
          {{ $t('admin.org.edit') }}
        </button>
      </view>
    </view>

    <!-- 下一级列表 -->
    <template v-if="!current || current.type !== 'class'">
      <view class="xm-section-head list-head">
        <text class="xm-section-title">{{ childTitle }}</text>
        <button
          class="xm-btn xm-btn-soft xm-btn-sm"
          @click="openCreate(childType, current)"
        >
          <xm-icon
            name="plus"
            :size="26"
          />
          {{ $t(CREATE_TITLE[childType]) }}
        </button>
      </view>
      <xm-empty
        v-if="!loading && !children.length"
        icon="org"
        :text="current ? $t('admin.org.none') : $t('admin.org.empty')"
      />
      <view
        v-for="node in children"
        :key="node.key"
        class="xm-card child"
        @click="enter(node)"
      >
        <view class="child-main">
          <view class="child-name">{{ node.name }}</view>
          <view class="child-sub">
            <text class="xm-num">{{ $t('admin.org.students', { n: node.students }) }}</text>
            <text v-if="node.type === 'speciality'"
              >· {{ $t('admin.org.classCount', { n: node.children.length }) }}</text
            >
            <text v-if="node.type === 'class'">· {{ node.teacherName || $t('admin.org.noHeadTeacher') }}</text>
          </view>
        </view>
        <view class="xm-cell-arrow">
          <xm-icon
            name="chevron-right"
            :size="32"
          />
        </view>
      </view>
    </template>

    <!-- 班级：班里的学生 -->
    <view
      v-else
      class="xm-card"
    >
      <view class="xm-section-head">
        <text class="xm-section-title">{{ $t('admin.org.classStudents') }}</text>
        <text
          v-if="classStudentsTotal"
          class="xm-link"
          @click="openPeople"
          >{{ $t('admin.org.viewInPeople', { n: classStudentsTotal }) }}</text
        >
      </view>
      <view
        v-if="classStudentsLoading"
        class="skeleton skeleton-line"
      />
      <view
        v-else-if="!classStudents.length"
        class="muted"
        >{{ $t('admin.org.noStudents') }}</view
      >
      <view
        v-for="s in classStudents"
        :key="s.id"
        class="student"
      >
        <xm-user-avatar
          :name="s.name"
          :avatar="s.avatar"
          :size="60"
        />
        <view class="student-main">
          <view class="student-name">{{ s.name }}</view>
          <view class="student-sub xm-num">{{ s.username }}</view>
        </view>
      </view>
    </view>

    <!-- 新建 / 编辑 -->
    <xm-form-popup
      :visible="formOpen"
      :title="formTitle"
      :saving="saving"
      :confirm-text="$t('common.save')"
      @close="formOpen = false"
      @save="save"
    >
      <view
        v-if="formParent"
        class="parent"
        >{{ $t('admin.org.types.' + formParent.type) }} · {{ formParent.name }}</view
      >
      <view class="xm-form-item">
        <view class="xm-form-label required">{{ $t('admin.org.name') }}</view>
        <input
          class="xm-input"
          v-model="form.name"
          maxlength="50"
        />
      </view>
      <view
        v-if="formType === 'speciality'"
        class="xm-form-item"
      >
        <view class="xm-form-label">{{ $t('admin.org.requiredCredits') }}</view>
        <input
          class="xm-input"
          v-model="form.score"
          type="number"
          maxlength="3"
        />
      </view>
      <view
        v-if="formType === 'class'"
        class="xm-form-item"
      >
        <view class="xm-form-label">{{ $t('admin.org.headTeacher') }}</view>
        <picker
          mode="selector"
          :range="teacherRange"
          :value="teacherIndex"
          @change="onTeacherPicked"
        >
          <view
            class="xm-select"
            :class="{ placeholder: !form.teacherId }"
          >
            <text>{{ form.teacherId ? teacherRange[teacherIndex] : $t('admin.org.headTeacherPlaceholder') }}</text>
            <xm-icon
              name="chevron-down"
              :size="28"
            />
          </view>
        </picker>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('admin.org.content') }}</view>
        <textarea
          class="xm-textarea"
          v-model="form.content"
          maxlength="200"
          :show-confirm-bar="false"
        />
      </view>
    </xm-form-popup>
    <xm-loader />
  </view>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { classesApi, collegeApi, peopleApi, specialityApi, teacherApi } from '@/api'
import { SILENT } from '@/utils/request'
import { ensureRole } from '@/utils/authGuard'
import { confirm } from '@/utils/confirm'
import { t } from '@/i18n'

/** 三级各自的通用接口 */
const API = { college: collegeApi, speciality: specialityApi, class: classesApi }
const CREATE_TITLE = {
  college: 'admin.org.addCollege',
  speciality: 'admin.org.addSpeciality',
  class: 'admin.org.addClass',
}
const CHILD_TYPE = { college: 'speciality', speciality: 'class' }

const raw = ref([])
const loading = ref(false)
const teachers = ref([])
/** 当前所在的层级：从学院到当前节点的 key 列表（空 = 学院列表） */
const path = ref([])

const tree = computed(() =>
  raw.value.map((c) => ({
    key: `college-${c.id}`,
    type: 'college',
    id: c.id,
    name: c.name,
    content: c.content,
    students: Number(c.students) || 0,
    children: (c.specialities || []).map((s) => ({
      key: `speciality-${s.id}`,
      type: 'speciality',
      id: s.id,
      name: s.name,
      content: s.content,
      score: s.score,
      students: Number(s.students) || 0,
      parentKey: `college-${c.id}`,
      children: (s.classes || []).map((k) => ({
        key: `class-${k.id}`,
        type: 'class',
        id: k.id,
        name: k.name,
        content: k.content,
        teacherId: k.teacherId,
        teacherName: k.teacherName,
        students: Number(k.students) || 0,
        parentKey: `speciality-${s.id}`,
        children: [],
      })),
    })),
  })),
)

const index = computed(() => {
  const map = {}
  const walk = (nodes) =>
    nodes.forEach((n) => {
      map[n.key] = n
      walk(n.children)
    })
  walk(tree.value)
  return map
})

const pathNodes = computed(() => path.value.map((key) => index.value[key]).filter(Boolean))
const current = computed(() => pathNodes.value[pathNodes.value.length - 1] || null)
const children = computed(() => (current.value ? current.value.children : tree.value))
const childType = computed(() => (current.value ? CHILD_TYPE[current.value.type] : 'college'))
const childTitle = computed(() =>
  current.value
    ? t(current.value.type === 'college' ? 'admin.org.specialities' : 'admin.org.classes')
    : t('admin.org.types.college'),
)

const enter = (node) => {
  path.value = [...path.value, node.key]
}
const goLevel = (depth) => {
  path.value = path.value.slice(0, depth)
}

const load = async () => {
  loading.value = true
  try {
    raw.value = (await peopleApi.org(SILENT)) || []
    // 当前所在的节点被删了，就退到还在的那一层
    const alive = []
    for (const key of path.value) {
      if (!index.value[key]) break
      alive.push(key)
    }
    path.value = alive
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    loading.value = false
  }
}

const loadTeachers = async () => {
  try {
    teachers.value = (await teacherApi.selectAll(undefined, SILENT)) || []
  } catch {
    // 班主任选择器拿不到时表单里就是空的，不影响其他操作
  }
}

// ---------- 班级学生 ----------
const classStudents = ref([])
const classStudentsTotal = ref(0)
const classStudentsLoading = ref(false)

watch(
  () => (current.value && current.value.type === 'class' ? current.value.id : null),
  async (classId) => {
    classStudents.value = []
    classStudentsTotal.value = 0
    if (!classId) return
    classStudentsLoading.value = true
    try {
      const page = await peopleApi.students({ classId, pageNum: 1, pageSize: 50 }, SILENT)
      // 切走了就丢掉过期的结果
      if (!current.value || current.value.type !== 'class' || current.value.id !== classId) return
      classStudents.value = (page && page.list) || []
      classStudentsTotal.value = (page && page.total) || 0
    } catch {
      // 提示已由请求层统一弹出
    } finally {
      classStudentsLoading.value = false
    }
  },
)

const openPeople = () => uni.navigateTo({ url: `/pages-admin/people/people?tab=students&classId=${current.value.id}` })

// ---------- 新建 / 编辑 ----------
const formOpen = ref(false)
const formType = ref('college')
const formParent = ref(null)
const editingId = ref(0)
const saving = ref(false)
const form = reactive({ name: '', content: '', score: '', teacherId: null })

const formTitle = computed(() =>
  editingId.value
    ? t('admin.org.editTitle', { type: t(`admin.org.types.${formType.value}`) })
    : t(CREATE_TITLE[formType.value]),
)

const teacherRange = computed(() => teachers.value.map((x) => x.name))
const teacherIndex = computed(() =>
  Math.max(
    0,
    teachers.value.findIndex((x) => x.id === form.teacherId),
  ),
)
const onTeacherPicked = (e) => {
  const teacher = teachers.value[Number(e.detail.value)]
  form.teacherId = teacher ? teacher.id : null
}

const openCreate = (type, parent) => {
  formType.value = type
  formParent.value = parent
  editingId.value = 0
  Object.assign(form, { name: '', content: '', score: type === 'speciality' ? '50' : '', teacherId: null })
  formOpen.value = true
}

const openEdit = (node) => {
  formType.value = node.type
  // 编辑时不能换上级：学生身上记着学院、专业，换了上级会和学生对不上
  formParent.value = node.parentKey ? index.value[node.parentKey] || null : null
  editingId.value = node.id
  Object.assign(form, {
    name: node.name,
    content: node.content || '',
    score: node.score == null ? '0' : String(node.score),
    teacherId: node.teacherId == null ? null : node.teacherId,
  })
  formOpen.value = true
}

const save = async () => {
  if (!form.name.trim()) {
    uni.showToast({ title: t('admin.org.nameRequired'), icon: 'none' })
    return
  }
  const body = { name: form.name.trim(), content: form.content.trim() }
  if (formType.value === 'speciality') {
    const score = Math.round(Number(form.score))
    body.score = Number.isFinite(score) && score >= 0 ? score : 0
    if (!editingId.value) body.collegeId = formParent.value && formParent.value.id
  }
  if (formType.value === 'class') {
    body.teacherId = form.teacherId
    if (!editingId.value) body.specialityId = formParent.value && formParent.value.id
  }
  saving.value = true
  try {
    const api = API[formType.value]
    if (editingId.value) await api.update({ ...body, id: editingId.value })
    else await api.add(body)
    uni.showToast({ title: t('admin.org.saved'), icon: 'success' })
    formOpen.value = false
    await load()
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    saving.value = false
  }
}

const remove = async (node) => {
  if (!(await confirm(t('admin.org.deleteConfirm', { name: node.name })))) return
  try {
    await API[node.type].delete(node.id)
    uni.showToast({ title: t('admin.org.deleted'), icon: 'none' })
    path.value = path.value.slice(0, -1)
    await load()
  } catch {
    // 下面还有专业、班级或学生的提示已由请求层统一弹出
  }
}

let shownOnce = false
onShow(() => {
  if (!ensureRole(['ADMIN'])) return
  uni.setNavigationBarTitle({ title: t('admin.org.title') })
  load()
  if (!shownOnce) {
    shownOnce = true
    loadTeachers()
  }
})

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
.sub {
  margin-bottom: 16rpx;
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.crumbs {
  white-space: nowrap;
  margin-bottom: 20rpx;
}

.crumb {
  display: inline-block;
  padding: 8rpx 4rpx;
  font-size: 26rpx;
  color: var(--xm-brand);
}

.crumb.on {
  color: var(--xm-text);
  font-weight: 600;
}

.crumb-sep {
  display: inline-block;
  padding: 0 10rpx;
  font-size: 26rpx;
  color: var(--xm-text-3);
}

.node-head {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.node-name {
  font-size: 34rpx;
  font-weight: bold;
  color: var(--xm-text);
}

.node-content {
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: var(--xm-text-2);
}

.list-head {
  margin-top: 8rpx;
}

.child {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.child:active {
  opacity: 0.85;
}

.child-main {
  flex: 1;
  min-width: 0;
}

.child-name {
  font-size: 30rpx;
  font-weight: 600;
  color: var(--xm-text);
}

.child-sub {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.muted {
  font-size: 26rpx;
  color: var(--xm-text-2);
}

.student {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 14rpx 0;
}

.student + .student {
  border-top: 1rpx solid var(--xm-border);
}

.student-main {
  flex: 1;
  min-width: 0;
}

.student-name {
  font-size: 28rpx;
  color: var(--xm-text);
}

.student-sub {
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.parent {
  margin-bottom: 20rpx;
  padding: 12rpx 18rpx;
  border-radius: 12rpx;
  background: var(--xm-bg-sunken);
  font-size: 24rpx;
  color: var(--xm-text-2);
}
</style>

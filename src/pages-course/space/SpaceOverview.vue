<template>
  <!-- 课程空间 · 概览（与 Web 端 views/course/CourseOverview.vue 一致）：签到提醒、待办、简介、公告、
       课程信息、任课教师、成绩构成，以及与当前登录人的关系（没选这门课的学生在这里选课） -->
  <view>
    <view
      v-if="overview.signing && isMember"
      class="banner"
      :class="{ 'is-student': isStudent }"
      @click="$emit('go', 'attendance')"
    >
      <view class="banner-dot" />
      <text class="banner-text">{{
        isStudent ? $t('space.overview.signingNow') : $t('space.overview.signingTeacher')
      }}</text>
      <text class="banner-go">{{ isStudent ? $t('space.overview.goSign') : $t('space.overview.viewSign') }} ›</text>
    </view>

    <!-- 没选这门课：选课入口放最上面 -->
    <view
      v-if="!isMember"
      class="xm-card relation"
    >
      <text class="xm-tag">{{ $t('space.relation.' + relation) }}</text>
      <view class="relation-text">{{ $t('space.relationHint.' + relation) }}</view>
      <template v-if="overview.enroll">
        <view
          v-if="overview.enroll.conflict"
          class="relation-warn"
          >{{ $t('square.conflict', { name: overview.enroll.conflict }) }}</view
        >
        <view
          v-else-if="overview.enroll.seatsLeft != null"
          class="relation-text xm-num"
        >
          {{
            overview.enroll.seatsLeft > 0 ? $t('square.seatsLeft', { n: overview.enroll.seatsLeft }) : $t('square.full')
          }}
        </view>
        <button
          v-if="!overview.enroll.ended"
          class="xm-btn xm-btn-primary xm-btn-block relation-btn"
          :disabled="!!overview.enroll.conflict || overview.enroll.seatsLeft === 0 || enrolling"
          :loading="enrolling"
          @click="enroll"
        >
          {{ $t('square.enroll') }}
        </button>
      </template>
    </view>

    <view
      v-if="isMember"
      class="xm-card"
    >
      <view class="xm-section-head">
        <text class="xm-section-title">{{ $t('space.overview.todo') }}</text>
      </view>
      <view v-if="todos.length">
        <view
          v-for="todo in todos"
          :key="todo.key"
          class="todo"
          @click="$emit('go', todo.tab)"
        >
          <view
            class="todo-mark"
            :class="todo.tone"
          />
          <text class="todo-text">{{ todo.text }}</text>
          <view class="xm-cell-arrow">
            <xm-icon
              name="chevron-right"
              :size="30"
            />
          </view>
        </view>
      </view>
      <view
        v-else
        class="muted"
        >{{ $t('space.overview.allClear') }}</view
      >
    </view>

    <view class="xm-card">
      <view class="xm-section-head">
        <text class="xm-section-title">{{ $t('space.overview.intro') }}</text>
        <text
          v-if="canTeach && !editingIntro"
          class="xm-link"
          @click="startEditIntro"
          >{{ $t('space.overview.editIntro') }}</text
        >
      </view>
      <template v-if="editingIntro">
        <textarea
          class="xm-textarea intro-input"
          v-model="introDraft"
          maxlength="500"
          :placeholder="$t('space.overview.introPlaceholder')"
          :show-confirm-bar="false"
        />
        <view class="xm-actions">
          <button
            class="xm-btn xm-btn-plain"
            style="flex: 1"
            @click="editingIntro = false"
          >
            {{ $t('common.cancel') }}
          </button>
          <button
            class="xm-btn xm-btn-primary"
            style="flex: 1"
            :loading="savingIntro"
            :disabled="savingIntro"
            @click="saveIntro"
          >
            {{ $t('common.save') }}
          </button>
        </view>
      </template>
      <view
        v-else-if="overview.intro"
        class="intro"
        >{{ overview.intro }}</view
      >
      <view
        v-else
        class="muted"
        >{{ $t('space.overview.introEmpty') }}</view
      >
    </view>

    <view
      v-if="isMember"
      class="xm-card"
    >
      <view class="xm-section-head">
        <text class="xm-section-title">{{ $t('space.overview.posts') }}</text>
        <button
          v-if="canTeach"
          class="xm-btn xm-btn-primary xm-btn-sm"
          @click="openCompose"
        >
          {{ $t('space.overview.newPost') }}
        </button>
      </view>
      <view
        v-if="postsLoading && !posts.length"
        class="skeleton skeleton-line"
      />
      <view
        v-else-if="!posts.length"
        class="muted"
        >{{ $t('space.overview.postsEmpty') }}</view
      >
      <view
        v-for="post in posts"
        :id="'post-' + post.id"
        :key="post.id"
        class="post"
        :class="{ 'is-focus': focusPostId === post.id }"
      >
        <view class="post-head">
          <text class="post-title">{{ post.title }}</text>
          <text
            v-if="canTeach"
            class="post-delete"
            @click="removePost(post)"
            >{{ $t('common.delete') }}</text
          >
        </view>
        <view class="post-meta">
          <text>{{ post.user || '—' }}</text>
          <text class="xm-num">{{ post.time }}</text>
        </view>
        <view class="post-content">{{ post.content }}</view>
      </view>
    </view>

    <view class="xm-card">
      <view class="xm-section-head">
        <text class="xm-section-title">{{ $t('space.overview.info') }}</text>
      </view>
      <view class="xm-kv">
        <text class="xm-kv-key">{{ $t('space.overview.time') }}</text>
        <text class="xm-kv-value">
          {{ timeText }}
        </text>
      </view>
      <view class="xm-kv">
        <text class="xm-kv-key">{{ $t('space.overview.room') }}</text>
        <text class="xm-kv-value">{{ course.room || '—' }}</text>
      </view>
      <view class="xm-kv">
        <text class="xm-kv-key">{{ $t('space.overview.type') }}</text>
        <text class="xm-kv-value">{{ courseTypeLabel(course.type) }}</text>
      </view>
      <view class="xm-kv">
        <text class="xm-kv-key">{{ $t('space.overview.credit') }}</text>
        <text class="xm-kv-value xm-num">{{ course.credit == null ? '—' : course.credit }}</text>
      </view>
      <view class="xm-kv">
        <text class="xm-kv-key">{{ $t('space.overview.status') }}</text>
        <text class="xm-kv-value">{{ courseStatusLabel(course.status) || '—' }}</text>
      </view>
      <view class="xm-kv">
        <text class="xm-kv-key">{{ $t('space.overview.enrolled') }}</text>
        <text class="xm-kv-value xm-num"
          >{{ overview.studentCount }}<template v-if="course.capacity"> / {{ course.capacity }}</template></text
        >
      </view>
      <view
        v-if="course.capacity"
        class="capacity"
      >
        <view
          class="capacity-bar"
          :style="'width:' + capacityPercent + '%'"
        />
      </view>
    </view>

    <view class="xm-card">
      <view class="xm-section-head">
        <text class="xm-section-title">{{ $t('space.overview.teacher') }}</text>
      </view>
      <view
        v-if="overview.teacher"
        class="teacher"
      >
        <xm-user-avatar
          :name="overview.teacher.name"
          :avatar="overview.teacher.avatar"
          :size="88"
        />
        <view class="teacher-info">
          <view class="teacher-name">{{ overview.teacher.name }}</view>
          <view class="teacher-sub">{{ overview.teacher.title || '—' }}</view>
          <view
            v-if="overview.teacher.email"
            class="teacher-mail"
            @click="copy(overview.teacher.email)"
            >{{ overview.teacher.email }}</view
          >
        </view>
      </view>
      <view
        v-else
        class="muted"
        >—</view
      >
    </view>

    <view
      v-if="weightParts.length"
      class="xm-card"
    >
      <view class="xm-section-head">
        <text class="xm-section-title">{{ $t('space.overview.weights') }}</text>
        <text class="xm-label">{{ $t('space.overview.weightsHint') }}</text>
      </view>
      <view class="weights-bar">
        <view
          v-for="part in weightParts"
          :key="part.key"
          class="weights-seg"
          :class="'weights-' + part.key"
          :style="'flex-grow:' + part.value"
        />
      </view>
      <view class="weights-legend">
        <view
          v-for="part in weightParts"
          :key="part.key"
          class="weights-item"
        >
          <view
            class="weights-dot"
            :class="'weights-' + part.key"
          />
          <text>{{ $t('space.weights.' + part.key) }}</text>
          <text class="xm-num">{{ part.value }}%</text>
        </view>
      </view>
    </view>

    <!-- 成员：身份说明；学生可以退选 -->
    <view
      v-if="isMember"
      class="xm-card relation"
    >
      <text class="xm-tag xm-tag-brand">{{ $t('space.relation.' + relation) }}</text>
      <view class="relation-text">{{ $t('space.relationHint.' + relation) }}</view>
      <button
        v-if="overview.canDrop"
        class="xm-btn xm-btn-plain xm-btn-sm relation-drop"
        :disabled="enrolling"
        @click="drop"
      >
        {{ $t('square.drop') }}
      </button>
    </view>

    <xm-form-popup
      :visible="composeVisible"
      :title="$t('space.overview.newPost')"
      :saving="posting"
      :confirm-text="$t('messageCenter.publish')"
      @close="composeVisible = false"
      @save="submitPost"
    >
      <view class="xm-form-item">
        <view class="xm-form-label required">{{ $t('space.overview.postTitle') }}</view>
        <input
          class="xm-input"
          v-model="compose.title"
          maxlength="100"
          :placeholder="$t('space.overview.postTitlePlaceholder')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label required">{{ $t('space.overview.postContent') }}</view>
        <textarea
          class="xm-textarea compose-content"
          v-model="compose.content"
          maxlength="2000"
          :placeholder="$t('space.overview.postContentPlaceholder')"
          :show-confirm-bar="false"
        />
      </view>
    </xm-form-popup>
  </view>
</template>

<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { courseApi } from '@/api'
import { SILENT } from '@/utils/request'
import { courseStatusLabel, courseTypeLabel, segmentLabel, weekLabel } from '@/utils/courseText'
import { useCourseEvents } from '@/composables/useCourseEvents'
import { confirm } from '@/utils/confirm'
import { t } from '@/i18n'

const props = defineProps({
  overview: { type: Object, required: true },
  courseId: { type: Number, required: true },
  /** 从消息点进来时要定位的公告 */
  focusPost: { type: Number, default: 0 },
})

const emit = defineEmits(['refresh', 'go'])

const course = computed(() => props.overview.course || {})
const relation = computed(() => props.overview.relation || 'visitor')
const isMember = computed(() => relation.value !== 'visitor')
const isStudent = computed(() => relation.value === 'student')
const canTeach = computed(() => relation.value === 'teacher' || relation.value === 'admin')

const timeText = computed(() => {
  const c = course.value
  if (!c.week) return t('courses.unscheduled')
  const time = c.start ? ` ${c.start}–${c.end}` : ''
  return `${weekLabel(c.week)} ${segmentLabel(c.segment)}${time}`
})

const capacityPercent = computed(() => {
  const cap = Number(course.value.capacity) || 0
  if (!cap) return 0
  return Math.min(100, Math.round((Number(props.overview.studentCount) / cap) * 100))
})

const copy = (text) => uni.setClipboardData({ data: text })

// ---------- 选课 / 退选（学生） ----------
const enrolling = ref(false)

const enroll = async () => {
  enrolling.value = true
  try {
    await courseApi.enroll(props.courseId)
    uni.showToast({ title: t('square.enrolledMsg', { name: course.value.name }), icon: 'none' })
    emit('refresh')
  } catch {
    // 满员、冲突等提示已由请求层统一弹出
  } finally {
    enrolling.value = false
  }
}

const drop = async () => {
  if (!(await confirm(t('square.dropConfirm', { name: course.value.name }), { title: t('square.drop') }))) return
  enrolling.value = true
  try {
    await courseApi.drop(props.courseId)
    uni.showToast({ title: t('square.droppedMsg', { name: course.value.name }), icon: 'none' })
    emit('refresh')
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    enrolling.value = false
  }
}

// ---------- 成绩构成 ----------
const weightParts = computed(() => {
  const w = props.overview.weights || {}
  return ['attendance', 'homework', 'ordinary', 'exam']
    .map((key) => ({ key, value: Number(w[key]) || 0 }))
    .filter((part) => part.value > 0)
})

// ---------- 待办 ----------
const todos = computed(() => {
  const list = []
  if (props.overview.signing) {
    list.push({
      key: 'sign',
      text: isStudent.value ? t('space.overview.signingNow') : t('space.overview.signingTeacher'),
      tab: 'attendance',
      tone: 'is-bad',
    })
  }
  if (isStudent.value && props.overview.pendingAssignments > 0) {
    list.push({
      key: 'pending',
      text: t('space.overview.pendingAssignments', { n: props.overview.pendingAssignments }),
      tab: 'assignments',
      tone: 'is-warn',
    })
  }
  if (canTeach.value && props.overview.ungraded > 0) {
    list.push({
      key: 'ungraded',
      text: t('space.overview.ungraded', { n: props.overview.ungraded }),
      tab: 'assignments',
      tone: 'is-warn',
    })
  }
  return list
})

// ---------- 简介 ----------
const editingIntro = ref(false)
const introDraft = ref('')
const savingIntro = ref(false)

const startEditIntro = () => {
  introDraft.value = props.overview.intro || ''
  editingIntro.value = true
}

const saveIntro = async () => {
  savingIntro.value = true
  try {
    await courseApi.updateIntro(props.courseId, introDraft.value)
    uni.showToast({ title: t('space.overview.introSaved'), icon: 'success' })
    editingIntro.value = false
    emit('refresh')
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    savingIntro.value = false
  }
}

// ---------- 公告 ----------
const posts = ref([])
const postsLoading = ref(false)
const focusPostId = ref(0)

const loadPosts = async () => {
  if (!isMember.value) return
  postsLoading.value = true
  try {
    posts.value = (await courseApi.posts(props.courseId, SILENT)) || []
  } catch {
    posts.value = []
  } finally {
    postsLoading.value = false
  }
}

/** 从消息点进来（post=ID）：滚到那条公告并高亮一下 */
const focusOnPost = async () => {
  const id = props.focusPost
  if (!id || !posts.value.some((p) => p.id === id)) return
  focusPostId.value = id
  await nextTick()
  uni.pageScrollTo({ selector: '#post-' + id, duration: 300, fail: () => {} })
  setTimeout(() => {
    if (focusPostId.value === id) focusPostId.value = 0
  }, 2400)
}

watch(
  [() => props.courseId, isMember],
  async () => {
    await loadPosts()
    await focusOnPost()
  },
  { immediate: true },
)

useCourseEvents(() => props.courseId, ['posts'], loadPosts)

const composeVisible = ref(false)
const posting = ref(false)
const compose = reactive({ title: '', content: '' })

const openCompose = () => {
  compose.title = ''
  compose.content = ''
  composeVisible.value = true
}

const submitPost = async () => {
  if (!compose.title.trim()) {
    uni.showToast({ title: t('space.overview.postTitleRequired'), icon: 'none' })
    return
  }
  if (!compose.content.trim()) {
    uni.showToast({ title: t('space.overview.postContentRequired'), icon: 'none' })
    return
  }
  posting.value = true
  try {
    await courseApi.addPost(props.courseId, { title: compose.title.trim(), content: compose.content.trim() })
    uni.showToast({ title: t('space.overview.postPublished'), icon: 'none' })
    composeVisible.value = false
    await loadPosts()
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    posting.value = false
  }
}

const removePost = async (post) => {
  if (!(await confirm(t('space.overview.deletePostConfirm', { title: post.title })))) return
  try {
    await courseApi.deletePost(props.courseId, post.id)
    posts.value = posts.value.filter((p) => p.id !== post.id)
  } catch {
    // 提示已由请求层统一弹出
  }
}

defineExpose({ reload: loadPosts })
</script>

<style lang="scss" scoped>
.banner {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 24rpx;
  padding: 24rpx 28rpx;
  border-radius: 20rpx;
  background: var(--xm-info-soft);
  color: var(--xm-info);
  font-size: 28rpx;
}

.banner.is-student {
  background: var(--xm-danger-soft);
  color: var(--xm-danger);
}

.banner-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: currentColor;
  animation: banner-blink 1.2s ease-in-out infinite;
}

@keyframes banner-blink {
  50% {
    opacity: 0.3;
  }
}

.banner-text {
  flex: 1;
  font-weight: 600;
}

.banner-go {
  font-weight: 600;
}

.todo {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 0;
}

.todo + .todo {
  border-top: 1rpx solid var(--xm-border);
}

.todo-mark {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  flex-shrink: 0;
}

.todo-mark.is-bad {
  background: var(--xm-danger);
}

.todo-mark.is-warn {
  background: var(--xm-warning);
}

.todo-text {
  flex: 1;
  font-size: 26rpx;
  color: var(--xm-text);
}

.muted {
  font-size: 26rpx;
  color: var(--xm-text-2);
}

.intro {
  font-size: 28rpx;
  line-height: 1.8;
  color: var(--xm-text);
  white-space: pre-wrap;
  word-break: break-all;
}

.intro-input {
  min-height: 240rpx;
}

.post {
  padding: 20rpx 0;
  border-radius: 12rpx;
  transition: background 0.3s;
}

.post + .post {
  border-top: 1rpx solid var(--xm-border);
}

.post.is-focus {
  background: var(--xm-brand-soft);
}

.post-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.post-title {
  flex: 1;
  font-size: 28rpx;
  font-weight: 600;
  color: var(--xm-text);
}

.post-delete {
  flex-shrink: 0;
  font-size: 24rpx;
  color: var(--xm-danger);
}

.post-meta {
  display: flex;
  gap: 16rpx;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.post-content {
  margin-top: 10rpx;
  font-size: 26rpx;
  line-height: 1.7;
  color: var(--xm-text);
  white-space: pre-wrap;
  word-break: break-all;
}

.capacity {
  height: 10rpx;
  margin-top: 12rpx;
  border-radius: 999rpx;
  background: var(--xm-bg-sunken);
  overflow: hidden;
}

.capacity-bar {
  height: 100%;
  border-radius: 999rpx;
  background: var(--xm-brand);
}

.teacher {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.teacher-info {
  flex: 1;
  min-width: 0;
}

.teacher-name {
  font-size: 30rpx;
  font-weight: 600;
  color: var(--xm-text);
}

.teacher-sub {
  margin-top: 4rpx;
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.teacher-mail {
  margin-top: 4rpx;
  font-size: 24rpx;
  color: var(--xm-brand);
  word-break: break-all;
}

.weights-bar {
  display: flex;
  gap: 4rpx;
  height: 20rpx;
  border-radius: 999rpx;
  overflow: hidden;
}

.weights-seg {
  flex-basis: 0;
}

.weights-attendance {
  background: #2565a8;
}

.weights-homework {
  background: #9a5c00;
}

.weights-ordinary {
  background: #6d48c9;
}

.weights-exam {
  background: var(--xm-brand);
}

.weights-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx 28rpx;
  margin-top: 16rpx;
  font-size: 24rpx;
  color: var(--xm-text-2);
}

.weights-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.weights-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 4rpx;
}

.relation-text {
  margin-top: 12rpx;
  font-size: 26rpx;
  line-height: 1.6;
  color: var(--xm-text-2);
}

.relation-warn {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: var(--xm-danger);
}

.relation-btn {
  margin-top: 20rpx;
}

.relation-drop {
  margin-top: 16rpx;
}

.compose-content {
  min-height: 280rpx;
}
</style>

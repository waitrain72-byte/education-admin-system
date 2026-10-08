<template>
  <div class="ov">
    <div class="ov__main">
      <!-- 正在签到：学生看到去签到，老师看到去签到页查看人数 -->
      <router-link
        v-if="overview.signing && isMember"
        :to="`/course/${courseId}/attendance`"
        class="ov-banner"
        :class="{ 'ov-banner--student': isStudent }"
      >
        <span class="ov-banner__dot" aria-hidden="true"></span>
        <span class="ov-banner__text">{{ isStudent ? $t('space.overview.signingNow') : $t('space.overview.signingTeacher') }}</span>
        <span class="ov-banner__go">{{ isStudent ? $t('space.overview.goSign') : $t('space.overview.viewSign') }} ›</span>
      </router-link>

      <section class="panel">
        <div class="panel__head">
          <span class="panel__title">{{ $t('space.overview.intro') }}</span>
          <el-button v-if="canTeach && !editingIntro" link type="primary" @click="startEditIntro">
            {{ $t('space.overview.editIntro') }}
          </el-button>
        </div>
        <div class="panel__body">
          <template v-if="editingIntro">
            <el-input
              v-model="introDraft"
              type="textarea"
              :rows="4"
              maxlength="500"
              show-word-limit
              :placeholder="$t('space.overview.introPlaceholder')"
            />
            <div class="ov-actions">
              <el-button @click="editingIntro = false">{{ $t('common.cancel') }}</el-button>
              <el-button type="primary" :loading="savingIntro" @click="saveIntro">{{ $t('common.save') }}</el-button>
            </div>
          </template>
          <p v-else-if="overview.intro" class="ov-intro">{{ overview.intro }}</p>
          <p v-else class="ov-muted">{{ $t('space.overview.introEmpty') }}</p>
        </div>
      </section>

      <section v-if="isMember" class="panel">
        <div class="panel__head">
          <span class="panel__title">{{ $t('space.overview.posts') }}</span>
          <el-button v-if="canTeach" type="primary" size="small" @click="openCompose">
            {{ $t('space.overview.newPost') }}
          </el-button>
        </div>
        <div class="panel__body">
          <el-skeleton v-if="postsLoading && !posts.length" animated :rows="3" />
          <div v-else-if="!posts.length" class="empty-note">{{ $t('space.overview.postsEmpty') }}</div>
          <ol v-else class="posts">
            <li
              v-for="post in posts"
              :key="post.id"
              :ref="(el) => setPostRef(post.id, el)"
              class="post"
              :class="{ 'is-focus': focusPostId === post.id }"
            >
              <div class="post__head">
                <h3 class="post__title">{{ post.title }}</h3>
                <el-button
                  v-if="canTeach"
                  link
                  size="small"
                  class="post__delete"
                  :aria-label="$t('space.overview.deletePost')"
                  @click="removePost(post)"
                >
                  {{ $t('common.delete') }}
                </el-button>
              </div>
              <div class="post__meta">
                <span>{{ post.user || '—' }}</span>
                <span class="num">{{ post.time }}</span>
              </div>
              <p class="post__content">{{ post.content }}</p>
            </li>
          </ol>
        </div>
      </section>
    </div>

    <aside class="ov__side">
      <section v-if="isMember" class="panel">
        <div class="panel__head">
          <span class="panel__title">{{ $t('space.overview.todo') }}</span>
        </div>
        <div class="panel__body">
          <ul v-if="todos.length" class="todos">
            <li v-for="todo in todos" :key="todo.key">
              <router-link :to="todo.to" class="todo">
                <span class="todo__mark" :class="todo.tone" aria-hidden="true"></span>
                <span class="todo__text">{{ todo.text }}</span>
                <span class="todo__go" aria-hidden="true">›</span>
              </router-link>
            </li>
          </ul>
          <div v-else class="ov-muted ov-muted--small">{{ $t('space.overview.allClear') }}</div>
        </div>
      </section>

      <section class="panel">
        <div class="panel__head">
          <span class="panel__title">{{ $t('space.overview.teacher') }}</span>
        </div>
        <div class="panel__body teacher">
          <template v-if="overview.teacher">
            <UserAvatar :name="overview.teacher.name" :avatar="overview.teacher.avatar" :size="48" />
            <div class="teacher__info">
              <div class="teacher__name">{{ overview.teacher.name }}</div>
              <div class="teacher__title">{{ overview.teacher.title || '—' }}</div>
              <a v-if="overview.teacher.email" class="teacher__mail" :href="`mailto:${overview.teacher.email}`">
                {{ overview.teacher.email }}
              </a>
            </div>
          </template>
          <div v-else class="empty-note">—</div>
        </div>
      </section>

      <section class="panel">
        <div class="panel__head">
          <span class="panel__title">{{ $t('space.overview.info') }}</span>
        </div>
        <div class="panel__body">
          <dl class="facts">
            <div>
              <dt>{{ $t('space.overview.time') }}</dt>
              <dd>
                <template v-if="course.week">{{ course.week }} {{ segmentShortName(course.segment) }}</template>
                <template v-else>{{ $t('courses.unscheduled') }}</template>
                <span v-if="course.start" class="num facts__sub">{{ course.start }}–{{ course.end }}</span>
              </dd>
            </div>
            <div>
              <dt>{{ $t('space.overview.room') }}</dt>
              <dd>{{ course.room || '—' }}</dd>
            </div>
            <div>
              <dt>{{ $t('space.overview.type') }}</dt>
              <dd>{{ course.type || '—' }}</dd>
            </div>
            <div>
              <dt>{{ $t('space.overview.credit') }}</dt>
              <dd class="num">{{ course.credit ?? '—' }}</dd>
            </div>
            <div>
              <dt>{{ $t('space.overview.status') }}</dt>
              <dd>{{ course.status || '—' }}</dd>
            </div>
            <div>
              <dt>{{ $t('space.overview.enrolled') }}</dt>
              <dd class="num">
                {{ overview.studentCount }}<template v-if="course.capacity"> / {{ course.capacity }}</template>
              </dd>
            </div>
          </dl>
          <div v-if="course.capacity" class="capacity" :aria-label="$t('space.overview.enrolled')">
            <div class="capacity__bar" :style="{ width: capacityPercent + '%' }"></div>
          </div>
        </div>
      </section>

      <section v-if="weightParts.length" class="panel">
        <div class="panel__head">
          <span class="panel__title">{{ $t('space.overview.weights') }}</span>
          <span class="panel__hint">{{ $t('space.overview.weightsHint') }}</span>
        </div>
        <div class="panel__body">
          <div class="weights-bar" role="img" :aria-label="weightsLabel">
            <span
              v-for="part in weightParts"
              :key="part.key"
              class="weights-bar__seg"
              :class="'weights-bar__seg--' + part.key"
              :style="{ flexGrow: part.value }"
            ></span>
          </div>
          <ul class="weights-legend">
            <li v-for="part in weightParts" :key="part.key">
              <span class="weights-legend__dot" :class="'weights-bar__seg--' + part.key"></span>
              <span>{{ $t('space.weights.' + part.key) }}</span>
              <span class="num">{{ part.value }}%</span>
            </li>
          </ul>
        </div>
      </section>

      <section class="panel panel--pad relation">
        <span class="pill" :class="relationPill">{{ $t('space.relation.' + overview.relation) }}</span>
        <p class="relation__text">{{ $t('space.relationHint.' + overview.relation) }}</p>
      </section>
    </aside>

    <el-dialog v-model="composeVisible" :title="$t('space.overview.newPost')" width="560px" destroy-on-close>
      <el-form ref="composeRef" :model="compose" :rules="composeRules" label-position="top">
        <el-form-item :label="$t('space.overview.postTitle')" prop="title">
          <el-input
            v-model="compose.title"
            maxlength="100"
            show-word-limit
            :placeholder="$t('space.overview.postTitlePlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="$t('space.overview.postContent')" prop="content">
          <el-input
            v-model="compose.content"
            type="textarea"
            :rows="6"
            maxlength="2000"
            show-word-limit
            :placeholder="$t('space.overview.postContentPlaceholder')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="composeVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="posting" @click="submitPost">{{ $t('messageCenter.publish') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import UserAvatar from '@/components/UserAvatar.vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from '@/utils/element-plus'
import { segmentShortName } from '@/utils/schedule'
import { useCourseEvents } from '@/composables/useCourseEvents'
import { t } from '@/i18n'

interface Post {
  id: number
  title: string
  content: string
  time: string
  user?: string
}

const props = defineProps<{ overview: Record<string, any> }>()
const emit = defineEmits<{ refresh: [] }>()

const route = useRoute()
const courseId = computed(() => Number(route.params.id))
const course = computed(() => props.overview.course || {})
const relation = computed<string>(() => props.overview.relation || 'visitor')
const isMember = computed(() => relation.value !== 'visitor')
const isStudent = computed(() => relation.value === 'student')
const canTeach = computed(() => relation.value === 'teacher' || relation.value === 'admin')

const capacityPercent = computed(() => {
  const cap = Number(course.value.capacity) || 0
  if (!cap) return 0
  return Math.min(100, Math.round((Number(props.overview.studentCount) / cap) * 100))
})

const relationPill = computed(() => (relation.value === 'visitor' ? '' : 'pill--brand'))

// ---------- 成绩构成 ----------
const weightParts = computed(() => {
  const w = props.overview.weights || {}
  return (['attendance', 'homework', 'ordinary', 'exam'] as const)
    .map((key) => ({ key, value: Number(w[key]) || 0 }))
    .filter((part) => part.value > 0)
})
const weightsLabel = computed(() =>
  weightParts.value.map((p) => `${t('space.weights.' + p.key)} ${p.value}%`).join('，'),
)

// ---------- 待办 ----------
const todos = computed(() => {
  const list: { key: string; text: string; to: string; tone: string }[] = []
  const base = `/course/${courseId.value}`
  if (props.overview.signing) {
    list.push({
      key: 'sign',
      text: isStudent.value ? t('space.overview.signingNow') : t('space.overview.signingTeacher'),
      to: `${base}/attendance`,
      tone: 'is-bad',
    })
  }
  if (isStudent.value && props.overview.pendingAssignments > 0) {
    list.push({
      key: 'pending',
      text: t('space.overview.pendingAssignments', { n: props.overview.pendingAssignments }),
      to: `${base}/assignments`,
      tone: 'is-warn',
    })
  }
  if (canTeach.value && props.overview.ungraded > 0) {
    list.push({
      key: 'ungraded',
      text: t('space.overview.ungraded', { n: props.overview.ungraded }),
      to: `${base}/assignments`,
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
    await request.put(`/course/${courseId.value}/intro`, { intro: introDraft.value })
    ElMessage.success(t('space.overview.introSaved'))
    editingIntro.value = false
    emit('refresh')
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    savingIntro.value = false
  }
}

// ---------- 公告 ----------
const posts = ref<Post[]>([])
const postsLoading = ref(false)
const focusPostId = ref<number | null>(null)
const postRefs = new Map<number, Element>()

const setPostRef = (id: number, el: unknown) => {
  if (el instanceof Element) postRefs.set(id, el)
  else postRefs.delete(id)
}

const loadPosts = async () => {
  if (!isMember.value) return
  postsLoading.value = true
  try {
    posts.value = (await request.get<Post[]>(`/course/${courseId.value}/posts`)) || []
  } catch {
    posts.value = []
  } finally {
    postsLoading.value = false
  }
}

/** 从消息点进来（?post=ID）：滚到那条公告并高亮一下 */
const focusFromQuery = async () => {
  const id = Number(route.query.post)
  if (!id || !posts.value.some((p) => p.id === id)) return
  focusPostId.value = id
  await nextTick()
  postRefs.get(id)?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  setTimeout(() => {
    if (focusPostId.value === id) focusPostId.value = null
  }, 2400)
}

watch(
  [courseId, isMember],
  async () => {
    await loadPosts()
    await focusFromQuery()
  },
  { immediate: true },
)

useCourseEvents(() => courseId.value, ['posts'], loadPosts)

const composeVisible = ref(false)
const composeRef = ref<FormInstance>()
const posting = ref(false)
const compose = reactive({ title: '', content: '' })
const composeRules = computed<FormRules>(() => ({
  title: [{ required: true, whitespace: true, message: t('space.overview.postTitleRequired'), trigger: 'blur' }],
  content: [{ required: true, whitespace: true, message: t('space.overview.postContentRequired'), trigger: 'blur' }],
}))

const openCompose = () => {
  compose.title = ''
  compose.content = ''
  composeVisible.value = true
}

const submitPost = async () => {
  const valid = await composeRef.value?.validate().catch(() => false)
  if (!valid) return
  posting.value = true
  try {
    await request.post(`/course/${courseId.value}/posts`, { title: compose.title, content: compose.content })
    ElMessage.success(t('space.overview.postPublished'))
    composeVisible.value = false
    await loadPosts()
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    posting.value = false
  }
}

const removePost = async (post: Post) => {
  try {
    await ElMessageBox.confirm(
      t('space.overview.deletePostConfirm', { title: post.title }),
      t('common.confirmDeleteTitle'),
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await request.delete(`/course/${courseId.value}/posts/${post.id}`)
    ElMessage.success(t('common.operationSuccess'))
    await loadPosts()
  } catch {
    // 错误提示已由拦截器统一处理
  }
}
</script>

<style scoped>
.ov {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  align-items: start;
}

.ov__main,
.ov__side {
  display: grid;
  gap: 20px;
  min-width: 0;
}

/* ---------- 签到横幅 ---------- */
.ov-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius-lg);
  background: var(--xm-bg-card);
  color: var(--xm-text-primary);
  font-size: 14px;
}

.ov-banner--student {
  border-color: transparent;
  background: var(--xm-brand);
  color: var(--xm-on-brand);
  font-weight: 600;
}

.ov-banner__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--xm-bad);
  flex-shrink: 0;
  animation: banner-pulse 1.6s ease-in-out infinite;
}

.ov-banner--student .ov-banner__dot {
  background: currentColor;
}

.ov-banner__text {
  flex: 1;
  min-width: 0;
}

.ov-banner__go {
  white-space: nowrap;
}

@keyframes banner-pulse {
  50% {
    opacity: 0.3;
  }
}

@media (prefers-reduced-motion: reduce) {
  .ov-banner__dot {
    animation: none;
  }
}

/* ---------- 简介 ---------- */
.ov-intro {
  font-size: 14px;
  line-height: 1.75;
  color: var(--xm-text-regular);
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.ov-muted {
  font-size: 14px;
  color: var(--xm-text-secondary);
}

.ov-muted--small {
  font-size: 13px;
}

.ov-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

/* ---------- 公告 ---------- */
.posts {
  list-style: none;
  display: grid;
}

.post {
  padding: 14px 0;
  border-bottom: 1px solid var(--xm-border);
  border-radius: var(--xm-radius-sm);
  transition: background-color 0.4s ease;
}

.post:first-child {
  padding-top: 2px;
}

.post:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.post.is-focus {
  background: var(--xm-brand-soft);
  box-shadow: 0 0 0 8px var(--xm-brand-soft);
}

.post__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.post__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--xm-text-primary);
  overflow-wrap: anywhere;
}

.post__delete {
  flex-shrink: 0;
  color: var(--xm-text-secondary);
}

.post__meta {
  display: flex;
  gap: 12px;
  margin-top: 2px;
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.post__content {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--xm-text-regular);
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

/* ---------- 待办 ---------- */
.todos {
  list-style: none;
  display: grid;
  gap: 6px;
}

.todo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 10px;
  margin: 0 -10px;
  border-radius: var(--xm-radius);
  color: var(--xm-text-primary);
  font-size: 14px;
}

.todo:hover {
  background: var(--xm-bg-hover);
}

.todo__mark {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.todo__mark.is-bad {
  background: var(--xm-bad);
}

.todo__mark.is-warn {
  background: var(--xm-warn);
}

.todo__text {
  flex: 1;
  min-width: 0;
}

.todo__go {
  color: var(--xm-text-secondary);
}

/* ---------- 任课教师 ---------- */
.teacher {
  display: flex;
  align-items: center;
  gap: 14px;
}

.teacher__info {
  min-width: 0;
}

.teacher__name {
  font-size: 16px;
  font-weight: 600;
  color: var(--xm-text-primary);
}

.teacher__title,
.teacher__mail {
  display: block;
  font-size: 13px;
  color: var(--xm-text-secondary);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.teacher__mail:hover {
  color: var(--xm-brand);
}

/* ---------- 课程信息 ---------- */
.facts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px 16px;
}

.facts dt {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.facts dd {
  margin-top: 3px;
  font-size: 14px;
  color: var(--xm-text-primary);
}

.facts__sub {
  display: block;
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.capacity {
  height: 6px;
  margin-top: 16px;
  border-radius: 999px;
  background: var(--xm-bg-sunken);
  overflow: hidden;
}

.capacity__bar {
  height: 100%;
  border-radius: inherit;
  background: var(--xm-brand);
}

/* ---------- 成绩构成 ---------- */
.weights-bar {
  display: flex;
  gap: 3px;
  height: 10px;
  border-radius: 999px;
  overflow: hidden;
}

.weights-bar__seg {
  min-width: 6px;
}

.weights-bar__seg--attendance {
  background: var(--xm-info);
}

.weights-bar__seg--homework {
  background: var(--xm-warn);
}

.weights-bar__seg--ordinary {
  background: var(--xm-brand-strong);
}

.weights-bar__seg--exam {
  background: var(--xm-brand);
}

.weights-legend {
  list-style: none;
  display: grid;
  gap: 6px;
  margin-top: 12px;
  font-size: 13px;
  color: var(--xm-text-regular);
}

.weights-legend li {
  display: flex;
  align-items: center;
  gap: 8px;
}

.weights-legend li .num {
  margin-left: auto;
  color: var(--xm-text-primary);
  font-weight: 600;
}

.weights-legend__dot {
  width: 8px;
  height: 8px;
  border-radius: 2px;
}

.relation {
  display: grid;
  gap: 8px;
  justify-items: start;
}

.relation__text {
  font-size: 13px;
  color: var(--xm-text-secondary);
}

@media (max-width: 960px) {
  .ov {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>

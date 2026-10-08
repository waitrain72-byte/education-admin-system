<template>
  <el-dialog
    :model-value="modelValue"
    class="cmdk"
    width="640px"
    top="12vh"
    :show-close="false"
    append-to-body
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
    @opened="focusInput"
    @closed="reset"
  >
    <div class="cmdk__box" @keydown="onKeydown">
      <div class="cmdk__input">
        <el-icon class="cmdk__icon"><Search /></el-icon>
        <input
          ref="inputEl"
          v-model="query"
          type="text"
          :placeholder="$t('shell.searchPlaceholder')"
          :aria-label="$t('shell.searchPlaceholder')"
          aria-controls="cmdk-results"
          autocomplete="off"
          spellcheck="false"
        />
        <span v-if="loading" class="cmdk__loading">{{ $t('shell.searching') }}</span>
        <kbd>Esc</kbd>
      </div>

      <div id="cmdk-results" class="cmdk__results" role="listbox">
        <template v-for="group in groups" :key="group.key">
          <div class="cmdk__group">{{ $t('shell.searchGroups.' + group.key) }}</div>
          <button
            v-for="item in group.items"
            :key="item.id"
            type="button"
            role="option"
            class="cmdk__item"
            :class="{ 'is-active': item.index === active }"
            :aria-selected="item.index === active"
            @mouseenter="active = item.index"
            @click="open(item)"
          >
            <span class="cmdk__dot" :style="{ background: item.color || 'var(--xm-border-dashed)' }"></span>
            <span class="cmdk__title">{{ item.title }}</span>
            <span v-if="item.sub" class="cmdk__sub">{{ item.sub }}</span>
          </button>
        </template>
        <div v-if="!flat.length" class="cmdk__empty">
          {{ query.trim() && !loading ? $t('shell.searchEmpty') : $t('shell.searchHint') }}
        </div>
      </div>
      <div class="cmdk__foot">{{ $t('shell.searchTip') }}</div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { t } from '@/i18n'
import { useUser } from '@/components/useUser'
import { childLinks, mainNav } from '@/layout/nav'
import { courseColor } from '@/utils/courseColor'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()

const router = useRouter()
const { user, hasPermission } = useUser()

interface ResultItem {
  id: string
  title: string
  sub?: string
  to: string
  color?: string
  index: number
}

const query = ref('')
const loading = ref(false)
const active = ref(0)
const inputEl = ref<HTMLInputElement>()
const remote = ref<Record<string, any[]>>({})

/** 本地可跳转的页面：主导航、选课广场、成绩单、个人中心、教务后台各页（按角色） */
const pages = computed(() => {
  const role = user.value.role
  const list: Array<{ title: string; to: string }> = mainNav(role).map((item) => ({ title: t(item.label), to: item.to }))
  list.push({ title: t('nav.square'), to: '/square' })
  if (role === 'STUDENT') {
    list.push({ title: t('transcript.title'), to: '/grades' })
  }
  list.push({ title: t('shell.profile'), to: '/profile' }, { title: t('shell.password'), to: '/profile?tab=password' })
  const layout = router.options.routes.find((r) => r.path === '/')
  const children = layout?.children || []
  const admin = children.find((r) => r.path === 'admin')
  if (admin && role === 'ADMIN') {
    for (const link of childLinks(admin.children || [], '/admin', role)) {
      list.push({ title: t(link.label), to: link.path })
    }
  }
  if (hasPermission('dashboard:view')) {
    list.push({ title: t('pages.dashboard.entry'), to: '/dashboard' })
  }
  return list
})

const groups = computed(() => {
  const q = query.value.trim().toLowerCase()
  let index = 0
  const result: Array<{ key: string; items: ResultItem[] }> = []
  const push = (key: string, items: Omit<ResultItem, 'index'>[]) => {
    if (items.length) {
      result.push({ key, items: items.map((item) => ({ ...item, index: index++ })) })
    }
  }

  const matchedPages = pages.value.filter((p) => !q || p.title.toLowerCase().includes(q)).slice(0, q ? 6 : 8)
  push(
    'pages',
    matchedPages.map((p) => ({ id: 'page:' + p.to, title: p.title, to: p.to })),
  )
  if (!q) return result

  push(
    'courses',
    (remote.value.courses || []).map((c: any) => ({
      id: 'course:' + c.id,
      title: c.name,
      sub: [c.teacherName, c.week, c.room].filter(Boolean).join(' · '),
      to: `/course/${c.id}`,
      color: courseColor(c.name),
    })),
  )
  push(
    'notices',
    (remote.value.notices || []).map((n: any) => ({
      id: 'notice:' + n.id,
      title: n.title,
      sub: n.time,
      to: `/messages?tab=notices&notice=${n.id}`,
    })),
  )
  push(
    'students',
    (remote.value.students || []).map((s: any) => ({
      id: 'student:' + s.id,
      title: s.name,
      sub: [s.username, s.className].filter(Boolean).join(' · '),
      to: `/admin/people?tab=students&keyword=${encodeURIComponent(s.username || '')}`,
    })),
  )
  push(
    'teachers',
    (remote.value.teachers || []).map((s: any) => ({
      id: 'teacher:' + s.id,
      title: s.name,
      sub: [s.username, s.title].filter(Boolean).join(' · '),
      to: `/admin/people?tab=teachers&keyword=${encodeURIComponent(s.username || '')}`,
    })),
  )
  return result
})

const flat = computed(() => groups.value.flatMap((g) => g.items))

// 输入防抖后查后端；用递增序号丢弃过期的响应（先发后到的旧结果不能覆盖新结果）
let timer: ReturnType<typeof setTimeout> | undefined
let seq = 0
watch(query, (value) => {
  active.value = 0
  if (timer) clearTimeout(timer)
  const keyword = value.trim()
  if (!keyword) {
    remote.value = {}
    loading.value = false
    return
  }
  loading.value = true
  timer = setTimeout(async () => {
    const mine = ++seq
    try {
      const data = await request.get<Record<string, any[]>>('/search', { params: { keyword } })
      if (mine === seq) remote.value = data || {}
    } catch {
      if (mine === seq) remote.value = {}
    } finally {
      if (mine === seq) loading.value = false
    }
  }, 250)
})

const onKeydown = (e: KeyboardEvent) => {
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    active.value = flat.value.length ? (active.value + 1) % flat.value.length : 0
    scrollActiveIntoView()
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    active.value = flat.value.length ? (active.value - 1 + flat.value.length) % flat.value.length : 0
    scrollActiveIntoView()
  } else if (e.key === 'Enter') {
    const item = flat.value[active.value]
    if (item) {
      e.preventDefault()
      open(item)
    }
  }
}

const scrollActiveIntoView = () => {
  nextTick(() => {
    document.querySelector('.cmdk__item.is-active')?.scrollIntoView({ block: 'nearest' })
  })
}

const open = (item: ResultItem) => {
  emit('update:modelValue', false)
  router.push(item.to)
}

const focusInput = () => inputEl.value?.focus()

const reset = () => {
  query.value = ''
  remote.value = {}
  active.value = 0
}

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) nextTick(focusInput)
  },
)
</script>

<style>
/* el-dialog 渲染在 body 下，样式不能 scoped */
.cmdk.el-dialog {
  --el-dialog-padding-primary: 0;
  max-width: calc(100vw - 32px);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: var(--xm-shadow-pop);
}

.cmdk .el-dialog__header {
  display: none;
}

.cmdk .el-dialog__body {
  padding: 0;
}

.cmdk__input {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--xm-border);
}

.cmdk__icon {
  color: var(--xm-text-secondary);
  font-size: 18px;
}

.cmdk__input input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  color: var(--xm-text-primary);
  font: inherit;
  font-size: 16px;
}

.cmdk__input kbd,
.cmdk__loading {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.cmdk__input kbd {
  padding: 1px 6px;
  border: 1px solid var(--xm-border);
  border-bottom-width: 2px;
  border-radius: 5px;
  font-family: var(--xm-font-num);
}

.cmdk__results {
  max-height: min(56vh, 460px);
  overflow-y: auto;
  padding: 6px 8px 10px;
}

.cmdk__group {
  padding: 10px 10px 4px;
  font-size: 12px;
  color: var(--xm-text-secondary);
  letter-spacing: 0.04em;
}

.cmdk__item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 10px;
  border: none;
  border-radius: 9px;
  background: transparent;
  color: var(--xm-text-primary);
  font: inherit;
  font-size: 14px;
  text-align: left;
  cursor: pointer;
}

.cmdk__item.is-active {
  background: var(--xm-brand-soft);
}

.cmdk__dot {
  width: 8px;
  height: 8px;
  border-radius: 3px;
  flex-shrink: 0;
}

.cmdk__title {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.cmdk__sub {
  margin-left: auto;
  padding-left: 12px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: var(--xm-text-secondary);
  font-size: 12px;
}

.cmdk__empty {
  padding: 28px 12px;
  text-align: center;
  color: var(--xm-text-secondary);
  font-size: 13px;
}

.cmdk__foot {
  padding: 8px 16px;
  border-top: 1px solid var(--xm-border);
  background: var(--xm-bg-sunken);
  color: var(--xm-text-secondary);
  font-size: 12px;
}
</style>

<template>
  <div class="msg">
    <div class="page-head">
      <div>
        <h1 class="page-title">{{ $t('messageCenter.title') }}</h1>
        <p class="page-sub">{{ $t('messageCenter.sub') }}</p>
      </div>
    </div>

    <el-tabs v-model="tab" class="msg-tabs" @tab-change="onTabChange">
      <el-tab-pane name="mine">
        <template #label>
          {{ $t('messageCenter.tabs.mine') }}
          <span v-if="messageStore.unread" class="msg-tabs__count num">{{ messageStore.unread }}</span>
        </template>

        <div class="msg-toolbar">
          <div class="seg">
            <button type="button" class="seg__item" :class="{ 'is-on': !unreadOnly }" @click="setUnreadOnly(false)">
              {{ $t('messageCenter.all') }}
            </button>
            <button type="button" class="seg__item" :class="{ 'is-on': unreadOnly }" @click="setUnreadOnly(true)">
              {{ $t('messageCenter.unread') }}
            </button>
          </div>
          <el-button :disabled="!messageStore.unread" @click="readAll">{{ $t('messageCenter.readAll') }}</el-button>
        </div>

        <section v-loading="mine.loading" class="panel">
          <ul v-if="mine.list.length" class="msg-list">
            <li v-for="m in mine.list" :key="m.id" class="msg-item" :class="{ 'is-unread': !m.isRead }">
              <button type="button" class="msg-item__main" @click="openMessage(m)">
                <span class="pill" :class="typePill(m.type)">{{ typeLabel(m.type) }}</span>
                <span class="msg-item__body">
                  <span class="msg-item__title">{{ m.title }}</span>
                  <span class="msg-item__content">{{ m.content }}</span>
                </span>
                <span class="msg-item__time num">{{ m.createTime }}</span>
              </button>
              <button
                type="button"
                class="msg-item__del"
                :aria-label="$t('messageCenter.delete')"
                :title="$t('messageCenter.delete')"
                @click="removeMessage(m)"
              >
                <el-icon><Close /></el-icon>
              </button>
            </li>
          </ul>
          <div v-else-if="!mine.loading" class="empty-note">
            {{ unreadOnly ? $t('messageCenter.emptyUnread') : $t('messageCenter.empty') }}
          </div>
        </section>
        <el-pagination
          v-if="mine.total > mine.pageSize"
          class="msg-pager"
          layout="prev, pager, next"
          :total="mine.total"
          :page-size="mine.pageSize"
          :current-page="mine.pageNum"
          @current-change="loadMine"
        />
      </el-tab-pane>

      <el-tab-pane name="notices" :label="$t('messageCenter.tabs.notices')">
        <div class="msg-toolbar">
          <el-input
            v-model="noticeKeyword"
            class="msg-toolbar__search"
            :placeholder="$t('messageCenter.noticeSearch')"
            clearable
            :prefix-icon="Search"
            @keyup.enter="loadNotices(1)"
            @clear="loadNotices(1)"
          />
          <el-button v-if="canManageNotice" type="primary" @click="compose()">{{ $t('messageCenter.compose') }}</el-button>
        </div>

        <section v-loading="notices.loading" class="panel">
          <ul v-if="notices.list.length" class="notice-list">
            <li v-for="n in notices.list" :key="n.id" class="notice-item">
              <button type="button" class="notice-item__main" @click="showNotice(n)">
                <span class="notice-item__title">{{ n.title }}</span>
                <span class="notice-item__content">{{ n.content }}</span>
              </button>
              <span class="notice-item__meta num">{{ n.time }}</span>
              <span v-if="canManageNotice" class="notice-item__ops">
                <el-button link type="primary" size="small" @click="compose(n)">{{ $t('common.edit') }}</el-button>
                <el-button link type="danger" size="small" @click="removeNotice(n)">{{ $t('common.delete') }}</el-button>
              </span>
            </li>
          </ul>
          <div v-else-if="!notices.loading" class="empty-note">{{ $t('common.empty') }}</div>
        </section>
        <el-pagination
          v-if="notices.total > notices.pageSize"
          class="msg-pager"
          layout="prev, pager, next"
          :total="notices.total"
          :page-size="notices.pageSize"
          :current-page="notices.pageNum"
          @current-change="loadNotices"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- 通知详情 -->
    <el-drawer v-model="detailVisible" :title="detail?.title" size="440px">
      <template v-if="detail">
        <p class="detail-meta num">{{ detail.time }}<template v-if="detail.user"> · {{ detail.user }}</template></p>
        <p class="detail-content">{{ detail.content }}</p>
      </template>
    </el-drawer>

    <!-- 发布 / 编辑通知（管理员） -->
    <el-drawer v-model="formVisible" :title="form.id ? $t('messageCenter.editNotice') : $t('messageCenter.compose')" size="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item prop="title" :label="$t('pages.notice.title')">
          <el-input id="notice-title" v-model="form.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item prop="content" :label="$t('pages.notice.content')">
          <el-input id="notice-content" v-model="form.content" type="textarea" :rows="8" maxlength="250" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="saveNotice">{{ $t('messageCenter.publish') }}</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { Close, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from '@/utils/element-plus'
import request, { type PageResult } from '@/utils/request'
import { useUser } from '@/components/useUser'
import { useMessageStore } from '@/stores/messages'
import { t } from '@/i18n'

defineOptions({ name: 'MessagesPage' })

const route = useRoute()
const router = useRouter()
const { hasPermission } = useUser()
const messageStore = useMessageStore()

const tab = ref<string>(route.query.tab === 'notices' ? 'notices' : 'mine')
const canManageNotice = computed(() => hasPermission('notice:manage'))

// ========== 我的消息 ==========
const unreadOnly = ref(false)
const mine = reactive({ list: [] as any[], total: 0, pageNum: 1, pageSize: 10, loading: false })

const loadMine = async (pageNum = 1) => {
  mine.loading = true
  mine.pageNum = pageNum
  try {
    const page = await request.get<PageResult<any>>('/message/page', {
      params: { unreadOnly: unreadOnly.value, pageNum, pageSize: mine.pageSize },
    })
    mine.list = page?.list || []
    mine.total = page?.total || 0
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    mine.loading = false
  }
}

const setUnreadOnly = (value: boolean) => {
  unreadOnly.value = value
  loadMine(1)
}

const TYPE_PILL: Record<string, string> = {
  score: 'pill--brand',
  homework: 'pill--info',
  apply: 'pill--warn',
  warning: 'pill--bad',
  attendance: 'pill--ok',
  course: 'pill--brand',
}
const typePill = (type: string) => TYPE_PILL[type] || ''
const typeLabel = (type: string) => t('messageCenter.types.' + (TYPE_PILL[type] ? type : 'other'))

const openMessage = async (m: any) => {
  if (!m.isRead) {
    m.isRead = true
    messageStore.markOneRead()
    request.put(`/message/read/${m.id}`).catch(() => {
      // 标记失败不打断跳转，下次进入会重新显示为未读
    })
  }
  if (m.link) router.push(m.link)
}

const readAll = async () => {
  try {
    await request.put('/message/readAll')
    messageStore.markAllRead()
    mine.list.forEach((m) => (m.isRead = true))
    if (unreadOnly.value) loadMine(1)
  } catch {
    // 错误提示已由拦截器统一处理
  }
}

const removeMessage = async (m: any) => {
  try {
    await request.delete(`/message/${m.id}`)
    if (!m.isRead) messageStore.markOneRead()
    loadMine(mine.list.length === 1 && mine.pageNum > 1 ? mine.pageNum - 1 : mine.pageNum)
  } catch {
    // 错误提示已由拦截器统一处理
  }
}

// ========== 通知公告 ==========
const noticeKeyword = ref('')
const notices = reactive({ list: [] as any[], total: 0, pageNum: 1, pageSize: 10, loading: false })
let noticesLoaded = false

const loadNotices = async (pageNum = 1) => {
  notices.loading = true
  notices.pageNum = pageNum
  noticesLoaded = true
  try {
    const params: Record<string, any> = { pageNum, pageSize: notices.pageSize }
    if (noticeKeyword.value.trim()) params.title = noticeKeyword.value.trim()
    const page = await request.get<PageResult<any>>('/notice/selectPage', { params })
    notices.list = page?.list || []
    notices.total = page?.total || 0
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    notices.loading = false
  }
}

const detailVisible = ref(false)
const detail = ref<any>(null)
const showNotice = (n: any) => {
  detail.value = n
  detailVisible.value = true
}

/** 从搜索结果、首页点进来时（?notice=ID）直接打开那条通知 */
const openNoticeFromQuery = async () => {
  const id = Number(route.query.notice)
  if (!id) return
  const found = notices.list.find((n) => n.id === id)
  if (found) {
    showNotice(found)
    return
  }
  try {
    const n = await request.get<any>(`/notice/selectById/${id}`)
    if (n) showNotice(n)
  } catch {
    // 通知可能已被删除，提示已由拦截器统一处理
  }
}

const formVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<{ id?: number; title: string; content: string }>({ title: '', content: '' })
const rules = computed<FormRules>(() => ({
  title: [{ required: true, message: t('pages.notice.ruleTitleRequired'), trigger: 'blur' }],
  content: [{ required: true, message: t('pages.notice.ruleContentRequired'), trigger: 'blur' }],
}))

const compose = (n?: any) => {
  form.id = n?.id
  form.title = n?.title || ''
  form.content = n?.content || ''
  formVisible.value = true
}

const saveNotice = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (form.id) {
      await request.put('/notice/update', { id: form.id, title: form.title, content: form.content })
    } else {
      await request.post('/notice/add', { title: form.title, content: form.content })
    }
    ElMessage.success(t('messageCenter.published'))
    formVisible.value = false
    loadNotices(1)
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    saving.value = false
  }
}

const removeNotice = (n: any) => {
  ElMessageBox.confirm(t('messageCenter.deleteNoticeConfirm', { title: n.title }), t('common.confirmDeleteTitle'), {
    type: 'warning',
  })
    .then(async () => {
      try {
        await request.delete(`/notice/delete/${n.id}`)
        ElMessage.success(t('common.operationSuccess'))
        loadNotices(1)
      } catch {
        // 错误提示已由拦截器统一处理
      }
    })
    .catch(() => {
      // 用户取消
    })
}

const onTabChange = (name: string | number) => {
  router.replace({ query: name === 'notices' ? { tab: 'notices' } : {} })
  if (name === 'notices' && !noticesLoaded) loadNotices(1)
}

onMounted(async () => {
  loadMine(1)
  messageStore.refresh()
  if (tab.value === 'notices') {
    await loadNotices(1)
    openNoticeFromQuery()
    if (route.query.compose && canManageNotice.value) compose()
  }
})
</script>

<style scoped>
.msg-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.msg-tabs__count {
  margin-left: 6px;
  padding: 0 6px;
  border-radius: 999px;
  background: var(--xm-bad);
  color: var(--xm-on-brand);
  font-size: 11px;
}

.msg-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.msg-toolbar__search {
  width: 280px;
}

.seg {
  display: inline-grid;
  grid-auto-flow: column;
  padding: 3px;
  border-radius: 9px;
  background: var(--xm-bg-sunken);
}

.seg__item {
  padding: 5px 16px;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: var(--xm-text-secondary);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.seg__item.is-on {
  background: var(--xm-bg-card);
  color: var(--xm-text-primary);
  font-weight: 600;
  box-shadow: var(--xm-shadow-card);
}

.msg-list,
.notice-list {
  list-style: none;
}

.msg-item {
  position: relative;
  display: flex;
  align-items: stretch;
  border-bottom: 1px solid var(--xm-border);
}

.msg-item:last-child {
  border-bottom: none;
}

/* 未读：左侧一个品牌色圆点，标题加粗 */
.msg-item.is-unread::before {
  content: '';
  position: absolute;
  left: 8px;
  top: 24px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--xm-brand);
}

.msg-item__main {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: start;
  gap: 12px;
  flex: 1;
  min-width: 0;
  padding: 16px 8px 16px 24px;
  border: none;
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.msg-item__main:hover {
  background: var(--xm-bg-hover);
}

.msg-item__body {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.msg-item__title {
  font-size: 14px;
  color: var(--xm-text-primary);
}

.msg-item.is-unread .msg-item__title {
  font-weight: 700;
}

.msg-item__content {
  font-size: 13px;
  color: var(--xm-text-secondary);
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.msg-item__time {
  font-size: 12px;
  color: var(--xm-text-secondary);
  white-space: nowrap;
}

.msg-item__del {
  flex-shrink: 0;
  width: 40px;
  border: none;
  background: transparent;
  color: var(--xm-text-secondary);
  cursor: pointer;
  opacity: 0;
}

.msg-item:hover .msg-item__del,
.msg-item__del:focus-visible {
  opacity: 1;
}

.msg-item__del:hover {
  color: var(--xm-bad);
}

.notice-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 12px;
  padding: 0 16px 0 0;
  border-bottom: 1px solid var(--xm-border);
}

.notice-item:last-child {
  border-bottom: none;
}

.notice-item__main {
  display: grid;
  gap: 2px;
  min-width: 0;
  padding: 14px 16px;
  border: none;
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.notice-item__main:hover .notice-item__title {
  color: var(--xm-brand);
}

.notice-item__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--xm-text-primary);
}

.notice-item__content {
  font-size: 13px;
  color: var(--xm-text-secondary);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.notice-item__meta {
  font-size: 12px;
  color: var(--xm-text-secondary);
  white-space: nowrap;
}

.notice-item__ops {
  white-space: nowrap;
}

.msg-pager {
  justify-content: center;
  margin-top: 16px;
}

.detail-meta {
  margin-bottom: 14px;
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.detail-content {
  font-size: 15px;
  line-height: 1.8;
  color: var(--xm-text-primary);
  white-space: pre-wrap;
}

@media (max-width: 640px) {
  .msg-toolbar__search {
    width: 100%;
  }

  .msg-item__main {
    grid-template-columns: minmax(0, 1fr);
  }

  .msg-item__del {
    opacity: 1;
  }

  .notice-item {
    grid-template-columns: minmax(0, 1fr);
    padding: 0 0 10px;
  }

  .notice-item__meta,
  .notice-item__ops {
    padding: 0 16px;
  }
}
</style>

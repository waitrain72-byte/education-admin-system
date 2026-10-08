<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 消息（与 Web 端 views/messages/MessagesPage.vue 一致）：我的消息（成绩发布、作业批改、请假审核……）+ 通知公告 -->
    <view class="xm-seg tabs">
      <view
        class="xm-seg-item"
        :class="{ on: tab === 'mine' }"
        @click="switchTab('mine')"
      >
        {{ $t('messageCenter.tabs.mine') }}
        <text
          v-if="messageStore.unread"
          class="xm-count tab-count"
          >{{ messageStore.unread > 99 ? '99+' : messageStore.unread }}</text
        >
      </view>
      <view
        class="xm-seg-item"
        :class="{ on: tab === 'notices' }"
        @click="switchTab('notices')"
        >{{ $t('messageCenter.tabs.notices') }}</view
      >
    </view>

    <!-- ========== 我的消息 ========== -->
    <view v-if="tab === 'mine'">
      <view class="toolbar">
        <view class="xm-seg filter">
          <view
            class="xm-seg-item"
            :class="{ on: !unreadOnly }"
            @click="setUnreadOnly(false)"
            >{{ $t('messageCenter.all') }}</view
          >
          <view
            class="xm-seg-item"
            :class="{ on: unreadOnly }"
            @click="setUnreadOnly(true)"
            >{{ $t('messageCenter.unread') }}</view
          >
        </view>
        <button
          class="xm-btn xm-btn-plain xm-btn-sm"
          :disabled="!messageStore.unread"
          @click="readAll"
        >
          {{ $t('messageCenter.readAll') }}
        </button>
      </view>

      <view
        v-if="mine.list.value.length"
        class="xm-card list"
      >
        <view
          v-for="m in mine.list.value"
          :key="m.id"
          class="msg"
          :class="{ 'is-unread': !m.isRead }"
          @click="openMessage(m)"
        >
          <view class="msg-head">
            <text
              class="xm-tag"
              :class="typeTag(m.type)"
              >{{ typeLabel(m.type) }}</text
            >
            <text class="msg-title">{{ m.title }}</text>
            <view
              v-if="!m.isRead"
              class="xm-dot"
            />
          </view>
          <view class="msg-content xm-clamp-2">{{ m.content }}</view>
          <view class="msg-foot">
            <text class="msg-time xm-num">{{ shortTime(m.createTime) }}</text>
            <view
              class="msg-del"
              @click.stop="removeMessage(m)"
            >
              <xm-icon
                name="trash"
                :size="28"
              />
            </view>
          </view>
        </view>
      </view>
      <xm-empty
        v-else-if="mine.loaded.value && !mine.loading.value"
        icon="inbox"
        :text="unreadOnly ? $t('messageCenter.emptyUnread') : $t('messageCenter.empty')"
      />
      <xm-list-footer
        :visible="mine.list.value.length > 0"
        :loading="mine.loading.value"
        :finished="mine.finished.value"
        @load-more="mine.loadNext()"
      />
    </view>

    <!-- ========== 通知公告 ========== -->
    <view v-else>
      <view class="toolbar">
        <view class="toolbar-search">
          <xm-search
            v-model="noticeKeyword"
            :placeholder="$t('messageCenter.noticeSearch')"
            @confirm="notices.reload()"
            @clear="notices.reload()"
          />
        </view>
      </view>
      <view
        v-if="notices.list.value.length"
        class="xm-list"
      >
        <view
          v-for="n in notices.list.value"
          :key="n.id"
          class="xm-card notice"
          @click="showNotice(n)"
        >
          <view class="notice-title">{{ n.title }}</view>
          <view class="notice-content xm-clamp-2">{{ n.content }}</view>
          <view class="notice-meta xm-num"
            >{{ n.time }}<template v-if="n.user"> · {{ n.user }}</template></view
          >
        </view>
      </view>
      <xm-empty
        v-else-if="notices.loaded.value && !notices.loading.value"
        icon="megaphone"
      />
      <xm-list-footer
        :visible="notices.list.value.length > 0"
        :loading="notices.loading.value"
        :finished="notices.finished.value"
        @load-more="notices.loadNext()"
      />
      <view
        v-if="canManageNotice"
        class="xm-fab"
        @click="compose()"
      >
        <xm-icon
          name="plus"
          :size="48"
        />
      </view>
    </view>

    <!-- 消息 / 通知详情 -->
    <view v-if="detail">
      <view
        class="xm-mask"
        @click="detail = null"
        @touchmove.stop.prevent="noop"
      />
      <view class="xm-popup">
        <view class="detail-title">{{ detail.title }}</view>
        <view class="detail-meta xm-num">
          {{ detail.time }}<template v-if="detail.user"> · {{ detail.user }}</template>
        </view>
        <view class="detail-content">{{ detail.content }}</view>
        <view
          v-if="detail.kind === 'notice' && canManageNotice"
          class="xm-actions"
        >
          <button
            class="xm-btn xm-btn-danger"
            style="flex: 1"
            @click="removeNotice(detail.raw)"
          >
            {{ $t('common.delete') }}
          </button>
          <button
            class="xm-btn xm-btn-primary"
            style="flex: 1"
            @click="compose(detail.raw)"
          >
            {{ $t('common.edit') }}
          </button>
        </view>
        <button
          v-else
          class="xm-btn xm-btn-plain xm-btn-block detail-close"
          @click="detail = null"
        >
          {{ $t('common.ok') }}
        </button>
      </view>
    </view>

    <!-- 发布 / 编辑通知（有 notice:manage 权限的账号） -->
    <xm-form-popup
      :visible="formVisible"
      :title="form.id ? $t('messageCenter.editNotice') : $t('messageCenter.compose')"
      :saving="saving"
      :confirm-text="$t('messageCenter.publish')"
      @close="formVisible = false"
      @save="saveNotice"
    >
      <view class="xm-form-item">
        <view class="xm-form-label required">{{ $t('pages.notice.title') }}</view>
        <input
          class="xm-input"
          v-model="form.title"
          maxlength="100"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label required">{{ $t('pages.notice.content') }}</view>
        <textarea
          class="xm-textarea compose-content"
          v-model="form.content"
          maxlength="250"
          :show-confirm-bar="false"
        />
        <view class="compose-count xm-num">{{ form.content.length }} / 250</view>
      </view>
    </xm-form-popup>

    <xm-loader />
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onHide, onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { messageApi, noticeApi } from '@/api'
import { SILENT } from '@/utils/request'
import { useMessageStore } from '@/stores/message'
import { ensureLoggedIn } from '@/utils/authGuard'
import { openLink, takeTabQuery } from '@/utils/link'
import { usePager } from '@/composables/usePager'
import { usePermission } from '@/composables/usePermission'
import { confirm } from '@/utils/confirm'
import { t } from '@/i18n'

const PAGE_URL = '/pages/messages/messages'

const messageStore = useMessageStore()
const { hasPermission } = usePermission()
const canManageNotice = computed(() => hasPermission('notice:manage'))

const tab = ref('mine')
const noop = () => {}

// ========== 我的消息 ==========
const unreadOnly = ref(false)
const mine = usePager((page) => messageApi.page({ ...page, unreadOnly: unreadOnly.value }, SILENT))

const setUnreadOnly = (value) => {
  if (unreadOnly.value === value) return
  unreadOnly.value = value
  mine.reload()
}

/** 消息类型 → 标签配色（与 Web 端同一套颜色语义） */
const TYPE_TAG = {
  score: 'xm-tag-brand',
  homework: 'xm-tag-info',
  apply: 'xm-tag-warning',
  warning: 'xm-tag-danger',
  attendance: 'xm-tag-success',
  course: 'xm-tag-brand',
}
const typeTag = (type) => TYPE_TAG[type] || ''
const typeLabel = (type) => t('messageCenter.types.' + (TYPE_TAG[type] ? type : 'other'))

/** 「2026-10-08 14:22:05」→ 今年的只显示「10-08 14:22」 */
const shortTime = (time) => {
  const text = String(time || '')
  if (text.length < 16) return text
  const thisYear = String(new Date().getFullYear())
  return text.startsWith(thisYear) ? text.slice(5, 16) : text.slice(0, 16)
}

const openMessage = (m) => {
  if (!m.isRead) {
    m.isRead = true
    messageStore.markOneRead()
    // 标记失败不打断跳转，下次进入会重新显示为未读
    messageApi.read(m.id, SILENT).catch(() => {})
  }
  // 带链接的跳到对应页面（成绩册、作业、请假……），没有链接或认不出的就地展开全文
  if (m.link && openLink(m.link)) return
  detail.value = { kind: 'message', title: m.title, time: shortTime(m.createTime), content: m.content }
}

const readAll = async () => {
  try {
    await messageApi.readAll()
    messageStore.markAllRead()
    mine.list.value.forEach((m) => {
      m.isRead = true
    })
    if (unreadOnly.value) mine.reload()
  } catch {
    // 提示已由请求层统一弹出
  }
}

const removeMessage = async (m) => {
  try {
    await messageApi.remove(m.id, SILENT)
    if (!m.isRead) messageStore.markOneRead()
    mine.list.value = mine.list.value.filter((x) => x.id !== m.id)
    mine.total.value = Math.max(0, mine.total.value - 1)
  } catch {
    // 提示已由请求层统一弹出
  }
}

// ========== 通知公告 ==========
const noticeKeyword = ref('')
const notices = usePager((page) => {
  const params = { ...page }
  if (noticeKeyword.value.trim()) params.title = noticeKeyword.value.trim()
  return noticeApi.selectPage(params, SILENT)
})

const detail = ref(null)
const showNotice = (n) => {
  detail.value = { kind: 'notice', title: n.title, time: n.time, user: n.user, content: n.content, raw: n }
}

/** 从首页、消息链接点进来时（notice=ID）直接打开那条通知 */
const openNoticeById = async (id) => {
  const found = notices.list.value.find((n) => n.id === id)
  if (found) {
    showNotice(found)
    return
  }
  try {
    const n = await noticeApi.selectById(id)
    if (n) showNotice(n)
  } catch {
    // 通知可能已被删除，提示已由请求层统一弹出
  }
}

const formVisible = ref(false)
const saving = ref(false)
const form = reactive({ id: null, title: '', content: '' })

const compose = (n) => {
  form.id = (n && n.id) || null
  form.title = (n && n.title) || ''
  form.content = (n && n.content) || ''
  detail.value = null
  formVisible.value = true
}

const saveNotice = async () => {
  if (!form.title.trim()) {
    uni.showToast({ title: t('pages.notice.ruleTitleRequired'), icon: 'none' })
    return
  }
  if (!form.content.trim()) {
    uni.showToast({ title: t('pages.notice.ruleContentRequired'), icon: 'none' })
    return
  }
  saving.value = true
  try {
    const data = { title: form.title.trim(), content: form.content.trim() }
    if (form.id) await noticeApi.update({ id: form.id, ...data })
    else await noticeApi.add(data)
    uni.showToast({ title: t('messageCenter.published'), icon: 'success' })
    formVisible.value = false
    notices.reload()
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    saving.value = false
  }
}

const removeNotice = async (n) => {
  if (!(await confirm(t('messageCenter.deleteNoticeConfirm', { title: n.title })))) return
  try {
    await noticeApi.delete(n.id)
    uni.showToast({ title: t('common.operationSuccess'), icon: 'success' })
    detail.value = null
    notices.reload()
  } catch {
    // 提示已由请求层统一弹出
  }
}

// ========== 切换与生命周期 ==========
const switchTab = (name) => {
  tab.value = name
  if (name === 'notices' && !notices.loaded.value) notices.reload()
}

/** 别的页面寄存过来的参数：{ tab: 'notices', notice: ID, compose: 1 } */
const applyQuery = (query) => {
  if (!query) return
  if (query.tab === 'notices' || query.notice || query.compose) switchTab('notices')
  if (query.notice) openNoticeById(Number(query.notice))
  if (query.compose && canManageNotice.value) compose()
}

const onTabQuery = (url) => {
  if (url === PAGE_URL) applyQuery(takeTabQuery(PAGE_URL))
}

/** 收到新推送：未读数已由 utils/websocket 刷新，这里把列表第一页也刷新 */
const onPush = () => {
  if (tab.value === 'mine') mine.reload()
}

onShow(() => {
  if (!ensureLoggedIn()) return
  uni.setNavigationBarTitle({ title: t('messageCenter.title') })
  messageStore.refresh()
  mine.reload()
  if (notices.loaded.value) notices.reload()
  applyQuery(takeTabQuery(PAGE_URL))
  uni.$off('xm:tab-query', onTabQuery)
  uni.$on('xm:tab-query', onTabQuery)
  uni.$off('ws:push', onPush)
  uni.$on('ws:push', onPush)
})

onHide(() => {
  uni.$off('xm:tab-query', onTabQuery)
  uni.$off('ws:push', onPush)
})

onPullDownRefresh(async () => {
  await Promise.all([tab.value === 'mine' ? mine.reload() : notices.reload(), messageStore.refresh()])
  uni.stopPullDownRefresh()
})

onReachBottom(() => (tab.value === 'mine' ? mine.loadNext() : notices.loadNext()))
</script>

<style lang="scss" scoped>
.tabs {
  margin-bottom: 24rpx;
}

.tabs .xm-seg-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  padding: 16rpx;
}

.tab-count {
  font-weight: bold;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.toolbar-search {
  flex: 1;
}

.filter {
  flex: 0 0 auto;
}

.list {
  padding-top: 8rpx;
  padding-bottom: 8rpx;
}

.msg {
  padding: 22rpx 0;
}

.msg + .msg {
  border-top: 1rpx solid var(--xm-border);
}

.msg-head {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.msg-title {
  flex: 1;
  min-width: 0;
  font-size: 28rpx;
  color: var(--xm-text-2);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.msg.is-unread .msg-title {
  font-weight: bold;
  color: var(--xm-text);
}

.msg-content {
  margin-top: 8rpx;
  font-size: 26rpx;
  line-height: 1.6;
  color: var(--xm-text-2);
}

.msg-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8rpx;
}

.msg-time {
  font-size: 22rpx;
  color: var(--xm-text-3);
}

.msg-del {
  display: flex;
  padding: 8rpx 0 8rpx 24rpx;
  color: var(--xm-text-3);
}

.notice-title {
  font-size: 30rpx;
  font-weight: bold;
  color: var(--xm-text);
}

.notice-content {
  margin-top: 10rpx;
  font-size: 26rpx;
  line-height: 1.6;
  color: var(--xm-text-2);
}

.notice-meta {
  margin-top: 12rpx;
  font-size: 22rpx;
  color: var(--xm-text-3);
}

.detail-title {
  font-size: 34rpx;
  font-weight: bold;
  line-height: 1.4;
  color: var(--xm-text);
}

.detail-meta {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.detail-content {
  margin: 24rpx 0 8rpx;
  font-size: 28rpx;
  line-height: 1.8;
  color: var(--xm-text);
  white-space: pre-wrap;
  word-break: break-all;
}

.detail-close {
  margin-top: 24rpx;
}

.compose-content {
  min-height: 300rpx;
}

.compose-count {
  margin-top: 8rpx;
  text-align: right;
  font-size: 22rpx;
  color: var(--xm-text-3);
}
</style>

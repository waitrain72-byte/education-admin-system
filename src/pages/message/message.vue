<template>
  <view
    class="xm-page xm-page-narrow"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 操作区 -->
    <view
      class="xm-card xm-row"
      v-if="messageStore.messages.length"
    >
      <button
        class="xm-btn xm-btn-plain"
        style="flex: 1"
        @click="markAllRead"
      >
        {{ $t('message.markAllRead') }}
      </button>
      <button
        class="xm-btn xm-btn-danger clear-btn"
        @click="clearAll"
      >
        {{ $t('message.clear') }}
      </button>
    </view>

    <xm-empty
      v-if="!messageStore.messages.length"
      icon="bell"
      :text="$t('message.empty')"
    />

    <view
      v-for="m in messageStore.messages"
      :key="m.id"
      class="xm-card msg-card"
      @click="openMessage(m)"
    >
      <view class="xm-between">
        <view class="xm-row msg-title-row">
          <view
            v-if="!m.read"
            class="unread-dot"
          ></view>
          <view
            class="xm-value msg-title"
            :class="{ unread: !m.read }"
            >{{ m.title }}</view
          >
        </view>
        <view class="xm-label">{{ m.time }}</view>
      </view>
      <view
        v-if="m.content"
        class="xm-label msg-content"
        >{{ m.content }}</view
      >
    </view>
    <xm-loader />
  </view>
</template>

<script setup>
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { ensureLoggedIn } from '@/utils/authGuard'
import { useMessageStore } from '@/stores/message'
import { resetWsUnread } from '@/utils/websocket'
import { t } from '@/i18n'
import { confirm } from '@/utils/confirm'

const userStore = useUserStore()
const messageStore = useMessageStore()

const markAllRead = () => messageStore.markAllRead()

const clearAll = () => {
  confirm(t('message.clearConfirm'), { title: t('message.clear') }).then((ok) => ok && messageStore.clear())
}

// 点击消息：按标题关键词跳转对应功能页（无匹配停留原页）
const openMessage = (m) => {
  const path = messageStore.open(m)
  if (path) uni.navigateTo({ url: path })
}

onShow(() => {
  if (!ensureLoggedIn()) return
  uni.setNavigationBarTitle({ title: t('menu.message') })
  messageStore.loadForUser(userStore.accountKey)
  // 打开消息中心即清除「首页」tab 推送角标（单条消息的已读在点击时各自标记）
  resetWsUnread()
})
</script>

<style lang="scss" scoped>
.msg-title-row {
  flex: 1;
  min-width: 0;
}

.msg-title {
  font-weight: normal;
  flex: 1;
  min-width: 0;
}

.msg-title.unread {
  font-weight: bold;
}

.unread-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: var(--xm-danger);
  flex-shrink: 0;
}

.msg-content {
  margin-top: 10rpx;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.clear-btn {
  flex: 1;
  margin-left: 16rpx;
}
</style>

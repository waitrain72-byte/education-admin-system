<template>
  <view
    class="xm-page"
    :class="themeClass"
  >
    <!-- 搜索区 -->
    <view class="xm-card">
      <view
        class="xm-form-item"
        style="margin-bottom: 16rpx"
      >
        <input
          class="xm-input"
          v-model="teacher"
          :placeholder="$t('pages.comment.teacherPlaceholder')"
        />
      </view>
      <view class="xm-row">
        <input
          class="xm-input"
          style="flex: 1"
          v-model="content"
          :placeholder="$t('pages.comment.contentPlaceholder')"
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
    </view>

    <!-- 列表 -->
    <view
      v-if="!list.length && !loading"
      class="xm-empty"
      >{{ $t('common.empty') }}</view
    >

    <view
      v-for="row in list"
      :key="row.id"
      class="xm-card"
    >
      <view class="xm-between">
        <view
          class="xm-value"
          style="font-weight: bold"
          >{{ row.name }}</view
        >
        <view class="xm-label">ID: {{ row.id }}</view>
      </view>
      <view
        class="xm-row"
        style="flex-wrap: wrap; margin-top: 12rpx"
      >
        <view class="xm-label field">{{ $t('pages.comment.teacherName') }}: {{ row.teacher }}</view>
        <view class="xm-label field">{{ $t('pages.comment.time') }}: {{ row.time }}</view>
      </view>
      <view style="margin-top: 8rpx">
        <view class="xm-label">{{ $t('pages.comment.content') }}</view>
        <view
          class="xm-value"
          style="margin-top: 4rpx"
          >{{ row.content }}</view
        >
      </view>

      <!-- 操作：仅管理员可删除（与 Web 端一致） -->
      <view
        class="xm-actions"
        v-if="userStore.role === 'ADMIN'"
      >
        <button
          class="xm-btn xm-btn-danger"
          @click="del(row.id)"
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
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { useCrud } from '@/composables/useCrud'
import { t } from '@/i18n'

const userStore = useUserStore()
const teacher = ref('')
const content = ref('')

const { list, loading, finished, load, loadNext, search, del } = useCrud({
  url: '/comment',
  getParams: () => ({ teacher: teacher.value, content: content.value }),
})

const onReset = () => {
  teacher.value = ''
  content.value = ''
  search()
}

onShow(() => {
  uni.setNavigationBarTitle({ title: t('menu.comment') })
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  load(true)
})

onReachBottom(() => loadNext())
</script>

<style lang="scss" scoped>
.field {
  width: 50%;
  margin-bottom: 8rpx;
}
</style>

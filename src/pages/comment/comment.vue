<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 搜索区 -->
    <view class="xm-card">
      <view class="xm-form-item search-item">
        <input
          class="xm-input"
          v-model="query.teacher"
          :placeholder="$t('pages.comment.teacherPlaceholder')"
          confirm-type="search"
          @confirm="search"
        />
      </view>
      <view class="xm-row">
        <input
          class="xm-input"
          style="flex: 1"
          v-model="query.content"
          :placeholder="$t('pages.comment.contentPlaceholder')"
          confirm-type="search"
          @confirm="search"
        />
        <button
          class="xm-btn xm-btn-primary"
          @click="search"
        >
          {{ $t('common.search') }}
        </button>
        <button
          class="xm-btn xm-btn-plain"
          @click="resetQuery"
        >
          {{ $t('common.reset') }}
        </button>
      </view>
    </view>

    <!-- 列表 -->
    <xm-empty
      v-if="!list.length && !loading"
      :action-text="$t('common.reload')"
      @action="load(true)"
    />

    <!-- 列表：手机单列，平板 ≥720px 两列、≥1248px 三列（theme.scss .xm-list） -->
    <view class="xm-list">
      <view
        v-for="row in list"
        :key="row.id"
        class="xm-card"
      >
        <view class="xm-between">
          <view class="xm-row xm-card-head">
            <text class="xm-card-name xm-ellipsis">{{ row.name }}</text>
          </view>
          <text class="xm-card-no">#{{ row._index }}</text>
        </view>
        <view class="xm-meta">
          <view class="xm-meta-item">
            <xm-icon
              name="briefcase"
              :size="28"
            />
            <text class="xm-meta-text">{{ row.teacher || '-' }}</text>
          </view>
          <view class="xm-meta-item">
            <xm-icon
              name="clock"
              :size="28"
            />
            <text class="xm-meta-text">{{ row.time || '-' }}</text>
          </view>
        </view>
        <view
          v-if="row.content"
          class="xm-quote"
          >{{ row.content }}</view
        >
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
    </view>

    <xm-list-footer
      :visible="!!list.length"
      :loading="loading"
      :finished="finished()"
      @load-more="loadNext"
    />
    <xm-loader />
  </view>
</template>

<script setup>
import { useUserStore } from '@/stores/user'
import { useListPage } from '@/composables/useListPage'
import { commentApi } from '@/api'

const userStore = useUserStore()

// 评教记录只读，管理员可删除（与 Web 端一致）
const { list, loading, finished, query, load, loadNext, search, resetQuery, del } = useListPage({
  api: commentApi,
  title: 'menu.comment',
  query: { teacher: '', content: '' },
})
</script>

<style lang="scss" scoped>
/* 搜索区第一行输入框与第二行的间距 */
.search-item {
  margin-bottom: 16rpx;
}
</style>

<template>
  <view
    class="xm-page"
    :class="themeClass"
  >
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
        <view
          class="xm-tag"
          :class="statusTagClass(row.status)"
          >{{ row.status }}</view
        >
      </view>
      <view
        class="xm-row"
        style="flex-wrap: wrap; margin-top: 12rpx"
      >
        <view class="xm-label field">{{ $t('pages.choice.id') }}: {{ row.id }}</view>
        <view class="xm-label field">{{ $t('pages.choice.courseType') }}: {{ row.type }}</view>
        <view class="xm-label field">{{ $t('pages.choice.teacherName') }}: {{ row.teacherName }}</view>
        <view class="xm-label field">{{ $t('pages.choice.credit') }}: {{ row.score }}</view>
        <view class="xm-label field">{{ $t('pages.choice.studentCount') }}: {{ row.num }}</view>
        <view class="xm-label field">{{ $t('pages.choice.room') }}: {{ row.room }}</view>
        <view class="xm-label field">{{ $t('pages.choice.week') }}: {{ row.week }}</view>
        <view class="xm-label field">{{ $t('pages.choice.segment') }}: {{ row.segment }}</view>
        <view class="xm-label field">{{ $t('pages.choice.studentName') }}: {{ row.studentName }}</view>
      </view>

      <!-- 操作：仅学生可见（与 Web 端 show-actions 一致） -->
      <view
        class="xm-actions"
        v-if="userStore.role === 'STUDENT'"
      >
        <button
          class="xm-btn xm-btn-danger"
          :disabled="row.status !== '未开课'"
          @click="onCancel(row)"
        >
          {{ $t('pages.choice.cancelChoice') }}
        </button>
        <button
          class="xm-btn xm-btn-plain"
          :disabled="row.status !== '已结课'"
          @click="initComment(row)"
        >
          {{ $t('pages.choice.comment') }}
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

    <!-- 评教表单（底部弹层） -->
    <view
      v-if="commentVisible"
      class="xm-mask"
      @click="commentVisible = false"
    ></view>

    <view
      v-if="commentVisible"
      class="xm-popup"
    >
      <view class="xm-popup-title">{{ $t('pages.choice.dialogTitle') }}</view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.choice.contentLabel') }}</view>
        <textarea
          class="xm-textarea"
          v-model="commentForm.content"
          :placeholder="$t('pages.choice.ruleContentRequired')"
        />
      </view>
      <view
        class="xm-row"
        style="margin-top: 16rpx"
      >
        <button
          class="xm-btn xm-btn-plain"
          style="flex: 1"
          @click="commentVisible = false"
        >
          {{ $t('common.cancel') }}
        </button>
        <button
          class="xm-btn xm-btn-primary"
          style="flex: 1"
          @click="saveComment"
        >
          {{ $t('common.ok') }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { useCrud } from '@/composables/useCrud'
import { del as delRequest, post } from '@/utils/request'
import { t, apiMessage } from '@/i18n'

const userStore = useUserStore()

// 分页复用通用 CRUD；取消选课确认文案自定义（与 Web 端一致）
const { list, loading, finished, load, loadNext } = useCrud({ url: '/choice' })

const commentVisible = ref(false)
const commentForm = ref({})

const statusTagClass = (status) => {
  if (status === '已结课') return 'xm-tag-success'
  if (status === '未开课') return 'xm-tag-warning'
  return ''
}

// 取消选课：确认文案与 Web 端一致（仅未开课可取消，disabled 已在按钮上控制）
const onCancel = (row) => {
  uni.showModal({
    title: t('common.confirmDeleteTitle'),
    content: t('pages.choice.deleteConfirm'),
    success: async (res) => {
      if (!res.confirm) return
      try {
        const r = await delRequest(`/choice/delete/${row.id}`)
        if (r.data && r.data.code === '200') {
          uni.showToast({ title: t('common.operationSuccess'), icon: 'success' })
          load(true)
        } else {
          uni.showToast({ title: apiMessage(r.data), icon: 'none' })
        }
      } catch {
        // 请求层已统一提示
      }
    },
  })
}

// 评教：与 Web 端 initComment 一致，携带整行课程信息
const initComment = (row) => {
  commentForm.value = JSON.parse(JSON.stringify(row))
  commentVisible.value = true
}

// 评教提交固定走 /comment/add（与普通 CRUD 不同，与 Web 端保持一致）
const saveComment = () => {
  if (!commentForm.value.content) {
    uni.showToast({ title: t('pages.choice.ruleContentRequired'), icon: 'none' })
    return
  }
  const data = {
    name: commentForm.value.name,
    teacher: commentForm.value.teacherName,
    student: userStore.user.name,
    content: commentForm.value.content,
  }
  post('/comment/add', data).then((res) => {
    if (res.data && res.data.code === '200') {
      uni.showToast({ title: t('pages.choice.commentSuccess'), icon: 'success' })
      commentVisible.value = false
    } else {
      uni.showToast({ title: apiMessage(res.data), icon: 'none' })
    }
  })
}

onShow(() => {
  uni.setNavigationBarTitle({ title: t('menu.choice') })
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

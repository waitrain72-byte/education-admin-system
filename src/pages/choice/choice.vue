<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
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
        <!-- 与课程页同一版式：标题行 + 标签行 + 图标信息栅格；枚举值按当前语言翻译 -->
        <view class="xm-between">
          <text class="xm-card-name xm-ellipsis">{{ row.name }}</text>
          <text
            v-if="row.status"
            class="xm-tag"
            :class="enumTag('courseStatus', row.status)"
            >{{ enumLabel('courseStatus', row.status) }}</text
          >
        </view>
        <view class="xm-tags">
          <text
            v-if="row.type"
            class="xm-tag xm-tag-brand"
            >{{ enumLabel('courseType', row.type) }}</text
          >
          <text
            v-if="row.score != null && row.score !== ''"
            class="xm-tag"
            >{{ $t('pages.course.credit', { n: row.score }) }}</text
          >
          <text
            v-if="row.num"
            class="xm-tag"
            >{{ $t('pages.course.people', { n: row.num }) }}</text
          >
          <text class="xm-tags-end">#{{ row._index }}</text>
        </view>
        <view class="xm-meta">
          <view class="xm-meta-item">
            <xm-icon
              name="briefcase"
              :size="28"
            />
            <text class="xm-meta-text">{{ row.teacherName || '-' }}</text>
          </view>
          <view class="xm-meta-item">
            <xm-icon
              name="map-pin"
              :size="28"
            />
            <text class="xm-meta-text">{{ row.room || '-' }}</text>
          </view>
          <view class="xm-meta-item full">
            <xm-icon
              name="clock"
              :size="28"
            />
            <text class="xm-meta-text">{{ scheduleText(row.week, row.segment) || '-' }}</text>
          </view>
          <!-- 教师 / 管理员视角才需要看是哪位学生选的课 -->
          <view
            v-if="userStore.role !== 'STUDENT'"
            class="xm-meta-item full"
          >
            <xm-icon
              name="user"
              :size="28"
            />
            <text class="xm-meta-text">{{ row.studentName || '-' }}</text>
          </view>
        </view>

        <!-- 操作：仅学生可见（与 Web 端 show-actions 一致） -->
        <view
          class="xm-actions"
          v-if="userStore.role === 'STUDENT'"
        >
          <button
            class="xm-btn xm-btn-danger"
            :disabled="row.status !== '未开课'"
            @click="del(row.id)"
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
    </view>

    <xm-list-footer
      :visible="!!list.length"
      :loading="loading"
      :finished="finished()"
      @load-more="loadNext"
    />

    <!-- 评教表单（底部弹层） -->
    <xm-form-popup
      :visible="commentVisible"
      :saving="commenting"
      :title="$t('pages.choice.commentTitle', { name: commentForm.name || '' })"
      @close="commentVisible = false"
      @save="saveComment"
    >
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.choice.contentLabel') }}</view>
        <textarea
          class="xm-textarea"
          v-model="commentForm.content"
          :placeholder="$t('pages.choice.ruleContentRequired')"
        />
      </view>
    </xm-form-popup>
    <xm-loader />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { useListPage } from '@/composables/useListPage'
import { choiceApi, commentApi } from '@/api'
import { t } from '@/i18n'
import { enumLabel, enumTag, scheduleText } from '@/utils/enums'

const userStore = useUserStore()

// 分页复用列表骨架；取消选课即删除选课记录，确认文案与 Web 端一致（仅未开课可取消，按钮已按状态禁用）
const { list, loading, finished, load, loadNext, del } = useListPage({
  api: choiceApi,
  title: 'menu.choice',
  deleteConfirm: 'pages.choice.deleteConfirm',
})

const commentVisible = ref(false)
const commentForm = ref({})
// 评教提交中：确定按钮转圈禁用，防止连点重复评教
const commenting = ref(false)

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
  if (commenting.value) return
  commenting.value = true
  commentApi
    .add(data)
    .then(() => {
      uni.showToast({ title: t('pages.choice.commentSuccess'), icon: 'success' })
      commentVisible.value = false
    })
    .catch(() => {
      // 重复评教等提示已由请求层统一弹出
    })
    .finally(() => {
      commenting.value = false
    })
}
</script>

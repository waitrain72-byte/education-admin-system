<template>
  <view
    class="xm-page"
    :class="themeClass"
  >
    <!-- 搜索区 -->
    <view class="xm-card xm-row">
      <input
        class="xm-input"
        style="flex: 1"
        v-model="content"
        :placeholder="$t('pages.homework.contentPlaceholder')"
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

    <!-- 操作区：学生可提交作业 -->
    <view
      class="xm-card"
      v-if="userStore.role === 'STUDENT'"
    >
      <button
        class="xm-btn xm-btn-primary xm-btn-block"
        @click="onAdd"
      >
        {{ $t('pages.homework.submitHomework') }}
      </button>
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
        <!-- 主标题：作业内容；右侧批改状态语义化（已批改绿/待批改橙） -->
        <view class="xm-value xm-ellipsis item-title">{{ row.content }}</view>
        <view
          class="xm-tag status-tag"
          :class="row.score ? 'xm-tag-success' : 'xm-tag-warning'"
          >{{ row.score ? row.score + ' 分' : '待批改' }}</view
        >
      </view>
      <view class="item-meta">
        <text>{{ row.courseName }} · </text>
        <text>{{ userStore.role === 'TEACHER' ? row.studentName : row.teacherName }}</text>
      </view>
      <view
        v-if="row.file"
        class="xm-between"
        style="margin-top: 12rpx"
      >
        <view class="xm-label file-name xm-ellipsis">{{ fileNameOf(row.file) }}</view>
        <button
          class="xm-btn xm-btn-plain"
          style="height: 56rpx; line-height: 56rpx; flex-shrink: 0"
          @click="down(row.file)"
        >
          {{ $t('pages.homework.download') }}
        </button>
      </view>
      <view
        v-if="row.score && row.descr"
        class="item-descr"
        >{{ row.descr }}</view
      >

      <!-- 操作：学生（未打分可编辑/可删除）、教师（打分），与 Web 端一致 -->
      <view
        class="xm-actions"
        v-if="userStore.role !== 'ADMIN'"
      >
        <button
          v-if="userStore.role === 'STUDENT' && !row.score"
          class="xm-btn xm-btn-plain"
          @click="onEdit(row)"
        >
          {{ $t('common.edit') }}
        </button>
        <button
          v-if="userStore.role === 'STUDENT'"
          class="xm-btn xm-btn-danger"
          @click="del(row.id)"
        >
          {{ $t('common.delete') }}
        </button>
        <button
          v-if="userStore.role === 'TEACHER'"
          class="xm-btn xm-btn-primary"
          @click="handleCheck(row)"
        >
          {{ $t('pages.homework.grade') }}
        </button>
      </view>
    </view>

    <xm-list-footer
      :visible="!!list.length"
      :loading="loading"
      :finished="finished()"
      @load-more="loadNext"
    />

    <!-- 作业提交/编辑表单（底部弹层，学生） -->
    <view
      v-if="formVisible"
      class="xm-mask"
      @click="closeForm"
    ></view>

    <view
      v-if="formVisible"
      class="xm-popup"
    >
      <view class="xm-popup-title">{{ $t('pages.homework.dialogTitle') }}</view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.homework.contentLabel') }}</view>
        <textarea
          class="xm-textarea"
          v-model="form.content"
          :placeholder="$t('pages.homework.contentPlaceholder')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.homework.selectCourse') }}</view>
        <picker
          :range="courseData"
          range-key="name"
          :value="formCourseIndex"
          @change="onFormCourseChange"
        >
          <view
            class="xm-input picker-display"
            :class="{ 'picker-placeholder': !form.courseId }"
          >
            {{ form.courseId ? courseNameOf(form.courseId) : $t('pages.homework.coursePlaceholder') }}
          </view>
        </picker>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.homework.fileLabel') }}</view>
        <button
          class="xm-btn xm-btn-primary"
          @click="chooseFile"
        >
          {{ $t('pages.homework.uploadFile') }}
        </button>
        <view
          class="xm-label file-name"
          v-if="form.file"
          style="margin-top: 8rpx"
          >{{ fileNameOf(form.file) }}</view
        >
      </view>
      <view
        class="xm-row"
        style="margin-top: 16rpx"
      >
        <button
          class="xm-btn xm-btn-plain"
          style="flex: 1"
          @click="closeForm"
        >
          {{ $t('common.cancel') }}
        </button>
        <button
          class="xm-btn xm-btn-primary"
          style="flex: 1"
          @click="save"
        >
          {{ $t('common.ok') }}
        </button>
      </view>
    </view>

    <!-- 打分弹层（教师） -->
    <view
      v-if="checkVisible"
      class="xm-mask"
      @click="checkVisible = false"
    ></view>

    <view
      v-if="checkVisible"
      class="xm-popup"
    >
      <view class="xm-popup-title">{{ $t('pages.homework.checkDialogTitle') }}</view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.homework.scoreLabel') }}</view>
        <input
          class="xm-input"
          v-model="form.score"
          type="digit"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.homework.descrLabel') }}</view>
        <textarea
          class="xm-textarea"
          v-model="form.descr"
        />
      </view>
      <view
        class="xm-row"
        style="margin-top: 16rpx"
      >
        <button
          class="xm-btn xm-btn-plain"
          style="flex: 1"
          @click="checkVisible = false"
        >
          {{ $t('common.cancel') }}
        </button>
        <button
          class="xm-btn xm-btn-primary"
          style="flex: 1"
          @click="check"
        >
          {{ $t('common.ok') }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { useCrud } from '@/composables/useCrud'
import { get, put, del as delRequest, resolveFileUrl } from '@/utils/request'
import { baseUrl } from '@/utils/config'
import { t, apiMessage } from '@/i18n'

const userStore = useUserStore()
const content = ref('')
const checkVisible = ref(false)
const courseData = ref([])

const { list, loading, finished, form, formVisible, load, loadNext, search, save, closeForm } = useCrud({
  url: '/homework',
  getParams: () => ({ content: content.value }),
  validate: (f) => {
    if (!f.courseId) return t('pages.homework.ruleCourseRequired')
    if (!f.content) return t('pages.homework.ruleContentRequired')
    return ''
  },
})

const formCourseIndex = computed(() => courseData.value.findIndex((c) => c.courseId === form.value.courseId))

const courseNameOf = (id) => {
  const c = courseData.value.find((item) => item.courseId === id)
  return c ? c.name : ''
}

const fileNameOf = (url) => {
  if (!url) return ''
  const path = String(url).split('?')[0]
  return decodeURIComponent(path.slice(path.lastIndexOf('/') + 1)) || url
}

// 学生已选课程列表（与 Web 端 loadCourse 一致）
const loadCourse = () => {
  get('/choice/selectAll', { studentId: userStore.user.id }).then((res) => {
    if (res.data && res.data.code === '200') {
      courseData.value = res.data.data || []
    } else {
      uni.showToast({ title: apiMessage(res.data), icon: 'none' })
    }
  })
}

const onFormCourseChange = (e) => {
  const c = courseData.value[e.detail.value]
  if (c) form.value.courseId = c.courseId
}

const onAdd = () => {
  form.value = { studentId: userStore.user.id }
  formVisible.value = true
}

// 学生编辑：重置回待审核并清空打分说明（与 Web 端 handleEdit 一致）
const onEdit = (row) => {
  form.value = JSON.parse(JSON.stringify(row))
  form.value.status = '待审核'
  form.value.descr = ''
  formVisible.value = true
}

const handleCheck = (row) => {
  form.value = JSON.parse(JSON.stringify(row))
  checkVisible.value = true
}

// 教师打分：与 Web 端 check 一致，直接走 /homework/update
const check = () => {
  put('/homework/update', form.value).then((res) => {
    if (res.data && res.data.code === '200') {
      uni.showToast({ title: t('common.operationSuccess'), icon: 'success' })
      load(true)
      checkVisible.value = false
    } else {
      uni.showToast({ title: apiMessage(res.data), icon: 'none' })
    }
  })
}

// 附件上传：选图后上传到 /files/upload，成功后保存返回的 URL（与 Web 端 el-upload 一致）
const chooseFile = () => {
  uni.chooseImage({
    count: 1,
    success: (res) => {
      const filePath = res.tempFilePaths[0]
      uni.uploadFile({
        url: `${baseUrl}/files/upload`,
        filePath,
        name: 'file',
        header: { token: userStore.token },
        success: (up) => {
          try {
            const data = typeof up.data === 'string' ? JSON.parse(up.data) : up.data
            if (data.code === '200') {
              form.value.file = data.data
              uni.showToast({ title: t('common.operationSuccess'), icon: 'success' })
            } else {
              uni.showToast({ title: apiMessage(data), icon: 'none' })
            }
          } catch {
            uni.showToast({ title: t('request.failed'), icon: 'none' })
          }
        },
        fail: () => {
          uni.showToast({ title: t('request.failed'), icon: 'none' })
        },
      })
    },
  })
}

// 附件下载查看：图片直接预览，其余走下载后打开
const down = (rawUrl) => {
  // 历史数据存的是老绝对地址：先归一成当前 baseUrl 的完整地址（真机才能访问）
  const url = resolveFileUrl(rawUrl)
  if (!url) return
  if (/\.(png|jpe?g|gif|webp|bmp)(\?.*)?$/i.test(url)) {
    uni.previewImage({ urls: [url] })
    return
  }
  uni.downloadFile({
    url,
    success: (res) => {
      if (res.statusCode === 200) {
        uni.openDocument({
          filePath: res.tempFilePath,
          showMenu: true,
          fail: () => uni.setClipboardData({ data: url }),
        })
      } else {
        uni.setClipboardData({ data: url })
      }
    },
    fail: () => {
      uni.setClipboardData({ data: url })
    },
  })
}

// 删除确认文案与 Web 端一致
const del = (id) => {
  uni.showModal({
    title: t('common.confirmDeleteTitle'),
    content: t('pages.homework.deleteConfirm'),
    success: async (res) => {
      if (!res.confirm) return
      try {
        const r = await delRequest(`/homework/delete/${id}`)
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

const onReset = () => {
  content.value = ''
  search()
}

onShow(() => {
  uni.setNavigationBarTitle({ title: t('menu.homework') })
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  load(true)
  loadCourse()
})

onReachBottom(() => loadNext())
</script>

<style lang="scss" scoped>
/* 主标题：作业内容单行省略，右侧状态标签不被挤压 */
.item-title {
  font-weight: bold;
  flex: 1;
  min-width: 0;
}

/* 状态标签：禁止收缩换行 */
.status-tag {
  flex-shrink: 0;
  white-space: nowrap;
}

/* 次要信息行：课程/师生弱化小字 */
.item-meta {
  font-size: 24rpx;
  color: var(--xm-text-2);
  margin-top: 12rpx;
}

/* 教师评语气泡 */
.item-descr {
  font-size: 26rpx;
  color: var(--xm-text);
  background: var(--xm-bg-input);
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
  margin-top: 12rpx;
}

.picker-display {
  display: flex;
  align-items: center;
}

.picker-placeholder {
  color: var(--xm-text-2);
}

.file-name {
  flex: 1;
  word-break: break-all;
  margin-right: 12rpx;
}
</style>

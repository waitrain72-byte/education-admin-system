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
        <view class="xm-row">
          <view
            class="xm-value"
            style="font-weight: bold"
            >{{ row.courseName }}</view
          >
          <view
            class="xm-tag"
            v-if="row.score"
            :class="scoreTagClass(row.score)"
            >{{ row.score }}</view
          >
        </view>
        <view class="xm-label">{{ $t('pages.homework.id') }}: {{ row._index }}</view>
      </view>
      <view
        class="xm-row"
        style="flex-wrap: wrap; margin-top: 12rpx"
      >
        <view class="xm-label field">{{ $t('pages.homework.studentName') }}: {{ row.studentName }}</view>
        <view class="xm-label field">{{ $t('pages.homework.teacherName') }}: {{ row.teacherName }}</view>
      </view>
      <view style="margin-top: 8rpx">
        <view class="xm-label">{{ $t('pages.homework.contentLabel') }}</view>
        <view
          class="xm-value"
          style="margin-top: 4rpx"
          >{{ row.content }}</view
        >
      </view>
      <view
        v-if="row.file"
        class="xm-between"
        style="margin-top: 12rpx"
      >
        <view class="xm-label file-name">{{ $t('pages.homework.fileLabel') }}: {{ fileNameOf(row.file) }}</view>
        <button
          class="xm-btn xm-btn-plain"
          style="height: 56rpx; line-height: 56rpx"
          @click="down(row.file)"
        >
          {{ $t('pages.homework.download') }}
        </button>
      </view>
      <view
        v-if="row.score"
        style="margin-top: 8rpx"
      >
        <view class="xm-label">{{ $t('pages.homework.scoreLabel') }}: {{ row.score }}</view>
        <view
          v-if="row.descr"
          class="xm-value"
          style="margin-top: 4rpx"
          >{{ row.descr }}</view
        >
      </view>

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

    <xm-list-footer :visible="!!list.length" :loading="loading" :finished="finished()" @load-more="loadNext" />

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
import { get, put, del as delRequest } from '@/utils/request'
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

const scoreTagClass = (score) => {
  const num = Number(score)
  if (Number.isNaN(num)) return ''
  if (num >= 80) return 'xm-tag-success'
  if (num >= 60) return 'xm-tag-warning'
  return 'xm-tag-danger'
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
const down = (url) => {
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
.field {
  width: 50%;
  margin-bottom: 8rpx;
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

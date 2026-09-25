<template>
  <xm-form-popup
    :visible="visible"
    :saving="saving"
    :title="form.id ? $t('pages.homework.editTitle') : $t('pages.homework.submitTitle')"
    @close="$emit('close')"
    @save="$emit('save')"
  >
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
      <xm-picker
        v-model="form.courseId"
        :options="courses"
        label-key="name"
        value-key="courseId"
        :placeholder="$t('pages.homework.coursePlaceholder')"
      />
    </view>
    <view class="xm-form-item">
      <view class="xm-form-label">{{ $t('pages.homework.fileLabel') }}</view>
      <!-- 已传附件：文件名 + 移除 -->
      <view
        v-if="form.file"
        class="attach-chip"
      >
        <xm-icon
          :name="isImageFile(form.file) ? 'image' : 'paperclip'"
          :size="32"
        />
        <text class="attach-name xm-ellipsis">{{ fileNameOf(form.file) }}</text>
        <view
          class="attach-remove"
          @click="form.file = ''"
        >
          <xm-icon
            name="close"
            :size="28"
          />
        </view>
      </view>
      <!-- 选择来源：相册图片 / 微信聊天里的文档；上传中按钮显示进度并禁用 -->
      <button
        class="xm-btn xm-btn-plain attach-btn"
        :loading="uploading"
        :disabled="uploading"
        @click="pickAttachment"
      >
        {{
          uploading
            ? $t('pages.homework.uploading', { p: uploadPercent })
            : form.file
              ? $t('pages.homework.reupload')
              : $t('pages.homework.uploadFile')
        }}
      </button>
      <view class="xm-label attach-tip">{{ $t('pages.homework.fileTip', { mb: MAX_UPLOAD_MB }) }}</view>
    </view>
  </xm-form-popup>
</template>

<script setup>
/**
 * 作业提交 / 编辑表单（学生，底部弹层）：内容 + 课程 + 附件上传。
 * form 由列表页的 useListPage 持有，这里通过 v-model:form 直接编辑它的字段；
 * 上传状态通过 v-model:uploading 交给页面，附件没传完时页面的 validate 会拦截保存。
 */
import { ref } from 'vue'
import { t } from '@/i18n'
import { chooseImage, chooseChatFile, canChooseChatFile, uploadFile, MAX_UPLOAD_MB } from '@/utils/upload'
import { fileNameOf, isImageFile } from '@/utils/attachment'

const form = defineModel('form', { type: Object, required: true })
const uploading = defineModel('uploading', { type: Boolean, default: false })
defineProps({
  visible: { type: Boolean, default: false },
  saving: { type: Boolean, default: false },
  /** 学生已选课程（选课记录，下拉取 courseId） */
  courses: { type: Array, default: () => [] },
})
defineEmits(['close', 'save'])

// 附件上传进度（按钮上显示百分比）
const uploadPercent = ref(0)

// 附件上传：相册图片 / 微信聊天文档二选一，上传到 /files/upload 后保存返回的地址（与 Web 端 el-upload 一致）
// 上传中不弹全屏蒙层，进度直接显示在按钮上
const uploadFrom = async (source) => {
  const picked = source === 'chat' ? await chooseChatFile() : await chooseImage()
  if (!picked) return
  // 记下发起上传时的表单：上传途中关掉弹层又打开别的作业时，结果不会写进另一条作业
  const target = form.value
  uploading.value = true
  uploadPercent.value = 0
  try {
    target.file = await uploadFile(picked.path, {
      loading: false,
      onProgress: (p) => (uploadPercent.value = p),
    })
    uni.showToast({ title: t('common.operationSuccess'), icon: 'success' })
  } catch {
    // 提示已由上传层统一弹出
  } finally {
    uploading.value = false
  }
}

// 微信小程序可从聊天记录选 PDF / Word 等文档；其它平台只能选图片
const pickAttachment = () => {
  if (!canChooseChatFile()) {
    uploadFrom('image')
    return
  }
  uni.showActionSheet({
    itemList: [t('pages.homework.fromAlbum'), t('pages.homework.fromChat')],
    success: (res) => uploadFrom(res.tapIndex === 1 ? 'chat' : 'image'),
  })
}
</script>

<style lang="scss" scoped>
/* 已上传的附件：图标 + 文件名 + 移除按钮 */
.attach-chip {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx 20rpx;
  margin-bottom: 16rpx;
  border-radius: 16rpx;
  color: var(--xm-brand);
  background: var(--xm-brand-soft);
}

.attach-name {
  flex: 1;
  font-size: 26rpx;
  color: var(--xm-text);
}

.attach-remove {
  display: flex;
  padding: 4rpx;
  color: var(--xm-text-2);
}

.attach-btn {
  width: 100%;
}

.attach-tip {
  margin-top: 8rpx;
}
</style>

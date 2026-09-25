<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 搜索区 -->
    <xm-search-card
      v-model="query.username"
      :placeholder="$t('pages.student.searchPlaceholder')"
      @search="search"
      @reset="resetQuery"
    />

    <!-- 操作区：新增 / 批量管理 -->
    <xm-action-bar
      :manage-mode="manageMode"
      :total="total"
      :selected-count="selectedIds.length"
      @add="handleAdd()"
      @toggle-manage="toggleManage"
      @del-batch="delBatch"
    />

    <!-- 列表 -->
    <xm-empty
      v-if="!list.length && !loading"
      :action-text="$t('common.reload')"
      @action="search"
    />

    <!-- 列表：手机单列，平板 ≥720px 两列、≥1248px 三列（theme.scss .xm-list） -->
    <view class="xm-list">
      <view
        v-for="item in list"
        :key="item.id"
        class="xm-card"
      >
        <view class="xm-between">
          <view class="xm-row xm-card-head">
            <!-- 批量管理模式下显示勾选框 -->
            <checkbox
              v-if="manageMode"
              :checked="selectedIds.includes(item.id)"
              style="transform: scale(0.8)"
              @click.stop="toggleSelect(item.id)"
            />
            <image
              v-if="item.avatar"
              lazy-load
              :src="resolveFileUrl(item.avatar)"
              class="xm-avatar"
              mode="aspectFill"
            />
            <view
              v-else
              class="xm-avatar xm-avatar-text"
              >{{ (item.name || item.username || '?').slice(0, 1) }}</view
            >
            <view class="xm-card-head">
              <view class="xm-card-name xm-ellipsis">{{ item.name || item.username }}</view>
              <view class="xm-card-sub xm-ellipsis">{{ item.username }}</view>
            </view>
          </view>
          <text class="xm-card-no">#{{ item._index }}</text>
        </view>
        <view class="xm-tags">
          <text
            v-if="item.className"
            class="xm-tag xm-tag-brand"
            >{{ item.className }}</text
          >
          <text
            v-if="item.score != null && item.score !== ''"
            class="xm-tag"
            >{{ $t('pages.course.credit', { n: item.score }) }}</text
          >
        </view>
        <view class="xm-meta">
          <view class="xm-meta-item">
            <xm-icon
              name="landmark"
              :size="28"
            />
            <text class="xm-meta-text">{{ item.collegeName || '-' }}</text>
          </view>
          <view class="xm-meta-item">
            <xm-icon
              name="book"
              :size="28"
            />
            <text class="xm-meta-text">{{ item.specialityName || '-' }}</text>
          </view>
        </view>
        <view
          class="xm-actions"
          v-if="!manageMode"
        >
          <button
            v-if="isAdminOrTeacher"
            class="xm-btn xm-btn-plain"
            @click="handleEdit(item)"
          >
            {{ $t('common.edit') }}
          </button>
          <button
            v-if="isAdmin"
            class="xm-btn xm-btn-plain"
            @click="resetPassword(item)"
          >
            {{ $t('common.resetPassword') }}
          </button>
          <button
            v-if="isAdminOrTeacher"
            class="xm-btn xm-btn-danger"
            @click="del(item.id)"
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

    <!-- 新增/编辑表单（底部弹层） -->
    <xm-form-popup
      :visible="formVisible"
      :saving="saving"
      :title="$t(form.id ? 'common.editTitle' : 'common.addTitle', { name: $t('pages.student.entity') })"
      @close="closeForm"
      @save="save"
    >
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.student.avatar') }}</view>
        <view
          class="xm-row"
          v-if="form.avatar"
        >
          <image
            :src="resolveFileUrl(form.avatar)"
            class="xm-avatar"
            mode="aspectFill"
          />
        </view>
        <button
          class="xm-btn xm-btn-primary"
          @click="uploadAvatar"
        >
          {{ $t('pages.student.uploadAvatar') }}
        </button>
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.student.username') }}</view>
        <input
          class="xm-input"
          v-model="form.username"
          :placeholder="$t('pages.student.username')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.student.name') }}</view>
        <input
          class="xm-input"
          v-model="form.name"
          :placeholder="$t('pages.student.name')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.student.college') }}</view>
        <xm-picker
          v-model="form.collegeId"
          :options="collegeData"
          label-key="name"
          value-key="id"
          :placeholder="$t('pages.student.collegePlaceholder')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.student.speciality') }}</view>
        <xm-picker
          v-model="form.specialityId"
          :options="specialityData"
          label-key="name"
          value-key="id"
          :placeholder="$t('pages.student.specialityPlaceholder')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label">{{ $t('pages.student.classes') }}</view>
        <xm-picker
          v-model="form.classId"
          :options="classesData"
          label-key="name"
          value-key="id"
          :placeholder="$t('pages.student.classesPlaceholder')"
        />
      </view>
    </xm-form-popup>

    <xm-loader />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { useListPage } from '@/composables/useListPage'
import { studentApi, collegeApi, specialityApi, classesApi } from '@/api'
import { useResetPassword } from '@/composables/useResetPassword'
import { useAvatarUpload, syncCurrentUser } from '@/composables/useUserForm'
import { orNull, resolveFileUrl, SILENT } from '@/utils/request'
import { t } from '@/i18n'

const userStore = useUserStore()
// 学院 / 专业 / 班级下拉数据
const collegeData = ref([])
const specialityData = ref([])
const classesData = ref([])

const isAdmin = computed(() => userStore.role === 'ADMIN')
// Web 端 Student.vue 的编辑/删除按钮为 v-permission="['ADMIN', 'TEACHER']"
const isAdminOrTeacher = computed(() => userStore.role === 'ADMIN' || userStore.role === 'TEACHER')

// 仅管理员可见（与 Web 端路由 meta.roles 一致）；下拉选项随列表一起加载
const {
  list,
  loading,
  finished,
  total,
  form,
  formVisible,
  saving,
  selectedIds,
  query,
  manageMode,
  loadNext,
  search,
  resetQuery,
  toggleManage,
  toggleSelect,
  handleAdd,
  handleEdit,
  closeForm,
  save,
  del,
  delBatch,
} = useListPage({
  api: studentApi,
  title: 'menu.student',
  roles: ['ADMIN'],
  query: { username: '' },
  loadExtras: () => {
    loadOptions(collegeApi, collegeData)
    loadOptions(specialityApi, specialityData)
    loadOptions(classesApi, classesData)
  },
  validate: (f) => (f.username ? '' : t('pages.student.ruleUsernameRequired')),
  // 改的是当前登录学生自己时，同步头像 / 姓名
  afterSave: syncCurrentUser('STUDENT', ['avatar', 'name']),
})

const loadOptions = async (api, target) => {
  target.value = (await orNull(api.selectAll(undefined, SILENT))) || []
}

const resetPassword = useResetPassword(studentApi, 'student')
const uploadAvatar = useAvatarUpload(form)
</script>

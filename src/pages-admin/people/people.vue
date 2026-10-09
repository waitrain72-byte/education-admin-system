<template>
  <view
    class="xm-page"
    :class="themeClass"
    :style="themeStyle"
  >
    <!-- 人员（与 Web 端 views/admin/PeopleAdmin.vue 一致）：学生 / 教师 / 管理员账号，新建、编辑、重置密码、删除；
         学生按学院 / 专业 / 班级筛选。批量导入导出 Excel 在电脑上的教务后台做 -->
    <view class="xm-seg tabs">
      <view
        v-for="key in TABS"
        :key="key"
        class="xm-seg-item"
        :class="{ on: tab === key }"
        @click="switchTab(key)"
        >{{ $t('admin.people.tabs.' + key) }}</view
      >
    </view>

    <view class="filters">
      <xm-search
        v-model="keyword"
        :placeholder="$t('admin.people.search')"
      />
      <xm-org-picker
        v-if="tab === 'students'"
        v-model="orgFilter"
        :tree="orgTree"
        :placeholder="$t('admin.people.filterOrg')"
        :any-label="$t('common.all')"
        clearable
        @change="people.reload()"
      />
    </view>

    <view class="count xm-num">{{ $t('common.totalCount', { n: people.total.value }) }}</view>

    <xm-empty
      v-if="people.loaded.value && !people.loading.value && !people.list.value.length"
      icon="users"
      :text="$t('admin.people.empty')"
    />
    <view
      v-else
      class="xm-list"
    >
      <view
        v-for="row in people.list.value"
        :key="tab + row.id"
        class="xm-card person"
      >
        <view class="person-head">
          <xm-user-avatar
            :name="row.name"
            :avatar="row.avatar"
            :size="80"
          />
          <view class="person-main">
            <view class="person-name">
              <text class="xm-ellipsis">{{ row.name }}</text>
              <text
                v-if="isSelf(row)"
                class="xm-tag xm-tag-brand"
                >{{ $t('admin.people.self') }}</text
              >
            </view>
            <view class="person-sub xm-num">{{ row.username }}</view>
          </view>
        </view>

        <view
          v-if="tab === 'students'"
          class="person-info"
        >
          <text>{{ row.className || '—' }}</text>
          <text v-if="row.collegeName">· {{ [row.collegeName, row.specialityName].filter(Boolean).join(' · ') }}</text>
          <text class="xm-num">· {{ $t('admin.people.credits') }} {{ row.credits == null ? 0 : row.credits }}</text>
        </view>
        <view
          v-else
          class="person-info"
        >
          <text v-if="tab === 'teachers'">{{ row.title || '—' }} ·</text>
          <text class="xm-num">{{ row.phone || '—' }}</text>
          <text v-if="row.email">· {{ row.email }}</text>
          <text
            v-if="tab === 'teachers'"
            class="xm-num"
            >· {{ $t('admin.people.courses') }} {{ row.activeCourses == null ? 0 : row.activeCourses }} ·
            {{ $t('admin.people.headOf') }} {{ row.headOf == null ? 0 : row.headOf }}</text
          >
        </view>

        <view class="xm-actions">
          <button
            class="xm-btn xm-btn-plain xm-btn-sm"
            @click="openForm(row)"
          >
            {{ $t('admin.people.editShort') }}
          </button>
          <button
            class="xm-btn xm-btn-plain xm-btn-sm"
            :disabled="isSelf(row)"
            @click="resetPassword(row)"
          >
            {{ $t('admin.people.resetPassword') }}
          </button>
          <button
            class="xm-btn xm-btn-danger xm-btn-sm"
            :disabled="isSelf(row)"
            @click="remove(row)"
          >
            {{ $t('admin.people.delete') }}
          </button>
        </view>
      </view>
    </view>
    <xm-list-footer
      :visible="people.list.value.length > 0"
      :loading="people.loading.value"
      :finished="people.finished.value"
      @load-more="people.loadNext()"
    />

    <view
      class="xm-fab"
      @click="openForm(null)"
    >
      <xm-icon
        name="user-plus"
        :size="44"
      />
    </view>

    <xm-form-popup
      :visible="formOpen"
      :title="formTitle"
      :saving="saving"
      :confirm-text="$t('common.save')"
      @close="formOpen = false"
      @save="save"
    >
      <view class="xm-form-item">
        <view class="xm-form-label required">{{ $t('admin.people.username') }}</view>
        <input
          class="xm-input"
          v-model="form.username"
          maxlength="30"
          :placeholder="$t('admin.people.usernamePlaceholder')"
        />
      </view>
      <view class="xm-form-item">
        <view class="xm-form-label required">{{ $t('admin.people.name') }}</view>
        <input
          class="xm-input"
          v-model="form.name"
          maxlength="30"
        />
      </view>
      <view
        v-if="tab === 'students'"
        class="xm-form-item"
      >
        <view class="xm-form-label">{{ $t('admin.people.org') }}</view>
        <xm-org-picker
          v-model="form.org"
          :tree="orgTree"
          :placeholder="$t('admin.people.orgPlaceholder')"
        />
      </view>
      <view
        v-if="tab === 'teachers'"
        class="xm-form-item"
      >
        <view class="xm-form-label">{{ $t('admin.people.jobTitle') }}</view>
        <input
          class="xm-input"
          v-model="form.title"
          maxlength="20"
        />
      </view>
      <template v-if="tab !== 'students'">
        <view class="xm-form-item">
          <view class="xm-form-label">{{ $t('admin.people.phone') }}</view>
          <input
            class="xm-input"
            v-model="form.phone"
            type="number"
            maxlength="20"
          />
        </view>
        <view class="xm-form-item">
          <view class="xm-form-label">{{ $t('admin.people.email') }}</view>
          <input
            class="xm-input"
            v-model="form.email"
            maxlength="50"
          />
        </view>
      </template>
      <view
        v-if="!editing"
        class="hint"
        >{{ $t('admin.people.passwordHint') }}</view
      >
    </xm-form-popup>
    <xm-loader />
  </view>
</template>

<script setup>
import { computed, onUnmounted, reactive, ref, watch } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { adminApi, peopleApi, studentApi, teacherApi } from '@/api'
import { SILENT } from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { ensureRole } from '@/utils/authGuard'
import { usePager } from '@/composables/usePager'
import { confirm } from '@/utils/confirm'
import { t } from '@/i18n'

const TABS = ['students', 'teachers', 'admins']
/** 各类账号的增删改接口（列表走 /people/*） */
const API = { students: studentApi, teachers: teacherApi, admins: adminApi }
const LIST = { students: peopleApi.students, teachers: peopleApi.teachers, admins: peopleApi.admins }

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const userStore = useUserStore()
const tab = ref('students')
const keyword = ref('')
const orgFilter = ref([])
const orgTree = ref([])

const roleName = computed(() => t(`admin.people.roleName.${tab.value}`))
const isSelf = (row) => tab.value === 'admins' && row.id === userStore.user.id && userStore.role === 'ADMIN'

const people = usePager((page) => {
  const params = { ...page, keyword: keyword.value.trim() || undefined }
  if (tab.value === 'students') {
    params.collegeId = orgFilter.value[0]
    params.specialityId = orgFilter.value[1]
    params.classId = orgFilter.value[2]
  }
  return LIST[tab.value](params, SILENT)
})

const switchTab = (key) => {
  if (tab.value === key) return
  tab.value = key
  orgFilter.value = []
  people.list.value = []
  people.total.value = 0
  people.reload()
}

// 搜索词 300ms 后再查，连续输入只发最后一次
let timer = null
watch(keyword, () => {
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => people.reload(), 300)
})
onUnmounted(() => {
  if (timer) clearTimeout(timer)
})

/** 由班级 id 反查 [学院, 专业, 班级]（从组织架构页跳过来时用） */
const pathOfClass = (classId) => {
  for (const c of orgTree.value) {
    for (const s of c.specialities || []) {
      if ((s.classes || []).some((k) => k.id === classId)) return [c.id, s.id, classId]
    }
  }
  return []
}

// ---------- 新建 / 编辑 ----------
const formOpen = ref(false)
const editing = ref(null)
const saving = ref(false)
const form = reactive({ username: '', name: '', org: [], title: '', phone: '', email: '' })

const formTitle = computed(() =>
  editing.value ? t('admin.people.edit', { role: roleName.value }) : t('admin.people.create', { role: roleName.value }),
)

const openForm = (row) => {
  editing.value = row
  Object.assign(form, {
    username: (row && row.username) || '',
    name: (row && row.name) || '',
    org: row ? [row.collegeId, row.specialityId, row.classId].filter((v) => v != null) : [],
    title: (row && row.title) || '',
    phone: (row && row.phone) || '',
    email: (row && row.email) || '',
  })
  formOpen.value = true
}

const save = async () => {
  if (!form.username.trim()) {
    uni.showToast({ title: t('admin.people.usernameRequired'), icon: 'none' })
    return
  }
  if (!form.name.trim()) {
    uni.showToast({ title: t('admin.people.nameRequired'), icon: 'none' })
    return
  }
  if (tab.value !== 'students' && form.email.trim() && !EMAIL_RE.test(form.email.trim())) {
    uni.showToast({ title: t('admin.people.emailInvalid'), icon: 'none' })
    return
  }
  const body = { username: form.username.trim(), name: form.name.trim() }
  if (tab.value === 'students') {
    body.collegeId = form.org[0] == null ? null : form.org[0]
    body.specialityId = form.org[1] == null ? null : form.org[1]
    body.classId = form.org[2] == null ? null : form.org[2]
  } else {
    body.phone = form.phone.trim()
    body.email = form.email.trim()
    if (tab.value === 'teachers') body.title = form.title.trim()
  }
  saving.value = true
  try {
    if (editing.value) {
      await API[tab.value].update({ ...body, id: editing.value.id })
      if (isSelf(editing.value)) userStore.patchUser({ name: body.name })
      uni.showToast({ title: t('admin.people.saved'), icon: 'success' })
    } else {
      await API[tab.value].add(body)
      uni.showToast({ title: t('admin.people.created'), icon: 'success' })
    }
    formOpen.value = false
    people.reload()
  } catch {
    // 账号重复等提示已由请求层统一弹出
  } finally {
    saving.value = false
  }
}

// ---------- 重置密码 / 删除 ----------
const resetPassword = async (row) => {
  if (
    !(await confirm(t('admin.people.resetConfirm', { name: row.name }), { title: t('admin.people.resetPassword') }))
  ) {
    return
  }
  try {
    await API[tab.value].resetPassword(row.id)
    uni.showToast({ title: t('admin.people.resetDone'), icon: 'none' })
  } catch {
    // 提示已由请求层统一弹出
  }
}

const remove = async (row) => {
  if (!(await confirm(t('admin.people.deleteConfirm', { name: row.name })))) return
  try {
    await API[tab.value].delete(row.id)
    uni.showToast({ title: t('admin.people.deleted'), icon: 'none' })
    people.reload()
  } catch {
    // 还有课、还有选课记录等提示已由请求层统一弹出
  }
}

/** 从组织架构页「在人员页查看全部」带 tab=students&classId=ID 进来 */
let pendingClassId = 0
onLoad((query) => {
  if (TABS.includes(query.tab)) tab.value = query.tab
  pendingClassId = Number(query.classId) || 0
})

let shownOnce = false
onShow(async () => {
  if (!ensureRole(['ADMIN'])) return
  uni.setNavigationBarTitle({ title: t('admin.people.title') })
  if (shownOnce) return
  shownOnce = true
  try {
    orgTree.value = (await peopleApi.org(SILENT)) || []
  } catch {
    // 组织架构拿不到时只是筛选和表单里没有选项
  }
  if (tab.value === 'students' && pendingClassId) orgFilter.value = pathOfClass(pendingClassId)
  people.reload()
})

onPullDownRefresh(async () => {
  await people.reload()
  uni.stopPullDownRefresh()
})

onReachBottom(() => people.loadNext())
</script>

<style lang="scss" scoped>
.tabs {
  margin-bottom: 20rpx;
}

.filters {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-bottom: 16rpx;
}

.count {
  margin-bottom: 16rpx;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.person-head {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.person-main {
  flex: 1;
  min-width: 0;
}

.person-name {
  display: flex;
  align-items: center;
  gap: 12rpx;
  font-size: 30rpx;
  font-weight: 600;
  color: var(--xm-text);
}

.person-sub {
  margin-top: 4rpx;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.person-info {
  display: flex;
  flex-wrap: wrap;
  gap: 4rpx 10rpx;
  margin-top: 14rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: var(--xm-text-2);
}

.hint {
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: var(--xm-text-3);
}
</style>

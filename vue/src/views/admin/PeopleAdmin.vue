<template>
  <div class="pa">
    <div class="page-head">
      <div>
        <h1 class="page-title">{{ $t('admin.people.title') }}</h1>
        <p class="page-sub">{{ $t('admin.people.sub') }}</p>
      </div>
      <div class="page-actions">
        <template v-if="tab === 'students'">
          <el-upload
            :action="IMPORT_URL"
            :headers="{ token: user.token }"
            accept=".xlsx,.xls"
            :show-file-list="false"
            :on-success="onImported"
            :on-error="() => ElMessage.error(t('admin.people.importFailed'))"
          >
            <el-button>{{ $t('admin.people.import') }}</el-button>
          </el-upload>
          <el-dropdown trigger="click" @command="(cmd: string) => (cmd === 'export' ? exportStudents() : downloadTemplate())">
            <el-button>{{ $t('admin.people.more') }}<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="export">{{ $t('admin.people.export') }}</el-dropdown-item>
                <el-dropdown-item command="template">{{ $t('admin.people.template') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <el-button type="primary" @click="openForm(null)">{{ $t('admin.people.create', { role: roleName }) }}</el-button>
      </div>
    </div>

    <div class="pa-filters">
      <div class="chips" role="tablist">
        <button
          v-for="key in TABS"
          :key="key"
          type="button"
          role="tab"
          class="chip"
          :class="{ 'is-on': tab === key }"
          :aria-selected="tab === key"
          @click="switchTab(key)"
        >
          {{ $t(`admin.people.tabs.${key}`) }}
        </button>
      </div>
      <el-cascader
        v-if="tab === 'students'"
        v-model="orgFilter"
        :options="orgOptions"
        :props="{ checkStrictly: true }"
        clearable
        filterable
        :placeholder="$t('admin.people.filterOrg')"
        class="pa-filters__org"
        @change="load(1)"
      />
      <el-input v-model="keyword" clearable :placeholder="$t('admin.people.search')" class="pa-filters__search" />
    </div>

    <section class="panel">
      <el-table :key="tab" v-loading="loading" :data="rows" row-key="id" class="pa-table" :empty-text="$t('admin.people.empty')">
        <el-table-column :label="$t('admin.people.name')" min-width="140">
          <template #default="{ row }">
            <div class="pa-who">
              <UserAvatar :name="row.name" :avatar="row.avatar" :size="30" />
              <span class="pa-who__name">{{ row.name }}</span>
              <span v-if="isSelf(row)" class="pill pill--brand">{{ $t('admin.people.self') }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="username" :label="$t('admin.people.username')" min-width="100">
          <template #default="{ row }"><span class="num">{{ row.username }}</span></template>
        </el-table-column>

        <template v-if="tab === 'students'">
          <el-table-column :label="$t('admin.people.org')" min-width="220">
            <template #default="{ row }">
              <div v-if="row.className || row.collegeName" class="pa-two">
                <span>{{ row.className || '—' }}</span>
                <span class="pa-two__sub">{{ [row.collegeName, row.specialityName].filter(Boolean).join(' · ') }}</span>
              </div>
              <span v-else>—</span>
            </template>
          </el-table-column>
          <el-table-column prop="credits" :label="$t('admin.people.credits')" width="80" align="center">
            <template #default="{ row }"><span class="num">{{ row.credits ?? 0 }}</span></template>
          </el-table-column>
        </template>

        <template v-else-if="tab === 'teachers'">
          <el-table-column prop="title" :label="$t('admin.people.jobTitle')" min-width="80">
            <template #default="{ row }">{{ row.title || '—' }}</template>
          </el-table-column>
          <el-table-column :label="$t('admin.people.contact')" min-width="170">
            <template #default="{ row }">
              <div class="pa-two">
                <span class="num">{{ row.phone || '—' }}</span>
                <span v-if="row.email" class="pa-two__sub">{{ row.email }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="$t('admin.people.courses')" width="80" align="center">
            <template #default="{ row }"><span class="num">{{ row.activeCourses ?? 0 }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('admin.people.headOf')" width="70" align="center">
            <template #default="{ row }"><span class="num">{{ row.headOf ?? 0 }}</span></template>
          </el-table-column>
        </template>

        <template v-else>
          <el-table-column :label="$t('admin.people.contact')" min-width="200">
            <template #default="{ row }">
              <div class="pa-two">
                <span class="num">{{ row.phone || '—' }}</span>
                <span v-if="row.email" class="pa-two__sub">{{ row.email }}</span>
              </div>
            </template>
          </el-table-column>
        </template>

        <el-table-column :label="$t('admin.people.actions')" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openForm(row)">{{ $t('admin.people.editShort') }}</el-button>
            <el-button link :disabled="isSelf(row)" @click="resetPassword(row)">{{ $t('admin.people.resetPassword') }}</el-button>
            <el-button link type="danger" :disabled="isSelf(row)" @click="remove(row)">{{ $t('admin.people.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="total > pageSize" class="pa-pager">
        <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          background
          size="small"
          @current-change="load"
        />
      </div>
    </section>

    <el-dialog v-model="formOpen" :title="formTitle" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item :label="$t('admin.people.username')" prop="username">
          <el-input v-model="form.username" maxlength="30" :placeholder="$t('admin.people.usernamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('admin.people.name')" prop="name">
          <el-input v-model="form.name" maxlength="30" />
        </el-form-item>
        <el-form-item v-if="tab === 'students'" :label="$t('admin.people.org')">
          <el-cascader
            v-model="form.org"
            :options="orgOptions"
            :props="{ checkStrictly: true }"
            clearable
            filterable
            :placeholder="$t('admin.people.orgPlaceholder')"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item v-if="tab === 'teachers'" :label="$t('admin.people.jobTitle')">
          <el-input v-model="form.title" maxlength="20" />
        </el-form-item>
        <template v-if="tab !== 'students'">
          <el-form-item :label="$t('admin.people.phone')" prop="phone">
            <el-input v-model="form.phone" maxlength="20" />
          </el-form-item>
          <el-form-item :label="$t('admin.people.email')" prop="email">
            <el-input v-model="form.email" maxlength="50" />
          </el-form-item>
        </template>
        <p v-if="!editing" class="pa-hint">{{ $t('admin.people.passwordHint') }}</p>
      </el-form>
      <template #footer>
        <el-button @click="formOpen = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { CascaderOption, FormInstance, FormRules, UploadProps } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import request, { type PageResult } from '@/utils/request'
import { ElMessage, ElMessageBox } from '@/utils/element-plus'
import UserAvatar from '@/components/UserAvatar.vue'
import { useUser } from '@/components/useUser'
import { useDownload } from '@/composables/useDownload'
import { apiMessage, t } from '@/i18n'

defineOptions({ name: 'PeopleAdmin' })

type Tab = 'students' | 'teachers' | 'admins'
const TABS: Tab[] = ['students', 'teachers', 'admins']
/** 各类账号的增删改接口前缀（列表走 /people/*） */
const API: Record<Tab, string> = { students: '/student', teachers: '/teacher', admins: '/admin' }

const IMPORT_URL = `${(import.meta.env.VITE_BASE_URL as string) || '/api'}/student/import`

const route = useRoute()
const router = useRouter()
const { user, patchUser } = useUser()

const tab = ref<Tab>(TABS.includes(route.query.tab as Tab) ? (route.query.tab as Tab) : 'students')
const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
const orgFilter = ref<number[]>([])
const rows = ref<Record<string, any>[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const loading = ref(false)

const roleName = computed(() => t(`admin.people.roleName.${tab.value}`))
const isSelf = (row: Record<string, any>) => tab.value === 'admins' && row.id === user.value.id && user.value.role === 'ADMIN'

// ---------- 组织架构（筛选与学生表单共用） ----------
const orgTree = ref<Record<string, any>[]>([])

const orgOptions = computed<CascaderOption[]>(() =>
  orgTree.value.map((c) => {
    const specialities: CascaderOption[] = (c.specialities || []).map((s: Record<string, any>) => {
      const classes: CascaderOption[] = (s.classes || []).map((k: Record<string, any>) => ({ value: k.id, label: k.name }))
      // 没有下级时不给 children，否则级联面板会展开一个空列
      return { value: s.id, label: s.name, children: classes.length ? classes : undefined }
    })
    return { value: c.id, label: c.name, children: specialities.length ? specialities : undefined }
  }),
)

/** 由班级 id 反查 [学院, 专业, 班级]（从组织架构页跳过来时用） */
const pathOfClass = (classId: number): number[] => {
  for (const c of orgTree.value) {
    for (const s of c.specialities || []) {
      if ((s.classes || []).some((k: Record<string, any>) => k.id === classId)) return [c.id, s.id, classId]
    }
  }
  return []
}

// ---------- 列表 ----------
let seq = 0
const load = async (page = pageNum.value) => {
  pageNum.value = page
  const params: Record<string, any> = { pageNum: page, pageSize, keyword: keyword.value.trim() || undefined }
  if (tab.value === 'students') {
    params.collegeId = orgFilter.value?.[0]
    params.specialityId = orgFilter.value?.[1]
    params.classId = orgFilter.value?.[2]
  }
  const mine = ++seq
  loading.value = true
  try {
    const data = await request.get<PageResult<Record<string, any>>>(`/people/${tab.value}`, { params })
    // 切了标签或改了搜索词，旧请求的结果丢掉
    if (mine !== seq) return
    rows.value = data?.list || []
    total.value = data?.total || 0
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    if (mine === seq) loading.value = false
  }
}

const switchTab = (key: Tab) => {
  if (tab.value === key) return
  tab.value = key
  orgFilter.value = []
  rows.value = []
  total.value = 0
  router.replace({ query: { tab: key } })
  load(1)
}

let timer: ReturnType<typeof setTimeout> | undefined
watch(keyword, () => {
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => load(1), 300)
})
onBeforeUnmount(() => timer && clearTimeout(timer))

onMounted(async () => {
  try {
    orgTree.value = (await request.get<Record<string, any>[]>('/people/org')) || []
  } catch {
    // 组织架构拿不到时只是筛选和表单里没有选项
  }
  const classId = Number(route.query.classId)
  if (tab.value === 'students' && classId) orgFilter.value = pathOfClass(classId)
  load(1)
})

// ---------- 新建 / 编辑 ----------
const formOpen = ref(false)
const formRef = ref<FormInstance>()
const editing = ref<Record<string, any> | null>(null)
const saving = ref(false)
const form = reactive({ username: '', name: '', org: [] as number[], title: '', phone: '', email: '' })

const rules = computed<FormRules>(() => ({
  username: [{ required: true, whitespace: true, message: t('admin.people.usernameRequired'), trigger: 'blur' }],
  name: [{ required: true, whitespace: true, message: t('admin.people.nameRequired'), trigger: 'blur' }],
  email: [{ type: 'email', message: t('admin.people.emailInvalid'), trigger: 'blur' }],
}))

const formTitle = computed(() =>
  editing.value ? t('admin.people.edit', { role: roleName.value }) : t('admin.people.create', { role: roleName.value }),
)

const openForm = (row: Record<string, any> | null) => {
  editing.value = row
  Object.assign(form, {
    username: row?.username || '',
    name: row?.name || '',
    org: row ? [row.collegeId, row.specialityId, row.classId].filter((v) => v != null) : [],
    title: row?.title || '',
    phone: row?.phone || '',
    email: row?.email || '',
  })
  formOpen.value = true
}

const save = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  const body: Record<string, any> = { username: form.username.trim(), name: form.name.trim() }
  if (tab.value === 'students') {
    body.collegeId = form.org[0] ?? null
    body.specialityId = form.org[1] ?? null
    body.classId = form.org[2] ?? null
  } else {
    body.phone = form.phone.trim()
    body.email = form.email.trim()
    if (tab.value === 'teachers') body.title = form.title.trim()
  }
  saving.value = true
  try {
    if (editing.value) {
      await request.put(`${API[tab.value]}/update`, { ...body, id: editing.value.id })
      if (isSelf(editing.value)) patchUser({ name: body.name })
      ElMessage.success(t('admin.people.saved'))
    } else {
      await request.post(`${API[tab.value]}/add`, body)
      ElMessage.success(t('admin.people.created'))
    }
    formOpen.value = false
    await load(editing.value ? pageNum.value : 1)
  } catch {
    // 账号重复等提示已由拦截器统一处理
  } finally {
    saving.value = false
  }
}

// ---------- 重置密码 / 删除 ----------
const resetPassword = async (row: Record<string, any>) => {
  try {
    await ElMessageBox.confirm(t('admin.people.resetConfirm', { name: row.name }), t('admin.people.resetPassword'), { type: 'warning' })
  } catch {
    return
  }
  try {
    await request.put(`${API[tab.value]}/resetPassword/${row.id}`)
    ElMessage.success(t('admin.people.resetDone'))
  } catch {
    // 错误提示已由拦截器统一处理
  }
}

const remove = async (row: Record<string, any>) => {
  try {
    await ElMessageBox.confirm(t('admin.people.deleteConfirm', { name: row.name }), t('common.confirmDeleteTitle'), { type: 'warning' })
  } catch {
    return
  }
  try {
    await request.delete(`${API[tab.value]}/delete/${row.id}`)
    ElMessage.success(t('admin.people.deleted'))
    // 删的是这一页最后一条就退回上一页
    await load(rows.value.length === 1 && pageNum.value > 1 ? pageNum.value - 1 : pageNum.value)
  } catch {
    // 还有课、还有选课记录等提示已由拦截器统一处理
  }
}

// ---------- 导入导出 ----------
const { download: downloadExport } = useDownload('/student/export')
const { download: downloadTemplateFile } = useDownload('/student/importTemplate')

const exportStudents = () => downloadExport(`${t('admin.people.exportName')}.xlsx`).catch(() => {})
const downloadTemplate = () => downloadTemplateFile(`${t('admin.people.templateName')}.xlsx`).catch(() => {})

const onImported: UploadProps['onSuccess'] = (response: Record<string, any>) => {
  if (response?.code === '200') {
    ElMessage({ type: 'success', message: t('admin.people.imported', { result: response.data }), duration: 6000 })
    load(1)
  } else {
    ElMessage.error(apiMessage(response))
  }
}
</script>

<style scoped>
.pa {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
}

.pa .page-head {
  margin-bottom: 0;
}

.pa-filters {
  display: flex;
  align-items: center;
  gap: 10px 14px;
  flex-wrap: wrap;
}

.pa-filters__org {
  width: 260px;
}

.pa-filters__search {
  width: 220px;
  margin-left: auto;
}

/* 表格底色要不透明：「操作」列固定在右侧，横向滚动时其他列从它下面滑过 */
.pa-table {
  --el-table-bg-color: var(--xm-bg-card);
  --el-table-tr-bg-color: var(--xm-bg-card);
  --el-table-header-bg-color: var(--xm-bg-card);
}

.pa-who {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.pa-who__name {
  min-width: 0;
  overflow: hidden;
  font-weight: 600;
  color: var(--xm-text-primary);
  white-space: nowrap;
  text-overflow: ellipsis;
}

.pa-two {
  display: grid;
  font-size: 13px;
  color: var(--xm-text-regular);
}

.pa-two__sub {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.pa-pager {
  display: flex;
  justify-content: flex-end;
  padding: 12px 4px 4px;
}

.pa-hint {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

@media (max-width: 640px) {
  .pa-filters__org,
  .pa-filters__search {
    width: 100%;
    margin-left: 0;
  }
}
</style>

<template>
  <div class="org">
    <div class="page-head">
      <div>
        <h1 class="page-title">{{ $t('admin.org.title') }}</h1>
        <p class="page-sub">{{ $t('admin.org.sub') }}</p>
      </div>
      <el-button type="primary" @click="openCreate('college', null)">{{ $t('admin.org.addCollege') }}</el-button>
    </div>

    <div class="org-body">
      <section v-loading="loading" class="panel org-tree">
        <el-tree
          v-if="tree.length"
          ref="treeRef"
          :data="tree"
          node-key="key"
          :props="{ label: 'name', children: 'children' }"
          default-expand-all
          highlight-current
          :expand-on-click-node="false"
          @node-click="(node: OrgNode) => select(node.key)"
        >
          <template #default="{ data }">
            <span class="org-node">
              <span class="org-node__type" :class="`is-${data.type}`">{{ $t(`admin.org.types.${data.type}`) }}</span>
              <span class="org-node__name">{{ data.name }}</span>
              <span class="org-node__count num" :title="$t('admin.org.students', { n: data.students })">{{ data.students }}</span>
            </span>
          </template>
        </el-tree>
        <div v-else-if="!loading" class="empty-note">{{ $t('admin.org.empty') }}</div>
      </section>

      <section class="panel org-detail">
        <template v-if="selected">
          <nav v-if="ancestors.length" class="org-crumbs">
            <template v-for="a in ancestors" :key="a.key">
              <button type="button" class="org-crumbs__link" @click="select(a.key)">{{ a.name }}</button>
              <span class="org-crumbs__sep">›</span>
            </template>
          </nav>
          <div class="org-detail__head">
            <span class="org-node__type" :class="`is-${selected.type}`">{{ $t(`admin.org.types.${selected.type}`) }}</span>
            <h2 class="org-detail__name">{{ selected.name }}</h2>
            <div class="org-detail__actions">
              <el-button size="small" @click="openEdit(selected)">{{ $t('admin.org.edit') }}</el-button>
              <el-button size="small" type="danger" plain @click="remove(selected)">{{ $t('admin.org.delete') }}</el-button>
            </div>
          </div>

          <dl class="org-facts">
            <div>
              <dt>{{ $t('admin.org.studentsLabel') }}</dt>
              <dd class="num">{{ selected.students }}</dd>
            </div>
            <div v-if="selected.type === 'speciality'">
              <dt>{{ $t('admin.org.requiredCredits') }}</dt>
              <dd class="num">{{ selected.score ?? '—' }}</dd>
            </div>
            <div v-if="selected.type === 'class'">
              <dt>{{ $t('admin.org.headTeacher') }}</dt>
              <dd>{{ selected.teacherName || $t('admin.org.none') }}</dd>
            </div>
            <div class="org-facts__wide">
              <dt>{{ $t('admin.org.content') }}</dt>
              <dd class="org-facts__text">{{ selected.content || $t('admin.org.none') }}</dd>
            </div>
          </dl>

          <!-- 学院 → 下设专业；专业 → 下设班级 -->
          <div v-if="selected.type !== 'class'" class="org-sub">
            <div class="org-sub__head">
              <span class="org-sub__title">{{ selected.type === 'college' ? $t('admin.org.specialities') : $t('admin.org.classes') }}</span>
              <el-button size="small" type="primary" plain @click="openCreate(selected.type === 'college' ? 'speciality' : 'class', selected)">
                {{ selected.type === 'college' ? $t('admin.org.addSpeciality') : $t('admin.org.addClass') }}
              </el-button>
            </div>
            <ul v-if="selected.children.length" class="org-rows">
              <li v-for="c in selected.children" :key="c.key">
                <button type="button" class="org-row" @click="select(c.key)">
                  <span class="org-row__name">{{ c.name }}</span>
                  <span class="org-row__meta">
                    <template v-if="c.type === 'speciality'">{{ $t('admin.org.classCount', { n: c.children.length }) }} · </template>
                    <template v-else>{{ c.teacherName || $t('admin.org.noHeadTeacher') }} · </template>
                    <span class="num">{{ $t('admin.org.students', { n: c.students }) }}</span>
                  </span>
                </button>
              </li>
            </ul>
            <div v-else class="empty-note">{{ $t('admin.org.none') }}</div>
          </div>

          <!-- 班级 → 班里的学生 -->
          <div v-else class="org-sub">
            <div class="org-sub__head">
              <span class="org-sub__title">{{ $t('admin.org.classStudents') }}</span>
              <router-link v-if="classStudentsTotal" :to="`/admin/people?tab=students&classId=${selected.id}`" class="panel__more">
                {{ $t('admin.org.viewInPeople', { n: classStudentsTotal }) }} ›
              </router-link>
            </div>
            <el-skeleton v-if="classStudentsLoading" animated :rows="3" />
            <ul v-else-if="classStudents.length" class="org-students">
              <li v-for="s in classStudents" :key="s.id" class="org-student">
                <UserAvatar :name="s.name" :avatar="s.avatar" :size="28" />
                <span class="org-student__name">{{ s.name }}</span>
                <span class="org-student__id num">{{ s.username }}</span>
              </li>
            </ul>
            <div v-else class="empty-note">{{ $t('admin.org.noStudents') }}</div>
          </div>
        </template>
        <div v-else class="empty-note org-detail__hint">{{ $t('admin.org.selectHint') }}</div>
      </section>
    </div>

    <el-dialog v-model="formOpen" :title="formTitle" width="460px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <p v-if="formParent" class="org-form__parent">{{ $t(`admin.org.types.${formParent.type}`) }}：{{ formParent.name }}</p>
        <el-form-item :label="$t('admin.org.name')" prop="name">
          <el-input v-model="form.name" maxlength="30" />
        </el-form-item>
        <el-form-item v-if="formType === 'speciality'" :label="$t('admin.org.requiredCredits')">
          <el-input-number v-model="form.score" :min="0" :max="300" :precision="0" controls-position="right" />
        </el-form-item>
        <el-form-item v-if="formType === 'class'" :label="$t('admin.org.headTeacher')">
          <el-select v-model="form.teacherId" filterable clearable :placeholder="$t('admin.org.headTeacherPlaceholder')" style="width: 100%">
            <el-option v-for="tc in teachers" :key="tc.id" :label="tc.name" :value="tc.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('admin.org.content')">
          <el-input v-model="form.content" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formOpen = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import request, { type PageResult } from '@/utils/request'
import { ElMessage, ElMessageBox } from '@/utils/element-plus'
import UserAvatar from '@/components/UserAvatar.vue'
import { t } from '@/i18n'

defineOptions({ name: 'OrgAdmin' })

type OrgType = 'college' | 'speciality' | 'class'

interface OrgNode {
  key: string
  type: OrgType
  id: number
  name: string
  content?: string
  students: number
  score?: number
  teacherId?: number | null
  teacherName?: string
  parentKey?: string
  children: OrgNode[]
}

/** 三级各自的通用接口前缀 */
const API: Record<OrgType, string> = { college: '/college', speciality: '/speciality', class: '/classes' }

const raw = ref<Record<string, any>[]>([])
const loading = ref(false)
const treeRef = ref()
const selectedKey = ref('')
const teachers = ref<Record<string, any>[]>([])

const tree = computed<OrgNode[]>(() =>
  raw.value.map((c) => ({
    key: `college-${c.id}`,
    type: 'college',
    id: c.id,
    name: c.name,
    content: c.content,
    students: Number(c.students) || 0,
    children: (c.specialities || []).map((s: Record<string, any>) => ({
      key: `speciality-${s.id}`,
      type: 'speciality',
      id: s.id,
      name: s.name,
      content: s.content,
      score: s.score,
      students: Number(s.students) || 0,
      parentKey: `college-${c.id}`,
      children: (s.classes || []).map((k: Record<string, any>) => ({
        key: `class-${k.id}`,
        type: 'class',
        id: k.id,
        name: k.name,
        content: k.content,
        teacherId: k.teacherId,
        teacherName: k.teacherName,
        students: Number(k.students) || 0,
        parentKey: `speciality-${s.id}`,
        children: [],
      })),
    })),
  })),
)

const index = computed(() => {
  const map = new Map<string, OrgNode>()
  const walk = (nodes: OrgNode[]) => nodes.forEach((n) => {
    map.set(n.key, n)
    walk(n.children)
  })
  walk(tree.value)
  return map
})

const selected = computed(() => index.value.get(selectedKey.value) || null)

/** 面包屑：学院 › 专业 */
const ancestors = computed(() => {
  const list: OrgNode[] = []
  let parent = selected.value?.parentKey ? index.value.get(selected.value.parentKey) : undefined
  while (parent) {
    list.unshift(parent)
    parent = parent.parentKey ? index.value.get(parent.parentKey) : undefined
  }
  return list
})

const select = (key: string) => {
  selectedKey.value = key
  nextTick(() => treeRef.value?.setCurrentKey(key || null))
}

const load = async () => {
  loading.value = true
  try {
    raw.value = (await request.get<Record<string, any>[]>('/people/org')) || []
    // 选中的节点被删了（或第一次进来）就选第一个学院
    if (!index.value.has(selectedKey.value)) {
      selectedKey.value = tree.value[0]?.key || ''
    }
    select(selectedKey.value)
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await load()
  try {
    teachers.value = (await request.get<Record<string, any>[]>('/teacher/selectAll')) || []
  } catch {
    // 班主任下拉拿不到时表单里就是空的，不影响其他操作
  }
})

// ---------- 班级学生 ----------
const classStudents = ref<Record<string, any>[]>([])
const classStudentsTotal = ref(0)
const classStudentsLoading = ref(false)

watch(
  () => (selected.value?.type === 'class' ? selected.value.id : null),
  async (classId) => {
    classStudents.value = []
    classStudentsTotal.value = 0
    if (!classId) return
    classStudentsLoading.value = true
    try {
      const page = await request.get<PageResult<Record<string, any>>>('/people/students', { params: { classId, pageSize: 50 } })
      // 切走了就丢掉过期的结果
      if (selected.value?.id !== classId || selected.value.type !== 'class') return
      classStudents.value = page?.list || []
      classStudentsTotal.value = page?.total || 0
    } catch {
      // 错误提示已由拦截器统一处理
    } finally {
      classStudentsLoading.value = false
    }
  },
)

// ---------- 新建 / 编辑 ----------
const formOpen = ref(false)
const formRef = ref<FormInstance>()
const formType = ref<OrgType>('college')
const formParent = ref<OrgNode | null>(null)
const editingId = ref<number | null>(null)
const saving = ref(false)
const form = reactive({ name: '', content: '', score: 0 as number | undefined, teacherId: null as number | null })

const rules = computed<FormRules>(() => ({
  name: [{ required: true, whitespace: true, message: t('admin.org.nameRequired'), trigger: 'blur' }],
}))

const CREATE_TITLE: Record<OrgType, string> = { college: 'admin.org.addCollege', speciality: 'admin.org.addSpeciality', class: 'admin.org.addClass' }

const formTitle = computed(() =>
  editingId.value ? t('admin.org.editTitle', { type: t(`admin.org.types.${formType.value}`) }) : t(CREATE_TITLE[formType.value]),
)

const openCreate = (type: OrgType, parent: OrgNode | null) => {
  formType.value = type
  formParent.value = parent
  editingId.value = null
  Object.assign(form, { name: '', content: '', score: type === 'speciality' ? 50 : undefined, teacherId: null })
  formOpen.value = true
}

const openEdit = (node: OrgNode) => {
  formType.value = node.type
  // 编辑时不能换上级：学生身上记着学院、专业，换了上级会和学生对不上
  formParent.value = node.parentKey ? index.value.get(node.parentKey) || null : null
  editingId.value = node.id
  Object.assign(form, { name: node.name, content: node.content || '', score: node.score ?? 0, teacherId: node.teacherId ?? null })
  formOpen.value = true
}

const save = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  const body: Record<string, any> = { name: form.name.trim(), content: form.content.trim() }
  if (formType.value === 'speciality') {
    body.score = form.score ?? 0
    if (!editingId.value) body.collegeId = formParent.value?.id
  }
  if (formType.value === 'class') {
    body.teacherId = form.teacherId
    if (!editingId.value) body.specialityId = formParent.value?.id
  }
  saving.value = true
  try {
    if (editingId.value) {
      await request.put(`${API[formType.value]}/update`, { ...body, id: editingId.value })
    } else {
      await request.post(`${API[formType.value]}/add`, body)
    }
    ElMessage.success(t('admin.org.saved'))
    formOpen.value = false
    await load()
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    saving.value = false
  }
}

const remove = async (node: OrgNode) => {
  try {
    await ElMessageBox.confirm(t('admin.org.deleteConfirm', { name: node.name }), t('common.confirmDeleteTitle'), { type: 'warning' })
  } catch {
    return
  }
  try {
    await request.delete(`${API[node.type]}/delete/${node.id}`)
    ElMessage.success(t('admin.org.deleted'))
    selectedKey.value = node.parentKey || ''
    await load()
  } catch {
    // 下面还有专业、班级或学生的提示已由拦截器统一处理
  }
}
</script>

<style scoped>
.org {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
}

.org .page-head {
  margin-bottom: 0;
}

.org-body {
  display: grid;
  grid-template-columns: minmax(240px, 320px) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.org-tree {
  padding: 10px 6px;
  min-height: 120px;
}

.org-tree :deep(.el-tree) {
  --el-tree-node-content-height: 34px;
  background: transparent;
}

.org-tree :deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: var(--xm-brand-soft);
}

.org-node {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
  padding-right: 8px;
}

.org-node__type {
  flex-shrink: 0;
  padding: 0 6px;
  border-radius: 4px;
  font-size: 11px;
  line-height: 18px;
  color: var(--xm-brand);
  background: var(--xm-brand-soft);
}

.org-node__type.is-speciality {
  color: var(--xm-info);
  background: var(--xm-info-soft);
}

.org-node__type.is-class {
  color: var(--xm-ok);
  background: var(--xm-ok-soft);
}

.org-node__name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  font-size: 14px;
  color: var(--xm-text-primary);
  white-space: nowrap;
  text-overflow: ellipsis;
}

.org-node__count {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.org-detail {
  display: grid;
  gap: 18px;
  padding: 20px;
}

.org-detail__hint {
  padding: 40px 0;
}

.org-crumbs {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: -10px;
  font-size: 13px;
}

.org-crumbs__link {
  padding: 0;
  border: 0;
  background: none;
  color: var(--xm-text-secondary);
  font: inherit;
  cursor: pointer;
}

.org-crumbs__link:hover {
  color: var(--xm-brand);
}

.org-crumbs__sep {
  color: var(--xm-text-secondary);
}

.org-detail__head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.org-detail__name {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--xm-text-primary);
}

.org-detail__actions {
  margin-left: auto;
}

.org-facts {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px 20px;
  padding: 14px 16px;
  border-radius: var(--xm-radius);
  background: var(--xm-bg-sunken);
}

.org-facts__wide {
  grid-column: 1 / -1;
}

.org-facts dt {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.org-facts dd {
  margin-top: 2px;
  font-size: 15px;
  color: var(--xm-text-primary);
}

.org-facts__text {
  font-size: 14px !important;
  white-space: pre-wrap;
  word-break: break-word;
}

.org-sub {
  display: grid;
  gap: 10px;
}

.org-sub__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.org-sub__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--xm-text-primary);
}

.org-rows {
  display: grid;
  gap: 8px;
  list-style: none;
}

.org-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius);
  background: var(--xm-bg-card);
  color: var(--xm-text-regular);
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.org-row:hover {
  border-color: var(--xm-brand);
}

.org-row__name {
  min-width: 0;
  overflow: hidden;
  font-weight: 600;
  color: var(--xm-text-primary);
  white-space: nowrap;
  text-overflow: ellipsis;
}

.org-row__meta {
  flex-shrink: 0;
  font-size: 13px;
  color: var(--xm-text-secondary);
}

.org-students {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 8px;
  list-style: none;
}

.org-student {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 8px 10px;
  border-radius: var(--xm-radius);
  background: var(--xm-bg-sunken);
}

.org-student__name {
  min-width: 0;
  overflow: hidden;
  font-size: 14px;
  color: var(--xm-text-primary);
  white-space: nowrap;
  text-overflow: ellipsis;
}

.org-student__id {
  margin-left: auto;
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.org-form__parent {
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--xm-text-secondary);
}

@media (max-width: 900px) {
  .org-body {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>

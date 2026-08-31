<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="perm-header">
          <span>{{ $t('pages.permission.title') }}</span>
          <span class="perm-sub">{{ $t('pages.permission.subtitle') }}</span>
        </div>
      </template>

      <!-- 角色切换 -->
      <el-tabs v-model="activeRole" type="card" @tab-change="onRoleChange">
        <el-tab-pane
            v-for="role in roles"
            :key="role.code"
            :name="role.code"
            :label="role.name"
        >
          <template #label>
            <span>{{ role.name }}</span>
            <el-tag v-if="role.code === 'ADMIN'" size="small" type="warning" style="margin-left: 6px">
              {{ $t('pages.permission.adminTag') }}
            </el-tag>
          </template>
        </el-tab-pane>
      </el-tabs>

      <el-alert
          v-if="activeRole === 'ADMIN'"
          :title="$t('pages.permission.adminHint')"
          type="warning"
          :closable="false"
          show-icon
          style="margin: 12px 0"
      />

      <!-- 树形权限分配：模块 -> 权限点，父子联动 -->
      <el-tree
          v-loading="loading"
          ref="treeRef"
          :data="treeData"
          :props="{ label: 'label', children: 'children' }"
          node-key="id"
          show-checkbox
          default-expand-all
          :expand-on-click-node="false"
          class="perm-tree"
      />

      <div class="perm-footer">
        <el-button type="primary" :disabled="activeRole === 'ADMIN'" :loading="saving" @click="save">
          {{ $t('common.save') }}
        </el-button>
        <el-button plain @click="reset">{{ $t('common.reset') }}</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Permission' })

import { ref, computed, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { apiMessage, t } from '@/i18n'
import { currentLocale } from '@/composables/useLocale'

interface Permission {
  id: number
  code: string
  name: string
  type: string
  module: string
  sortNum: number
}

interface Role {
  id: number
  code: string
  name: string
  descr?: string
  permissions: string[]
}

interface TreeNode {
  id: string
  label: string
  disabled?: boolean
  children?: TreeNode[]
}

const roles = ref<Role[]>([])
const permissions = ref<Permission[]>([])
const activeRole = ref<string>('')
const saving = ref(false)
const loading = ref(false)
const treeRef = ref<any>()
const isZh = computed(() => currentLocale() === 'zh-CN')

/** 模块 -> 中文标题（树的一级节点） */
const MODULE_LABELS: Record<string, string> = {
  dashboard: '数据大屏',
  notice: '教务通知',
  examplan: '考试安排',
  roomplan: '教室安排',
  college: '学院信息',
  speciality: '专业信息',
  classes: '班级信息',
  course: '课程信息',
  choice: '我的选课',
  score: '成绩管理',
  comment: '评教管理',
  apply: '请假管理',
  homework: '作业管理',
  attendance: '考勤管理',
  admin: '管理员',
  teacher: '教师',
  student: '学生',
  log: '系统日志',
  permission: '权限设置',
  file: '文件管理',
}

/** 模块的业务展示顺序（数据大屏 → 信息公告 → 行政管理 → 教学 → 教务 → 用户 → 系统） */
const MODULE_ORDER: string[] = [
  'dashboard',
  'notice', 'examplan', 'roomplan',
  'college', 'speciality', 'classes',
  'course', 'choice', 'score', 'comment',
  'apply', 'homework', 'attendance',
  'admin', 'teacher', 'student',
  'log', 'permission', 'file',
]

/** 各角色的系统默认权限码（与 sql/rbac_permission.sql 的授权矩阵保持一致），用于"恢复默认" */
const DEFAULT_ROLE_PERMISSIONS: Record<string, string[]> = {
  ADMIN: [
    'dashboard:view',
    'college:view', 'college:manage',
    'speciality:view', 'speciality:manage',
    'classes:view', 'classes:manage',
    'course:view', 'course:manage',
    'choice:view', 'choice:manage',
    'score:view', 'score:manage',
    'comment:view', 'comment:manage',
    'apply:view', 'apply:manage',
    'homework:view', 'homework:manage',
    'attendance:view', 'attendance:manage',
    'notice:view', 'notice:manage',
    'examplan:view', 'examplan:manage',
    'roomplan:view', 'roomplan:manage',
    'admin:view', 'admin:manage', 'admin:self',
    'teacher:view', 'teacher:manage', 'teacher:self',
    'student:view', 'student:manage', 'student:self', 'student:export', 'student:resetPwd',
    'log:view', 'log:manage',
    'file:upload', 'file:delete',
    'permission:manage',
  ],
  TEACHER: [
    'dashboard:view',
    'course:view',
    'choice:view',
    'score:view', 'score:manage',
    'comment:view',
    'apply:view',
    'homework:view', 'homework:manage',
    'attendance:view', 'attendance:manage',
    'notice:view',
    'examplan:view',
    'roomplan:view',
    'teacher:view', 'teacher:self',
    'file:upload',
  ],
  STUDENT: [
    'dashboard:view',
    'course:view',
    'choice:view', 'choice:manage',
    'score:view',
    'comment:view', 'comment:manage',
    'apply:view', 'apply:manage',
    'homework:view', 'homework:manage',
    'attendance:view',
    'notice:view',
    'examplan:view',
    'roomplan:view',
    'student:view', 'student:self',
    'file:upload',
  ],
}

const moduleLabel = (module: string) => {
  // 中文环境用映射标题；英文回退为大写模块名
  if (isZh.value && MODULE_LABELS[module]) return MODULE_LABELS[module]
  return module.toUpperCase()
}

/** 树节点：一级=模块，二级=权限点（node-key 用 code，含 ":" 便于过滤） */
const treeData = computed<TreeNode[]>(() => {
  const disabled = activeRole.value === 'ADMIN'
  const map: Record<string, TreeNode> = {}
  for (const p of permissions.value) {
    if (!map[p.module]) {
      map[p.module] = {
        id: p.module,
        label: moduleLabel(p.module),
        disabled,
        children: [],
      }
    }
    map[p.module].children!.push({
      id: p.code,
      label: isZh.value ? `${p.name}` : `${p.name} (${p.code})`,
      disabled,
    })
  }
  // 按业务顺序重排一级模块；不在清单内的模块追加到末尾，避免新模块丢失
  const orderOf = (m: string) => {
    const idx = MODULE_ORDER.indexOf(m)
    return idx === -1 ? MODULE_ORDER.length : idx
  }
  const modules = [...new Set(permissions.value.map((p) => p.module))].sort((a, b) => orderOf(a) - orderOf(b))
  return modules.map((m) => map[m])
})

const load = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/permission/selectAll')
    const data = res.data?.data
    roles.value = data?.roles || []
    permissions.value = data?.permissions || []
    if (!activeRole.value && roles.value.length) {
      activeRole.value = roles.value[0].code
    }
    await nextTick()
    restoreChecked()
  } finally {
    loading.value = false
  }
}

/** 把当前角色的权限码回显到树（仅勾选叶子节点，父节点联动显示全选/半选） */
const restoreChecked = () => {
  const role = roles.value.find((r) => r.code === activeRole.value)
  const codes = role ? role.permissions.filter((c) => c.includes(':')) : []
  treeRef.value?.setCheckedKeys(codes)
}

const onRoleChange = (tabName?: string | number) => {
  if (tabName) activeRole.value = String(tabName)
  nextTick(() => restoreChecked())
}

const reset = () => {
  if (activeRole.value === 'ADMIN') return
  ElMessageBox.confirm(t('pages.permission.resetConfirm'), t('common.confirmModify'), { type: 'warning' })
      .then(() => {
        const defaults = DEFAULT_ROLE_PERMISSIONS[activeRole.value] || []
        treeRef.value?.setCheckedKeys(defaults.filter((c) => c.includes(':')))
        ElMessage.success(t('pages.permission.resetDone'))
      })
      .catch(() => {
        // 用户取消，保持当前勾选
      })
}

const save = async () => {
  saving.value = true
  try {
    // 只取叶子权限码（node-key 中含 ":"），父节点(模块)仅作分组不入库
    const checkedKeys = (treeRef.value?.getCheckedKeys() || []) as string[]
    const permissionCodes = checkedKeys.filter((k) => k.includes(':'))
    const res: any = await request.put('/permission/updateRolePermissions', {
      roleCode: activeRole.value,
      permissionCodes,
    })
    if (res.data.code === '200') {
      ElMessage.success(t('common.saveSuccess'))
      await load()
    } else {
      ElMessage.error(apiMessage(res.data))
    }
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.perm-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.perm-sub {
  font-size: 12px;
  color: var(--xm-text-secondary);
}
.perm-tree {
  margin-top: 4px;
}
/* 让树节点更宽松、便于阅读 */
.perm-tree :deep(.el-tree-node__label) {
  font-size: 13px;
}
.perm-tree :deep(.el-tree-node__content) {
  height: 30px;
}
.perm-footer {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}
</style>

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

      <!-- 权限点分组勾选 -->
      <el-checkbox-group v-model="selectedCodes" :disabled="activeRole === 'ADMIN'">
        <div v-for="group in groupedPermissions" :key="group.module" class="perm-group">
          <div class="perm-group-title">
            <span class="perm-module">{{ group.module.toUpperCase() }}</span>
            <el-button
                v-if="activeRole !== 'ADMIN'"
                link
                type="primary"
                size="small"
                @click="toggleGroup(group.module)"
            >{{ $t('pages.permission.checkAll') }}</el-button>
          </div>
          <el-checkbox
              v-for="p in group.items"
              :key="p.code"
              :label="p.code"
              :disabled="activeRole === 'ADMIN'"
              class="perm-item"
          >
            <span class="perm-name">{{ p.name }}</span>
            <span class="perm-code">{{ p.code }}</span>
          </el-checkbox>
        </div>
      </el-checkbox-group>

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

import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { apiMessage, t } from '@/i18n'

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

const roles = ref<Role[]>([])
const permissions = ref<Permission[]>([])
const activeRole = ref<string>('')
const selectedCodes = ref<string[]>([])
const saving = ref(false)

/** 按模块分组（保持后端的 module/sort 顺序） */
const groupedPermissions = computed(() => {
  const map: Record<string, Permission[]> = {}
  for (const p of permissions.value) {
    ;(map[p.module] || (map[p.module] = [])).push(p)
  }
  return Object.entries(map).map(([module, items]) => ({ module, items }))
})

const load = async () => {
  const res: any = await request.get('/permission/selectAll')
  const data = res.data?.data
  roles.value = data?.roles || []
  permissions.value = data?.permissions || []
  if (!activeRole.value && roles.value.length) {
    activeRole.value = roles.value[0].code
  }
  onRoleChange(activeRole.value)
}

const onRoleChange = (tabName?: string | number) => {
  if (tabName) activeRole.value = String(tabName)
  const role = roles.value.find((r) => r.code === activeRole.value)
  selectedCodes.value = role ? [...role.permissions] : []
}

/** 全选 / 反选当前模块 */
const toggleGroup = (module: string) => {
  const moduleCodes = groupedPermissions.value
      .find((g) => g.module === module)?.items.map((p) => p.code) || []
  const allSelected = moduleCodes.every((c) => selectedCodes.value.includes(c))
  const rest = selectedCodes.value.filter((c) => !moduleCodes.includes(c))
  selectedCodes.value = allSelected ? rest : [...rest, ...moduleCodes]
}

const reset = () => onRoleChange()

const save = async () => {
  saving.value = true
  try {
    const res: any = await request.put('/permission/updateRolePermissions', {
      roleCode: activeRole.value,
      permissionCodes: selectedCodes.value,
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
.perm-group {
  padding: 12px 0;
  border-bottom: 1px solid var(--xm-border);
}
.perm-group:last-child {
  border-bottom: none;
}
.perm-group-title {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}
.perm-module {
  font-weight: 600;
  color: var(--xm-text-primary);
  margin-right: 12px;
}
.perm-item {
  margin-right: 24px;
  margin-bottom: 6px;
}
.perm-name {
  margin-right: 6px;
}
.perm-code {
  font-size: 12px;
  color: var(--xm-text-secondary);
}
.perm-footer {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}
</style>

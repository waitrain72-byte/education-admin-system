<template>
  <div class="table">
    <el-table v-loading="loading" :data="data" stripe @selection-change="$emit('selection-change', $event)">
      <el-table-column v-if="selectable" type="selection" width="55" align="center" />
      <el-table-column
          v-for="col in columns"
          :key="col.prop || col.label"
          :prop="col.prop"
          :label="col.label"
          :width="col.width"
          :align="col.align"
          :sortable="col.sortable"
          :show-overflow-tooltip="col.showOverflowTooltip"
      >
        <!-- 自定义列：页面提供 <template #file="{ row }"> 时渲染该插槽，否则按 prop 渲染 -->
        <template v-if="col.prop && $slots[col.prop]" #default="scope">
          <slot :name="col.prop" :row="scope.row" />
        </template>
      </el-table-column>
      <el-table-column v-if="showActions && $slots.actions" label="操作" :width="actionsWidth" align="center">
        <template #default="scope">
          <slot name="actions" :row="scope.row" />
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination">
      <el-pagination
          background
          :current-page="pageNum"
          :page-sizes="[5, 10, 20]"
          :page-size="pageSize"
          layout="total, prev, pager, next"
          :total="total"
          @current-change="$emit('page-change', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
export interface CrudColumn {
  prop?: string
  label: string
  width?: number | string
  align?: 'left' | 'center' | 'right'
  sortable?: boolean
  showOverflowTooltip?: boolean
}

withDefaults(defineProps<{
    data: any[]
    columns: CrudColumn[]
    pageNum: number
    pageSize: number
    total: number
    loading?: boolean
    selectable?: boolean
    showActions?: boolean
    /** 操作列宽度：按钮较多时（如用户管理的 编辑/重置密码/删除）可传更大值 */
    actionsWidth?: number
}>(), {
    loading: false,
    selectable: false,
    showActions: true,
    actionsWidth: 180,
})

defineEmits<{
  (e: 'selection-change', rows: any[]): void
  (e: 'page-change', pageNum: number): void
}>()
</script>

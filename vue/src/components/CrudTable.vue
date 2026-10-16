<template>
  <div class="table">
    <!-- 首次加载且无数据：表头照常显示，表身画占位行；翻页等已有数据的刷新才在表格上转圈 -->
    <el-table
      v-loading="loading && !skeleton"
      :data="skeleton ? [] : data"
      stripe
      @selection-change="$emit('selection-change', $event)"
    >
      <el-table-column v-if="selectable" type="selection" width="55" align="center" />
      <!-- 主键 id 列统一渲染为"当前页行号"（跨页连续），避免显示全局自增的大数值 -->
      <el-table-column
          v-for="col in columns"
          :key="col.prop || col.label"
          :prop="col.prop === 'id' ? undefined : col.prop"
          :type="col.prop === 'id' ? 'index' : undefined"
          :index="col.prop === 'id' ? indexMethod : undefined"
          :label="col.label"
          :width="col.width"
          :align="col.align"
          :sortable="col.prop === 'id' ? false : col.sortable"
          :show-overflow-tooltip="col.showOverflowTooltip"
      >
        <!-- 自定义列：页面提供 <template #file="{ row }"> 时渲染该插槽，否则按 prop 渲染 -->
        <template v-if="col.prop && $slots[col.prop]" #default="scope">
          <slot :name="col.prop" :row="scope.row" />
        </template>
      </el-table-column>
      <el-table-column v-if="showActions && $slots.actions" :label="$t('common.operation')" :width="actionsWidth" align="center">
        <template #default="scope">
          <slot name="actions" :row="scope.row" />
        </template>
      </el-table-column>
      <template #empty>
        <TableSkeleton v-if="skeleton" />
        <el-empty v-else :image-size="88" :description="$t('common.empty')" />
      </template>
    </el-table>
    <!-- 骨架期间总数还不知道，不显示「共 0 条」 -->
    <div v-if="!skeleton" class="pagination">
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
import { useSkeleton } from '@/composables/useSkeleton'
import TableSkeleton from './skeleton/TableSkeleton.vue'

export interface CrudColumn {
  prop?: string
  label: string
  width?: number | string
  align?: 'left' | 'center' | 'right'
  sortable?: boolean
  showOverflowTooltip?: boolean
}

const props = withDefaults(defineProps<{
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

const skeleton = useSkeleton(() => props.loading && !props.data.length)

/** 计算当前页行号：第 n 页的第 index 行 → (pageNum-1)*pageSize + index + 1（跨页连续） */
const indexMethod = (index: number) => {
    const pageNum = props.pageNum || 1
    const pageSize = props.pageSize || 10
    return (pageNum - 1) * pageSize + index + 1
}

defineEmits<{
  (e: 'selection-change', rows: any[]): void
  (e: 'page-change', pageNum: number): void
}>()
</script>

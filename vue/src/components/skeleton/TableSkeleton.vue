<template>
  <!--
    表格占位：放进 el-table 的 #empty 插槽，表头照常显示，下面画几行占位。
    列宽、对齐从真实表头量出来（含勾选列），所以每格占位都对得上表头；表格宽度变了会重新量。
  -->
  <div ref="root" class="sk-region table-sk" aria-busy="true">
    <span class="sr-only">{{ $t('common.loading') }}</span>
    <div
      v-for="r in rows"
      :key="r"
      class="table-sk__row"
      :style="{ gridTemplateColumns: template, height: rowHeight + 'px' }"
      aria-hidden="true"
    >
      <span v-for="(col, c) in cols" :key="c" class="table-sk__cell" :class="'is-' + col.align">
        <SkBlock v-if="col.check" :w="14" :h="14" />
        <template v-else>
          <SkBlock v-if="avatar && c === firstText" :w="28" :h="28" shape="circle" />
          <SkBlock :w="cellWidth(r, c, col.width)" />
        </template>
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import SkBlock from './SkBlock.vue'

const props = withDefaults(
  defineProps<{
    rows?: number
    rowHeight?: number
    /** 第一列前面带头像（人员、学生名单） */
    avatar?: boolean
  }>(),
  { rows: 6, rowHeight: 48, avatar: false },
)

interface Col {
  width: number
  align: 'left' | 'center' | 'right'
  check: boolean
}

const root = ref<HTMLElement>()
const cols = ref<Col[]>([])

const template = computed(() => cols.value.map((c) => `${c.width}px`).join(' '))
const firstText = computed(() => cols.value.findIndex((c) => !c.check))

// 占位长度在列宽的 45%~80% 之间按固定序列变化，最长 160px
const RATIOS = [0.62, 0.48, 0.74, 0.55, 0.8, 0.45, 0.68]
const cellWidth = (row: number, col: number, width: number) => {
  const room = width - 24 - (props.avatar && col === firstText.value ? 38 : 0)
  return Math.max(20, Math.min(160, Math.round(room * RATIOS[(row * 3 + col) % RATIOS.length])))
}

const measure = () => {
  const table = root.value?.closest('.el-table')
  if (!table) return
  const ths = table.querySelectorAll<HTMLElement>('.el-table__header-wrapper thead tr:first-child > th')
  cols.value = Array.from(ths, (th) => ({
    width: th.getBoundingClientRect().width,
    align: th.classList.contains('is-center') ? 'center' : th.classList.contains('is-right') ? 'right' : 'left',
    check: !!th.querySelector('.el-checkbox'),
  }))
}

let observer: ResizeObserver | null = null
onMounted(() => {
  measure()
  const table = root.value?.closest('.el-table')
  if (table && typeof ResizeObserver !== 'undefined') {
    observer = new ResizeObserver(measure)
    observer.observe(table)
  }
})
onBeforeUnmount(() => observer?.disconnect())
</script>

<style scoped>
.table-sk__row {
  display: grid;
  align-items: center;
  border-bottom: 1px solid var(--el-table-border-color);
}

.table-sk__cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 0 12px;
}

.table-sk__cell.is-center {
  justify-content: center;
}

.table-sk__cell.is-right {
  justify-content: flex-end;
}
</style>

<style>
/* el-table 的空状态块默认居中、只占一半宽、行高 60px：放骨架时让它铺满 */
.el-table__empty-block:has(.table-sk) {
  display: block;
}

.el-table__empty-text:has(> .table-sk) {
  display: block;
  width: 100%;
  line-height: normal;
  text-align: left;
}
</style>

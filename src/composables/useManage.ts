import { ref } from 'vue'
import type { Ref } from 'vue'

/**
 * 批量管理模式（与 Web 端批量删除交互一致）：
 * - manageMode：是否处于勾选管理态
 * - toggleManage：切换管理态，退出时清空勾选
 * - toggleSelect：勾选/取消一行
 * 供 xm-action-bar 操作区 + 列表勾选框组合使用，替代各管理页复制粘贴的同名逻辑
 */
export function useManage(selectedIds: Ref<number[]>) {
  const manageMode = ref(false)

  const toggleManage = () => {
    manageMode.value = !manageMode.value
    if (!manageMode.value) selectedIds.value = []
  }

  const toggleSelect = (id: number) => {
    const idx = selectedIds.value.indexOf(id)
    if (idx >= 0) selectedIds.value.splice(idx, 1)
    else selectedIds.value.push(id)
  }

  return { manageMode, toggleManage, toggleSelect }
}

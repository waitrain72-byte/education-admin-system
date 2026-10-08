import { computed, ref } from 'vue'

/**
 * 「加载更多」式分页：fetchPage({ pageNum, pageSize }) 返回后端分页结果 { list, total }。
 * reload() 回到第一页（覆盖列表），loadNext() 追加下一页（加载中或已到底时忽略）。
 * 连续切换条件时只认最后一次请求的结果，先发后到的旧结果直接丢弃。
 */
export function usePager(fetchPage, pageSize = 10) {
  const list = ref([])
  const total = ref(0)
  const pageNum = ref(0)
  const loading = ref(false)
  const loaded = ref(false)
  const finished = computed(() => loaded.value && list.value.length >= total.value)
  let seq = 0

  async function load(page) {
    const mine = ++seq
    loading.value = true
    try {
      const res = await fetchPage({ pageNum: page, pageSize })
      if (mine !== seq) return
      const rows = (res && res.list) || []
      list.value = page === 1 ? rows : list.value.concat(rows)
      total.value = (res && res.total) || 0
      pageNum.value = page
      loaded.value = true
    } catch {
      // 提示已由请求层统一弹出，保留已加载的内容
    } finally {
      if (mine === seq) loading.value = false
    }
  }

  const reload = () => load(1)
  const loadNext = () => {
    if (loading.value || finished.value || !loaded.value) return Promise.resolve()
    return load(pageNum.value + 1)
  }

  return { list, total, pageNum, loading, loaded, finished, reload, loadNext }
}

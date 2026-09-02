import { ref } from 'vue'
import { get, post, put, del as delRequest } from '@/utils/request'
import { apiMessage, t } from '@/i18n'

/**
 * 通用 CRUD 组合式函数（移动端版，与 Web 端 useCrud 语义对齐）：
 * - 分页列表：load(true) 重置到第一页，load() 追加下一页
 * - 新增/编辑表单：form + formVisible
 * - 删除/批量删除：uni.showModal 原生确认框
 */
export function useCrud(options) {
  const list = ref([])
  const pageNum = ref(1)
  const pageSize = 10
  const total = ref(0)
  const loading = ref(false)
  const formVisible = ref(false)
  const form = ref({})
  const selectedIds = ref([])

  const finished = () => list.value.length >= total.value && total.value > 0

  const load = async (reset = false) => {
    if (loading.value) return
    if (reset) pageNum.value = 1
    loading.value = true
    try {
      const params = Object.assign(
        { pageNum: pageNum.value, pageSize },
        options.getParams ? options.getParams() || {} : {},
      )
      // 空值条件不传给后端（与 Web 端一致，避免空字符串被当作过滤值）
      for (const key of Object.keys(params)) {
        const v = params[key]
        if (v === '' || v === null || v === undefined) delete params[key]
      }
      const res = await get(`${options.url}/selectPage`, params)
      if (res.data && res.data.code === '200') {
        const rows = (res.data.data && res.data.data.list) || []
        const count = (res.data.data && res.data.data.total) || 0
        // 注入当前分页连续行号（跨页累计），供列表展示"序号"用，避免直接显示全局自增 id
        rows.forEach((r, i) => {
          r._index = (pageNum.value - 1) * pageSize + i + 1
        })
        if (reset) {
          list.value = rows
        } else {
          const seen = new Set(list.value.map((r) => r.id))
          list.value = list.value.concat(rows.filter((r) => !seen.has(r.id)))
        }
        total.value = count
      } else {
        uni.showToast({ title: apiMessage(res.data), icon: 'none' })
      }
    } catch {
      // 请求层已统一提示
    } finally {
      loading.value = false
    }
  }

  const loadNext = () => {
    if (finished() || loading.value) return
    pageNum.value += 1
    load()
  }

  const search = () => load(true)

  const resetSearch = (defaults = {}) => {
    if (options.resetParams) options.resetParams()
    load(true)
  }

  const handleAdd = (initForm = {}) => {
    form.value = { ...initForm }
    formVisible.value = true
  }

  const handleEdit = (row) => {
    form.value = JSON.parse(JSON.stringify(row))
    formVisible.value = true
  }

  const closeForm = () => {
    formVisible.value = false
  }

  const save = async () => {
    if (options.validate) {
      const msg = options.validate(form.value)
      if (msg) {
        uni.showToast({ title: msg, icon: 'none' })
        return
      }
    }
    try {
      if (options.beforeSave) await options.beforeSave(form.value)
      const isEdit = !!form.value.id
      const res = isEdit ? await put(`${options.url}/update`, form.value) : await post(`${options.url}/add`, form.value)
      if (res.data && res.data.code === '200') {
        uni.showToast({ title: t('common.saveSuccess'), icon: 'success' })
        if (options.afterSave) await options.afterSave(form.value)
        formVisible.value = false
        load(true)
      } else {
        uni.showToast({ title: apiMessage(res.data), icon: 'none' })
      }
    } catch {
      // 请求层已统一提示
    }
  }

  const confirmAndDo = (content, action) => {
    uni.showModal({
      title: t('common.confirmDeleteTitle'),
      content,
      success: async (res) => {
        if (!res.confirm) return
        try {
          const r = await action()
          if (r.data && r.data.code === '200') {
            uni.showToast({ title: t('common.operationSuccess'), icon: 'success' })
            load(true)
          } else {
            uni.showToast({ title: apiMessage(r.data), icon: 'none' })
          }
        } catch {
          // 请求层已统一提示
        }
      },
    })
  }

  const del = (id) => {
    confirmAndDo(t('common.deleteConfirm'), () => delRequest(`${options.url}/delete/${id}`))
  }

  const delBatch = () => {
    if (!selectedIds.value.length) {
      uni.showToast({ title: t('common.pleaseSelectData'), icon: 'none' })
      return
    }
    confirmAndDo(t('common.batchDeleteConfirm'), () => delRequest(`${options.url}/delete/batch`, selectedIds.value))
  }

  return {
    list,
    pageNum,
    pageSize,
    total,
    loading,
    finished,
    form,
    formVisible,
    selectedIds,
    load,
    loadNext,
    search,
    resetSearch,
    handleAdd,
    handleEdit,
    closeForm,
    save,
    del,
    delBatch,
  }
}

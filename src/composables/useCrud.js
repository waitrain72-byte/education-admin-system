import { ref } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import { get, post, put, del as delRequest } from '@/utils/request'
import { t } from '@/i18n'

/**
 * 通用 CRUD 组合式函数（移动端版，与 Web 端 useCrud 语义对齐）：
 * - 分页列表：load(true) 重置到第一页，load() 追加下一页
 * - 新增/编辑表单：form + formVisible + saving（保存防重复提交）
 * - 删除/批量删除：uni.showModal 原生确认框
 * - 下拉刷新：页面在 pages.json 开启 enablePullDownRefresh 后，此处统一注册
 *   onPullDownRefresh（重置到第一页 + 收起下拉动画），页面无需重复实现
 * - 加载动画：首屏/搜索（重置且非下拉触发）由请求层唤起统一 xm-loader 蒙层；
 *   触底加载更多保持静默，进度反馈交给列表底部的 xm-list-footer
 */
export function useCrud(options) {
  const list = ref([])
  const pageNum = ref(1)
  const pageSize = 10
  const total = ref(0)
  const loading = ref(false)
  const saving = ref(false)
  const formVisible = ref(false)
  const form = ref({})
  const selectedIds = ref([])

  // total 为 0（含首次）视为没有更多，避免空结果页面反复触底请求
  const finished = () => total.value === 0 || list.value.length >= total.value

  const load = async (reset = false, fromPullDown = false) => {
    if (loading.value) return
    const pageBefore = pageNum.value
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
      // 下拉刷新走原生动画反馈；追加分页静默（底部 footer 已有 loading 态）
      const page = await get(`${options.url}/selectPage`, params, {
        loading: reset && !fromPullDown,
      })
      const rows = (page && page.list) || []
      const count = (page && page.total) || 0
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
    } catch {
      // 业务错误与网络异常的提示均已由请求层统一弹出；
      // 追加失败回滚页码到上一已加载页，否则下次 loadNext 会跳页漏数据
      if (!reset) pageNum.value = Math.max(1, pageBefore - 1)
    } finally {
      loading.value = false
    }
  }

  // 下拉刷新：重置到第一页并收起下拉动画（仅开启了 enablePullDownRefresh 的页面会触发）
  onPullDownRefresh(() => {
    load(true, true).finally(() => uni.stopPullDownRefresh())
  })

  const loadNext = () => {
    if (finished() || loading.value) return
    pageNum.value += 1
    load()
  }

  const search = () => load(true)

  const resetSearch = (_defaults = {}) => {
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
    if (saving.value) return
    if (options.validate) {
      const msg = options.validate(form.value)
      if (msg) {
        uni.showToast({ title: msg, icon: 'none' })
        return
      }
    }
    // 防重复提交：保存请求进行中忽略再次点击（弹层确定按钮同步按 saving 禁用）
    saving.value = true
    try {
      if (options.beforeSave) {
        const msg = await options.beforeSave(form.value)
        if (msg) {
          uni.showToast({ title: msg, icon: 'none' })
          return
        }
      }
      const isEdit = !!form.value.id
      if (isEdit) {
        await put(`${options.url}/update`, form.value)
      } else {
        await post(`${options.url}/add`, form.value)
      }
      uni.showToast({ title: t('common.saveSuccess'), icon: 'success' })
      if (options.afterSave) await options.afterSave(form.value)
      formVisible.value = false
      load(true)
    } catch {
      // 业务错误与网络异常的提示均已由请求层统一弹出
    } finally {
      saving.value = false
    }
  }

  const confirmAndDo = (content, action) => {
    uni.showModal({
      title: t('common.confirmDeleteTitle'),
      content,
      success: async (res) => {
        if (!res.confirm) return
        try {
          await action()
          uni.showToast({ title: t('common.operationSuccess'), icon: 'success' })
          load(true)
        } catch {
          // 业务错误与网络异常的提示均已由请求层统一弹出
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
    saving,
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

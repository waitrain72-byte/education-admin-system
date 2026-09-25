import { ref } from 'vue'
import { onHide, onPullDownRefresh } from '@dcloudio/uni-app'
import { confirm } from '@/utils/confirm'
import { t } from '@/i18n'

/**
 * 通用 CRUD 组合式函数（移动端版，与 Web 端 useCrud 语义对齐）。列表页一般不直接用它，
 * 而是用在它之上封装了搜索条件 / 批量管理 / 页面生命周期的 useListPage。
 * - options.api：接口对象（src/api 里的 xxxApi），分页 / 新增 / 修改 / 删除都通过它请求
 * - 分页列表：load(true) 重置到第一页，load() 追加下一页
 * - 新增/编辑表单：form + formVisible + saving（保存防重复提交）；handleEdit(row, overrides) 可覆盖部分字段
 * - 删除/批量删除：原生确认框；options.deleteConfirm 可换成页面自己的确认文案词条
 * - 下拉刷新：页面在 pages.json 开启 enablePullDownRefresh 后，此处统一注册
 *   onPullDownRefresh（重置到第一页 + 收起下拉动画），页面无需重复实现
 * - 加载动画：首屏/搜索（重置且非下拉触发）由请求层唤起统一 xm-loader 蒙层；
 *   触底加载更多保持静默，进度反馈交给列表底部的 xm-list-footer
 * - 页面 onShow 调 loadOnShow()：只在首次进入时加载，返回页面保持列表/页码/滚动位置（对齐 z-paging）
 */

/** 数据过期时长：页面被遮挡/切后台超过此时长再回来，静默刷新第一页 */
export const STALE_MS = 5 * 60 * 1000

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
  // 是否已成功加载过第一页（首次失败时下次 onShow 会重试）；页面最近一次被遮挡的时间
  let loaded = false
  let hiddenAt = 0

  // total 为 0（含首次）视为没有更多，避免空结果页面反复触底请求
  const finished = () => total.value === 0 || list.value.length >= total.value

  // silent：不唤起全屏加载蒙层（下拉刷新有原生动画、过期刷新在后台完成）
  const load = async (reset = false, silent = false) => {
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
      const page = await options.api.selectPage(params, {
        loading: reset && !silent,
      })
      const rows = (page && page.list) || []
      const count = (page && page.total) || 0
      // 注入当前分页连续行号（跨页累计），供列表展示"序号"用，避免直接显示全局自增 id
      rows.forEach((r, i) => {
        r._index = (pageNum.value - 1) * pageSize + i + 1
      })
      if (reset) {
        list.value = rows
        loaded = true
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

  /**
   * 页面 onShow 调用：首次进入加载第一页；之后回到页面（选图、预览图片、打开文档、切后台回来都会触发 onShow）
   * 保持列表、页码与滚动位置不动，不再每次重置并弹全屏蒙层。
   * 例外：离开超过 STALE_MS（如切后台很久）回来时静默刷新第一页；表单弹层打开时不动，避免打断填写。
   * 返回是否发起了加载，页面可据此联动刷新下拉选项等附属数据。
   */
  const loadOnShow = () => {
    if (!loaded) {
      load(true)
      return true
    }
    const hiddenFor = hiddenAt ? Date.now() - hiddenAt : 0
    hiddenAt = 0
    if (formVisible.value || hiddenFor < STALE_MS) return false
    load(true, true)
    return true
  }

  onHide(() => {
    hiddenAt = Date.now()
  })

  const loadNext = () => {
    if (finished() || loading.value) return
    pageNum.value += 1
    load()
  }

  const search = () => load(true)

  const handleAdd = (initForm = {}) => {
    form.value = { ...initForm }
    formVisible.value = true
  }

  // overrides：编辑时需要重置的字段（如学生重新提交请假时状态回到「待审核」）
  const handleEdit = (row, overrides = {}) => {
    form.value = { ...JSON.parse(JSON.stringify(row)), ...overrides }
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
        await options.api.update(form.value)
      } else {
        await options.api.add(form.value)
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

  const confirmAndDo = async (content, action) => {
    if (!(await confirm(content))) return
    try {
      await action()
      uni.showToast({ title: t('common.operationSuccess'), icon: 'success' })
      load(true)
    } catch {
      // 业务错误与网络异常的提示均已由请求层统一弹出
    }
  }

  const del = (id) => confirmAndDo(t(options.deleteConfirm || 'common.deleteConfirm'), () => options.api.delete(id))

  const delBatch = () => {
    if (!selectedIds.value.length) {
      uni.showToast({ title: t('common.pleaseSelectData'), icon: 'none' })
      return
    }
    return confirmAndDo(t('common.batchDeleteConfirm'), () => options.api.deleteBatch(selectedIds.value))
  }

  return {
    list,
    total,
    loading,
    saving,
    finished,
    form,
    formVisible,
    selectedIds,
    load,
    loadOnShow,
    loadNext,
    search,
    handleAdd,
    handleEdit,
    closeForm,
    save,
    del,
    delBatch,
  }
}

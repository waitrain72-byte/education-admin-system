import { reactive } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { useCrud } from './useCrud'
import { useManage } from './useManage'
import { useUserStore } from '@/stores/user'
import { ensureLoggedIn } from '@/utils/authGuard'
import { t } from '@/i18n'

/**
 * 列表页标准骨架：分页列表 + 搜索条件 + 新增 / 编辑表单 + 批量管理 + 页面生命周期，一次调用搞定。
 * 新增一个 CRUD 列表页，脚本部分只需要（完整示例见 README「新增一个列表页」）：
 *
 *   const { list, query, resetQuery, form, handleAdd, handleEdit, save, del, … } = useListPage({
 *     api: noticeApi,              // 接口对象（src/api），分页 / 新增 / 修改 / 删除都走它
 *     title: 'menu.notice',        // 导航栏标题词条
 *     query: { title: '' },        // 搜索条件初值
 *     validate: (f) => (f.title ? '' : t('pages.notice.ruleTitleRequired')),
 *   })
 *
 * 选项（其余字段原样传给 useCrud：api / validate / beforeSave / afterSave / deleteConfirm）：
 * - title：导航栏标题词条，每次显示页面时按当前语言设置
 * - roles：允许访问的角色，如 ['ADMIN']；其他角色提示无权限并返回上一页
 * - query：搜索条件初值，自动作为分页请求参数（空值不传）；resetQuery() 恢复初值并重新查询
 * - loadExtras：列表在页面显示时加载（首次进入 / 离开过久回来）后一并执行的附属加载，如下拉选项
 *
 * 另外自动完成：上拉触底加载下一页、下拉刷新（useCrud）、批量管理勾选（useManage）。
 */
export function useListPage(options) {
  const userStore = useUserStore()
  const initialQuery = { ...(options.query || {}) }
  const query = reactive({ ...initialQuery })

  const crud = useCrud({ ...options, getParams: () => ({ ...query }) })
  const { manageMode, toggleManage, toggleSelect } = useManage(crud.selectedIds)

  /** 清空搜索条件（恢复初值）并回到第一页 */
  const resetQuery = () => {
    Object.assign(query, initialQuery)
    return crud.search()
  }

  onShow(() => {
    if (options.title) uni.setNavigationBarTitle({ title: t(options.title) })
    if (!ensureLoggedIn()) return
    if (options.roles && !options.roles.includes(userStore.role)) {
      uni.showToast({ title: t('forbidden.message'), icon: 'none' })
      setTimeout(() => uni.navigateBack(), 800)
      return
    }
    // 首次进入才加载；选图 / 预览后返回保持列表与滚动位置，附属数据随列表一起刷新
    if (crud.loadOnShow() && options.loadExtras) options.loadExtras()
  })

  onReachBottom(() => crud.loadNext())

  return { ...crud, query, resetQuery, manageMode, toggleManage, toggleSelect }
}

import { get, post, put, del } from '@/utils/request'

/**
 * 通用 CRUD 接口：与后端 CrudController 的 7 个标准端点一一对应，方法名与后端保持一致。
 *
 *   selectPage(params, opts)   GET    {base}/selectPage      分页：{ pageNum, pageSize, ...搜索条件 } → { list, total }
 *   selectAll(params, opts)    GET    {base}/selectAll       全量（下拉选项等），可带过滤条件
 *   selectById(id, opts)       GET    {base}/selectById/{id}
 *   add(data, opts)            POST   {base}/add
 *   update(data, opts)         PUT    {base}/update
 *   delete(id, opts)           DELETE {base}/delete/{id}
 *   deleteBatch(ids, opts)     DELETE {base}/delete/batch    请求体为 id 数组
 *
 * opts 与请求层一致（如 SILENT 静默、不弹加载蒙层）；成功返回业务数据，失败已统一提示并 reject。
 */
export function createCrudApi(base) {
  return {
    selectPage: (params, opts) => get(`${base}/selectPage`, params, opts),
    selectAll: (params, opts) => get(`${base}/selectAll`, params, opts),
    selectById: (id, opts) => get(`${base}/selectById/${id}`, undefined, opts),
    add: (data, opts) => post(`${base}/add`, data, opts),
    update: (data, opts) => put(`${base}/update`, data, opts),
    delete: (id, opts) => del(`${base}/delete/${id}`, undefined, opts),
    deleteBatch: (ids, opts) => del(`${base}/delete/batch`, ids, opts),
  }
}

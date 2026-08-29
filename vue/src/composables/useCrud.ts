import { ref } from 'vue'
import type { ComputedRef } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/utils/request'
import { apiMessage, t } from '@/i18n'

export interface UseCrudOptions {
    /** 接口基础路径，如 '/college'，自动拼接 /selectPage、/add、/update、/delete/{id}、/delete/batch */
    url: string
    /** 表单校验规则，供页面 el-form 使用（支持 computed 以响应语言切换） */
    rules?: FormRules | ComputedRef<FormRules>
    /** 分页查询参数（搜索条件），每次查询时求值 */
    getParams?: () => Record<string, any>
    /** 保存前钩子：可补充或修正表单数据 */
    beforeSave?: (form: Record<string, any>) => void | Promise<void>
    /** 保存成功后钩子：如同步全局用户信息 */
    afterSave?: (form: Record<string, any>) => void | Promise<void>
    /** 单个删除的确认文案（默认“您确定删除吗？”） */
    deleteConfirmMessage?: string
}

/**
 * 通用 CRUD 组合式函数：统一管理列表分页、增删改查、批量删除与表单校验，
 * 消除各业务页面重复的样板代码。业务差异通过 options 钩子注入。
 */
export function useCrud<T = any>(options: UseCrudOptions) {
    const tableData = ref<T[]>([])
    const pageNum = ref(1)
    const pageSize = ref(10)
    const total = ref(0)
    const loading = ref(false)
    const formVisible = ref(false)
    const form = ref<Record<string, any>>({})
    const formRef = ref<FormInstance>()
    const selectedIds = ref<(number | string)[]>([])

    const load = async (pNum?: number) => {
        if (pNum) pageNum.value = pNum
        loading.value = true
        try {
            const params: Record<string, any> = {
                pageNum: pageNum.value,
                pageSize: pageSize.value,
                ...(options.getParams?.() || {}),
            }
            // 空字符串/null/undefined 的查询条件不传给后端：
            // 后端 Mapper 使用 <if test="x != null"> 判断，空字符串会被当成有效过滤值
            // （如 status = ''），导致首次加载查不到数据
            for (const key of Object.keys(params)) {
                const value = params[key]
                if (value === '' || value === null || value === undefined) {
                    delete params[key]
                }
            }
            const res: any = await request.get(`${options.url}/selectPage`, {
                params,
            })
            tableData.value = res.data?.data?.list || []
            total.value = res.data?.data?.total || 0
        } catch {
            // 错误提示已由 axios 拦截器统一处理
        } finally {
            loading.value = false
        }
    }

    const handleAdd = () => {
        form.value = {}
        formVisible.value = true
    }

    const handleEdit = (row: T) => {
        // 深拷贝行数据，避免表单编辑直接污染表格数据
        form.value = JSON.parse(JSON.stringify(row))
        formVisible.value = true
    }

    const save = async () => {
        if (formRef.value) {
            const valid = await formRef.value.validate().catch(() => false)
            if (!valid) return
        }
        try {
            await options.beforeSave?.(form.value)
            const isEdit = !!form.value.id
            const res: any = await request({
                url: isEdit ? `${options.url}/update` : `${options.url}/add`,
                method: isEdit ? 'PUT' : 'POST',
                data: form.value,
            })
            if (res.data.code === '200') {
                ElMessage.success(t('common.saveSuccess'))
                await options.afterSave?.(form.value)
                load(1)
                formVisible.value = false
            } else {
                ElMessage.error(apiMessage(res.data))
            }
        } catch {
            // 错误提示已由 axios 拦截器统一处理
        }
    }

    const del = (id: number | string) => {
        ElMessageBox.confirm(options.deleteConfirmMessage || t('common.deleteConfirm'), t('common.confirmDeleteTitle'), { type: 'warning' })
            .then(async () => {
                try {
                    const res: any = await request.delete(`${options.url}/delete/${id}`)
                    if (res.data.code === '200') {
                        ElMessage.success(t('common.operationSuccess'))
                        load(1)
                    } else {
                        ElMessage.error(apiMessage(res.data))
                    }
                } catch {
                    // 错误提示已由 axios 拦截器统一处理
                }
            })
            .catch(() => {
                // 用户取消确认框
            })
    }

    const handleSelectionChange = (rows: T[]) => {
        selectedIds.value = rows.map((v: any) => v.id)
    }

    const delBatch = () => {
        if (!selectedIds.value.length) {
            ElMessage.warning(t('common.pleaseSelectData'))
            return
        }
        ElMessageBox.confirm(t('common.batchDeleteConfirm'), t('common.confirmDeleteTitle'), { type: 'warning' })
            .then(async () => {
                try {
                    const res: any = await request.delete(`${options.url}/delete/batch`, {
                        data: selectedIds.value,
                    })
                    if (res.data.code === '200') {
                        ElMessage.success(t('common.operationSuccess'))
                        load(1)
                    } else {
                        ElMessage.error(apiMessage(res.data))
                    }
                } catch {
                    // 错误提示已由 axios 拦截器统一处理
                }
            })
            .catch(() => {
                // 用户取消确认框
            })
    }

    return {
        tableData,
        pageNum,
        pageSize,
        total,
        loading,
        formVisible,
        form,
        formRef,
        selectedIds,
        rules: options.rules,
        load,
        handleAdd,
        handleEdit,
        save,
        del,
        handleSelectionChange,
        delBatch,
    }
}

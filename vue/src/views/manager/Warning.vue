<template>
    <div>
        <el-card shadow="never">
            <el-alert type="info" :closable="false" :title="$t('pages.warning.desc')" style="margin-bottom: 16px" />
            <el-table v-loading="loading" :data="list" stripe>
                <el-table-column prop="studentName" :label="$t('pages.warning.student')" min-width="100" />
                <el-table-column
                    prop="courseCount"
                    :label="$t('pages.warning.courseCount')"
                    width="100"
                    align="center"
                />
                <el-table-column prop="avgScore" :label="$t('pages.warning.avg')" width="100" align="center" />
                <el-table-column prop="failedCount" :label="$t('pages.warning.failed')" width="100" align="center" />
                <el-table-column prop="absentRate" :label="$t('pages.warning.absentRate')" width="130" align="center" />
                <el-table-column
                    prop="riskIndex"
                    :label="$t('pages.warning.riskIndex')"
                    width="110"
                    align="center"
                    sortable
                />
                <el-table-column prop="level" :label="$t('pages.warning.level')" width="110" align="center">
                    <template #default="{ row }">
                        <el-tag :type="tagType(row.level)" effect="light">{{ row.level }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column
                    prop="suggestion"
                    :label="$t('pages.warning.suggestion')"
                    min-width="220"
                    show-overflow-tooltip
                />
                <el-table-column
                    v-if="userStore.role !== 'STUDENT'"
                    :label="$t('common.operation')"
                    width="130"
                    align="center"
                >
                    <template #default="{ row }">
                        <el-button size="small" type="warning" plain @click="notify(row)">{{
                            $t('pages.warning.notify')
                        }}</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>
    </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Warning' })

import { ref, onMounted } from 'vue'
import { ElMessage } from '@/utils/element-plus'
import request from '@/utils/request'
import { t } from '@/i18n'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const list = ref<any[]>([])
const loading = ref(false)

const load = () => {
    loading.value = true
    request
        .get<any[]>('/warning/list')
        .then((rows) => {
            list.value = rows || []
        })
        .catch(() => {
            // 错误提示已由 axios 拦截器统一处理
        })
        .finally(() => {
            loading.value = false
        })
}

/** 向学生实时推送预警提醒（WebSocket：App 端 toast + 角标，Web 端弹窗） */
const notify = (row: any) => {
    request.post(`/warning/notify/${row.studentId}`)
        .then(() => ElMessage.success(t('pages.warning.notifyOk')))
        .catch(() => {
            // 错误提示已由 axios 拦截器统一处理
        })
}

const tagType = (level: string) => {
    if (level === '高风险') return 'danger'
    if (level === '中风险') return 'warning'
    if (level === '低风险') return 'info'
    return 'success'
}

onMounted(load)
</script>

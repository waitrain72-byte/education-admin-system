<template>
  <section class="panel">
    <div class="panel__head">
      <span class="panel__title">{{ $t('workbench.attention.title') }}</span>
    </div>
    <div class="panel__body">
      <el-skeleton v-if="loading" animated :rows="2" />
      <template v-else-if="students.length">
        <p class="att-hint">{{ $t('workbench.attention.hint') }}</p>
        <ul class="att">
          <li v-for="s in shown" :key="s.studentId" class="att__row">
            <UserAvatar :name="s.studentName" :size="30" />
            <div class="att__main">
              <div class="att__name">
                {{ s.studentName }}
                <span class="pill" :class="s.level === '高风险' ? 'pill--bad' : 'pill--warn'">{{ levelLabel(s.level) }}</span>
              </div>
              <div class="att__meta num">{{ $t('workbench.attention.meta', { index: s.riskIndex, failed: s.failedCount, absent: s.absentRate }) }}</div>
            </div>
            <el-button size="small" :loading="sending === s.studentId" :disabled="sent.includes(s.studentId)" @click="remind(s)">
              {{ $t('workbench.attention.notify') }}
            </el-button>
          </li>
        </ul>
        <button v-if="students.length > LIMIT && !expanded" type="button" class="att-more" @click="expanded = true">
          {{ $t('workbench.attention.more', { n: students.length - LIMIT }) }}
        </button>
      </template>
      <div v-else class="empty-note">{{ $t('workbench.attention.empty') }}</div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import request from '@/utils/request'
import { ElMessage } from '@/utils/element-plus'
import UserAvatar from '@/components/UserAvatar.vue'
import { t } from '@/i18n'

/** 老师首页：自己课上学业风险不是「正常」的学生（原来在旧版「学业预警」页里），可以直接发提醒 */
const LIMIT = 5

const students = ref<Record<string, any>[]>([])
const loading = ref(false)
const expanded = ref(false)
const sending = ref<number | null>(null)
const sent = ref<number[]>([])

const shown = computed(() => (expanded.value ? students.value : students.value.slice(0, LIMIT)))

/** 后端返回的等级是中文原值，显示时翻译 */
const LEVEL_KEY: Record<string, string> = { 高风险: 'high', 中风险: 'medium', 低风险: 'low' }
const levelLabel = (level: string) => (LEVEL_KEY[level] ? t(`workbench.attention.levels.${LEVEL_KEY[level]}`) : level)

onMounted(async () => {
  loading.value = true
  try {
    const list = (await request.get<Record<string, any>[]>('/warning/list')) || []
    students.value = list.filter((s) => s.level !== '正常')
  } catch {
    // 拿不到就显示空，首页其他部分不受影响
  } finally {
    loading.value = false
  }
})

const remind = async (s: Record<string, any>) => {
  sending.value = s.studentId
  try {
    await request.post(`/warning/notify/${s.studentId}`)
    sent.value.push(s.studentId)
    ElMessage.success(t('workbench.attention.notified', { name: s.studentName }))
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    sending.value = null
  }
}
</script>

<style scoped>
.att-hint {
  margin-bottom: 10px;
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.att {
  display: grid;
  gap: 12px;
  list-style: none;
}

.att__row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.att__main {
  flex: 1;
  min-width: 0;
}

.att__name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--xm-text-primary);
}

.att__meta {
  margin-top: 2px;
  overflow: hidden;
  font-size: 12px;
  color: var(--xm-text-secondary);
  white-space: nowrap;
  text-overflow: ellipsis;
}

.att-more {
  margin-top: 10px;
  padding: 0;
  border: 0;
  background: none;
  color: var(--xm-brand);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}
</style>

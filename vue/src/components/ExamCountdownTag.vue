<template>
  <el-tag
      v-if="tag"
      :type="tag.type"
      size="small"
      effect="light"
      disable-transitions
      class="exam-countdown"
  >{{ tag.text }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { t } from '@/i18n'
import { examCountdown, countdownText, countdownTagType } from '@/utils/examCountdown'
import { useNow } from '@/composables/useNow'

/** 考试倒计时标签：今天 / 明天 / 还有 N 天 / 已结束；没有考试时间时不渲染（考试安排列表与首页共用） */
defineOptions({ name: 'ExamCountdownTag' })

const props = defineProps<{ examTime?: string | null }>()
// 当前时间每分钟刷新：页面开着跨过零点后标签自动更新
const now = useNow()

const tag = computed(() => {
  const cd = examCountdown(props.examTime, now.value)
  return cd ? { text: countdownText(cd, t), type: countdownTagType(cd) } : null
})
</script>

<style scoped>
.exam-countdown {
  margin-left: 8px;
}
</style>

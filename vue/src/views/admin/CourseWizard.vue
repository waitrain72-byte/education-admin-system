<template>
  <el-dialog
    :model-value="modelValue"
    :title="course ? $t('admin.wizard.editTitle') : $t('admin.wizard.createTitle')"
    width="640px"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-steps :active="step" finish-status="success" simple class="cw-steps">
      <el-step :title="$t('admin.wizard.steps.basic')" />
      <el-step :title="$t('admin.wizard.steps.schedule')" />
      <el-step :title="$t('admin.wizard.steps.confirm')" />
    </el-steps>

    <!-- 第一步：基本信息 -->
    <el-form v-show="step === 0" ref="basicRef" :model="form" :rules="rules" label-position="top" class="cw-form">
      <el-form-item :label="$t('admin.wizard.name')" prop="name">
        <el-input v-model="form.name" maxlength="50" :placeholder="$t('admin.wizard.namePlaceholder')" />
      </el-form-item>
      <div class="cw-row">
        <el-form-item :label="$t('admin.wizard.type')">
          <el-radio-group v-model="form.type">
            <el-radio-button value="必修">{{ $t('pages.course.required') }}</el-radio-button>
            <el-radio-button value="选修">{{ $t('pages.course.elective') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="$t('admin.wizard.credit')">
          <el-input-number v-model="form.score" :min="1" :max="10" :precision="0" controls-position="right" />
        </el-form-item>
        <el-form-item :label="$t('admin.wizard.capacity')">
          <el-input-number v-model="form.num" :min="1" :max="500" :precision="0" :step="10" controls-position="right" />
        </el-form-item>
      </div>
      <el-form-item :label="$t('admin.wizard.teacher')" prop="teacherId">
        <el-select v-model="form.teacherId" filterable :placeholder="$t('admin.wizard.teacherPlaceholder')" style="width: 100%">
          <el-option v-for="tc in teachers" :key="tc.id" :label="tc.title ? `${tc.name}（${tc.title}）` : tc.name" :value="tc.id" />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('admin.wizard.intro')">
        <el-input v-model="form.intro" type="textarea" :rows="3" maxlength="500" show-word-limit :placeholder="$t('admin.wizard.introPlaceholder')" />
      </el-form-item>
    </el-form>

    <!-- 第二步：时间与教室 -->
    <div v-show="step === 1" class="cw-form">
      <el-checkbox v-model="unscheduled">{{ $t('admin.wizard.noSchedule') }}</el-checkbox>
      <template v-if="!unscheduled">
        <div class="cw-row">
          <el-form-item :label="$t('admin.wizard.week')" class="cw-grow">
            <el-select v-model="form.week" style="width: 100%" @change="loadRooms">
              <el-option v-for="w in WEEKDAYS" :key="w" :label="w" :value="w" />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('admin.wizard.segment')" class="cw-grow">
            <el-select v-model="form.segment" style="width: 100%" @change="loadRooms">
              <el-option v-for="s in SEGMENTS" :key="s" :label="s" :value="s" />
            </el-select>
          </el-form-item>
        </div>
        <div class="cw-label">{{ $t('admin.wizard.room') }}</div>
        <p class="cw-hint">{{ $t('admin.wizard.roomHint') }}</p>
        <el-skeleton v-if="roomsLoading" animated :rows="2" />
        <p v-else-if="form.week && form.segment && !rooms.length" class="cw-warn">{{ $t('admin.wizard.noRoom') }}</p>
        <div v-else class="cw-rooms" role="radiogroup" :aria-label="$t('admin.wizard.room')">
          <button
            v-for="r in rooms"
            :key="r.code"
            type="button"
            role="radio"
            class="cw-room"
            :class="{ 'is-on': form.room === r.code }"
            :aria-checked="form.room === r.code"
            @click="form.room = r.code"
          >
            <span class="cw-room__code num">{{ r.code }}</span>
            <span class="cw-room__name">{{ r.type === '运动场馆' ? r.name : r.content || r.name }}</span>
            <span class="cw-room__seats num">{{ $t('admin.wizard.seats', { n: r.num }) }}</span>
          </button>
        </div>
      </template>
    </div>

    <!-- 第三步：确认 -->
    <div v-show="step === 2" class="cw-form">
      <dl class="cw-summary">
        <div><dt>{{ $t('admin.wizard.name') }}</dt><dd>{{ form.name }}</dd></div>
        <div><dt>{{ $t('admin.wizard.type') }}</dt><dd>{{ form.type }}</dd></div>
        <div><dt>{{ $t('admin.wizard.credit') }}</dt><dd class="num">{{ form.score }}</dd></div>
        <div><dt>{{ $t('admin.wizard.capacity') }}</dt><dd class="num">{{ form.num }}</dd></div>
        <div><dt>{{ $t('admin.wizard.teacher') }}</dt><dd>{{ teacherName }}</dd></div>
        <div>
          <dt>{{ $t('admin.courses.time') }}</dt>
          <dd>{{ unscheduled || !form.week ? $t('courses.unscheduled') : `${form.week} ${segmentShortName(form.segment)}` }}</dd>
        </div>
        <div><dt>{{ $t('admin.courses.room') }}</dt><dd>{{ (!unscheduled && form.room) || '—' }}</dd></div>
      </dl>
      <el-form-item :label="$t('admin.wizard.status')">
        <el-radio-group v-model="form.status">
          <el-radio-button value="未开课">{{ $t('courses.status.notStarted') }}</el-radio-button>
          <el-radio-button value="已开课">{{ $t('courses.filter.active') }}</el-radio-button>
          <el-radio-button value="已结课">{{ $t('courses.status.finished') }}</el-radio-button>
        </el-radio-group>
      </el-form-item>
    </div>

    <template #footer>
      <el-button v-if="step > 0" @click="step -= 1">{{ $t('admin.wizard.prev') }}</el-button>
      <el-button v-else @click="emit('update:modelValue', false)">{{ $t('common.cancel') }}</el-button>
      <el-button v-if="step < 2" type="primary" @click="next">{{ $t('admin.wizard.next') }}</el-button>
      <el-button v-else type="primary" :loading="saving" @click="save">
        {{ course ? $t('admin.wizard.save') : $t('admin.wizard.create') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/utils/request'
import { ElMessage } from '@/utils/element-plus'
import { SEGMENTS, WEEKDAYS, segmentShortName } from '@/utils/schedule'
import { t } from '@/i18n'

/**
 * 开课向导：基本信息 → 时间与教室（只给出该时段空着、坐得下的教室）→ 确认。编辑课程也走这里。
 */
const props = defineProps<{
  modelValue: boolean
  course: Record<string, any> | null
  teachers: Record<string, any>[]
}>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: []
}>()

const step = ref(0)
const basicRef = ref<FormInstance>()
const saving = ref(false)
const unscheduled = ref(false)
const rooms = ref<Record<string, any>[]>([])
const roomsLoading = ref(false)

const blank = () => ({
  name: '',
  type: '必修',
  score: 2,
  num: 60,
  teacherId: null as number | null,
  intro: '',
  week: '' as string,
  segment: '' as string,
  room: '' as string,
  status: '未开课',
})
const form = reactive(blank())

const rules = computed<FormRules>(() => ({
  name: [{ required: true, whitespace: true, message: t('admin.wizard.nameRequired'), trigger: 'blur' }],
  teacherId: [{ required: true, message: t('admin.wizard.teacherRequired'), trigger: 'change' }],
}))

const teacherName = computed(() => props.teachers.find((x) => x.id === form.teacherId)?.name || '—')

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    step.value = 0
    const c = props.course
    Object.assign(form, blank(), c ? {
      name: c.name || '',
      type: c.type || '必修',
      score: c.score ?? 2,
      num: c.num ?? 60,
      teacherId: c.teacherId ?? null,
      intro: c.intro || '',
      week: c.week || '',
      segment: c.segment || '',
      room: c.room || '',
      status: c.status || '未开课',
    } : {})
    unscheduled.value = !!c && !c.week
    rooms.value = []
  },
  { immediate: true },
)

/** 这个时段空着、坐得下的教室；体育课优先找运动场馆 */
const loadRooms = async () => {
  if (!form.week || !form.segment) {
    rooms.value = []
    return
  }
  roomsLoading.value = true
  const base = { week: form.week, segment: form.segment, num: form.num || undefined, excludeId: props.course?.id || undefined }
  try {
    let list: Record<string, any>[] = []
    if (form.name.includes('体育')) {
      list = (await request.get<Record<string, any>[]>('/course/roomFree', { params: { ...base, typeFilter: '运动场馆' } })) || []
    }
    if (!list.length) {
      list = (await request.get<Record<string, any>[]>('/course/roomFree', { params: base })) || []
    }
    rooms.value = list
    // 原来的教室还空着就保留，否则默认选最贴近人数的那间
    if (!list.some((r) => r.code === form.room)) form.room = list[0]?.code || ''
  } catch {
    rooms.value = []
  } finally {
    roomsLoading.value = false
  }
}

const next = async () => {
  if (step.value === 0) {
    const valid = await basicRef.value?.validate().catch(() => false)
    if (!valid) return
    step.value = 1
    if (!unscheduled.value) loadRooms()
    return
  }
  if (step.value === 1 && !unscheduled.value) {
    if (!form.week || !form.segment || !form.room) {
      ElMessage.warning(t('admin.wizard.roomRequired'))
      return
    }
  }
  step.value = 2
}

const save = async () => {
  const body: Record<string, any> = {
    name: form.name.trim(),
    type: form.type,
    score: form.score,
    num: form.num,
    teacherId: form.teacherId,
    intro: form.intro,
    status: form.status,
    week: unscheduled.value ? '' : form.week,
    segment: unscheduled.value ? '' : form.segment,
    room: unscheduled.value ? '' : form.room,
  }
  saving.value = true
  try {
    if (props.course) {
      await request.put('/course/update', { ...body, id: props.course.id })
      ElMessage.success(t('admin.wizard.saved'))
    } else {
      await request.post('/course/add', body)
      ElMessage.success(t('admin.wizard.created', { name: body.name }))
    }
    emit('saved')
    emit('update:modelValue', false)
  } catch {
    // 教室被占用等提示已由拦截器统一处理
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.cw-steps {
  margin-bottom: 18px;
}

.cw-form {
  display: grid;
  gap: 4px;
}

.cw-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.cw-grow {
  flex: 1;
  min-width: 180px;
}

.cw-label {
  font-size: 14px;
  color: var(--xm-text-regular);
}

.cw-hint {
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.cw-warn {
  font-size: 13px;
  color: var(--xm-warn);
}

.cw-rooms {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 8px;
  max-height: 260px;
  overflow-y: auto;
}

.cw-room {
  display: grid;
  gap: 2px;
  padding: 10px 12px;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius);
  background: var(--xm-bg-card);
  color: var(--xm-text-primary);
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.cw-room:hover {
  border-color: var(--xm-brand);
}

.cw-room.is-on {
  border-color: var(--xm-brand);
  background: var(--xm-brand-soft);
  box-shadow: 0 0 0 1px var(--xm-brand) inset;
}

.cw-room__code {
  font-size: 15px;
  font-weight: 700;
}

.cw-room__name,
.cw-room__seats {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.cw-summary {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 20px;
  margin-bottom: 16px;
  padding: 14px 16px;
  border-radius: var(--xm-radius);
  background: var(--xm-bg-sunken);
}

.cw-summary dt {
  font-size: 12px;
  color: var(--xm-text-secondary);
}

.cw-summary dd {
  margin-top: 2px;
  font-size: 14px;
  color: var(--xm-text-primary);
}
</style>

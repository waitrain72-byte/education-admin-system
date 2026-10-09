<template>
  <!-- 学院 / 专业 / 班级三级联动选择（easycom 自动注册）：原生多列选择器，滚动上一列时下一列跟着换；
       每列第一项是「不限」（筛选）或「不选」（表单），v-model 是选中的 id 数组（可以只选到学院或专业） -->
  <picker
    mode="multiSelector"
    :range="columns"
    :value="draft"
    @columnchange="onColumnChange"
    @change="onChange"
    @cancel="resetDraft"
  >
    <view
      class="xm-select"
      :class="{ placeholder: !label }"
    >
      <text class="xm-ellipsis">{{ label || placeholder }}</text>
      <view
        v-if="label && clearable"
        class="clear"
        @click.stop="clear"
      >
        <xm-icon
          name="x-circle"
          :size="28"
        />
      </view>
      <xm-icon
        v-else
        name="chevron-down"
        :size="28"
      />
    </view>
  </picker>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  /** [学院 id, 专业 id?, 班级 id?] */
  modelValue: { type: Array, default: () => [] },
  /** /people/org 返回的组织架构树：[{ id, name, specialities: [{ id, name, classes: [{ id, name }] }] }] */
  tree: { type: Array, default: () => [] },
  placeholder: { type: String, default: '' },
  /** 每列第一项的文字：「不限」/「不选」 */
  anyLabel: { type: String, default: '—' },
  clearable: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'change'])

/** 每列当前停在第几项（0 = 不限 / 不选） */
const draft = ref([0, 0, 0])

const levelsOf = (d) => {
  const college = d[0] > 0 ? props.tree[d[0] - 1] : null
  const specialities = (college && college.specialities) || []
  const speciality = d[1] > 0 ? specialities[d[1] - 1] : null
  const classes = (speciality && speciality.classes) || []
  const klass = d[2] > 0 ? classes[d[2] - 1] : null
  return { college, specialities, speciality, classes, klass }
}

const columns = computed(() => {
  const { specialities, classes } = levelsOf(draft.value)
  const names = (list) => [props.anyLabel, ...list.map((x) => x.name)]
  return [names(props.tree), names(specialities), names(classes)]
})

/** 由 id 数组反查每列的位置 */
const indexesOf = (ids) => {
  const [collegeId, specialityId, classId] = ids || []
  const ci = props.tree.findIndex((c) => c.id === collegeId)
  if (ci < 0) return [0, 0, 0]
  const specialities = props.tree[ci].specialities || []
  const si = specialities.findIndex((s) => s.id === specialityId)
  if (si < 0) return [ci + 1, 0, 0]
  const ki = (specialities[si].classes || []).findIndex((k) => k.id === classId)
  return [ci + 1, si + 1, ki < 0 ? 0 : ki + 1]
}

const resetDraft = () => {
  draft.value = indexesOf(props.modelValue)
}

watch(() => [props.modelValue, props.tree], resetDraft, { immediate: true, deep: true })

const label = computed(() => {
  const { college, speciality, klass } = levelsOf(indexesOf(props.modelValue))
  return [college, speciality, klass]
    .filter(Boolean)
    .map((x) => x.name)
    .join(' · ')
})

const onColumnChange = (e) => {
  const { column, value } = e.detail
  const next = [...draft.value]
  next[column] = value
  // 上一列换了，后面的列回到「不限」
  for (let i = column + 1; i < 3; i += 1) next[i] = 0
  draft.value = next
}

/** 确定：以 draft 为准（它跟着每次滚动更新，并且上一列换了会把后面的列归零；
 * 有的平台 change 里带回的下级列位置还是换列之前的，可能超出新列的长度） */
const onChange = () => {
  const { college, speciality, klass } = levelsOf(draft.value)
  const ids = [college, speciality, klass].filter(Boolean).map((x) => x.id)
  emit('update:modelValue', ids)
  emit('change', ids)
}

const clear = () => {
  emit('update:modelValue', [])
  emit('change', [])
}
</script>

<style lang="scss" scoped>
.clear {
  display: flex;
  color: var(--xm-text-3);
}
</style>

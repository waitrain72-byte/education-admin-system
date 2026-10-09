<template>
  <!-- 课程空间 · 成员（与 Web 端 views/course/CourseMembers.vue 一致）：
       老师看学情（学号、班级、出勤率、作业提交）；学生看同学名单 -->
  <view>
    <view
      v-if="!data && loading"
      class="xm-card"
    >
      <view class="skeleton skeleton-line" />
      <view class="skeleton skeleton-line short" />
    </view>
    <template v-else-if="data">
      <view
        v-if="data.teacher"
        class="xm-card teacher"
      >
        <xm-user-avatar
          :name="data.teacher.name"
          :avatar="data.teacher.avatar"
          :size="88"
        />
        <view class="teacher-info">
          <view class="teacher-label">{{ $t('space.members.teacher') }}</view>
          <view class="teacher-name">
            {{ data.teacher.name }}
            <text
              v-if="data.teacher.title"
              class="teacher-title"
              >{{ data.teacher.title }}</text
            >
          </view>
        </view>
      </view>

      <view class="xm-card">
        <view class="xm-section-head">
          <text class="xm-section-title">
            {{ $t('space.members.students') }}
            <text class="count xm-num">{{ $t('space.members.count', { n: students.length }) }}</text>
          </text>
        </view>
        <view class="search">
          <xm-search
            v-model="keyword"
            :placeholder="$t('space.members.search')"
          />
        </view>
        <view
          v-if="!students.length"
          class="muted"
          >{{ $t('space.members.empty') }}</view
        >
        <view
          v-else-if="!visible.length"
          class="muted"
          >{{ $t('space.members.noMatch') }}</view
        >
        <view
          v-for="s in visible"
          :key="s.id"
          class="member"
        >
          <xm-user-avatar
            :name="s.name"
            :avatar="s.avatar"
            :size="72"
          />
          <view class="member-main">
            <view class="member-name">{{ s.name }}</view>
            <view class="member-sub">
              <text
                v-if="teaching"
                class="xm-num"
                >{{ s.username }} ·
              </text>
              {{ s.className || '—' }}
            </view>
            <view
              v-if="teaching"
              class="member-stats"
            >
              <view class="member-rate">
                <text class="member-stat-label">{{ $t('space.members.attendanceRate') }}</text>
                <template v-if="s.attendanceRate != null">
                  <view class="rate-track">
                    <view
                      class="rate-fill"
                      :class="rateTone(s.attendanceRate)"
                      :style="'width:' + s.attendanceRate + '%'"
                    />
                  </view>
                  <text class="xm-num">{{ s.attendanceRate }}%</text>
                </template>
                <text
                  v-else
                  class="none"
                  >—</text
                >
              </view>
              <view class="member-submitted">
                <text class="member-stat-label">{{ $t('space.members.submitted') }}</text>
                <text class="xm-num"
                  >{{ s.submitted }}<text class="none"> / {{ data.assignmentCount }}</text></text
                >
              </view>
            </view>
          </view>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { courseApi } from '@/api'
import { SILENT } from '@/utils/request'
import { matchesPerson } from '@/utils/courseSpace'

const props = defineProps({
  overview: { type: Object, required: true },
  courseId: { type: Number, required: true },
})

const teaching = computed(() => ['teacher', 'admin'].includes(props.overview.relation))

const data = ref(null)
const loading = ref(false)
const keyword = ref('')

const students = computed(() => (data.value && data.value.students) || [])
const visible = computed(() => students.value.filter((s) => matchesPerson(s, keyword.value)))

const load = async () => {
  loading.value = true
  try {
    data.value = await courseApi.members(props.courseId, SILENT)
  } catch {
    // 提示已由请求层统一弹出
  } finally {
    loading.value = false
  }
}

watch(() => props.courseId, load, { immediate: true })

const rateTone = (rate) => (rate >= 90 ? 'is-ok' : rate >= 75 ? 'is-warn' : 'is-bad')

defineExpose({ reload: load })
</script>

<style lang="scss" scoped>
.teacher {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.teacher-label {
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.teacher-name {
  margin-top: 4rpx;
  font-size: 30rpx;
  font-weight: 600;
  color: var(--xm-text);
}

.teacher-title {
  margin-left: 12rpx;
  font-size: 24rpx;
  font-weight: normal;
  color: var(--xm-text-2);
}

.count {
  margin-left: 8rpx;
  font-size: 24rpx;
  font-weight: normal;
  color: var(--xm-text-2);
}

.search {
  margin-bottom: 12rpx;
}

.muted {
  padding: 16rpx 0;
  font-size: 26rpx;
  color: var(--xm-text-2);
}

.member {
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
  padding: 18rpx 0;
}

.member + .member {
  border-top: 1rpx solid var(--xm-border);
}

.member-main {
  flex: 1;
  min-width: 0;
}

.member-name {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--xm-text);
}

.member-sub {
  margin-top: 2rpx;
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.member-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx 32rpx;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: var(--xm-text);
}

.member-rate,
.member-submitted {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.member-stat-label {
  font-size: 22rpx;
  color: var(--xm-text-2);
}

.rate-track {
  width: 120rpx;
  height: 10rpx;
  border-radius: 999rpx;
  background: var(--xm-bg-sunken);
  overflow: hidden;
}

.rate-fill {
  height: 100%;
}

.rate-fill.is-ok {
  background: var(--xm-success);
}

.rate-fill.is-warn {
  background: var(--xm-warning);
}

.rate-fill.is-bad {
  background: var(--xm-danger);
}

.none {
  color: var(--xm-text-3);
}
</style>

<template>
  <view
    class="xm-page"
    :class="themeClass"
  >
    <xm-empty v-if="!tableData.length" />

    <view
      class="xm-card"
      v-else
    >
      <scroll-view
        scroll-x
        class="table-scroll"
      >
        <view class="table">
          <view class="tr tr-head">
            <view
              class="th"
              v-for="h in headers"
              :key="h"
              :class="{ 'th-today': h === todayLabel }"
              >{{ h }}</view
            >
          </view>
          <view
            class="tr"
            v-for="row in tableData"
            :key="row.segment"
          >
            <view class="td td-time">{{ row.segment }}</view>
            <view
              class="td"
              v-for="f in dayFields"
              :key="f"
              :class="{ 'td-today': f === todayField }"
            >
              <view
                v-if="row[f]"
                class="course-block"
                :style="blockStyle(row[f])"
                >{{ row[f] }}</view
              >
            </view>
          </view>
        </view>
      </scroll-view>
      <view class="xm-label table-tip">{{ $t('pages.curriculum.colorTip') }}</view>
    </view>
    <xm-loader />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { get } from '@/utils/request'
import { courseBlockStyle } from '@/utils/courseColor'
import { t, apiMessage } from '@/i18n'

const userStore = useUserStore()
const tableData = ref([])

const dayFields = ['monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday']

const headers = computed(() => [
  t('pages.curriculum.time'),
  t('pages.curriculum.monday'),
  t('pages.curriculum.tuesday'),
  t('pages.curriculum.wednesday'),
  t('pages.curriculum.thursday'),
  t('pages.curriculum.friday'),
  t('pages.curriculum.saturday'),
  t('pages.curriculum.sunday'),
])

// 今日高亮：getDay() 周一=1 … 周六=6、周日=0 → dayFields 下标 (getDay()+6)%7
const todayIndex = (new Date().getDay() + 6) % 7
const todayField = dayFields[todayIndex]
const todayLabel = computed(() => headers.value[todayIndex + 1])

// 课程块配色与首页「今日课程」共用同一套工具（utils/courseColor.ts），保证颜色语言一致
const blockStyle = courseBlockStyle

const load = () => {
  get('/choice/getCurriculum').then((res) => {
    if (res.data && res.data.code === '200') {
      tableData.value = res.data.data || []
    } else {
      uni.showToast({ title: apiMessage(res.data), icon: 'none' })
    }
  })
}

onShow(() => {
  uni.setNavigationBarTitle({ title: t('menu.curriculum') })
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  // 仅学生可访问（与 Web 端路由 meta.roles 一致）
  if (!['STUDENT'].includes(userStore.role)) {
    uni.showToast({ title: t('forbidden.message'), icon: 'none' })
    setTimeout(() => uni.navigateBack(), 800)
    return
  }
  load()
})
</script>

<style lang="scss" scoped>
.table-scroll {
  width: 100%;
}

.table {
  width: 1680rpx;
}

.tr {
  display: flex;
}

.tr-head {
  background: var(--xm-bg-input);
}

.th,
.td {
  width: 210rpx;
  padding: 16rpx 12rpx;
  font-size: 26rpx;
  color: var(--xm-text);
  border: 1rpx solid var(--xm-border);
  box-sizing: border-box;
  word-break: break-all;
}

.th {
  font-weight: bold;
  text-align: center;
}

/* 今日列：表头品牌色 + 列体品牌软底 */
.th-today {
  color: var(--xm-brand);
}

.td-today {
  background: var(--xm-brand-soft);
}

.td-time {
  font-weight: bold;
}

/* 课程块：软色底 + 同色描边，颜色由 blockStyle 按课程名注入 */
.course-block {
  border-radius: 10rpx;
  padding: 10rpx 8rpx;
  font-size: 22rpx;
  line-height: 1.4;
  min-height: 72rpx;
  box-sizing: border-box;
}

.table-tip {
  margin-top: 12rpx;
  text-align: center;
}
</style>

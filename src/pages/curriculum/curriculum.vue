<template>
  <view
    class="xm-page"
    :class="themeClass"
  >
    <!-- 8 列课表：时间 + 星期一~星期日，横向滚动查看 -->
    <view
      v-if="!tableData.length"
      class="xm-empty"
      >{{ $t('common.empty') }}</view
    >

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
              >{{ row[f] }}</view
            >
          </view>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { get } from '@/utils/request'
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

.td-time {
  font-weight: bold;
}
</style>

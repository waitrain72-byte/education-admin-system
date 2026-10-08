<template>
  <!-- 教务后台入口（管理员的第三个 tab，对应 Web 端 layout/AdminLayout.vue 的侧栏）：按「教学 / 档案 / 系统」分组 -->
  <view>
    <view
      v-for="section in sections"
      :key="section.key"
      class="xm-card"
    >
      <view class="xm-section-head">
        <text class="xm-section-title">{{ $t('admin.sections.' + section.key) }}</text>
      </view>
      <view class="xm-grid">
        <view
          v-for="item in section.items"
          :key="item.page"
          class="xm-grid-item"
          @click="open(item.page)"
        >
          <view class="xm-grid-item-icon">
            <xm-icon
              :name="item.icon"
              :size="44"
            />
          </view>
          <view class="xm-grid-item-label">{{ $t(item.label) }}</view>
        </view>
      </view>
    </view>
    <view class="note">{{ $t('mobile.consoleNote') }}</view>
  </view>
</template>

<script setup>
/**
 * Web 端教务后台里沿用旧版界面的「教学记录」（选课 / 成绩 / 考勤 / 作业 / 评价记录）和权限、日志页面
 * 表格列多、主要在电脑上用，小程序不提供，页脚提示到 Web 端处理。
 */
const sections = [
  {
    key: 'teaching',
    items: [
      { page: 'courses', label: 'admin.menu.courses', icon: 'book-open' },
      { page: 'rooms', label: 'admin.menu.rooms', icon: 'building' },
      { page: 'leaves', label: 'admin.menu.leaves', icon: 'clipboard' },
      { page: 'exams', label: 'menu.examplan', icon: 'calendar' },
      { page: 'warnings', label: 'menu.warning', icon: 'alert-triangle' },
    ],
  },
  {
    key: 'archives',
    items: [
      { page: 'org', label: 'admin.menu.org', icon: 'org' },
      { page: 'people', label: 'admin.menu.people', icon: 'users' },
    ],
  },
  {
    key: 'system',
    items: [{ page: 'semester', label: 'admin.menu.semester', icon: 'sliders' }],
  },
]

const open = (page) => uni.navigateTo({ url: `/pages-admin/${page}/${page}` })
</script>

<style lang="scss" scoped>
.note {
  padding: 8rpx 8rpx 24rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: var(--xm-text-3);
  text-align: center;
}
</style>

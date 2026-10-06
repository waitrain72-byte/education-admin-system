<template>
  <div class="admin-area">
    <aside class="admin-side" :aria-label="$t('nav.admin')">
      <div class="admin-side__title">{{ $t('nav.admin') }}</div>
      <template v-for="section in sections" :key="section.key">
        <div class="admin-side__section">{{ $t('admin.sections.' + section.key) }}</div>
        <router-link
          v-for="link in section.links"
          :key="link.path"
          :to="link.path"
          class="admin-side__link"
          active-class="is-active"
        >
          {{ $t(link.label) }}
        </router-link>
      </template>
      <router-link to="/dashboard" class="admin-side__link admin-side__link--screen">
        {{ $t('pages.dashboard.entry') }} ↗
      </router-link>
    </aside>

    <section class="admin-content">
      <!-- 旧页面没有自己的标题，这里补一个；新页面自带页头 -->
      <h1 v-if="route.meta.legacy" class="page-title">{{ $t(String(route.meta.name)) }}</h1>
      <div :class="{ 'legacy-page': route.meta.legacy }">
        <router-view />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { childLinks } from '@/layout/nav'
import { useUser } from '@/components/useUser'

const route = useRoute()
const router = useRouter()
const { user } = useUser()

const SECTION_ORDER = ['teaching', 'records', 'archives', 'system']

/** 侧栏分组：路由表里 /admin 的子路由，按 meta.section 归组 */
const sections = computed(() => {
  const layout = router.options.routes.find((r) => r.path === '/')
  const admin = layout?.children?.find((r) => r.path === 'admin')
  const links = childLinks(admin?.children || [], '/admin', user.value.role)
  return SECTION_ORDER.map((key) => ({ key, links: links.filter((l) => l.section === key) })).filter((s) => s.links.length)
})
</script>

<style scoped>
.admin-area {
  display: grid;
  grid-template-columns: 208px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.admin-side {
  position: sticky;
  top: calc(var(--xm-nav-height) + 24px);
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-height: calc(100vh - var(--xm-nav-height) - 48px);
  overflow-y: auto;
  padding: 16px 10px;
  border: 1px solid var(--xm-border);
  border-radius: var(--xm-radius-lg);
  background: var(--xm-bg-card);
}

.admin-side__title {
  padding: 0 10px 8px;
  font-size: 15px;
  font-weight: 700;
  color: var(--xm-text-primary);
}

.admin-side__section {
  margin-top: 10px;
  padding: 6px 10px 4px;
  font-size: 12px;
  color: var(--xm-text-secondary);
  letter-spacing: 0.06em;
}

.admin-side__link {
  padding: 7px 10px;
  border-radius: 8px;
  color: var(--xm-text-regular);
  font-size: 14px;
}

.admin-side__link:hover {
  background: var(--xm-bg-hover);
  color: var(--xm-text-primary);
}

.admin-side__link.is-active {
  background: var(--xm-brand-soft);
  color: var(--xm-brand);
  font-weight: 600;
}

.admin-side__link--screen {
  margin-top: 12px;
  border: 1px dashed var(--xm-border-dashed);
  text-align: center;
}

.admin-content {
  min-width: 0;
}

@media (max-width: 960px) {
  .admin-area {
    grid-template-columns: minmax(0, 1fr);
  }

  /* 窄屏：侧栏变成可横向滑动的一行 */
  .admin-side {
    position: static;
    flex-direction: row;
    flex-wrap: nowrap;
    max-height: none;
    overflow-x: auto;
    padding: 8px;
  }

  .admin-side__title,
  .admin-side__section {
    display: none;
  }

  .admin-side__link {
    flex-shrink: 0;
    white-space: nowrap;
  }

  .admin-side__link--screen {
    margin-top: 0;
  }
}
</style>

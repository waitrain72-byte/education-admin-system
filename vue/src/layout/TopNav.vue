<template>
  <header class="top-nav">
    <div class="top-nav__inner">
      <button
        type="button"
        class="icon-btn top-nav__burger"
        :aria-expanded="menuOpen"
        :aria-label="$t('nav.menu')"
        @click="menuOpen = !menuOpen"
      >
        <el-icon><Menu /></el-icon>
      </button>

      <router-link to="/home" class="brand">
        <BrandMark />
        <span class="brand__name">{{ $t('layout.title') }}</span>
      </router-link>

      <nav class="top-nav__links" :class="{ 'is-open': menuOpen }" :aria-label="$t('nav.menu')">
        <router-link
          v-for="item in items"
          :key="item.key"
          :to="item.to"
          class="nav-link"
          :class="{ 'is-active': isNavActive(item, route.path) }"
        >
          {{ $t(item.label) }}
          <span v-if="item.key === 'messages' && messageStore.unread" class="nav-link__count">
            {{ messageStore.unread > 99 ? '99+' : messageStore.unread }}
          </span>
        </router-link>
      </nav>

      <div class="top-nav__tools">
        <button type="button" class="search-trigger" :aria-label="$t('shell.searchShort')" @click="$emit('open-search')">
          <el-icon><Search /></el-icon>
          <span class="search-trigger__text">{{ $t('shell.searchShort') }}</span>
          <kbd class="search-trigger__kbd">Ctrl K</kbd>
        </button>

        <router-link to="/messages" class="icon-btn" :aria-label="$t('nav.messages')">
          <el-icon><Bell /></el-icon>
          <span v-if="messageStore.unread" class="icon-btn__dot"></span>
        </router-link>

        <button
          type="button"
          class="icon-btn"
          :aria-label="isDark ? $t('shell.themeToLight') : $t('shell.themeToDark')"
          :title="isDark ? $t('shell.themeToLight') : $t('shell.themeToDark')"
          @click="setThemeMode(isDark ? 'light' : 'dark')"
        >
          <el-icon><Sunny v-if="isDark" /><Moon v-else /></el-icon>
        </button>

        <UserMenu />
      </div>
    </div>
    <div v-if="menuOpen" class="top-nav__mask" @click="menuOpen = false"></div>
  </header>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Bell, Menu, Moon, Search, Sunny } from '@element-plus/icons-vue'
import BrandMark from '@/layout/BrandMark.vue'
import UserMenu from '@/layout/UserMenu.vue'
import { isNavActive, mainNav } from '@/layout/nav'
import { useUser } from '@/components/useUser'
import { useMessageStore } from '@/stores/messages'
import { isDark, setThemeMode } from '@/composables/useTheme'

defineEmits<{ (e: 'open-search'): void }>()

const route = useRoute()
const { user } = useUser()
const messageStore = useMessageStore()

const menuOpen = ref(false)
watch(
  () => route.fullPath,
  () => {
    menuOpen.value = false
  },
)

const items = computed(() => mainNav(user.value.role))
</script>

<style scoped>
.top-nav {
  position: sticky;
  top: 0;
  z-index: 50;
  height: var(--xm-nav-height);
  background: var(--xm-nav-bg);
  backdrop-filter: saturate(1.4) blur(10px);
  -webkit-backdrop-filter: saturate(1.4) blur(10px);
  border-bottom: 1px solid var(--xm-nav-border);
}

.top-nav__inner {
  display: flex;
  align-items: center;
  gap: 20px;
  max-width: var(--xm-content-width);
  height: 100%;
  margin: 0 auto;
  padding: 0 24px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--xm-text-primary);
  flex-shrink: 0;
}

.brand__name {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.top-nav__links {
  display: flex;
  align-items: stretch;
  gap: 4px;
  height: 100%;
  min-width: 0;
}

.nav-link {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
  border: none;
  background: transparent;
  color: var(--xm-text-secondary);
  font: inherit;
  font-size: 15px;
  white-space: nowrap;
  cursor: pointer;
}

.nav-link:hover {
  color: var(--xm-text-primary);
}

/* 当前栏目：文字加深 + 底部一条品牌色短线 */
.nav-link.is-active {
  color: var(--xm-text-primary);
  font-weight: 600;
}

.nav-link.is-active::after {
  content: '';
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 0;
  height: 3px;
  border-radius: 3px 3px 0 0;
  background: var(--xm-brand);
}

.nav-link__count {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--xm-bad);
  color: var(--xm-on-brand);
  font-size: 11px;
  font-weight: 700;
  line-height: 18px;
  text-align: center;
}

.top-nav__tools {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
}

.search-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  width: 220px;
  height: 36px;
  padding: 0 8px 0 12px;
  border: 1px solid var(--xm-border);
  border-radius: 10px;
  background: var(--xm-bg-sunken);
  color: var(--xm-text-secondary);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
  transition: border-color 0.2s ease;
}

.search-trigger:hover,
.search-trigger:focus-visible {
  border-color: var(--xm-brand);
}

.search-trigger__kbd {
  margin-left: auto;
  padding: 1px 6px;
  border: 1px solid var(--xm-border);
  border-bottom-width: 2px;
  border-radius: 5px;
  background: var(--xm-bg-card);
  font-family: var(--xm-font-num);
  font-size: 11px;
}

.icon-btn {
  position: relative;
  display: inline-grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: var(--xm-text-regular);
  font-size: 18px;
  cursor: pointer;
}

.icon-btn:hover,
.icon-btn:focus-visible {
  background: var(--xm-bg-hover);
  color: var(--xm-text-primary);
}

.icon-btn__dot {
  position: absolute;
  top: 8px;
  right: 9px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--xm-bad);
  box-shadow: 0 0 0 2px var(--xm-bg-card);
}

.top-nav__burger {
  display: none;
}

.top-nav__mask {
  display: none;
}

@media (max-width: 1080px) {
  .search-trigger {
    width: auto;
  }

  .search-trigger__text,
  .search-trigger__kbd {
    display: none;
  }
}

/* 窄屏：导航链接收进汉堡菜单，从顶栏下方展开 */
@media (max-width: 860px) {
  .top-nav__inner {
    gap: 8px;
    padding: 0 12px;
  }

  .top-nav__burger {
    display: inline-grid;
  }

  .top-nav__links {
    position: fixed;
    top: var(--xm-nav-height);
    left: 0;
    right: 0;
    z-index: 2;
    display: none;
    flex-direction: column;
    height: auto;
    padding: 8px 12px 12px;
    background: var(--xm-bg-card);
    border-bottom: 1px solid var(--xm-border);
    box-shadow: var(--xm-shadow-pop);
  }

  .top-nav__links.is-open {
    display: flex;
  }

  .nav-link {
    height: 44px;
    border-radius: 8px;
  }

  .nav-link.is-active {
    background: var(--xm-brand-soft);
    color: var(--xm-brand);
  }

  .nav-link.is-active::after {
    display: none;
  }

  .top-nav__mask {
    display: block;
    position: fixed;
    inset: var(--xm-nav-height) 0 0 0;
    z-index: 1;
    background: rgba(0, 0, 0, 0.3);
  }
}

@media (max-width: 480px) {
  .brand__name {
    display: none;
  }
}
</style>

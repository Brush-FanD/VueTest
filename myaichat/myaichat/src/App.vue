<template>
  <div id="app">
    <main class="app-content">
      <router-view v-slot="{ Component, route }">
        <transition name="fade-slide" mode="out-in">
          <component :is="Component" :key="route.path" />
        </transition>
      </router-view>
    </main>

    <nav class="tabbar">
      <button
        v-for="tab in tabList"
        :key="tab.path"
        class="tab"
        :class="{ active: activeTab === tab.path }"
        type="button"
        @click="goPage(tab.path)"
      >
        <svg class="tab-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path v-for="(d, i) in tab.paths" :key="i" :d="d" />
        </svg>
        <span class="tab-text">{{ tab.label }}</span>
      </button>
    </nav>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

/** 底部导航：图标用 path 数据内联，避免引入整套图标库 */
const tabList = [
  {
    path: '/home',
    label: '首页',
    paths: ['M3 10.2 12 3l9 7.2V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1v-9.8Z']
  },
  {
    path: '/cars',
    label: '选车',
    paths: ['M3 4h7v7H3z', 'M14 4h7v7h-7z', 'M3 14h7v7H3z', 'M14 14h7v7h-7z']
  },
  {
    path: '/videos',
    label: '视频',
    paths: ['M3 5h18v14H3z', 'M10 9.5l5 2.5-5 2.5v-5z']
  },
  {
    path: '/ai',
    label: '懂车AI',
    paths: ['M21 12a8 8 0 0 1-8 8H7l-4 3 1.2-4.2A8 8 0 1 1 21 12Z']
  }
]

/** 播放详情页归属「视频」这个 Tab */
const activeTab = computed(() => {
  if (route.path.startsWith('/video/')) return '/videos'
  if (route.path.startsWith('/car/')) return '/cars'
  return route.path
})

const goPage = (path) => {
  if (route.path !== path) router.push(path)
}
</script>

<style scoped>
.app-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
}

.tabbar {
  flex-shrink: 0;
  display: flex;
  height: calc(var(--nav-h) + env(safe-area-inset-bottom, 0px));
  padding-bottom: env(safe-area-inset-bottom, 0px);
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: saturate(180%) blur(12px);
  border-top: 1px solid var(--c-line);
}

.tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  border: none;
  background: none;
  padding: 0;
  cursor: pointer;
  color: var(--c-text-3);
  font-family: inherit;
  transition: color .18s ease;
  -webkit-tap-highlight-color: transparent;
}

.tab-icon {
  width: 22px;
  height: 22px;
  transition: transform .18s ease;
}

.tab-text {
  font-size: 10px;
  line-height: 1;
}

.tab.active {
  color: var(--c-brand);
}

.tab.active .tab-text {
  font-weight: 600;
}

.tab.active .tab-icon {
  transform: translateY(-1px) scale(1.06);
}

.tab:active .tab-icon {
  transform: scale(.92);
}
</style>
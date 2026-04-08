<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { Button } from '@/components/ui'
import { BarChart3, Globe2, LogOut, Menu, X } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const mobileOpen = ref(false)

const navItems = [
  { path: '/dashboard', label: '概览', icon: BarChart3 },
  { path: '/websites', label: '站点', icon: Globe2 }
]

const currentLabel = computed(() => navItems.find((item) => route.path.startsWith(item.path))?.label || '工作台')

function navigate(path: string) {
  router.push(path)
  mobileOpen.value = false
}

function logout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="apple-shell">
    <div class="apple-grid">
      <aside
        :class="[
          'fixed inset-y-4 left-4 z-40 w-[min(20rem,88vw)] transition-transform duration-300 lg:static lg:inset-auto lg:w-auto lg:translate-x-0',
          mobileOpen ? 'translate-x-0' : '-translate-x-[120%]'
        ]"
      >
        <div class="apple-sidebar flex h-[calc(100vh-2rem)] flex-col lg:sticky lg:top-6 lg:h-[calc(100vh-3rem)]">
          <div class="flex items-center gap-3">
            <div class="flex h-12 w-12 items-center justify-center overflow-hidden rounded-[18px] bg-white shadow-[0_18px_40px_-24px_rgba(15,23,42,0.18)] ring-1 ring-slate-200/80">
              <img src="/logo.png" alt="SEB Logo" class="h-full w-full object-contain p-1.5" />
            </div>
            <div>
              <p class="text-lg font-semibold tracking-[-0.03em] text-slate-950">SEB</p>
              <p class="text-xs text-slate-500">Analytics Console</p>
            </div>
          </div>

          <nav class="mt-8 space-y-2">
            <button
              v-for="item in navItems"
              :key="item.path"
              :class="[
                'apple-nav-btn',
                route.path.startsWith(item.path)
                  ? 'apple-nav-btn-active'
                  : 'hover:bg-white/85 hover:text-slate-950'
              ]"
              @click="navigate(item.path)"
            >
              <component :is="item.icon" class="h-5 w-5" />
              <span>{{ item.label }}</span>
            </button>
          </nav>

          <div class="mt-6 rounded-[24px] bg-slate-50/90 px-4 py-3 text-xs leading-6 text-slate-500">
            <p>概览看全局，站点管配置，统计看细节。</p>
          </div>

          <div class="mt-auto pt-6">
            <Button variant="ghost" class="w-full justify-start rounded-2xl text-slate-600 hover:bg-white/85 hover:text-slate-950" @click="logout">
              <LogOut class="mr-2 h-4 w-4" />
              退出登录
            </Button>
          </div>
        </div>
      </aside>

      <div class="apple-main">
        <header class="apple-header lg:hidden">
          <div class="flex min-w-0 items-center gap-3">
            <Button variant="ghost" size="icon" class="rounded-2xl bg-slate-50 lg:hidden" @click="mobileOpen = !mobileOpen">
              <Menu v-if="!mobileOpen" class="h-5 w-5" />
              <X v-else class="h-5 w-5" />
            </Button>
            <div class="flex min-w-0 items-center gap-3">
              <div class="flex h-10 w-10 items-center justify-center overflow-hidden rounded-[14px] bg-white shadow-[0_12px_28px_-20px_rgba(15,23,42,0.18)] ring-1 ring-slate-200/80 lg:hidden">
                <img src="/logo.png" alt="SEB Logo" class="h-full w-full object-contain p-1.5" />
              </div>
              <div class="min-w-0">
                <p class="truncate text-[11px] uppercase tracking-[0.24em] text-slate-400">Workspace</p>
                <h1 class="truncate text-lg font-semibold tracking-[-0.03em] text-slate-950">{{ currentLabel }}</h1>
              </div>
            </div>
          </div>

          <div class="hidden items-center gap-2 rounded-full bg-slate-950 px-3 py-1.5 text-xs font-medium text-white sm:inline-flex">
            <span class="h-2 w-2 rounded-full bg-emerald-400"></span>
            Active
          </div>
        </header>

        <main class="apple-page px-1 pt-1 lg:pt-0">
          <router-view />
        </main>
      </div>
    </div>

    <div v-if="mobileOpen" class="fixed inset-0 z-30 bg-slate-950/20 backdrop-blur-sm lg:hidden" @click="mobileOpen = false"></div>
  </div>
</template>

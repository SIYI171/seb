<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { Button } from '@/components/ui'
import {
  BarChart3,
  Monitor,
  LogOut,
  Menu,
  X,
  ChevronRight
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const collapsed = ref(false)
const mobileOpen = ref(false)

const navItems = [
  { path: '/dashboard', label: '概览', icon: BarChart3 },
  { path: '/websites', label: '网站管理', icon: Monitor }
]

const currentPath = computed(() => route.path)

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
  <div class="min-h-screen bg-slate-50 dark:bg-slate-900">
    <header class="lg:hidden fixed top-0 left-0 right-0 h-16 bg-white dark:bg-slate-800 border-b z-40 flex items-center justify-between px-4">
      <div class="flex items-center gap-2">
        <img src="/logo.png" alt="SEB" class="w-8 h-8 rounded-lg" />
        <span class="font-semibold">SEB</span>
      </div>
      <Button variant="ghost" size="icon" @click="mobileOpen = !mobileOpen">
        <Menu v-if="!mobileOpen" class="w-5 h-5" />
        <X v-else class="w-5 h-5" />
      </Button>
    </header>

    <aside
      :class="[
        'fixed top-0 left-0 h-full bg-white dark:bg-slate-800 border-r z-50 transition-all duration-300',
        'lg:translate-x-0',
        mobileOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0',
        collapsed ? 'lg:w-16' : 'lg:w-64',
        'w-64'
      ]"
    >
      <div class="h-16 flex items-center justify-between px-4 border-b">
        <div v-if="!collapsed" class="flex items-center gap-2">
          <img src="/logo.png" alt="SEB" class="w-8 h-8 rounded-lg" />
          <span class="font-semibold">SEB Analytics</span>
        </div>
        <img v-else src="/logo.png" alt="SEB" class="w-8 h-8 rounded-lg mx-auto" />
        <Button
          variant="ghost"
          size="icon"
          class="hidden lg:flex"
          @click="collapsed = !collapsed"
        >
          <ChevronRight :class="['w-4 h-4 transition-transform', collapsed && 'rotate-180']" />
        </Button>
      </div>

      <nav class="p-2 space-y-1">
        <button
          v-for="item in navItems"
          :key="item.path"
          :class="[
            'w-full flex items-center gap-3 px-3 py-2.5 rounded-lg transition-colors text-left',
            currentPath === item.path
              ? 'bg-primary text-white'
              : 'hover:bg-slate-100 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-300'
          ]"
          @click="navigate(item.path)"
        >
          <component :is="item.icon" class="w-5 h-5 flex-shrink-0" />
          <span v-if="!collapsed" class="text-sm font-medium">{{ item.label }}</span>
        </button>
      </nav>

      <div class="absolute bottom-0 left-0 right-0 p-4 border-t">
        <Button
          variant="ghost"
          :class="['w-full justify-start gap-3 text-slate-500', collapsed && 'justify-center']"
          @click="logout"
        >
          <LogOut class="w-5 h-5" />
          <span v-if="!collapsed">退出登录</span>
        </Button>
      </div>
    </aside>

    <div
      v-if="mobileOpen"
      class="fixed inset-0 bg-black/50 z-40 lg:hidden"
      @click="mobileOpen = false"
    ></div>

    <main
      :class="[
        'pt-16 lg:pt-0 transition-all duration-300 min-h-screen',
        collapsed ? 'lg:ml-16' : 'lg:ml-64'
      ]"
    >
      <div class="p-4 lg:p-8">
        <router-view />
      </div>
    </main>
  </div>
</template>

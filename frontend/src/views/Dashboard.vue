<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Button, Badge, Skeleton } from '@/components/ui'
import { Activity, ArrowUpRight, Compass, Eye, Globe2, Sparkles, Users } from 'lucide-vue-next'
import api from '../api'

interface Website {
  id: number
  name: string
  domain: string
}

interface WebsiteStats {
  id: number
  name: string
  domain: string
  pageviews: number
  visitors: number
  realtime: number
}

const router = useRouter()
const websites = ref<Website[]>([])
const websiteStats = ref<WebsiteStats[]>([])
const loading = ref(true)
const totalPageviews = ref(0)
const totalVisitors = ref(0)
const totalRealtime = ref(0)
let refreshTimer: number | null = null

const spotlight = computed(() => {
  return [...websiteStats.value].sort((a, b) => {
    if (b.realtime !== a.realtime) return b.realtime - a.realtime
    if (b.pageviews !== a.pageviews) return b.pageviews - a.pageviews
    return b.visitors - a.visitors
  })[0] || null
})

const activeSites = computed(() => websiteStats.value.filter((site) => site.realtime > 0))
const topSites = computed(() => [...websiteStats.value].sort((a, b) => b.pageviews - a.pageviews).slice(0, 5))

async function loadData() {
  try {
    const res = await api.get('/websites')
    if (res.code === 200) {
      websites.value = res.data
      await loadAllStats()
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function loadAllStats() {
  const statsPromises = websites.value.map(async (site) => {
    try {
      const [statsRes, realtimeRes] = await Promise.all([
        api.get(`/stats/${site.id}`),
        api.get(`/stats/realtime/${site.id}`)
      ])
      return {
        id: site.id,
        name: site.name,
        domain: site.domain,
        pageviews: statsRes.code === 200 ? statsRes.data.pageviews : 0,
        visitors: statsRes.code === 200 ? statsRes.data.visitors : 0,
        realtime: realtimeRes.code === 200 ? realtimeRes.data : 0
      }
    } catch {
      return { id: site.id, name: site.name, domain: site.domain, pageviews: 0, visitors: 0, realtime: 0 }
    }
  })

  websiteStats.value = await Promise.all(statsPromises)
  totalPageviews.value = websiteStats.value.reduce((sum, site) => sum + site.pageviews, 0)
  totalVisitors.value = websiteStats.value.reduce((sum, site) => sum + site.visitors, 0)
  totalRealtime.value = websiteStats.value.reduce((sum, site) => sum + site.realtime, 0)
}

function formatNumber(num: number) {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return num.toString()
}

function goToStats(id: number) {
  router.push(`/stats/${id}`)
}

function goToWebsites() {
  router.push('/websites')
}

onMounted(() => {
  loadData()
  refreshTimer = window.setInterval(loadAllStats, 10000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<template>
  <div class="space-y-6">
    <section class="apple-hero">
      <div class="grid gap-8 xl:grid-cols-[1.12fr_0.88fr] xl:items-start">
        <div class="min-w-0">
          <span class="apple-label">
            <Sparkles class="h-3.5 w-3.5" />
            Overview
          </span>
          <h2 class="mt-6 max-w-3xl apple-title sm:mt-8">先判断现在是否值得继续追，再决定进入哪个站点。</h2>
        </div>

        <div class="apple-card">
          <div class="flex items-start justify-between gap-4">
            <div>
              <p class="text-sm text-slate-500">当前焦点</p>
              <h3 class="mt-2 text-[1.45rem] font-semibold leading-tight tracking-[-0.04em] text-slate-950 sm:text-[1.75rem]">最活跃站点</h3>
            </div>
            <Compass class="h-5 w-5 text-slate-400" />
          </div>

          <div v-if="loading" class="mt-8 space-y-3">
            <Skeleton class="h-8 w-40" />
            <Skeleton class="h-5 w-52" />
            <Skeleton class="h-28 w-full" />
          </div>

          <div v-else-if="spotlight" class="mt-8 space-y-5">
            <div>
              <div class="flex flex-wrap items-center gap-2">
                <h4 class="break-words text-[1.4rem] font-semibold leading-tight tracking-[-0.04em] text-slate-950 sm:text-2xl">{{ spotlight.name }}</h4>
                <Badge v-if="spotlight.realtime > 0" variant="success">{{ spotlight.realtime }} 在线</Badge>
              </div>
              <p class="mt-2 break-all text-sm text-slate-500">{{ spotlight.domain }}</p>
            </div>

            <div class="grid gap-3 grid-cols-1 sm:grid-cols-3">
              <div class="apple-soft">
                <p class="text-xs text-slate-500">浏览量</p>
                <p class="mt-2 text-xl font-semibold tracking-[-0.03em] text-slate-950">{{ formatNumber(spotlight.pageviews) }}</p>
              </div>
              <div class="apple-soft">
                <p class="text-xs text-slate-500">访客</p>
                <p class="mt-2 text-xl font-semibold tracking-[-0.03em] text-slate-950">{{ formatNumber(spotlight.visitors) }}</p>
              </div>
              <div class="apple-soft">
                <p class="text-xs text-slate-500">状态</p>
                <p class="mt-2 text-xl font-semibold tracking-[-0.03em] text-slate-950">{{ spotlight.realtime > 0 ? '活跃' : '平稳' }}</p>
              </div>
            </div>

            <div class="grid gap-2 sm:flex sm:flex-wrap">
              <Button class="w-full rounded-2xl bg-slate-950 text-white hover:bg-slate-800 sm:w-auto" @click="goToStats(spotlight.id)">进入统计</Button>
              <Button variant="outline" class="w-full rounded-2xl border-slate-200 bg-white/80 sm:w-auto" @click="goToWebsites()">管理站点</Button>
            </div>
          </div>

          <div v-else class="mt-8 space-y-4">
            <p class="apple-copy">还没有站点数据，先添加网站并安装追踪脚本。</p>
            <Button class="rounded-2xl bg-slate-950 text-white hover:bg-slate-800" @click="goToWebsites()">添加网站</Button>
          </div>
        </div>
      </div>
    </section>

    <section class="grid gap-4 md:grid-cols-4">
      <div class="apple-card">
        <div class="flex items-center justify-between text-slate-500">
          <span class="text-sm">总浏览量</span>
          <Eye class="h-4 w-4" />
        </div>
        <p v-if="loading" class="mt-4"><Skeleton class="h-10 w-24" /></p>
        <p v-else class="apple-stat-value">{{ formatNumber(totalPageviews) }}</p>
      </div>
      <div class="apple-card">
        <div class="flex items-center justify-between text-slate-500">
          <span class="text-sm">总访客数</span>
          <Users class="h-4 w-4" />
        </div>
        <p v-if="loading" class="mt-4"><Skeleton class="h-10 w-24" /></p>
        <p v-else class="apple-stat-value">{{ formatNumber(totalVisitors) }}</p>
      </div>
      <div class="apple-card">
        <div class="flex items-center justify-between text-slate-500">
          <span class="text-sm">实时在线</span>
          <Activity class="h-4 w-4" />
        </div>
        <p v-if="loading" class="mt-4"><Skeleton class="h-10 w-24" /></p>
        <p v-else class="apple-stat-value">{{ totalRealtime }}</p>
      </div>
      <div class="apple-card">
        <div class="flex items-center justify-between text-slate-500">
          <span class="text-sm">活跃站点</span>
          <Globe2 class="h-4 w-4" />
        </div>
        <p v-if="loading" class="mt-4"><Skeleton class="h-10 w-24" /></p>
        <p v-else class="apple-stat-value">{{ activeSites.length }}</p>
      </div>
    </section>

    <section class="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
      <div class="apple-surface p-6">
        <div class="flex items-start justify-between gap-4">
          <div>
            <p class="text-[11px] uppercase tracking-[0.22em] text-slate-400">Priority List</p>
            <h3 class="mt-2 text-[1.5rem] font-semibold tracking-[-0.04em] text-slate-950 sm:text-[1.8rem]">优先查看这些站点</h3>
          </div>
          <Button variant="ghost" class="hidden rounded-2xl sm:inline-flex" @click="goToWebsites()">查看全部</Button>
        </div>

        <div v-if="loading" class="mt-6 space-y-3">
          <div v-for="i in 4" :key="i" class="rounded-[24px] border border-slate-200 p-4">
            <Skeleton class="h-6 w-40" />
            <Skeleton class="mt-2 h-4 w-56" />
          </div>
        </div>

        <div v-else-if="topSites.length" class="mt-6 space-y-3">
          <button
            v-for="site in topSites"
            :key="site.id"
            class="flex w-full items-start justify-between gap-4 rounded-[24px] border border-slate-200 bg-white/70 px-4 py-4 text-left transition hover:border-slate-300 hover:bg-white sm:px-5"
            @click="goToStats(site.id)"
          >
            <div class="min-w-0">
              <div class="flex flex-wrap items-center gap-2">
                <h4 class="break-words text-base font-semibold tracking-[-0.03em] text-slate-950 sm:text-lg">{{ site.name }}</h4>
                <Badge v-if="site.realtime > 0" variant="success">{{ site.realtime }} 在线</Badge>
              </div>
              <p class="mt-1 break-all text-sm text-slate-500 sm:truncate">{{ site.domain }}</p>
              <div class="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-sm text-slate-600">
                <span>PV {{ formatNumber(site.pageviews) }}</span>
                <span>UV {{ formatNumber(site.visitors) }}</span>
              </div>
            </div>
            <ArrowUpRight class="mt-1 h-4 w-4 flex-shrink-0 text-slate-400" />
          </button>
        </div>

        <div v-else class="mt-6 rounded-[24px] border border-dashed border-slate-200 p-10 text-center text-sm text-slate-500">
          暂时还没有可展示的数据。
        </div>
      </div>

      <div class="space-y-6">
        <div class="apple-surface p-6">
          <p class="text-[11px] uppercase tracking-[0.22em] text-slate-400">How To Read</p>
          <h3 class="mt-2 text-[1.5rem] font-semibold tracking-[-0.04em] text-slate-950 sm:text-[1.8rem]">这个首页该怎么用</h3>
          <div class="mt-6 space-y-4">
            <div class="apple-soft">
              <p class="text-sm font-medium text-slate-950">先看现在热不热</p>
              <p class="mt-2 text-sm leading-6 text-slate-600">先看实时在线。</p>
            </div>
            <div class="apple-soft">
              <p class="text-sm font-medium text-slate-950">再看哪个站点最值得点</p>
              <p class="mt-2 text-sm leading-6 text-slate-600">再点最活跃的站点。</p>
            </div>
          </div>
        </div>

        <div class="apple-card">
          <p class="text-sm text-slate-500">当前判断</p>
          <p class="mt-4 text-[1.5rem] font-semibold leading-tight tracking-[-0.04em] text-slate-950 sm:text-[1.8rem]">{{ totalRealtime > 0 ? '现在适合看实时变化' : '现在适合回看历史趋势' }}</p>
          <p class="mt-4 apple-copy">
            {{ totalRealtime > 0 ? '建议优先进入在线站点，观察最近访问和会话变化。' : '建议回到趋势和页面表现，做更冷静的分析。' }}
          </p>
        </div>
      </div>
    </section>
  </div>
</template>

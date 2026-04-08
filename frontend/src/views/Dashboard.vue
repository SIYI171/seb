<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Card, CardContent, CardHeader, CardTitle, Skeleton, Badge } from '@/components/ui'
import { Eye, Users, Globe, TrendingUp, ArrowUpRight } from 'lucide-vue-next'
import api from '../api'

interface Website {
  id: number
  name: string
  domain: string
  trackingId: string
  createdAt: string
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
      return {
        id: site.id,
        name: site.name,
        domain: site.domain,
        pageviews: 0,
        visitors: 0,
        realtime: 0
      }
    }
  })

  websiteStats.value = await Promise.all(statsPromises)

  totalPageviews.value = websiteStats.value.reduce((sum, s) => sum + s.pageviews, 0)
  totalVisitors.value = websiteStats.value.reduce((sum, s) => sum + s.visitors, 0)
  totalRealtime.value = websiteStats.value.reduce((sum, s) => sum + s.realtime, 0)
}

function formatNumber(num: number): string {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return num.toString()
}

function goToStats(id: number) {
  router.push(`/stats/${id}`)
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
  <div class="space-y-8">
    <div>
      <h1 class="text-3xl font-bold tracking-tight">概览</h1>
      <p class="text-muted-foreground mt-1">查看所有网站的统计数据</p>
    </div>

    <div class="grid gap-4 md:grid-cols-3">
      <Card class="relative overflow-hidden">
        <CardContent class="p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-muted-foreground">总浏览量</p>
              <p v-if="loading" class="text-3xl font-bold mt-1">
                <Skeleton class="h-9 w-20" />
              </p>
              <p v-else class="text-3xl font-bold mt-1">{{ formatNumber(totalPageviews) }}</p>
            </div>
            <div class="w-12 h-12 rounded-full bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center">
              <Eye class="w-6 h-6 text-blue-600 dark:text-blue-400" />
            </div>
          </div>
        </CardContent>
      </Card>

      <Card class="relative overflow-hidden">
        <CardContent class="p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-muted-foreground">总访客数</p>
              <p v-if="loading" class="text-3xl font-bold mt-1">
                <Skeleton class="h-9 w-20" />
              </p>
              <p v-else class="text-3xl font-bold mt-1">{{ formatNumber(totalVisitors) }}</p>
            </div>
            <div class="w-12 h-12 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
              <Users class="w-6 h-6 text-green-600 dark:text-green-400" />
            </div>
          </div>
        </CardContent>
      </Card>

      <Card class="relative overflow-hidden">
        <CardContent class="p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-muted-foreground">
                <span class="flex items-center gap-1">
                  实时在线
                  <span class="relative flex h-2 w-2">
                    <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span>
                    <span class="relative inline-flex rounded-full h-2 w-2 bg-green-500"></span>
                  </span>
                </span>
              </p>
              <p v-if="loading" class="text-3xl font-bold mt-1">
                <Skeleton class="h-9 w-20" />
              </p>
              <p v-else class="text-3xl font-bold mt-1">{{ totalRealtime }}</p>
            </div>
            <div class="w-12 h-12 rounded-full bg-purple-100 dark:bg-purple-900/30 flex items-center justify-center">
              <TrendingUp class="w-6 h-6 text-purple-600 dark:text-purple-400" />
            </div>
          </div>
        </CardContent>
      </Card>
    </div>

    <Card>
      <CardHeader>
        <CardTitle class="flex items-center gap-2">
          <Globe class="w-5 h-5" />
          网站列表
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div v-if="loading" class="space-y-4">
          <div v-for="i in 3" :key="i" class="flex items-center justify-between p-4 rounded-lg border">
            <div class="space-y-2">
              <Skeleton class="h-5 w-32" />
              <Skeleton class="h-4 w-48" />
            </div>
            <Skeleton class="h-9 w-20" />
          </div>
        </div>

        <div v-else-if="websiteStats.length === 0" class="text-center py-12">
          <Globe class="w-12 h-12 text-muted-foreground mx-auto mb-4" />
          <p class="text-muted-foreground">暂无网站，请先添加网站</p>
        </div>

        <div v-else class="space-y-3">
          <div
            v-for="site in websiteStats"
            :key="site.id"
            class="flex items-center justify-between p-4 rounded-lg border hover:border-primary/50 hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors cursor-pointer group"
            @click="goToStats(site.id)"
          >
            <div class="flex items-center gap-4">
              <div class="w-10 h-10 rounded-lg bg-gradient-to-br from-primary/20 to-primary/5 flex items-center justify-center">
                <Globe class="w-5 h-5 text-primary" />
              </div>
              <div>
                <h3 class="font-semibold">{{ site.name }}</h3>
                <p class="text-sm text-muted-foreground">{{ site.domain }}</p>
              </div>
            </div>
            <div class="flex items-center gap-6">
              <div class="text-right hidden sm:block">
                <div class="flex items-center gap-4 text-sm text-muted-foreground">
                  <span class="flex items-center gap-1">
                    <Eye class="w-4 h-4" />
                    {{ formatNumber(site.pageviews) }}
                  </span>
                  <span class="flex items-center gap-1">
                    <Users class="w-4 h-4" />
                    {{ formatNumber(site.visitors) }}
                  </span>
                  <Badge v-if="site.realtime > 0" variant="success" class="flex items-center gap-1">
                    <span class="relative flex h-2 w-2">
                      <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-white opacity-75"></span>
                      <span class="relative inline-flex rounded-full h-2 w-2 bg-white"></span>
                    </span>
                    {{ site.realtime }} 在线
                  </Badge>
                </div>
              </div>
              <ArrowUpRight class="w-5 h-5 text-muted-foreground group-hover:text-primary transition-colors" />
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { echarts } from '../lib/echarts'
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Button,
  Badge,
  Skeleton,
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell
} from '@/components/ui'
import {
  ArrowLeft,
  Eye,
  Users,
  TrendingUp,
  Globe,
  ExternalLink,
  RefreshCw,
  MapPin,
  Clock
} from 'lucide-vue-next'
import api from '../api'

interface Website {
  id: number
  name: string
  domain: string
}

interface Stats {
  pageviews: number
  visitors: number
  sessions: number
  averageDuration: number
  trend: Array<{ date: string; count: number }>
  browsers: Array<{ browser: string; count: number }>
  os: Array<{ os: string; count: number }>
  pages: Array<{ url: string; count: number }>
  referrers: Array<{ referrer: string; count: number }>
  countries: Array<{ country: string; count: number }>
  entryPages: Array<{ url: string; count: number }>
  exitPages: Array<{ url: string; count: number }>
  recentSessions: Array<{
    session_id: string
    visitor_id: string
    entry_url: string
    exit_url: string
    duration: number
    created_at: string
    last_activity_at: string
    ended_at: string
  }>
}

interface RecentVisit {
  id: number
  url: string
  referrer: string
  browser: string
  os: string
  device: string
  country: string
  ip: string
  created_at: string
}

interface TopIp {
  ip: string
  count: number
}

const route = useRoute()
const router = useRouter()
const website = ref<Website | null>(null)
const stats = ref<Stats | null>(null)
const realtimeCount = ref(0)
const recentVisits = ref<RecentVisit[]>([])
const topIps = ref<TopIp[]>([])
const loading = ref(true)
const refreshing = ref(false)

const trendChartRef = ref<HTMLElement | null>(null)
const browserChartRef = ref<HTMLElement | null>(null)
const osChartRef = ref<HTMLElement | null>(null)
const countryChartRef = ref<HTMLElement | null>(null)

let trendChart: echarts.ECharts | null = null
let browserChart: echarts.ECharts | null = null
let osChart: echarts.ECharts | null = null
let countryChart: echarts.ECharts | null = null
let refreshTimer: number | null = null

const websiteId = ref(Number(route.params.id))

async function loadWebsite() {
  try {
    const res = await api.get(`/websites/${websiteId.value}`)
    if (res.code === 200) {
      website.value = res.data
    }
  } catch (e) {
    console.error(e)
  }
}

async function loadStats() {
  try {
    const res = await api.get(`/stats/${websiteId.value}`)
    if (res.code === 200) {
      stats.value = res.data
    }
  } catch (e) {
    console.error(e)
  }
}

async function loadRealtime() {
  try {
    const res = await api.get(`/stats/realtime/${websiteId.value}`)
    if (res.code === 200) {
      realtimeCount.value = res.data
    }
  } catch (e) {
    console.error(e)
  }
}

async function loadRecentVisits() {
  try {
    const res = await api.get(`/stats/recent/${websiteId.value}?limit=20`)
    if (res.code === 200) {
      recentVisits.value = res.data
    }
  } catch (e) {
    console.error(e)
  }
}

async function loadTopIps() {
  try {
    const res = await api.get(`/stats/ips/${websiteId.value}`)
    if (res.code === 200) {
      topIps.value = res.data
    }
  } catch (e) {
    console.error(e)
  }
}

async function refreshAll() {
  refreshing.value = true
  await Promise.all([loadStats(), loadRealtime(), loadRecentVisits(), loadTopIps()])
  await nextTick()
  setTimeout(() => {
    renderCharts()
  }, 100)
  refreshing.value = false
}

function formatNumber(num: number): string {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return num.toString()
}

function truncateUrl(url: string, maxLen: number = 40): string {
  if (!url) return '-'
  if (url.length <= maxLen) return url
  return url.substring(0, maxLen) + '...'
}

function formatTime(dateStr: string): string {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  return date.toLocaleDateString('zh-CN') + ' ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function formatDuration(seconds: number): string {
  if (!seconds || seconds <= 0) return '0s'
  if (seconds < 60) return `${seconds}s`

  const minutes = Math.floor(seconds / 60)
  const remainSeconds = seconds % 60
  if (minutes < 60) return remainSeconds ? `${minutes}m ${remainSeconds}s` : `${minutes}m`

  const hours = Math.floor(minutes / 60)
  const remainMinutes = minutes % 60
  return remainMinutes ? `${hours}h ${remainMinutes}m` : `${hours}h`
}

function renderCharts() {
  if (trendChartRef.value && stats.value?.trend) {
    if (trendChart) trendChart.dispose()
    trendChart = echarts.init(trendChartRef.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: stats.value.trend.map((item) => item.date),
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisLabel: { color: '#64748b' }
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        splitLine: { lineStyle: { color: '#e2e8f0' } },
        axisLabel: { color: '#64748b' }
      },
      series: [{
        name: '浏览量',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: stats.value.trend.map((item) => item.count),
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(59, 130, 246, 0.3)' },
            { offset: 1, color: 'rgba(59, 130, 246, 0.05)' }
          ])
        },
        lineStyle: { color: '#3b82f6', width: 2 },
        itemStyle: { color: '#3b82f6' }
      }]
    })
    trendChart.resize()
  }

  const pieColors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16']

  if (browserChartRef.value && stats.value?.browsers) {
    if (browserChart) browserChart.dispose()
    browserChart = echarts.init(browserChartRef.value)
    browserChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 12, fontWeight: 'bold' } },
        data: stats.value.browsers.map((item, i) => ({
          name: item.browser || '未知',
          value: item.count,
          itemStyle: { color: pieColors[i % pieColors.length] }
        }))
      }]
    })
    browserChart.resize()
  }

  if (osChartRef.value && stats.value?.os) {
    if (osChart) osChart.dispose()
    osChart = echarts.init(osChartRef.value)
    osChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 12, fontWeight: 'bold' } },
        data: stats.value.os.map((item, i) => ({
          name: item.os || '未知',
          value: item.count,
          itemStyle: { color: pieColors[i % pieColors.length] }
        }))
      }]
    })
    osChart.resize()
  }

  if (countryChartRef.value && stats.value?.countries) {
    if (countryChart) countryChart.dispose()
    countryChart = echarts.init(countryChartRef.value)
    countryChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 12, fontWeight: 'bold' } },
        data: stats.value.countries.map((item, i) => ({
          name: item.country || '未知',
          value: item.count,
          itemStyle: { color: pieColors[i % pieColors.length] }
        }))
      }]
    })
    countryChart.resize()
  }
}

function handleResize() {
  trendChart?.resize()
  browserChart?.resize()
  osChart?.resize()
  countryChart?.resize()
}

onMounted(async () => {
  await loadWebsite()
  await loadStats()
  await loadRealtime()
  await loadRecentVisits()
  await loadTopIps()
  loading.value = false

  await nextTick()
  setTimeout(() => {
    renderCharts()
  }, 100)

  refreshTimer = window.setInterval(() => {
    loadRealtime()
    loadRecentVisits()
  }, 10000)

  window.addEventListener('resize', handleResize)
})

watch(
  () => route.params.id,
  async (newId, oldId) => {
    if (newId === oldId) {
      return
    }

    websiteId.value = Number(newId)
    loading.value = true
    await loadWebsite()
    await refreshAll()
    loading.value = false
  }
)

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  trendChart?.dispose()
  browserChart?.dispose()
  osChart?.dispose()
  countryChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <div class="space-y-8">
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <Button variant="ghost" size="icon" @click="router.push('/dashboard')">
          <ArrowLeft class="w-5 h-5" />
        </Button>
        <div>
          <h1 class="text-3xl font-bold tracking-tight">{{ website?.name || '统计详情' }}</h1>
          <a
            v-if="website"
            :href="'https://' + website.domain"
            target="_blank"
            class="text-muted-foreground hover:text-primary flex items-center gap-1 text-sm mt-1"
          >
            {{ website.domain }}
            <ExternalLink class="w-3 h-3" />
          </a>
        </div>
      </div>
      <Button variant="outline" :loading="refreshing" @click="refreshAll">
        <RefreshCw class="w-4 h-4 mr-2" />
        刷新
      </Button>
    </div>

    <div class="grid gap-4 md:grid-cols-4">
      <Card>
        <CardContent class="p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-muted-foreground">浏览量 (PV)</p>
              <p v-if="loading" class="text-3xl font-bold mt-1"><Skeleton class="h-9 w-20" /></p>
              <p v-else class="text-3xl font-bold mt-1">{{ formatNumber(stats?.pageviews || 0) }}</p>
            </div>
            <div class="w-12 h-12 rounded-full bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center">
              <Eye class="w-6 h-6 text-blue-600 dark:text-blue-400" />
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent class="p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-muted-foreground">访客数 (UV)</p>
              <p v-if="loading" class="text-3xl font-bold mt-1"><Skeleton class="h-9 w-20" /></p>
              <p v-else class="text-3xl font-bold mt-1">{{ formatNumber(stats?.visitors || 0) }}</p>
            </div>
            <div class="w-12 h-12 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
              <Users class="w-6 h-6 text-green-600 dark:text-green-400" />
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent class="p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-muted-foreground">实时在线</p>
              <p v-if="loading" class="text-3xl font-bold mt-1"><Skeleton class="h-9 w-20" /></p>
              <p v-else class="text-3xl font-bold mt-1">{{ realtimeCount }}</p>
            </div>
            <div class="w-12 h-12 rounded-full bg-purple-100 dark:bg-purple-900/30 flex items-center justify-center">
              <TrendingUp class="w-6 h-6 text-purple-600 dark:text-purple-400" />
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent class="p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-muted-foreground">平均会话时长</p>
              <p v-if="loading" class="text-3xl font-bold mt-1"><Skeleton class="h-9 w-20" /></p>
              <p v-else class="text-3xl font-bold mt-1">{{ formatDuration(stats?.averageDuration || 0) }}</p>
            </div>
            <div class="w-12 h-12 rounded-full bg-amber-100 dark:bg-amber-900/30 flex items-center justify-center">
              <Clock class="w-6 h-6 text-amber-600 dark:text-amber-400" />
            </div>
          </div>
        </CardContent>
      </Card>
    </div>

    <Card>
      <CardHeader>
        <CardTitle class="flex items-center gap-2">
          <TrendingUp class="w-5 h-5" />
          访问趋势
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div class="relative h-[300px]">
          <div v-show="loading" class="absolute inset-0"><Skeleton class="h-full w-full" /></div>
          <div v-show="!loading" ref="trendChartRef" class="h-full w-full"></div>
        </div>
      </CardContent>
    </Card>

    <div class="grid gap-6 lg:grid-cols-3">
      <Card>
        <CardHeader>
          <CardTitle class="text-base">浏览器分布</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="relative h-[200px]">
            <div v-show="loading" class="absolute inset-0"><Skeleton class="h-full w-full" /></div>
            <div v-show="!loading" ref="browserChartRef" class="h-full w-full"></div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle class="text-base">操作系统分布</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="relative h-[200px]">
            <div v-show="loading" class="absolute inset-0"><Skeleton class="h-full w-full" /></div>
            <div v-show="!loading" ref="osChartRef" class="h-full w-full"></div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle class="text-base">地区分布</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="relative h-[200px]">
            <div v-show="loading" class="absolute inset-0"><Skeleton class="h-full w-full" /></div>
            <div v-show="!loading" ref="countryChartRef" class="h-full w-full"></div>
          </div>
        </CardContent>
      </Card>
    </div>

    <div class="grid gap-6 lg:grid-cols-2">
      <Card>
        <CardHeader>
          <CardTitle class="flex items-center gap-2">
            <Globe class="w-5 h-5" />
            热门页面
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div v-if="loading" class="space-y-3">
            <div v-for="i in 5" :key="i" class="flex justify-between"><Skeleton class="h-5 w-40" /><Skeleton class="h-5 w-12" /></div>
          </div>
          <Table v-else-if="stats?.pages?.length">
            <TableHeader>
              <TableRow>
                <TableHead>页面</TableHead>
                <TableHead class="text-right">访问量</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-for="page in stats.pages" :key="page.url">
                <TableCell class="font-mono text-xs">{{ truncateUrl(page.url) }}</TableCell>
                <TableCell class="text-right">{{ page.count }}</TableCell>
              </TableRow>
            </TableBody>
          </Table>
          <p v-else class="text-muted-foreground text-center py-8">暂无数据</p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle class="flex items-center gap-2">
            <MapPin class="w-5 h-5" />
            访问 IP 排行
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div v-if="loading" class="space-y-3">
            <div v-for="i in 5" :key="i" class="flex justify-between"><Skeleton class="h-5 w-32" /><Skeleton class="h-5 w-12" /></div>
          </div>
          <Table v-else-if="topIps?.length">
            <TableHeader>
              <TableRow>
                <TableHead>IP 地址</TableHead>
                <TableHead class="text-right">访问次数</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-for="item in topIps" :key="item.ip">
                <TableCell class="font-mono text-xs">{{ item.ip || '-' }}</TableCell>
                <TableCell class="text-right">{{ item.count }}</TableCell>
              </TableRow>
            </TableBody>
          </Table>
          <p v-else class="text-muted-foreground text-center py-8">暂无数据</p>
        </CardContent>
      </Card>
    </div>

    <Card>
      <CardHeader>
        <CardTitle class="flex items-center gap-2">
          <Users class="w-5 h-5" />
          会话分析
        </CardTitle>
      </CardHeader>
      <CardContent class="space-y-6">
        <div class="grid gap-4 md:grid-cols-2">
          <div class="rounded-xl border p-4">
            <p class="text-sm text-muted-foreground mb-2">会话数</p>
            <p v-if="loading" class="text-2xl font-bold"><Skeleton class="h-8 w-20" /></p>
            <p v-else class="text-2xl font-bold">{{ formatNumber(stats?.sessions || 0) }}</p>
          </div>
          <div class="rounded-xl border p-4">
            <p class="text-sm text-muted-foreground mb-2">会话/访客</p>
            <p v-if="loading" class="text-2xl font-bold"><Skeleton class="h-8 w-20" /></p>
            <p v-else class="text-2xl font-bold">
              {{ stats?.visitors ? ((stats?.sessions || 0) / stats.visitors).toFixed(2) : '0.00' }}
            </p>
          </div>
        </div>

        <div class="grid gap-6 lg:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle class="text-base">入口页面</CardTitle>
            </CardHeader>
            <CardContent>
              <Table v-if="stats?.entryPages?.length">
                <TableHeader>
                  <TableRow>
                    <TableHead>入口 URL</TableHead>
                    <TableHead class="text-right">会话数</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  <TableRow v-for="entry in stats.entryPages" :key="entry.url">
                    <TableCell class="font-mono text-xs">{{ truncateUrl(entry.url, 48) }}</TableCell>
                    <TableCell class="text-right">{{ entry.count }}</TableCell>
                  </TableRow>
                </TableBody>
              </Table>
              <p v-else class="text-muted-foreground text-center py-8">暂无数据</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle class="text-base">退出页面</CardTitle>
            </CardHeader>
            <CardContent>
              <Table v-if="stats?.exitPages?.length">
                <TableHeader>
                  <TableRow>
                    <TableHead>退出 URL</TableHead>
                    <TableHead class="text-right">会话数</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  <TableRow v-for="exit in stats.exitPages" :key="exit.url">
                    <TableCell class="font-mono text-xs">{{ truncateUrl(exit.url, 48) }}</TableCell>
                    <TableCell class="text-right">{{ exit.count }}</TableCell>
                  </TableRow>
                </TableBody>
              </Table>
              <p v-else class="text-muted-foreground text-center py-8">暂无数据</p>
            </CardContent>
          </Card>
        </div>

        <div>
          <h3 class="text-base font-semibold mb-3">最近会话</h3>
          <div v-if="stats?.recentSessions?.length" class="space-y-2">
            <div
              v-for="session in stats.recentSessions"
              :key="session.session_id"
              class="rounded-xl border p-4 hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors"
            >
              <div class="flex flex-wrap items-center gap-2 mb-2">
                <Badge variant="secondary" class="font-mono">{{ session.session_id }}</Badge>
                <Badge variant="outline" class="font-mono">{{ session.visitor_id || '-' }}</Badge>
                <span class="text-xs text-muted-foreground ml-auto">
                  {{ formatDuration(session.duration || 0) }}
                </span>
              </div>
              <div class="grid gap-2 text-xs text-muted-foreground">
                <div>入口：{{ truncateUrl(session.entry_url, 72) }}</div>
                <div>退出：{{ truncateUrl(session.exit_url, 72) }}</div>
                <div>
                  活动：{{ formatTime(session.last_activity_at) }}
                  <span v-if="session.ended_at"> / 结束：{{ formatTime(session.ended_at) }}</span>
                </div>
              </div>
            </div>
          </div>
          <p v-else class="text-muted-foreground text-center py-8">暂无会话记录</p>
        </div>
      </CardContent>
    </Card>

    <Card>
      <CardHeader>
        <CardTitle class="flex items-center gap-2">
          <Clock class="w-5 h-5" />
          最近访问记录
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div v-if="loading" class="space-y-3">
          <div v-for="i in 5" :key="i" class="flex items-center gap-4 p-3 rounded-lg border">
            <Skeleton class="h-5 w-20" />
            <Skeleton class="h-5 flex-1" />
            <Skeleton class="h-5 w-24" />
          </div>
        </div>
        <div v-else-if="recentVisits?.length" class="space-y-2">
          <div
            v-for="visit in recentVisits"
            :key="visit.id"
            class="flex flex-wrap items-center gap-3 p-3 rounded-lg border hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors"
          >
            <Badge variant="secondary" class="font-mono">
              {{ visit.ip || '-' }}
            </Badge>
            <Badge v-if="visit.country" variant="outline" class="text-xs">{{ visit.country }}</Badge>
            <span class="font-mono text-xs text-muted-foreground flex-1 min-w-0 truncate">
              {{ truncateUrl(visit.url, 50) }}
            </span>
            <div class="flex items-center gap-2 text-xs text-muted-foreground">
              <Badge variant="outline" class="text-xs">{{ visit.browser || '-' }}</Badge>
              <Badge variant="outline" class="text-xs">{{ visit.os || '-' }}</Badge>
              <Badge v-if="visit.device" variant="outline" class="text-xs">{{ visit.device }}</Badge>
            </div>
            <span class="text-xs text-muted-foreground whitespace-nowrap">
              {{ formatTime(visit.created_at) }}
            </span>
          </div>
        </div>
        <p v-else class="text-muted-foreground text-center py-8">暂无访问记录</p>
      </CardContent>
    </Card>
  </div>
</template>

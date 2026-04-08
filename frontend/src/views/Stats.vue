<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
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
  Activity,
  ArrowLeft,
  Clock,
  Download,
  ExternalLink,
  Eye,
  Globe2,
  MapPin,
  RefreshCw,
  Sparkles,
  Users
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

interface PageStat {
  url: string
  count: number
}

interface TopIp {
  ip: string
  count: number
}

interface RecentSession {
  session_id: string
  visitor_id: string
  entry_url: string
  exit_url: string
  duration: number
  created_at: string
  last_activity_at: string
  ended_at: string
}

interface PagedResponse<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
}

type ExportDataset = 'recent' | 'pages' | 'ips' | 'sessions'
type RangePreset = 'today' | '7d' | '30d' | 'custom'

const route = useRoute()
const router = useRouter()
const website = ref<Website | null>(null)
const stats = ref<Stats | null>(null)
const realtimeCount = ref(0)
const recentVisits = ref<RecentVisit[]>([])
const recentVisitsTotal = ref(0)
const pageRankings = ref<PageStat[]>([])
const pageRankingsTotal = ref(0)
const recentSessions = ref<RecentSession[]>([])
const recentSessionsTotal = ref(0)
const topIps = ref<TopIp[]>([])
const loading = ref(true)
const refreshing = ref(false)
const exporting = ref(false)

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
const rangePreset = ref<RangePreset>('7d')
const startDate = ref('')
const endDate = ref('')
const recentPage = ref(1)
const recentPageSize = 20
const pagesPage = ref(1)
const pagesPageSize = 10
const sessionsPage = ref(1)
const sessionsPageSize = 10
const exportDataset = ref<ExportDataset>('recent')

initializeDateRange()

const sessionRatio = computed(() => {
  if (!stats.value?.visitors) return '0.00'
  return ((stats.value.sessions || 0) / stats.value.visitors).toFixed(2)
})

const summaryNote = computed(() => {
  if (!stats.value) return '正在汇总当前站点的数据。'
  if (!stats.value.pageviews) return '当前时间范围内还没有采集到访问数据。'
  if (realtimeCount.value > 0) return `现在有 ${realtimeCount.value} 位访客在线，建议优先观察最近访问与会话变化。`
  return '当前没有明显实时流量，更适合安静地回看趋势和内容表现。'
})

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
    const res = await api.get(`/stats/${websiteId.value}`, { params: buildRangeParams() })
    if (res.code === 200) {
      stats.value = res.data
      pageRankings.value = res.data.pages || []
      pageRankingsTotal.value = Math.max((res.data.pages || []).length, pageRankingsTotal.value)
      recentSessions.value = res.data.recentSessions || []
      recentSessionsTotal.value = Math.max((res.data.recentSessions || []).length, recentSessionsTotal.value)
    }
  } catch (e) {
    console.error(e)
  }
}

async function loadRealtime() {
  try {
    const res = await api.get(`/stats/realtime/${websiteId.value}`)
    if (res.code === 200) realtimeCount.value = res.data
  } catch (e) {
    console.error(e)
  }
}

async function loadRecentVisits() {
  try {
    const res = await api.get(`/stats/recent/${websiteId.value}`, {
      params: { page: recentPage.value, pageSize: recentPageSize, ...buildRangeParams() }
    })
    if (res.code === 200) {
      const data = res.data as PagedResponse<RecentVisit>
      recentVisits.value = data.items || []
      recentVisitsTotal.value = data.total || 0
      recentPage.value = data.page || 1
    }
  } catch (e) {
    console.error(e)
  }
}

async function loadTopIps() {
  try {
    const res = await api.get(`/stats/ips/${websiteId.value}`, { params: buildRangeParams() })
    if (res.code === 200) topIps.value = res.data
  } catch (e) {
    console.error(e)
  }
}

async function loadPages() {
  try {
    const res = await api.get(`/stats/pages/${websiteId.value}`, {
      params: { page: pagesPage.value, pageSize: pagesPageSize, ...buildRangeParams() }
    })
    if (res.code === 200) {
      const data = res.data as PagedResponse<PageStat>
      pageRankings.value = data.items || []
      pageRankingsTotal.value = data.total || 0
      pagesPage.value = data.page || 1
    }
  } catch (e) {
    console.error(e)
  }
}

async function loadRecentSessions() {
  try {
    const res = await api.get(`/stats/sessions/${websiteId.value}`, {
      params: { page: sessionsPage.value, pageSize: sessionsPageSize, ...buildRangeParams() }
    })
    if (res.code === 200) {
      const data = res.data as PagedResponse<RecentSession>
      recentSessions.value = data.items || []
      recentSessionsTotal.value = data.total || 0
      sessionsPage.value = data.page || 1
    }
  } catch (e) {
    console.error(e)
  }
}

async function refreshAll() {
  refreshing.value = true
  await Promise.all([loadStats(), loadRealtime(), loadRecentVisits(), loadTopIps(), loadPages(), loadRecentSessions()])
  await nextTick()
  window.setTimeout(renderCharts, 100)
  refreshing.value = false
}

function formatNumber(num: number): string {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return num.toString()
}

function initializeDateRange() {
  applyPreset('7d')
}

function formatDate(date: Date): string {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

function applyPreset(preset: RangePreset) {
  rangePreset.value = preset
  const today = new Date()
  const end = formatDate(today)

  if (preset === 'today') {
    startDate.value = end
    endDate.value = end
    return
  }

  if (preset === '7d') {
    const start = new Date(today)
    start.setDate(start.getDate() - 6)
    startDate.value = formatDate(start)
    endDate.value = end
    return
  }

  const start = new Date(today)
  start.setDate(start.getDate() - 29)
  startDate.value = formatDate(start)
  endDate.value = end
}

function buildRangeParams() {
  return { start: startDate.value, end: endDate.value }
}

async function applyPresetAndRefresh(preset: RangePreset) {
  applyPreset(preset)
  recentPage.value = 1
  pagesPage.value = 1
  sessionsPage.value = 1
  await refreshAll()
}

async function applyCustomRange() {
  if (!startDate.value || !endDate.value) return
  rangePreset.value = 'custom'
  recentPage.value = 1
  pagesPage.value = 1
  sessionsPage.value = 1
  loading.value = true
  await refreshAll()
  loading.value = false
}

const totalRecentPages = computed(() => {
  if (!recentVisitsTotal.value) return 1
  return Math.max(1, Math.ceil(recentVisitsTotal.value / recentPageSize))
})

const totalPageRankingPages = computed(() => {
  if (!pageRankingsTotal.value) return 1
  return Math.max(1, Math.ceil(pageRankingsTotal.value / pagesPageSize))
})

const totalRecentSessionPages = computed(() => {
  if (!recentSessionsTotal.value) return 1
  return Math.max(1, Math.ceil(recentSessionsTotal.value / sessionsPageSize))
})

async function changeRecentPage(direction: 'prev' | 'next') {
  if (direction === 'prev' && recentPage.value > 1) {
    recentPage.value -= 1
  } else if (direction === 'next' && recentPage.value < totalRecentPages.value) {
    recentPage.value += 1
  } else {
    return
  }
  await loadRecentVisits()
}

async function changePagesPage(direction: 'prev' | 'next') {
  if (direction === 'prev' && pagesPage.value > 1) {
    pagesPage.value -= 1
  } else if (direction === 'next' && pagesPage.value < totalPageRankingPages.value) {
    pagesPage.value += 1
  } else {
    return
  }
  await loadPages()
}

async function changeSessionsPage(direction: 'prev' | 'next') {
  if (direction === 'prev' && sessionsPage.value > 1) {
    sessionsPage.value -= 1
  } else if (direction === 'next' && sessionsPage.value < totalRecentSessionPages.value) {
    sessionsPage.value += 1
  } else {
    return
  }
  await loadRecentSessions()
}

async function exportCurrentData() {
  exporting.value = true
  try {
    const token = localStorage.getItem('token') || ''
    const params = new URLSearchParams({
      dataset: exportDataset.value,
      start: startDate.value,
      end: endDate.value
    })
    const response = await fetch(`/api/stats/export/${websiteId.value}?${params.toString()}`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    })
    if (!response.ok) throw new Error(`Export failed: ${response.status}`)

    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    const disposition = response.headers.get('content-disposition') || ''
    const match = disposition.match(/filename\*=UTF-8''([^;]+)/)
    anchor.href = url
    anchor.download = match ? decodeURIComponent(match[1]) : `${website.value?.name || 'stats'}.csv`
    document.body.appendChild(anchor)
    anchor.click()
    document.body.removeChild(anchor)
    window.URL.revokeObjectURL(url)
  } catch (e) {
    console.error(e)
  } finally {
    exporting.value = false
  }
}

function truncateUrl(url: string, maxLen = 52): string {
  if (!url) return '-'
  if (url.length <= maxLen) return url
  return `${url.substring(0, maxLen)}...`
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
    trendChart?.dispose()
    trendChart = echarts.init(trendChartRef.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '2%', right: '2%', bottom: '2%', containLabel: true },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: stats.value.trend.map((item) => item.date),
        axisLine: { lineStyle: { color: '#dbe3ee' } },
        axisLabel: { color: '#64748b' }
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        splitLine: { lineStyle: { color: '#e7edf5' } },
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
            { offset: 0, color: 'rgba(58, 123, 213, 0.22)' },
            { offset: 1, color: 'rgba(58, 123, 213, 0.03)' }
          ])
        },
        lineStyle: { color: '#3b82f6', width: 2 },
        itemStyle: { color: '#3b82f6' }
      }]
    })
    trendChart.resize()
  }

  const pieColors = ['#3b82f6', '#06b6d4', '#10b981', '#8b5cf6', '#f59e0b', '#ec4899', '#84cc16', '#ef4444']

  if (browserChartRef.value && stats.value?.browsers) {
    browserChart?.dispose()
    browserChart = echarts.init(browserChartRef.value)
    browserChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      series: [{
        type: 'pie',
        radius: ['48%', '74%'],
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 3 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 12, fontWeight: 'bold' } },
        data: stats.value.browsers.map((item, i) => ({ name: item.browser || '未知', value: item.count, itemStyle: { color: pieColors[i % pieColors.length] } }))
      }]
    })
    browserChart.resize()
  }

  if (osChartRef.value && stats.value?.os) {
    osChart?.dispose()
    osChart = echarts.init(osChartRef.value)
    osChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      series: [{
        type: 'pie',
        radius: ['48%', '74%'],
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 3 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 12, fontWeight: 'bold' } },
        data: stats.value.os.map((item, i) => ({ name: item.os || '未知', value: item.count, itemStyle: { color: pieColors[i % pieColors.length] } }))
      }]
    })
    osChart.resize()
  }

  if (countryChartRef.value && stats.value?.countries) {
    countryChart?.dispose()
    countryChart = echarts.init(countryChartRef.value)
    countryChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      series: [{
        type: 'pie',
        radius: ['48%', '74%'],
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 3 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 12, fontWeight: 'bold' } },
        data: stats.value.countries.map((item, i) => ({ name: item.country || '未知', value: item.count, itemStyle: { color: pieColors[i % pieColors.length] } }))
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
  await loadPages()
  await loadRecentSessions()
  loading.value = false
  await nextTick()
  window.setTimeout(renderCharts, 100)
  refreshTimer = window.setInterval(() => {
    loadRealtime()
    loadRecentVisits()
  }, 10000)
  window.addEventListener('resize', handleResize)
})

watch(
  () => route.params.id,
  async (newId, oldId) => {
    if (newId === oldId) return
    websiteId.value = Number(newId)
    recentPage.value = 1
    pagesPage.value = 1
    sessionsPage.value = 1
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
  <div class="space-y-6">
    <section class="apple-hero">
      <div class="grid gap-8 xl:grid-cols-[1.08fr_0.92fr] xl:items-start">
        <div>
          <div class="flex items-center gap-3">
            <Button variant="ghost" size="icon" class="rounded-2xl bg-white/80" @click="router.push('/dashboard')">
              <ArrowLeft class="h-5 w-5" />
            </Button>
            <span class="apple-label">
              <Sparkles class="h-3.5 w-3.5" />
              Detail Analytics
            </span>
          </div>

          <h1 class="mt-6 apple-title">{{ website?.name || '统计详情' }}</h1>
          <a
            v-if="website"
            :href="website.domain.startsWith('http') ? website.domain : `https://${website.domain}`"
            target="_blank"
            class="mt-4 inline-flex items-center gap-1 text-sm text-slate-500 transition hover:text-slate-900"
          >
            {{ website.domain }}
            <ExternalLink class="h-3.5 w-3.5" />
          </a>
          <p class="mt-4 max-w-xl apple-subtitle">{{ summaryNote }}</p>
        </div>

        <div class="apple-card">
          <p class="text-sm font-medium text-slate-950">时间范围</p>
          <div class="mt-4 grid grid-cols-3 gap-2">
            <Button :variant="rangePreset === 'today' ? 'default' : 'outline'" :class="rangePreset === 'today' ? 'rounded-2xl bg-slate-950 text-white hover:bg-slate-800' : 'rounded-2xl border-slate-200 bg-white/80'" @click="applyPresetAndRefresh('today')">今天</Button>
            <Button :variant="rangePreset === '7d' ? 'default' : 'outline'" :class="rangePreset === '7d' ? 'rounded-2xl bg-slate-950 text-white hover:bg-slate-800' : 'rounded-2xl border-slate-200 bg-white/80'" @click="applyPresetAndRefresh('7d')">7 天</Button>
            <Button :variant="rangePreset === '30d' ? 'default' : 'outline'" :class="rangePreset === '30d' ? 'rounded-2xl bg-slate-950 text-white hover:bg-slate-800' : 'rounded-2xl border-slate-200 bg-white/80'" @click="applyPresetAndRefresh('30d')">30 天</Button>
          </div>

          <div class="mt-4 grid gap-3 sm:grid-cols-2">
            <input v-model="startDate" type="date" class="apple-input [color-scheme:light]" />
            <input v-model="endDate" type="date" class="apple-input [color-scheme:light]" />
          </div>

          <div class="mt-4 grid gap-2 sm:flex sm:flex-wrap">
            <Button class="w-full rounded-2xl bg-slate-950 text-white hover:bg-slate-800 sm:w-auto" @click="applyCustomRange">应用自定义范围</Button>
            <Button variant="outline" class="w-full rounded-2xl border-slate-200 bg-white/80 sm:w-auto" :loading="refreshing" @click="refreshAll">
              <RefreshCw class="mr-2 h-4 w-4" />
              刷新
            </Button>
            <div class="grid w-full gap-2 sm:w-auto sm:grid-cols-[minmax(0,140px)_auto]">
              <select v-model="exportDataset" class="apple-input min-w-0 bg-white/80 text-sm">
                <option value="recent">最近访问</option>
                <option value="pages">热门页面</option>
                <option value="ips">IP 排行</option>
                <option value="sessions">最近会话</option>
              </select>
              <Button variant="outline" class="w-full rounded-2xl border-slate-200 bg-white/80 sm:w-auto" :loading="exporting" @click="exportCurrentData">
                <Download class="mr-2 h-4 w-4" />
                导出 CSV
              </Button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="grid gap-4 md:grid-cols-4">
      <div class="apple-card">
        <div class="flex items-center justify-between text-slate-500"><span class="text-sm">浏览量 (PV)</span><Eye class="h-4 w-4" /></div>
        <p v-if="loading" class="mt-4"><Skeleton class="h-10 w-20" /></p>
        <p v-else class="apple-stat-value">{{ formatNumber(stats?.pageviews || 0) }}</p>
      </div>
      <div class="apple-card">
        <div class="flex items-center justify-between text-slate-500"><span class="text-sm">访客数 (UV)</span><Users class="h-4 w-4" /></div>
        <p v-if="loading" class="mt-4"><Skeleton class="h-10 w-20" /></p>
        <p v-else class="apple-stat-value">{{ formatNumber(stats?.visitors || 0) }}</p>
      </div>
      <div class="apple-card">
        <div class="flex items-center justify-between text-slate-500"><span class="text-sm">实时在线</span><Activity class="h-4 w-4" /></div>
        <p v-if="loading" class="mt-4"><Skeleton class="h-10 w-20" /></p>
        <p v-else class="apple-stat-value">{{ realtimeCount }}</p>
      </div>
      <div class="apple-card">
        <div class="flex items-center justify-between text-slate-500"><span class="text-sm">平均会话时长</span><Clock class="h-4 w-4" /></div>
        <p v-if="loading" class="mt-4"><Skeleton class="h-10 w-20" /></p>
        <p v-else class="apple-stat-value">{{ formatDuration(stats?.averageDuration || 0) }}</p>
      </div>
    </section>

    <section class="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
      <div class="apple-surface p-6">
        <div class="mb-4">
          <p class="text-[11px] uppercase tracking-[0.22em] text-slate-400">Trend</p>
          <h2 class="mt-2 text-[1.8rem] font-semibold tracking-[-0.04em] text-slate-950">访问趋势</h2>
        </div>
        <div class="relative h-[320px]">
          <div v-show="loading" class="absolute inset-0"><Skeleton class="h-full w-full" /></div>
          <div v-show="!loading" ref="trendChartRef" class="h-full w-full"></div>
        </div>
      </div>

      <div class="space-y-6">
        <div class="apple-surface p-6">
          <p class="text-[11px] uppercase tracking-[0.22em] text-slate-400">Quick Read</p>
          <h3 class="mt-2 text-[1.8rem] font-semibold tracking-[-0.04em] text-slate-950">快速判断</h3>
          <div class="mt-6 space-y-4">
            <div class="apple-soft">
              <p class="text-sm text-slate-500">会话数</p>
              <p v-if="loading" class="mt-2"><Skeleton class="h-8 w-20" /></p>
              <p v-else class="mt-2 text-2xl font-semibold tracking-[-0.03em] text-slate-950">{{ formatNumber(stats?.sessions || 0) }}</p>
            </div>
            <div class="apple-soft">
              <p class="text-sm text-slate-500">会话 / 访客</p>
              <p v-if="loading" class="mt-2"><Skeleton class="h-8 w-20" /></p>
              <p v-else class="mt-2 text-2xl font-semibold tracking-[-0.03em] text-slate-950">{{ sessionRatio }}</p>
            </div>
            <div class="apple-soft text-sm leading-7 text-slate-600">
              先看趋势，再看会话。
            </div>
          </div>
        </div>

        <div class="apple-card">
          <p class="text-sm text-slate-500">当前状态</p>
          <p class="mt-4 text-[1.8rem] font-semibold tracking-[-0.04em] text-slate-950">{{ realtimeCount > 0 ? '现在适合追实时' : '现在适合做回看' }}</p>
          <p class="mt-4 text-sm leading-7 text-slate-600">{{ realtimeCount > 0 ? '建议观察最近访问和会话变化。' : '建议优先看入口页、退出页和热门页面。' }}</p>
        </div>
      </div>
    </section>

    <section>
      <div class="mb-4">
        <h2 class="text-xl font-semibold tracking-[-0.03em] text-slate-950">受众构成</h2>
        <p class="mt-1 text-sm text-slate-500">浏览器、操作系统和地区能快速还原访问轮廓。</p>
      </div>
      <div class="grid gap-6 lg:grid-cols-3">
        <Card class="border-0 bg-white/80 shadow-[0_18px_45px_-36px_rgba(15,23,42,0.22)] backdrop-blur-xl"><CardHeader><CardTitle class="text-base">浏览器分布</CardTitle></CardHeader><CardContent><div class="relative h-[220px]"><div v-show="loading" class="absolute inset-0"><Skeleton class="h-full w-full" /></div><div v-show="!loading" ref="browserChartRef" class="h-full w-full"></div></div></CardContent></Card>
        <Card class="border-0 bg-white/80 shadow-[0_18px_45px_-36px_rgba(15,23,42,0.22)] backdrop-blur-xl"><CardHeader><CardTitle class="text-base">操作系统分布</CardTitle></CardHeader><CardContent><div class="relative h-[220px]"><div v-show="loading" class="absolute inset-0"><Skeleton class="h-full w-full" /></div><div v-show="!loading" ref="osChartRef" class="h-full w-full"></div></div></CardContent></Card>
        <Card class="border-0 bg-white/80 shadow-[0_18px_45px_-36px_rgba(15,23,42,0.22)] backdrop-blur-xl"><CardHeader><CardTitle class="text-base">地区分布</CardTitle></CardHeader><CardContent><div class="relative h-[220px]"><div v-show="loading" class="absolute inset-0"><Skeleton class="h-full w-full" /></div><div v-show="!loading" ref="countryChartRef" class="h-full w-full"></div></div></CardContent></Card>
      </div>
    </section>

    <section class="grid gap-6 lg:grid-cols-2">
      <Card class="border-0 bg-white/80 shadow-[0_18px_45px_-36px_rgba(15,23,42,0.22)] backdrop-blur-xl">
        <CardHeader><CardTitle class="flex items-center gap-2"><Globe2 class="h-5 w-5" />热门页面</CardTitle></CardHeader>
        <CardContent>
          <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
            <p class="text-sm text-slate-500">第 {{ pagesPage }} / {{ totalPageRankingPages }} 页，共 {{ pageRankingsTotal }} 条</p>
            <div class="flex items-center gap-2">
              <Button variant="outline" class="rounded-2xl border-slate-200 bg-white/80" :disabled="pagesPage <= 1" @click="changePagesPage('prev')">上一页</Button>
              <Button variant="outline" class="rounded-2xl border-slate-200 bg-white/80" :disabled="pagesPage >= totalPageRankingPages" @click="changePagesPage('next')">下一页</Button>
            </div>
          </div>
          <div v-if="loading" class="space-y-3"><div v-for="i in 5" :key="i" class="flex justify-between"><Skeleton class="h-5 w-40" /><Skeleton class="h-5 w-12" /></div></div>
          <div v-else-if="pageRankings?.length" class="overflow-x-auto"><Table><TableHeader><TableRow><TableHead>页面</TableHead><TableHead class="text-right">访问量</TableHead></TableRow></TableHeader><TableBody><TableRow v-for="page in pageRankings" :key="page.url"><TableCell class="min-w-[220px] font-mono text-xs">{{ truncateUrl(page.url) }}</TableCell><TableCell class="text-right">{{ page.count }}</TableCell></TableRow></TableBody></Table></div>
          <p v-else class="py-8 text-center text-muted-foreground">暂无数据</p>
        </CardContent>
      </Card>

      <Card class="border-0 bg-white/80 shadow-[0_18px_45px_-36px_rgba(15,23,42,0.22)] backdrop-blur-xl">
        <CardHeader><CardTitle class="flex items-center gap-2"><MapPin class="h-5 w-5" />访问 IP 排行</CardTitle></CardHeader>
        <CardContent>
          <div v-if="loading" class="space-y-3"><div v-for="i in 5" :key="i" class="flex justify-between"><Skeleton class="h-5 w-32" /><Skeleton class="h-5 w-12" /></div></div>
          <div v-else-if="topIps?.length" class="overflow-x-auto"><Table><TableHeader><TableRow><TableHead>IP 地址</TableHead><TableHead class="text-right">访问次数</TableHead></TableRow></TableHeader><TableBody><TableRow v-for="item in topIps" :key="item.ip"><TableCell class="min-w-[140px] font-mono text-xs">{{ item.ip || '-' }}</TableCell><TableCell class="text-right">{{ item.count }}</TableCell></TableRow></TableBody></Table></div>
          <p v-else class="py-8 text-center text-muted-foreground">暂无数据</p>
        </CardContent>
      </Card>
    </section>

    <section class="space-y-6">
      <div>
        <h2 class="text-xl font-semibold tracking-[-0.03em] text-slate-950">会话分析</h2>
        <p class="mt-1 text-sm text-slate-500">这里更接近用户在站内怎么走，而不是单纯的访问量。</p>
      </div>
      <div class="grid gap-6 lg:grid-cols-2">
        <Card class="border-0 bg-white/80 shadow-[0_18px_45px_-36px_rgba(15,23,42,0.22)] backdrop-blur-xl"><CardHeader><CardTitle class="text-base">入口页面</CardTitle></CardHeader><CardContent><div v-if="stats?.entryPages?.length" class="overflow-x-auto"><Table><TableHeader><TableRow><TableHead>入口 URL</TableHead><TableHead class="text-right">会话数</TableHead></TableRow></TableHeader><TableBody><TableRow v-for="entry in stats.entryPages" :key="entry.url"><TableCell class="min-w-[220px] font-mono text-xs">{{ truncateUrl(entry.url) }}</TableCell><TableCell class="text-right">{{ entry.count }}</TableCell></TableRow></TableBody></Table></div><p v-else class="py-8 text-center text-muted-foreground">暂无数据</p></CardContent></Card>
        <Card class="border-0 bg-white/80 shadow-[0_18px_45px_-36px_rgba(15,23,42,0.22)] backdrop-blur-xl"><CardHeader><CardTitle class="text-base">退出页面</CardTitle></CardHeader><CardContent><div v-if="stats?.exitPages?.length" class="overflow-x-auto"><Table><TableHeader><TableRow><TableHead>退出 URL</TableHead><TableHead class="text-right">会话数</TableHead></TableRow></TableHeader><TableBody><TableRow v-for="exit in stats.exitPages" :key="exit.url"><TableCell class="min-w-[220px] font-mono text-xs">{{ truncateUrl(exit.url) }}</TableCell><TableCell class="text-right">{{ exit.count }}</TableCell></TableRow></TableBody></Table></div><p v-else class="py-8 text-center text-muted-foreground">暂无数据</p></CardContent></Card>
      </div>

      <Card class="border-0 bg-white/80 shadow-[0_18px_45px_-36px_rgba(15,23,42,0.22)] backdrop-blur-xl">
        <CardHeader><CardTitle class="text-base">最近会话</CardTitle></CardHeader>
        <CardContent>
          <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
            <p class="text-sm text-slate-500">第 {{ sessionsPage }} / {{ totalRecentSessionPages }} 页，共 {{ recentSessionsTotal }} 条</p>
            <div class="flex items-center gap-2">
              <Button variant="outline" class="rounded-2xl border-slate-200 bg-white/80" :disabled="sessionsPage <= 1" @click="changeSessionsPage('prev')">上一页</Button>
              <Button variant="outline" class="rounded-2xl border-slate-200 bg-white/80" :disabled="sessionsPage >= totalRecentSessionPages" @click="changeSessionsPage('next')">下一页</Button>
            </div>
          </div>
          <div v-if="recentSessions?.length" class="space-y-3">
            <div v-for="session in recentSessions" :key="session.session_id" class="rounded-[24px] border border-slate-200 p-4 transition hover:bg-slate-50/80">
              <div class="mb-3 flex flex-wrap items-center gap-2"><Badge variant="secondary" class="font-mono">{{ session.session_id }}</Badge><Badge variant="outline" class="font-mono">{{ session.visitor_id || '-' }}</Badge><span class="ml-auto text-xs text-muted-foreground">{{ formatDuration(session.duration || 0) }}</span></div>
              <div class="grid gap-2 text-xs text-muted-foreground"><div>入口：{{ truncateUrl(session.entry_url, 72) }}</div><div>退出：{{ truncateUrl(session.exit_url, 72) }}</div><div>活动：{{ formatTime(session.last_activity_at) }}<span v-if="session.ended_at"> / 结束：{{ formatTime(session.ended_at) }}</span></div></div>
            </div>
          </div>
          <p v-else class="py-8 text-center text-muted-foreground">暂无会话记录</p>
        </CardContent>
      </Card>
    </section>

    <section>
      <div class="mb-4">
        <h2 class="text-xl font-semibold tracking-[-0.03em] text-slate-950">最近访问</h2>
        <p class="mt-1 text-sm text-slate-500">用最近访问补足图表之外的上下文。</p>
      </div>
      <Card class="border-0 bg-white/80 shadow-[0_18px_45px_-36px_rgba(15,23,42,0.22)] backdrop-blur-xl">
        <CardContent class="p-4 sm:p-6">
          <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
            <p class="text-sm text-slate-500">第 {{ recentPage }} / {{ totalRecentPages }} 页，共 {{ recentVisitsTotal }} 条</p>
            <div class="flex items-center gap-2">
              <Button variant="outline" class="rounded-2xl border-slate-200 bg-white/80" :disabled="recentPage <= 1" @click="changeRecentPage('prev')">上一页</Button>
              <Button variant="outline" class="rounded-2xl border-slate-200 bg-white/80" :disabled="recentPage >= totalRecentPages" @click="changeRecentPage('next')">下一页</Button>
            </div>
          </div>
          <div v-if="loading" class="space-y-3"><div v-for="i in 5" :key="i" class="flex items-center gap-4 rounded-lg border p-3"><Skeleton class="h-5 w-20" /><Skeleton class="h-5 flex-1" /><Skeleton class="h-5 w-24" /></div></div>
          <div v-else-if="recentVisits?.length" class="space-y-2">
            <div v-for="visit in recentVisits" :key="visit.id" class="flex flex-wrap items-center gap-3 rounded-[24px] border border-slate-200 p-3 transition hover:bg-slate-50/80">
              <Badge variant="secondary" class="font-mono">{{ visit.ip || '-' }}</Badge>
              <Badge v-if="visit.country" variant="outline" class="text-xs">{{ visit.country }}</Badge>
              <span class="min-w-0 flex-1 truncate font-mono text-xs text-muted-foreground">{{ truncateUrl(visit.url, 56) }}</span>
              <div class="flex items-center gap-2 text-xs text-muted-foreground"><Badge variant="outline" class="text-xs">{{ visit.browser || '-' }}</Badge><Badge variant="outline" class="text-xs">{{ visit.os || '-' }}</Badge><Badge v-if="visit.device" variant="outline" class="text-xs">{{ visit.device }}</Badge></div>
              <span class="whitespace-nowrap text-xs text-muted-foreground">{{ formatTime(visit.created_at) }}</span>
            </div>
          </div>
          <p v-else class="py-8 text-center text-muted-foreground">暂无访问记录</p>
        </CardContent>
      </Card>
    </section>
  </div>
</template>

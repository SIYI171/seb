<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { echarts } from '../lib/echarts'
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Badge,
  Skeleton
} from '@/components/ui'
import { Eye, Users, TrendingUp, Globe, Clock } from 'lucide-vue-next'
import axios from 'axios'

interface Website {
  id: number
  name: string
  domain: string
}

interface Stats {
  pageviews: number
  visitors: number
  trend: Array<{ date: string; count: number }>
  browsers: Array<{ browser: string; count: number }>
  os: Array<{ os: string; count: number }>
  pages: Array<{ url: string; count: number }>
  countries: Array<{ country: string; count: number }>
}

interface RecentVisit {
  id: number
  url: string
  browser: string
  os: string
  device: string
  country: string
  created_at: string
}

const route = useRoute()
const token = route.params.token as string

const website = ref<Website | null>(null)
const stats = ref<Stats | null>(null)
const realtimeCount = ref(0)
const recentVisits = ref<RecentVisit[]>([])
const loading = ref(true)
const error = ref('')

const trendChartRef = ref<HTMLElement | null>(null)
const browserChartRef = ref<HTMLElement | null>(null)
const osChartRef = ref<HTMLElement | null>(null)
const countryChartRef = ref<HTMLElement | null>(null)

let trendChart: echarts.ECharts | null = null
let browserChart: echarts.ECharts | null = null
let osChart: echarts.ECharts | null = null
let countryChart: echarts.ECharts | null = null
let refreshTimer: number | null = null

const apiBase = import.meta.env.VITE_API_URL || ''

async function loadWebsite() {
  try {
    const res = await axios.get(`${apiBase}/api/share/${token}`)
    if (res.data.code === 200) {
      website.value = res.data.data
    } else {
      error.value = res.data.message || '加载失败'
    }
  } catch (e: any) {
    error.value = e.response?.data?.message || '加载失败'
  }
}

async function loadStats() {
  try {
    const res = await axios.get(`${apiBase}/api/share/${token}/stats`)
    if (res.data.code === 200) {
      stats.value = res.data.data
    }
  } catch (e) {
    console.error(e)
  }
}

async function loadRealtime() {
  try {
    const res = await axios.get(`${apiBase}/api/share/${token}/realtime`)
    if (res.data.code === 200) {
      realtimeCount.value = res.data.data
    }
  } catch (e) {
    console.error(e)
  }
}

async function loadRecentVisits() {
  try {
    const res = await axios.get(`${apiBase}/api/share/${token}/recent?limit=10`)
    if (res.data.code === 200) {
      recentVisits.value = res.data.data
    }
  } catch (e) {
    console.error(e)
  }
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
  return date.toLocaleDateString('zh-CN')
}

function renderCharts() {
  if (trendChartRef.value && stats.value?.trend?.length) {
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

  if (browserChartRef.value && stats.value?.browsers?.length) {
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

  if (osChartRef.value && stats.value?.os?.length) {
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

  if (countryChartRef.value && stats.value?.countries?.length) {
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
  if (!error.value) {
    await loadStats()
    await loadRealtime()
    await loadRecentVisits()
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
  }
})

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
  <div class="min-h-screen bg-slate-50 dark:bg-slate-900">
    <div class="max-w-7xl mx-auto p-4 lg:p-8">
      <div v-if="error" class="flex flex-col items-center justify-center min-h-[60vh]">
        <div class="w-16 h-16 rounded-2xl bg-red-100 dark:bg-red-900/30 flex items-center justify-center mb-4">
          <Globe class="w-8 h-8 text-red-500" />
        </div>
        <h2 class="text-xl font-semibold mb-2">{{ error }}</h2>
        <p class="text-muted-foreground">请检查链接是否正确或联系分享者</p>
      </div>

      <div v-else class="space-y-6">
        <div class="flex items-center gap-3">
          <img src="/logo.png" alt="SEB" class="w-8 h-8 rounded-lg flex-shrink-0" />
          <div class="min-w-0 flex-1">
            <h1 class="text-lg font-bold tracking-tight truncate">{{ website?.name || '统计数据' }}</h1>
            <p v-if="website" class="text-muted-foreground text-xs truncate">{{ website.domain }}</p>
          </div>
          <Badge variant="secondary" class="text-xs flex-shrink-0">
            公开
          </Badge>
        </div>

        <div class="grid gap-3 grid-cols-3">
          <Card class="p-0">
            <CardContent class="p-4">
              <div class="flex flex-col items-center text-center">
                <div class="w-10 h-10 rounded-full bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center mb-2">
                  <Eye class="w-5 h-5 text-blue-600 dark:text-blue-400" />
                </div>
                <p v-if="loading" class="text-2xl font-bold"><Skeleton class="h-7 w-12" /></p>
                <p v-else class="text-2xl font-bold">{{ formatNumber(stats?.pageviews || 0) }}</p>
                <p class="text-xs text-muted-foreground mt-1">浏览量</p>
              </div>
            </CardContent>
          </Card>

          <Card class="p-0">
            <CardContent class="p-4">
              <div class="flex flex-col items-center text-center">
                <div class="w-10 h-10 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center mb-2">
                  <Users class="w-5 h-5 text-green-600 dark:text-green-400" />
                </div>
                <p v-if="loading" class="text-2xl font-bold"><Skeleton class="h-7 w-12" /></p>
                <p v-else class="text-2xl font-bold">{{ formatNumber(stats?.visitors || 0) }}</p>
                <p class="text-xs text-muted-foreground mt-1">访客数</p>
              </div>
            </CardContent>
          </Card>

          <Card class="p-0">
            <CardContent class="p-4">
              <div class="flex flex-col items-center text-center">
                <div class="w-10 h-10 rounded-full bg-purple-100 dark:bg-purple-900/30 flex items-center justify-center mb-2">
                  <TrendingUp class="w-5 h-5 text-purple-600 dark:text-purple-400" />
                </div>
                <p v-if="loading" class="text-2xl font-bold"><Skeleton class="h-7 w-12" /></p>
                <p v-else class="text-2xl font-bold">{{ realtimeCount }}</p>
                <p class="text-xs text-muted-foreground mt-1">在线</p>
              </div>
            </CardContent>
          </Card>
        </div>

        <Card>
          <CardHeader class="pb-2">
            <CardTitle class="flex items-center gap-2 text-base">
              <TrendingUp class="w-4 h-4" />
              访问趋势
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div class="relative h-[250px]">
              <div v-show="loading" class="absolute inset-0"><Skeleton class="h-full w-full" /></div>
              <div v-show="!loading" ref="trendChartRef" class="h-full w-full"></div>
            </div>
          </CardContent>
        </Card>

        <div class="grid gap-4 grid-cols-1 sm:grid-cols-3">
          <Card>
            <CardHeader class="pb-2">
              <CardTitle class="text-sm">浏览器分布</CardTitle>
            </CardHeader>
            <CardContent>
              <div class="relative h-[180px]">
                <div v-show="loading" class="absolute inset-0"><Skeleton class="h-full w-full" /></div>
                <div v-show="!loading" ref="browserChartRef" class="h-full w-full"></div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader class="pb-2">
              <CardTitle class="text-sm">操作系统分布</CardTitle>
            </CardHeader>
            <CardContent>
              <div class="relative h-[180px]">
                <div v-show="loading" class="absolute inset-0"><Skeleton class="h-full w-full" /></div>
                <div v-show="!loading" ref="osChartRef" class="h-full w-full"></div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader class="pb-2">
              <CardTitle class="text-sm">地区分布</CardTitle>
            </CardHeader>
            <CardContent>
              <div class="relative h-[180px]">
                <div v-show="loading" class="absolute inset-0"><Skeleton class="h-full w-full" /></div>
                <div v-show="!loading" ref="countryChartRef" class="h-full w-full"></div>
              </div>
            </CardContent>
          </Card>
        </div>

        <Card>
          <CardHeader class="pb-2">
            <CardTitle class="flex items-center gap-2 text-base">
              <Clock class="w-4 h-4" />
              最近访问
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div v-if="loading" class="space-y-3">
              <div v-for="i in 5" :key="i" class="p-3 rounded-lg border">
                <Skeleton class="h-5 w-full mb-2" />
                <Skeleton class="h-4 w-32" />
              </div>
            </div>
            <div v-else-if="recentVisits?.length" class="space-y-2">
              <div
                v-for="visit in recentVisits"
                :key="visit.id"
                class="p-3 rounded-lg border hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors"
              >
                <div class="font-mono text-xs text-foreground truncate mb-2">
                  {{ truncateUrl(visit.url, 60) }}
                </div>
                <div class="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                  <Badge v-if="visit.country" variant="outline" class="text-xs">{{ visit.country }}</Badge>
                  <Badge variant="outline" class="text-xs">{{ visit.browser || '-' }}</Badge>
                  <Badge variant="outline" class="text-xs">{{ visit.os || '-' }}</Badge>
                  <span class="ml-auto">{{ formatTime(visit.created_at) }}</span>
                </div>
              </div>
            </div>
            <p v-else class="text-muted-foreground text-center py-8">暂无访问记录</p>
          </CardContent>
        </Card>

        <div class="text-center text-xs text-muted-foreground py-2">
          由 <a href="/" class="text-primary hover:underline">SEB Analytics</a> 提供统计
        </div>
      </div>
    </div>
  </div>
</template>

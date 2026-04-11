<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { echarts } from '../lib/echarts'
import { Card, CardContent, CardHeader, CardTitle, Badge, Skeleton } from '@/components/ui'
import { Activity, Eye, Globe2, Sparkles, TrendingUp, Users } from 'lucide-vue-next'
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

const summaryText = computed(() => {
  if (!stats.value) return '正在加载分享数据。'
  if (!stats.value.pageviews) return '当前时间范围内还没有公开可见的访问数据。'
  if (realtimeCount.value > 0) return `当前有 ${realtimeCount.value} 位访客正在浏览这个站点。`
  return '这里展示的是这个站点最近的访问趋势和基本访问构成。'
})

async function loadWebsite() {
  try {
    const res = await axios.get(`${apiBase}/api/share/${token}`)
    if (res.data.code === 200) website.value = res.data.data
    else error.value = res.data.message || '加载失败'
  } catch (e: any) {
    error.value = e.response?.data?.message || '加载失败'
  }
}

async function loadStats() {
  try {
    const res = await axios.get(`${apiBase}/api/share/${token}/stats`)
    if (res.data.code === 200) stats.value = res.data.data
  } catch (e) {
    console.error(e)
  }
}

async function loadRealtime() {
  try {
    const res = await axios.get(`${apiBase}/api/share/${token}/realtime`)
    if (res.data.code === 200) realtimeCount.value = res.data.data
  } catch (e) {
    console.error(e)
  }
}

async function loadRecentVisits() {
  try {
    const res = await axios.get(`${apiBase}/api/share/${token}/recent?limit=10`)
    if (res.data.code === 200) recentVisits.value = res.data.data
  } catch (e) {
    console.error(e)
  }
}

function formatNumber(num: number): string {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return num.toString()
}

function truncateUrl(url: string, maxLen = 48): string {
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
  return date.toLocaleDateString('zh-CN')
}

function renderCharts() {
  if (trendChartRef.value && stats.value?.trend?.length) {
    trendChart?.dispose()
    trendChart = echarts.init(trendChartRef.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '2%', right: '2%', bottom: '2%', containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: stats.value.trend.map((item) => item.date), axisLine: { lineStyle: { color: '#dbe3ee' } }, axisLabel: { color: '#64748b' } },
      yAxis: { type: 'value', axisLine: { show: false }, splitLine: { lineStyle: { color: '#e7edf5' } }, axisLabel: { color: '#64748b' } },
      series: [{
        name: '浏览量',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: stats.value.trend.map((item) => item.count),
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(59,130,246,0.22)' }, { offset: 1, color: 'rgba(59,130,246,0.03)' }]) },
        lineStyle: { color: '#3b82f6', width: 2 },
        itemStyle: { color: '#3b82f6' }
      }]
    })
    trendChart.resize()
  }

  const pieColors = ['#3b82f6', '#06b6d4', '#10b981', '#8b5cf6', '#f59e0b', '#ec4899', '#84cc16', '#ef4444']

  if (browserChartRef.value && stats.value?.browsers?.length) {
    browserChart?.dispose()
    browserChart = echarts.init(browserChartRef.value)
    browserChart.setOption({ tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' }, series: [{ type: 'pie', radius: ['48%', '74%'], itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 3 }, label: { show: false }, emphasis: { label: { show: true, fontSize: 12, fontWeight: 'bold' } }, data: stats.value.browsers.map((item, i) => ({ name: item.browser || '未知', value: item.count, itemStyle: { color: pieColors[i % pieColors.length] } })) }] })
    browserChart.resize()
  }

  if (osChartRef.value && stats.value?.os?.length) {
    osChart?.dispose()
    osChart = echarts.init(osChartRef.value)
    osChart.setOption({ tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' }, series: [{ type: 'pie', radius: ['48%', '74%'], itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 3 }, label: { show: false }, emphasis: { label: { show: true, fontSize: 12, fontWeight: 'bold' } }, data: stats.value.os.map((item, i) => ({ name: item.os || '未知', value: item.count, itemStyle: { color: pieColors[i % pieColors.length] } })) }] })
    osChart.resize()
  }

  if (countryChartRef.value && stats.value?.countries?.length) {
    countryChart?.dispose()
    countryChart = echarts.init(countryChartRef.value)
    countryChart.setOption({ tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' }, series: [{ type: 'pie', radius: ['48%', '74%'], itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 3 }, label: { show: false }, emphasis: { label: { show: true, fontSize: 12, fontWeight: 'bold' } }, data: stats.value.countries.map((item, i) => ({ name: item.country || '未知', value: item.count, itemStyle: { color: pieColors[i % pieColors.length] } })) }] })
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
    window.setTimeout(renderCharts, 100)
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
  <div class="apple-shell min-h-screen px-4 py-4 lg:px-8 lg:py-8">
    <div class="mx-auto max-w-7xl">
      <div v-if="error" class="apple-surface flex min-h-[70vh] flex-col items-center justify-center p-8 text-center sm:p-10">
        <div class="flex h-16 w-16 items-center justify-center rounded-[20px] bg-slate-100 text-slate-500"><Globe2 class="h-8 w-8" /></div>
        <h2 class="mt-6 text-[1.7rem] font-semibold tracking-[-0.04em] text-slate-950 sm:text-[2rem]">{{ error }}</h2>
        <p class="mt-2 max-w-md text-sm leading-6 text-slate-600">请检查链接是否正确，或联系分享者重新生成公开链接。</p>
      </div>

      <div v-else class="space-y-6">
        <section class="apple-hero pt-4 sm:pt-6 lg:pt-8">
          <div class="grid gap-8 xl:grid-cols-[1.08fr_0.92fr] xl:items-start">
            <div class="min-w-0">
              <span class="apple-label">
                <Sparkles class="h-3.5 w-3.5" />
                Public Report
              </span>
              <h1 class="mt-6 break-words apple-title sm:mt-8">{{ website?.name || '公开统计页' }}</h1>
              <p v-if="website" class="mt-3 break-all text-sm text-slate-500 sm:mt-4">{{ website.domain }}</p>
              <p class="mt-6 max-w-xl apple-subtitle">{{ summaryText }}</p>
            </div>

            <div class="grid gap-3 sm:grid-cols-3">
              <div class="apple-card"><div class="flex items-center justify-between text-slate-500"><span class="text-sm">浏览量</span><Eye class="h-4 w-4" /></div><p v-if="loading" class="mt-4"><Skeleton class="h-10 w-16" /></p><p v-else class="apple-stat-value">{{ formatNumber(stats?.pageviews || 0) }}</p></div>
              <div class="apple-card"><div class="flex items-center justify-between text-slate-500"><span class="text-sm">访客数</span><Users class="h-4 w-4" /></div><p v-if="loading" class="mt-4"><Skeleton class="h-10 w-16" /></p><p v-else class="apple-stat-value">{{ formatNumber(stats?.visitors || 0) }}</p></div>
              <div class="apple-card"><div class="flex items-center justify-between text-slate-500"><span class="text-sm">在线</span><Activity class="h-4 w-4" /></div><p v-if="loading" class="mt-4"><Skeleton class="h-10 w-16" /></p><p v-else class="apple-stat-value">{{ realtimeCount }}</p></div>
            </div>
          </div>
        </section>

        <section class="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
          <div class="apple-surface p-4 sm:p-6">
            <div class="mb-4 flex items-center gap-2"><TrendingUp class="h-5 w-5 text-slate-500" /><h2 class="text-[1.8rem] font-semibold tracking-[-0.04em] text-slate-950">访问趋势</h2></div>
            <div class="relative h-[240px] sm:h-[300px]"><div v-show="loading" class="absolute inset-0"><Skeleton class="h-full w-full" /></div><div v-show="!loading" ref="trendChartRef" class="h-full w-full"></div></div>
          </div>

          <div>
            <div class="apple-card h-full">
              <p class="text-sm text-slate-500">公开状态</p>
              <p class="mt-4 text-[1.55rem] font-semibold leading-tight tracking-[-0.04em] text-slate-950 sm:text-[1.8rem]">{{ realtimeCount > 0 ? '这份数据现在是活的' : '这份数据更适合回看' }}</p>
              <p class="mt-4 apple-copy">{{ realtimeCount > 0 ? '当前有访客正在浏览，你看到的是接近实时的公开状态。' : '当前没有明显实时流量，这页更适合用于趋势回顾和对外汇报。' }}</p>
            </div>
          </div>
        </section>

        <section>
          <div class="mb-4"><h2 class="apple-section-title">访问构成</h2><p class="mt-1 max-w-2xl text-sm leading-6 text-slate-500">从浏览器、系统和地区三个角度快速理解受众轮廓。</p></div>
          <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            <Card class="border-0 bg-white/80 shadow-[0_18px_45px_-36px_rgba(15,23,42,0.22)] backdrop-blur-xl">
              <CardHeader class="pb-2">
                <CardTitle class="text-sm">浏览器分布</CardTitle>
              </CardHeader>
              <CardContent>
                <div class="relative h-[200px] sm:h-[240px]">
                  <div v-show="loading" class="absolute inset-0"><Skeleton class="h-full w-full" /></div>
                  <div v-show="!loading" ref="browserChartRef" class="h-full w-full"></div>
                </div>
              </CardContent>
            </Card>
            <Card class="border-0 bg-white/80 shadow-[0_18px_45px_-36px_rgba(15,23,42,0.22)] backdrop-blur-xl">
              <CardHeader class="pb-2">
                <CardTitle class="text-sm">操作系统分布</CardTitle>
              </CardHeader>
              <CardContent>
                <div class="relative h-[200px] sm:h-[240px]">
                  <div v-show="loading" class="absolute inset-0"><Skeleton class="h-full w-full" /></div>
                  <div v-show="!loading" ref="osChartRef" class="h-full w-full"></div>
                </div>
              </CardContent>
            </Card>
            <Card class="border-0 bg-white/80 shadow-[0_18px_45px_-36px_rgba(15,23,42,0.22)] backdrop-blur-xl md:col-span-2 xl:col-span-1">
              <CardHeader class="pb-2">
                <CardTitle class="text-sm">地区分布</CardTitle>
              </CardHeader>
              <CardContent>
                <div class="relative h-[200px] sm:h-[240px]">
                  <div v-show="loading" class="absolute inset-0"><Skeleton class="h-full w-full" /></div>
                  <div v-show="!loading" ref="countryChartRef" class="h-full w-full"></div>
                </div>
              </CardContent>
            </Card>
          </div>
        </section>

        <section>
          <div class="mb-4"><h2 class="apple-section-title">最近访问</h2><p class="mt-1 max-w-2xl text-sm leading-6 text-slate-500">结合页面、设备和时间，快速了解最近发生了什么访问行为。</p></div>
          <Card class="border-0 bg-white/80 shadow-[0_18px_45px_-36px_rgba(15,23,42,0.22)] backdrop-blur-xl">
            <CardContent class="p-4 sm:p-6">
              <div v-if="loading" class="space-y-3"><div v-for="i in 5" :key="i" class="rounded-lg border p-3"><Skeleton class="mb-2 h-5 w-full" /><Skeleton class="h-4 w-32" /></div></div>
              <div v-else-if="recentVisits?.length" class="space-y-2">
                <div v-for="visit in recentVisits" :key="visit.id" class="rounded-[24px] border border-slate-200 p-3 transition hover:bg-slate-50/80">
                  <div class="mb-2 break-all font-mono text-xs leading-5 text-slate-900 sm:truncate">{{ truncateUrl(visit.url, 64) }}</div>
                  <div class="flex flex-wrap items-center gap-2 text-xs text-slate-500">
                    <Badge v-if="visit.country" variant="outline" class="text-xs">{{ visit.country }}</Badge>
                    <Badge variant="outline" class="text-xs">{{ visit.browser || '-' }}</Badge>
                    <Badge variant="outline" class="text-xs">{{ visit.os || '-' }}</Badge>
                    <span class="w-full pt-1 text-right sm:ml-auto sm:w-auto sm:pt-0">{{ formatTime(visit.created_at) }}</span>
                  </div>
                </div>
              </div>
              <p v-else class="py-8 text-center text-muted-foreground">暂无访问记录</p>
            </CardContent>
          </Card>
        </section>

        <footer class="pb-4 pt-1">
          <div class="apple-attribution">
            <span>Public report by SEB Analytics</span>
            <span>Author: SIYI171</span>
            <a href="https://github.com/SIYI171/seb" target="_blank" rel="noreferrer">github.com/SIYI171/seb</a>
          </div>
        </footer>
      </div>
    </div>
  </div>
</template>

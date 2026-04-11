<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Button, Dialog, Input, Label, Badge, Skeleton } from '@/components/ui'
import { BarChart3, Check, Copy, Globe2, Link2, Plus, Share2, Trash2, Wrench } from 'lucide-vue-next'
import api from '../api'

interface Website {
  id: number
  name: string
  domain: string
  trackingId: string
  shareToken: string | null
  createdAt: string
}

const router = useRouter()
const websites = ref<Website[]>([])
const loading = ref(true)
const showDialog = ref(false)
const shareDialog = ref(false)
const shareSite = ref<Website | null>(null)
const copied = ref<string | null>(null)
const form = ref({ name: '', domain: '' })
const submitting = ref(false)

const sharedCount = computed(() => websites.value.filter((site) => site.shareToken).length)

async function loadWebsites() {
  try {
    const res = await api.get('/websites')
    if (res.code === 200) websites.value = res.data
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!form.value.name || !form.value.domain) return
  submitting.value = true
  try {
    const res = await api.post('/websites', form.value)
    if (res.code === 200) {
      showDialog.value = false
      form.value = { name: '', domain: '' }
      await loadWebsites()
    }
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  if (!confirm('确定要删除该网站吗？所有统计数据将被删除。')) return
  try {
    await api.delete(`/websites/${id}`)
    await loadWebsites()
  } catch (e) {
    console.error(e)
  }
}

async function enableShare() {
  if (!shareSite.value) return
  try {
    const res = await api.post(`/websites/${shareSite.value.id}/share`)
    if (res.code === 200) {
      shareSite.value.shareToken = res.data
      await loadWebsites()
    }
  } catch (e) {
    console.error(e)
  }
}

async function disableShare() {
  if (!shareSite.value) return
  try {
    await api.delete(`/websites/${shareSite.value.id}/share`)
    shareSite.value.shareToken = null
    shareDialog.value = false
    await loadWebsites()
  } catch (e) {
    console.error(e)
  }
}

function openShareDialog(site: Website) {
  shareSite.value = site
  shareDialog.value = true
}

function goToStats(id: number) {
  router.push(`/stats/${id}`)
}

function getTrackingCode(trackingId: string) {
  const origin = window.location.origin
  return `<script src="${origin}/tracker/seb.js" data-tracking-id="${trackingId}"><\\/script>`
}

function getShareUrl(token: string) {
  return `${window.location.origin}/share/${token}`
}

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

async function copyToClipboard(text: string) {
  try {
    await navigator.clipboard.writeText(text)
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
  }
  copied.value = text
  setTimeout(() => {
    copied.value = null
  }, 2000)
}

onMounted(loadWebsites)
</script>

<template>
  <div class="space-y-6">
    <section class="apple-hero">
      <div class="grid gap-8 xl:grid-cols-[1.12fr_0.88fr] xl:items-start">
        <div class="min-w-0">
          <span class="apple-label">
            <Wrench class="h-3.5 w-3.5" />
            Site Configuration
          </span>
          <h2 class="mt-6 max-w-2xl apple-title">添加站点，复制脚本，按需分享。</h2>
          <div class="mt-8">
            <Button class="w-full rounded-2xl bg-slate-950 text-white hover:bg-slate-800 sm:w-auto" @click="showDialog = true">
              <Plus class="mr-2 h-4 w-4" />
              添加网站
            </Button>
          </div>
        </div>

        <div class="grid gap-4 sm:grid-cols-3 xl:grid-cols-1">
          <div class="apple-card">
            <div class="flex items-center justify-between text-slate-500">
              <span class="text-sm">站点总数</span>
              <Globe2 class="h-4 w-4" />
            </div>
            <p v-if="loading" class="mt-4"><Skeleton class="h-10 w-20" /></p>
            <p v-else class="apple-stat-value">{{ websites.length }}</p>
          </div>
          <div class="apple-card">
            <div class="flex items-center justify-between text-slate-500">
              <span class="text-sm">分享已开启</span>
              <Share2 class="h-4 w-4" />
            </div>
            <p v-if="loading" class="mt-4"><Skeleton class="h-10 w-20" /></p>
            <p v-else class="apple-stat-value">{{ sharedCount }}</p>
          </div>
          <div class="apple-card">
            <p class="text-sm text-slate-500">流程</p>
            <p class="mt-4 text-sm leading-7 text-slate-600">建站点，装脚本，确认数据，再开分享。</p>
          </div>
        </div>
      </div>
    </section>

    <div v-if="loading" class="grid gap-5">
      <div v-for="i in 3" :key="i" class="apple-surface p-6">
        <Skeleton class="h-8 w-40" />
        <Skeleton class="mt-3 h-5 w-60" />
        <Skeleton class="mt-6 h-28 w-full" />
      </div>
    </div>

    <div v-else-if="!websites.length" class="apple-surface p-10 text-center sm:p-16">
      <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-[20px] bg-slate-100 text-slate-500">
        <Globe2 class="h-8 w-8" />
      </div>
      <h3 class="mt-6 text-[1.7rem] font-semibold tracking-[-0.04em] text-slate-950 sm:text-[2rem]">还没有网站</h3>
      <p class="mx-auto mt-3 max-w-lg text-sm leading-6 text-slate-600 sm:leading-7">先添加一个站点，再把追踪脚本放到目标页面中，数据就会开始流入控制台。</p>
      <Button class="mt-8 rounded-2xl bg-slate-950 text-white hover:bg-slate-800" @click="showDialog = true">
        <Plus class="mr-2 h-4 w-4" />
        添加第一个网站
      </Button>
    </div>

    <div v-else class="space-y-5">
      <div v-for="site in websites" :key="site.id" class="apple-surface overflow-hidden">
        <div class="border-b border-slate-200/80 px-4 py-5 sm:px-6">
          <div class="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
            <div class="min-w-0">
              <div class="flex flex-wrap items-center gap-2">
                <h3 class="break-words text-[1.45rem] font-semibold tracking-[-0.04em] text-slate-950 sm:text-[1.8rem]">{{ site.name }}</h3>
                <Badge variant="outline">创建于 {{ formatDate(site.createdAt) }}</Badge>
                <Badge v-if="site.shareToken" variant="success">分享已启用</Badge>
              </div>
              <p class="mt-2 break-all text-sm text-slate-500">{{ site.domain }}</p>
            </div>

            <div class="grid w-full gap-2 sm:flex sm:w-auto sm:flex-wrap">
              <Button variant="outline" class="w-full rounded-2xl border-slate-200 bg-white/80 sm:w-auto" @click="goToStats(site.id)">
                <BarChart3 class="mr-2 h-4 w-4" />
                查看统计
              </Button>
              <Button variant="outline" class="w-full rounded-2xl border-slate-200 bg-white/80 sm:w-auto" @click="openShareDialog(site)">
                <Share2 class="mr-2 h-4 w-4" />
                分享设置
              </Button>
              <Button variant="ghost" class="w-full rounded-2xl text-destructive hover:bg-red-50 hover:text-destructive sm:w-auto" @click="handleDelete(site.id)">
                <Trash2 class="mr-2 h-4 w-4" />
                删除
              </Button>
            </div>
          </div>
        </div>

        <div class="grid gap-5 p-4 sm:gap-6 sm:p-6 xl:grid-cols-[1.2fr_0.8fr]">
          <div class="space-y-4">
            <div class="apple-soft min-w-0 overflow-hidden">
              <div class="flex items-center justify-between gap-3">
                <p class="text-sm font-medium text-slate-950">追踪脚本</p>
                <Button variant="ghost" size="icon" class="h-8 w-8 flex-shrink-0 rounded-xl" @click="copyToClipboard(getTrackingCode(site.trackingId))">
                  <Check v-if="copied === getTrackingCode(site.trackingId)" class="h-4 w-4 text-green-500" />
                  <Copy v-else class="h-4 w-4" />
                </Button>
              </div>
              <div class="apple-code-scroll mt-3 max-w-full overflow-x-auto rounded-[20px] bg-white">
                <code class="block max-w-full whitespace-pre-wrap break-all px-4 py-4 font-mono text-xs leading-6 text-slate-700 sm:w-max sm:min-w-full sm:whitespace-nowrap">
                  {{ getTrackingCode(site.trackingId) }}
                </code>
              </div>
            </div>
          </div>

          <div class="min-w-0 space-y-4">
            <div class="apple-soft min-w-0 overflow-hidden">
              <p class="text-sm font-medium text-slate-950">当前状态</p>
              <div class="mt-4 space-y-3 text-sm">
                <div class="grid gap-2 sm:grid-cols-[auto,minmax(0,1fr)] sm:items-start sm:gap-4">
                  <span class="min-w-0 text-slate-500">追踪 ID</span>
                  <code class="block max-w-full break-all rounded-xl bg-white px-3 py-2 text-xs leading-5 sm:text-right">{{ site.trackingId }}</code>
                </div>
                <div class="grid gap-2 sm:grid-cols-[auto,minmax(0,1fr)] sm:items-center sm:gap-4">
                  <span class="text-slate-500">分享状态</span>
                  <span class="font-medium text-slate-900 sm:text-right">{{ site.shareToken ? '已启用' : '未启用' }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <Dialog v-model:open="showDialog">
      <div class="space-y-6">
        <div>
          <h2 class="text-[1.6rem] font-semibold tracking-[-0.04em] text-slate-950">添加网站</h2>
          <p class="mt-2 text-sm text-slate-500">填写后立即生成追踪 ID。</p>
        </div>
        <div class="space-y-4">
          <div class="space-y-2">
            <Label label="网站名称" class="text-slate-600" />
            <Input v-model="form.name" placeholder="我的网站" class="apple-input" />
          </div>
          <div class="space-y-2">
            <Label label="域名" class="text-slate-600" />
            <Input v-model="form.domain" placeholder="example.com" class="apple-input" />
          </div>
        </div>
        <div class="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <Button variant="outline" class="rounded-2xl border-slate-200 bg-white/80" @click="showDialog = false">取消</Button>
          <Button class="rounded-2xl bg-slate-950 text-white hover:bg-slate-800" :loading="submitting" @click="handleCreate">添加</Button>
        </div>
      </div>
    </Dialog>

    <Dialog v-model:open="shareDialog">
      <div class="space-y-6">
        <div>
          <h2 class="text-[1.6rem] font-semibold tracking-[-0.04em] text-slate-950">分享设置</h2>
          <p class="mt-2 text-sm text-slate-500">生成公开链接供外部查看。</p>
        </div>
        <div v-if="shareSite" class="space-y-4">
          <div class="rounded-2xl bg-slate-50 p-4">
            <p class="font-medium text-slate-900">{{ shareSite.name }}</p>
            <p class="mt-1 text-sm text-slate-500">{{ shareSite.domain }}</p>
          </div>

          <div v-if="shareSite.shareToken" class="space-y-3">
            <Badge variant="success">分享已启用</Badge>
            <div class="rounded-2xl bg-slate-50 p-3 font-mono text-xs text-slate-700">
              <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <code class="break-all leading-6">{{ getShareUrl(shareSite.shareToken) }}</code>
                <Button variant="ghost" size="icon" class="h-8 w-8 flex-shrink-0 self-end sm:self-auto" @click="copyToClipboard(getShareUrl(shareSite.shareToken))">
                  <Check v-if="copied === getShareUrl(shareSite.shareToken)" class="h-4 w-4 text-green-500" />
                  <Copy v-else class="h-4 w-4" />
                </Button>
              </div>
            </div>
            <Button variant="outline" class="w-full rounded-2xl text-destructive hover:bg-red-50 hover:text-destructive" @click="disableShare">
              <Trash2 class="mr-2 h-4 w-4" />
              关闭分享
            </Button>
          </div>

          <div v-else class="space-y-3">
            <p class="text-sm text-slate-500">启用后会生成公开地址。</p>
            <Button class="w-full rounded-2xl bg-slate-950 text-white hover:bg-slate-800" @click="enableShare">
              <Link2 class="mr-2 h-4 w-4" />
              生成分享链接
            </Button>
          </div>
        </div>
        <div class="flex justify-end">
          <Button variant="outline" class="rounded-2xl border-slate-200 bg-white/80" @click="shareDialog = false">关闭</Button>
        </div>
      </div>
    </Dialog>
  </div>
</template>

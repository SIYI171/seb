<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { 
  Card, CardContent, 
  Button, Input, Label, Dialog,
  Badge, Skeleton
} from '@/components/ui'
import { Plus, Trash2, BarChart3, Copy, Check, ExternalLink, Globe, Share2, Link2, X } from 'lucide-vue-next'
import api from '../api'

interface Website {
  id: number
  name: string
  domain: string
  trackingId: string
  shareToken: string
  createdAt: string
}

const router = useRouter()
const websites = ref<Website[]>([])
const loading = ref(true)
const showDialog = ref(false)
const copied = ref<string | null>(null)
const form = ref({ name: '', domain: '' })
const submitting = ref(false)
const shareDialog = ref(false)
const shareSite = ref<Website | null>(null)

async function loadWebsites() {
  try {
    const res = await api.get('/websites')
    if (res.code === 200) {
      websites.value = res.data
    }
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

function getTrackingCode(trackingId: string): string {
  const origin = window.location.origin
  return `<script src="${origin}/tracker/seb.js" data-tracking-id="${trackingId}"><\/script>`
}

function getShareUrl(token: string): string {
  return `${window.location.origin}/share/${token}`
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
  setTimeout(() => { copied.value = null }, 2000)
}

function goToStats(id: number) {
  router.push(`/stats/${id}`)
}

function openShareDialog(site: Website) {
  shareSite.value = site
  shareDialog.value = true
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
    shareSite.value.shareToken = ''
    await loadWebsites()
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  loadWebsites()
})
</script>

<template>
  <div class="space-y-8">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-3xl font-bold tracking-tight">网站管理</h1>
        <p class="text-muted-foreground mt-1">添加和管理您的网站</p>
      </div>
      <Button @click="showDialog = true">
        <Plus class="w-4 h-4 mr-2" />
        添加网站
      </Button>
    </div>

    <div v-if="loading" class="space-y-4">
      <div v-for="i in 3" :key="i" class="p-6 rounded-xl border">
        <div class="flex items-start justify-between">
          <div class="space-y-3 flex-1">
            <Skeleton class="h-6 w-40" />
            <Skeleton class="h-4 w-60" />
            <Skeleton class="h-10 w-full max-w-lg" />
          </div>
          <div class="flex gap-2">
            <Skeleton class="h-9 w-20" />
            <Skeleton class="h-9 w-9" />
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="websites.length === 0" class="text-center py-16">
      <div class="w-16 h-16 rounded-2xl bg-muted flex items-center justify-center mx-auto mb-4">
        <Globe class="w-8 h-8 text-muted-foreground" />
      </div>
      <h3 class="text-lg font-semibold mb-2">还没有网站</h3>
      <p class="text-muted-foreground mb-4">添加您的第一个网站开始追踪数据</p>
      <Button @click="showDialog = true">
        <Plus class="w-4 h-4 mr-2" />
        添加网站
      </Button>
    </div>

    <div v-else class="space-y-4">
      <Card v-for="site in websites" :key="site.id">
        <CardContent class="p-6">
          <div class="flex flex-col lg:flex-row lg:items-start justify-between gap-4">
            <div class="space-y-3 flex-1">
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-lg bg-gradient-to-br from-primary/20 to-primary/5 flex items-center justify-center">
                  <Globe class="w-5 h-5 text-primary" />
                </div>
                <div>
                  <div class="flex items-center gap-2">
                    <h3 class="font-semibold text-lg">{{ site.name }}</h3>
                    <Badge v-if="site.shareToken" variant="secondary" class="text-xs">
                      已分享
                    </Badge>
                  </div>
                  <a 
                    :href="'https://' + site.domain" 
                    target="_blank"
                    class="text-sm text-muted-foreground hover:text-primary flex items-center gap-1"
                  >
                    {{ site.domain }}
                    <ExternalLink class="w-3 h-3" />
                  </a>
                </div>
              </div>
              
              <div class="bg-slate-100 dark:bg-slate-800 rounded-lg p-3 font-mono text-xs overflow-x-auto">
                <div class="flex items-center justify-between gap-2">
                  <code class="text-slate-600 dark:text-slate-300 break-all">{{ getTrackingCode(site.trackingId) }}</code>
                  <Button 
                    variant="ghost" 
                    size="icon"
                    class="flex-shrink-0"
                    @click="copyToClipboard(getTrackingCode(site.trackingId))"
                  >
                    <Check v-if="copied === getTrackingCode(site.trackingId)" class="w-4 h-4 text-green-500" />
                    <Copy v-else class="w-4 h-4" />
                  </Button>
                </div>
              </div>
              
              <p class="text-xs text-muted-foreground">
                追踪 ID: <code class="bg-muted px-1.5 py-0.5 rounded">{{ site.trackingId }}</code>
              </p>
            </div>
            
            <div class="flex gap-2 lg:flex-col">
              <Button variant="outline" @click="goToStats(site.id)">
                <BarChart3 class="w-4 h-4 mr-2" />
                查看统计
              </Button>
              <Button variant="outline" @click="openShareDialog(site)">
                <Share2 class="w-4 h-4 mr-2" />
                分享
              </Button>
              <Button variant="ghost" class="text-destructive hover:text-destructive" @click="handleDelete(site.id)">
                <Trash2 class="w-4 h-4 mr-2" />
                删除
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>

    <Dialog v-model:open="showDialog">
      <div class="space-y-6">
        <div>
          <h2 class="text-lg font-semibold">添加网站</h2>
          <p class="text-sm text-muted-foreground">添加一个新网站来开始追踪数据</p>
        </div>
        
        <div class="space-y-4">
          <div class="space-y-2">
            <Label label="网站名称" />
            <Input v-model="form.name" placeholder="我的网站" />
          </div>
          <div class="space-y-2">
            <Label label="域名" />
            <Input v-model="form.domain" placeholder="example.com" />
          </div>
        </div>
        
        <div class="flex justify-end gap-3">
          <Button variant="outline" @click="showDialog = false">取消</Button>
          <Button :loading="submitting" @click="handleCreate">添加</Button>
        </div>
      </div>
    </Dialog>

    <Dialog v-model:open="shareDialog">
      <div class="space-y-6">
        <div>
          <h2 class="text-lg font-semibold">分享统计</h2>
          <p class="text-sm text-muted-foreground">生成公开链接，让游客查看统计数据</p>
        </div>
        
        <div v-if="shareSite" class="space-y-4">
          <div class="flex items-center gap-3 p-3 bg-slate-100 dark:bg-slate-800 rounded-lg">
            <Globe class="w-5 h-5 text-muted-foreground" />
            <div>
              <p class="font-medium">{{ shareSite.name }}</p>
              <p class="text-sm text-muted-foreground">{{ shareSite.domain }}</p>
            </div>
          </div>
          
          <div v-if="shareSite.shareToken" class="space-y-3">
            <div class="flex items-center gap-2">
              <Badge variant="default" class="bg-green-500">分享已启用</Badge>
            </div>
            
            <div class="bg-slate-100 dark:bg-slate-800 rounded-lg p-3 font-mono text-xs">
              <div class="flex items-center justify-between gap-2">
                <code class="text-slate-600 dark:text-slate-300 break-all">{{ getShareUrl(shareSite.shareToken) }}</code>
                <Button 
                  variant="ghost" 
                  size="icon"
                  class="flex-shrink-0"
                  @click="copyToClipboard(getShareUrl(shareSite.shareToken))"
                >
                  <Check v-if="copied === getShareUrl(shareSite.shareToken)" class="w-4 h-4 text-green-500" />
                  <Copy v-else class="w-4 h-4" />
                </Button>
              </div>
            </div>
            
            <Button variant="outline" class="w-full text-destructive hover:text-destructive" @click="disableShare">
              <X class="w-4 h-4 mr-2" />
              关闭分享
            </Button>
          </div>
          
          <div v-else class="space-y-3">
            <p class="text-sm text-muted-foreground">
              启用分享后将生成一个公开链接，任何人都可以查看该网站的统计数据。
            </p>
            <Button class="w-full" @click="enableShare">
              <Link2 class="w-4 h-4 mr-2" />
              生成分享链接
            </Button>
          </div>
        </div>
        
        <div class="flex justify-end">
          <Button variant="outline" @click="shareDialog = false">关闭</Button>
        </div>
      </div>
    </Dialog>
  </div>
</template>

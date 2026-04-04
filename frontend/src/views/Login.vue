<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { Button, Card, CardContent, Input, Label } from '@/components/ui'
import { Eye, EyeOff, BarChart3 } from 'lucide-vue-next'
import api from '../api'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const showPassword = ref(false)
const form = ref({
  username: '',
  password: ''
})

async function handleLogin() {
  if (!form.value.username || !form.value.password) {
    return
  }
  
  loading.value = true
  try {
    const res = await api.post('/admin/login', form.value)
    if (res.code === 200) {
      authStore.setToken(res.data.token)
      router.push('/')
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 p-4">
    <Card class="w-full max-w-md">
      <CardContent class="p-8">
        <div class="flex flex-col items-center mb-8">
          <img src="/logo.png" alt="SEB" class="w-16 h-16 rounded-2xl mb-4" />
          <h1 class="text-2xl font-bold">SEB Analytics</h1>
          <p class="text-muted-foreground mt-1">登录到您的控制台</p>
        </div>
        
        <form @submit.prevent="handleLogin" class="space-y-4">
          <div class="space-y-2">
            <Label label="用户名" />
            <Input v-model="form.username" placeholder="请输入用户名" />
          </div>
          
          <div class="space-y-2">
            <Label label="密码" />
            <div class="relative">
              <Input 
                v-model="form.password" 
                :type="showPassword ? 'text' : 'password'" 
                placeholder="请输入密码"
                class="pr-10"
              />
              <button 
                type="button"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                @click="showPassword = !showPassword"
              >
                <Eye v-if="!showPassword" class="w-4 h-4" />
                <EyeOff v-else class="w-4 h-4" />
              </button>
            </div>
          </div>
          
          <Button type="submit" :loading="loading" class="w-full">
            登录
          </Button>
        </form>
      </CardContent>
    </Card>
  </div>
</template>

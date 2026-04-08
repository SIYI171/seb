<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { Button, Input, Label } from '@/components/ui'
import { AlertCircle, ArrowRight, Eye, EyeOff } from 'lucide-vue-next'
import api from '../api'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const showPassword = ref(false)
const form = ref({ username: '', password: '' })
const errorMessage = ref('')

function clearError() {
  if (errorMessage.value) {
    errorMessage.value = ''
  }
}

async function handleLogin() {
  const username = form.value.username.trim()
  const password = form.value.password

  if (!username) {
    errorMessage.value = '请输入用户名'
    return
  }

  if (!password) {
    errorMessage.value = '请输入密码'
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    const res = await api.post('/admin/login', {
      username,
      password
    })
    if (res.code === 200) {
      authStore.setToken(res.data.token)
      router.push('/')
      return
    }

    errorMessage.value = res.message || '登录失败，请检查账号或密码'
  } catch (e) {
    const message = (e as any)?.response?.data?.message
    errorMessage.value = message || '网络异常，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="apple-shell flex min-h-screen items-center justify-center px-4 py-6 sm:px-6 lg:px-8">
    <section class="apple-surface w-full max-w-md px-5 py-6 sm:px-7 sm:py-8 lg:p-10">
      <div class="w-full">
        <div class="mb-8 flex items-center gap-4">
          <div class="flex h-14 w-14 items-center justify-center overflow-hidden rounded-[18px] bg-white shadow-[0_18px_40px_-24px_rgba(15,23,42,0.24)] ring-1 ring-slate-200/80">
            <img src="/logo.png" alt="SEB Logo" class="h-full w-full object-contain p-1.5" />
          </div>
          <div>
            <h1 class="text-[1.5rem] font-semibold tracking-[-0.04em] text-slate-950">SEB Analytics</h1>
            <p class="mt-1 text-sm text-slate-500">请输入账号信息</p>
          </div>
        </div>

        <form class="space-y-5" @submit.prevent="handleLogin">
          <div class="space-y-2.5">
            <Label label="用户名" class="text-slate-600" />
            <Input
              v-model="form.username"
              placeholder="请输入用户名"
              :class="[
                'apple-input',
                errorMessage ? 'border-amber-200 bg-amber-50/60 focus:border-amber-300 focus:ring-amber-100' : ''
              ]"
              @input="clearError"
            />
          </div>

          <div class="space-y-2.5">
            <Label label="密码" class="text-slate-600" />
            <div class="relative">
              <Input
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="请输入密码"
                :class="[
                  'apple-input pr-11',
                  errorMessage ? 'border-amber-200 bg-amber-50/60 focus:border-amber-300 focus:ring-amber-100' : ''
                ]"
                @input="clearError"
              />
              <button type="button" class="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 transition hover:text-slate-700" @click="showPassword = !showPassword">
                <Eye v-if="!showPassword" class="h-4 w-4" />
                <EyeOff v-else class="h-4 w-4" />
              </button>
            </div>
          </div>

          <Transition
            enter-active-class="transition-all duration-200"
            enter-from-class="translate-y-1 opacity-0"
            enter-to-class="translate-y-0 opacity-100"
            leave-active-class="transition-all duration-150"
            leave-from-class="translate-y-0 opacity-100"
            leave-to-class="translate-y-1 opacity-0"
          >
            <div
              v-if="errorMessage"
              class="flex items-center gap-2 rounded-2xl border border-amber-200/80 bg-white/85 px-4 py-3 text-sm text-slate-600 shadow-[0_10px_30px_-24px_rgba(15,23,42,0.25)]"
            >
              <AlertCircle class="h-4 w-4 flex-shrink-0 text-amber-500" />
              <span>{{ errorMessage }}</span>
            </div>
          </Transition>

          <Button type="submit" :loading="loading" class="h-12 w-full rounded-2xl bg-slate-950 text-white hover:bg-slate-800">
            登录
            <ArrowRight class="ml-2 h-4 w-4" />
          </Button>
        </form>
      </div>
    </section>
  </div>
</template>

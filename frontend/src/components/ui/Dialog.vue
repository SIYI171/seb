<script setup lang="ts">
import { cn } from '@/lib/utils'

interface Props {
  class?: string
  open?: boolean
}

const props = defineProps<Props>()
const emit = defineEmits(['update:open'])

function close() {
  emit('update:open', false)
}
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition-opacity duration-250"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-200"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div v-if="open" class="fixed inset-0 z-50 bg-slate-950/28 backdrop-blur-sm" @click="close"></div>
    </Transition>
    <Transition
      enter-active-class="transition-all duration-250 ease-out"
      enter-from-class="translate-y-2 opacity-0 scale-[0.985]"
      enter-to-class="opacity-100 scale-100"
      leave-active-class="transition-all duration-180 ease-in"
      leave-from-class="opacity-100 scale-100"
      leave-to-class="translate-y-2 opacity-0 scale-[0.985]"
    >
      <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6">
        <div :class="cn('max-h-[calc(100vh-2rem)] overflow-y-auto', props.class || 'apple-dialog')" @click.stop>
          <slot />
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

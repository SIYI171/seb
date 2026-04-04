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
      enter-active-class="transition-opacity duration-200"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-200"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div v-if="open" class="fixed inset-0 z-50 bg-black/80" @click="close"></div>
    </Transition>
    <Transition
      enter-active-class="transition-all duration-200"
      enter-from-class="opacity-0 scale-95"
      enter-to-class="opacity-100 scale-100"
      leave-active-class="transition-all duration-200"
      leave-from-class="opacity-100 scale-100"
      leave-to-class="opacity-0 scale-95"
    >
      <div v-if="open" class="fixed left-1/2 top-1/2 z-50 -translate-x-1/2 -translate-y-1/2">
        <div :class="cn('w-full max-w-lg rounded-lg border bg-background p-6 shadow-lg', props.class)">
          <slot />
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/share/:token',
    name: 'Share',
    component: () => import('../views/Share.vue')
  },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue')
      },
      {
        path: 'websites',
        name: 'Websites',
        component: () => import('../views/Websites.vue')
      },
      {
        path: 'stats/:id',
        name: 'Stats',
        component: () => import('../views/Stats.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  const publicPages = ['/login', '/share']
  const isPublic = publicPages.some(path => to.path.startsWith(path))
  
  if (!isPublic && !authStore.token) {
    next('/login')
  } else {
    next()
  }
})

export default router

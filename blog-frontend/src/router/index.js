import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Auth.vue'),
    meta: { public: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Auth.vue'),
    meta: { public: true }
  },
  // 手机端首页
  {
    path: '/m/home',
    name: 'MobileHome',
    component: () => import('@/views/MobileHome.vue'),
    meta: { title: '首页', public: true }
  },
  // 手机端文章列表
  {
    path: '/m/articles',
    name: 'MobileArticles',
    component: () => import('@/views/MobileArticles.vue'),
    meta: { title: '文章', public: true }
  },
  // 手机端分类
  {
    path: '/m/categories',
    name: 'MobileCategories',
    component: () => import('@/views/MobileCategories.vue'),
    meta: { title: '分类', public: true }
  },
  // 手机端 AI 助手
  {
    path: '/m/ai',
    name: 'MobileAI',
    component: () => import('@/views/MobileAI.vue'),
    meta: { title: 'AI助手', public: true }
  },
  // 手机端文章详情
  {
    path: '/m/articles/:id',
    name: 'MobileArticleDetail',
    component: () => import('@/views/MobileArticleDetail.vue'),
    meta: { title: '文章详情', public: true }
  },
  // 手机端个人资料
  {
    path: '/m/profile',
    name: 'MobileProfile',
    component: () => import('@/views/MobileProfile.vue'),
    meta: { title: '个人资料' }
  },
  // 手机端写文章
  {
    path: '/m/editor',
    name: 'MobileEditor',
    component: () => import('@/views/MobileEditor.vue'),
    meta: { title: '写文章', public: true }
  },
  {
    path: '/m/editor/:id',
    name: 'MobileEditorEdit',
    component: () => import('@/views/MobileEditor.vue'),
    meta: { title: '编辑文章', public: true }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'articles',
        name: 'ArticleList',
        component: () => import('@/views/article/List.vue'),
        meta: { title: '文章管理' }
      },
      {
        path: 'articles/create',
        name: 'ArticleCreate',
        component: () => import('@/views/article/Edit.vue'),
        meta: { title: '新建文章' }
      },
      {
        path: 'articles/edit/:id',
        name: 'ArticleEdit',
        component: () => import('@/views/article/Edit.vue'),
        meta: { title: '编辑文章' }
      },
      {
        path: 'articles/:id',
        name: 'ArticleDetail',
        component: () => import('@/views/article/Detail.vue'),
        meta: { title: '文章详情' }
      },
      {
        path: 'categories',
        name: 'Categories',
        component: () => import('@/views/Category.vue'),
        meta: { title: '分类管理' }
      },
      {
        path: 'tags',
        name: 'Tags',
        component: () => import('@/views/Tag.vue'),
        meta: { title: '标签管理' }
      },
      {
        path: 'knowledge',
        name: 'KnowledgeBase',
        component: () => import('@/views/KnowledgeBase.vue'),
        meta: { title: '知识库' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人资料' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  if (to.meta.public) {
    next()
    return
  }
  
  if (!userStore.isLoggedIn) {
    next('/login')
    return
  }
  
  next()
})

export default router

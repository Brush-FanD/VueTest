import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'HomeFeed',
    // 路由级懒加载：首屏只加载当前页面，其余按需下载
    component: () => import('./views/HomeFeed.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/cars',
    name: 'CarSelect',
    component: () => import('./views/CarSelect.vue'),
    meta: { title: '选车' }
  },
  {
    path: '/car/:carId',
    name: 'CarDetail',
    component: () => import('./views/CarDetail.vue'),
    meta: { title: '车型详情' }
  },
  {
    path: '/videos',
    name: 'VideoManagement',
    component: () => import('./views/VideoManagement.vue'),
    meta: { title: '视频库' }
  },
  {
    path: '/video/:videoId',
    name: 'VideoView',
    component: () => import('./views/VideoView.vue'),
    meta: { title: '视频详情' }
  },
  {
    path: '/ai',
    name: 'AiChat',
    component: () => import('./components/AiChat.vue'),
    meta: { title: '懂车AI' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('./views/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  // 切换页面回到顶部，否则从详情页返回会停在上一页的滚动位置
  scrollBehavior: () => ({ top: 0 })
})

router.afterEach((to) => {
  document.title = to.meta?.title ? `${to.meta.title} - 懂车帝` : '懂车帝 - 看车买车用车'
})

export default router

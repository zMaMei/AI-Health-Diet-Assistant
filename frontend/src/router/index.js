import { createRouter, createWebHistory } from 'vue-router'
import RecordView from '../views/RecordView.vue'
import NutritionView from '../views/NutritionView.vue'
import RecommendView from '../views/RecommendView.vue'
import ProfileView from '../views/ProfileView.vue'

const routes = [
  { path: '/', redirect: '/record' },
  { path: '/record', name: 'Record', component: RecordView, meta: { title: '记录' } },
  { path: '/nutrition', name: 'Nutrition', component: NutritionView, meta: { title: '分析' } },
  { path: '/recommend', name: 'Recommend', component: RecommendView, meta: { title: '推荐' } },
  { path: '/profile', name: 'Profile', component: ProfileView, meta: { title: '我的' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  next()
})

export default router

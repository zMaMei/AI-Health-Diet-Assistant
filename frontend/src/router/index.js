import { createRouter, createWebHistory } from 'vue-router'
import RecordView from '../views/RecordView.vue'
import NutritionView from '../views/NutritionView.vue'
import ScoreView from '../views/ScoreView.vue'
import RecommendView from '../views/RecommendView.vue'
import ProfileView from '../views/ProfileView.vue'

const routes = [
  { path: '/', redirect: '/record' },
  { path: '/record', name: 'Record', component: RecordView, meta: { title: '记录' } },
  { path: '/nutrition', name: 'Nutrition', component: NutritionView, meta: { title: '分析' } },
  { path: '/score', name: 'Score', component: ScoreView, meta: { title: '评分' } },
  { path: '/recommend', name: 'Recommend', component: RecommendView, meta: { title: '推荐' } },
  { path: '/profile', name: 'Profile', component: ProfileView, meta: { title: '我的' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 不做页面级拦截，四个 tab 始终可访问
// 登录态检查由各页面组件内部处理
router.beforeEach((to, from, next) => {
  next()
})

export default router

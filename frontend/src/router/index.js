/* Vue Router路由配置 */
import { createRouter, createWebHistory } from 'vue-router'
/* 饮食记录页 */
import RecordView from '../views/RecordView.vue'
/* 营养分析页 */
import NutritionView from '../views/NutritionView.vue'
/* 食谱推荐页 */
import RecommendView from '../views/RecommendView.vue'
/* 个人中心页 */
import ProfileView from '../views/ProfileView.vue'

/* 路由定义 */
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

/* 路由守卫 */
router.beforeEach((to, from, next) => {
  next()
})

export default router

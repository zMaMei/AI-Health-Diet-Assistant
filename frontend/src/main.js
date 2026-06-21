/* Vue应用入口 */
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import auth from './auth.js'

/* 从localStorage恢复登录态 */
auth.init()

/* 创建Vue应用实例并挂载 */
const app = createApp(App)
/* 注册Vue Router */
app.use(router)
/* 挂载到#app节点 */
app.mount('#app')

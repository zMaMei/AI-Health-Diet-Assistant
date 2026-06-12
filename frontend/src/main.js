import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import auth from './auth.js'

// 从 localStorage 恢复登录态
auth.init()

const app = createApp(App)
app.use(router)
app.mount('#app')

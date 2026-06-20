<template>
  <div id="app-container">
    <header class="app-header">
      <h1>{{ currentTitle }}</h1>
    </header>
    <main class="app-main">
      <router-view />
    </main>
    <!-- 全局 toast -->
    <transition name="fade">
      <div v-if="toast.visible.value" class="global-toast">{{ toast.message.value }}</div>
    </transition>

    <nav class="app-nav">
      <router-link to="/record" class="nav-item" active-class="active">
        <span class="nav-icon">📝</span>
        <span class="nav-label">记录</span>
      </router-link>
      <router-link to="/nutrition" class="nav-item" active-class="active">
        <span class="nav-icon">📊</span>
        <span class="nav-label">分析</span>
      </router-link>
      <router-link to="/score" class="nav-item" active-class="active">
        <span class="nav-icon">⭐</span>
        <span class="nav-label">评分</span>
      </router-link>
      <router-link to="/recommend" class="nav-item" active-class="active">
        <span class="nav-icon">🍽️</span>
        <span class="nav-label">推荐</span>
      </router-link>
      <router-link to="/profile" class="nav-item" active-class="active">
        <span class="nav-icon">👤</span>
        <span class="nav-label">我的</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import toast from './toast.js'

const route = useRoute()
const currentTitle = computed(() => route.meta.title || 'AI健康饮食助手')
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: #f5f5f5;
  color: #333;
  -webkit-font-smoothing: antialiased;
}
#app-container {
  max-width: 480px;
  margin: 0 auto;
  min-height: 100vh;
  background: #fff;
  display: flex;
  flex-direction: column;
  position: relative;
}
.app-header {
  background: linear-gradient(135deg, #4CAF50, #2E7D32);
  color: #fff;
  padding: 16px 20px;
  position: fixed;
  top: 0;
  max-width: 480px;
  width: 100%;
  z-index: 100;
}
.app-header h1 {
  font-size: 18px;
  font-weight: 600;
  text-align: center;
}
.app-main {
  flex: 1;
  padding: 60px 16px 70px;
  overflow-y: auto;
}
.app-nav {
  position: fixed;
  bottom: 0;
  max-width: 480px;
  width: 100%;
  background: #fff;
  display: flex;
  border-top: 1px solid #e0e0e0;
  padding: 6px 0;
  padding-bottom: max(6px, env(safe-area-inset-bottom));
  z-index: 100;
}
.nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-decoration: none;
  color: #999;
  font-size: 11px;
  gap: 2px;
}
.nav-item.active {
  color: #4CAF50;
}
.nav-icon { font-size: 20px; }
.nav-label { font-size: 11px; }

/* Common styles */
.card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}
.btn {
  display: inline-block;
  padding: 10px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
}
.btn:active { opacity: 0.8; }
.btn-primary { background: #4CAF50; color: #fff; }
.btn-danger { background: #f44336; color: #fff; }
.btn-outline { background: transparent; color: #4CAF50; border: 1px solid #4CAF50; }
.btn-sm { padding: 6px 12px; font-size: 12px; }

input, select, textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}
input:focus, select:focus, textarea:focus {
  border-color: #4CAF50;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin: 16px 0 8px;
  color: #333;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #999;
  font-size: 14px;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #999;
}

.empty-state .empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  margin: 2px;
}
.tag-green { background: #E8F5E9; color: #2E7D32; }
.tag-orange { background: #FFF3E0; color: #E65100; }
.tag-red { background: #FFEBEE; color: #C62828; }
.tag-blue { background: #E3F2FD; color: #1565C0; }

/* 全局 toast */
.global-toast {
  position: fixed;
  top: 80px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0,0,0,0.75);
  color: #fff;
  padding: 10px 20px;
  border-radius: 20px;
  font-size: 14px;
  z-index: 9999;
  white-space: nowrap;
  pointer-events: none;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.35s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

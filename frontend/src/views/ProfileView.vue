<template>
  <div class="profile-page">
    <div v-if="loading" class="loading">加载中...</div>

    <template v-else-if="profile">
      <!-- User info -->
      <div class="card user-card">
        <div class="user-avatar">👤</div>
        <div class="user-info">
          <h3>{{ profile.nickname || '用户' }}</h3>
          <p v-if="profile.age || profile.heightCm || profile.weightKg">
            {{ profile.age || '-' }}岁 |
            {{ profile.heightCm || '-' }}cm |
            {{ profile.weightKg || '-' }}kg
          </p>
        </div>
      </div>

      <!-- Health goal -->
      <div class="card">
        <h3 class="card-title">🎯 健康目标</h3>
        <select v-model="form.goal" class="profile-select">
          <option value="均衡">均衡饮食</option>
          <option value="减脂">减脂</option>
          <option value="增肌">增肌</option>
          <option value="控糖">控糖</option>
        </select>
      </div>

      <!-- Personal info -->
      <div class="card">
        <h3 class="card-title">📋 个人资料</h3>
        <div class="form-row">
          <label>年龄</label>
          <input type="number" v-model.number="form.age" placeholder="岁">
        </div>
        <div class="form-row">
          <label>身高(cm)</label>
          <input type="number" v-model.number="form.heightCm" placeholder="cm" step="0.1">
        </div>
        <div class="form-row">
          <label>体重(kg)</label>
          <input type="number" v-model.number="form.weightKg" placeholder="kg" step="0.1">
        </div>
      </div>

      <!-- Preferences -->
      <div class="card">
        <h3 class="card-title">😋 口味偏好</h3>
        <div class="tag-selector">
          <button v-for="taste in tasteOptions" :key="taste"
                  class="tag-btn"
                  :class="{ selected: selectedTastes.includes(taste) }"
                  @click="toggleTaste(taste)">
            {{ taste }}
          </button>
        </div>
      </div>

      <!-- Taboo -->
      <div class="card">
        <h3 class="card-title">🚫 忌口</h3>
        <div class="tag-selector">
          <button v-for="t in tabooOptions" :key="t"
                  class="tag-btn"
                  :class="{ selected: selectedTaboos.includes(t) }"
                  @click="toggleTaboo(t)">
            {{ t }}
          </button>
        </div>
      </div>

      <!-- Warning Profile -->
      <div class="card">
        <h3 class="card-title">⚠️ 慢性病/特殊饮食</h3>
        <div class="tag-selector">
          <button v-for="w in warningOptions" :key="w"
                  class="tag-btn"
                  :class="{ selected: (form.warningProfile || '').includes(w) }"
                  @click="toggleWarning(w)">
            {{ w }}
          </button>
        </div>
      </div>

      <!-- Alert rules -->
      <div class="card">
        <h3 class="card-title">🔔 预警阈值设置</h3>
        <div class="alert-rule" v-for="rule in alertRules" :key="rule.id">
          <div class="rule-header">
            <span class="rule-name">{{ nutrientLabels[rule.nutrientType] || rule.nutrientType }}</span>
            <label class="switch">
              <input type="checkbox" v-model="rule.enabled" @change="toggleRule(rule)">
              <span class="slider"></span>
            </label>
          </div>
          <div class="rule-input" v-if="rule.enabled">
            <input type="number" v-model.number="rule.threshold" @change="updateRule(rule)">
            <span class="rule-unit">{{ nutrientUnits[rule.nutrientType] || '' }}</span>
          </div>
        </div>
      </div>

      <!-- Save button -->
      <button class="btn btn-primary save-btn" @click="saveProfile">保存设置</button>

      <!-- App info -->
      <div class="app-info">
        <p>AI智能个人健康饮食助手 v1.0</p>
        <p>本系统为课程实验作品，饮食建议仅供参考</p>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api/index.js'

const loading = ref(false)
const profile = ref(null)
const alertRules = ref([])

const tasteOptions = ['清淡', '中式', '西式', '日式', '辣味', '酸甜', '咸鲜']
const tabooOptions = ['海鲜', '花生', '牛奶', '鸡蛋', '豆制品', ' gluten', '辛辣']
const warningOptions = ['糖尿病', '高血压', '高血脂', '痛风', '无']

const nutrientLabels = { calorie: '每日热量上限', sugar: '每日糖分上限', sodium: '每日钠上限' }
const nutrientUnits = { calorie: 'kcal', sugar: 'g', sodium: 'mg' }

const form = ref({
  goal: '均衡',
  age: null,
  heightCm: null,
  weightKg: null,
  tastePreference: '',
  taboo: '',
  warningProfile: '',
})

const selectedTastes = ref([])
const selectedTaboos = ref([])

async function fetchData() {
  loading.value = true
  try {
    const [profileRes, rulesRes] = await Promise.all([
      api.getProfile(),
      api.getAlertRules(),
    ])
    const p = profileRes.data.data
    profile.value = p
    form.value.goal = p.goal || '均衡'
    form.value.age = p.age
    form.value.heightCm = p.heightCm
    form.value.weightKg = p.weightKg
    selectedTastes.value = p.tastePreference ? p.tastePreference.split(',').map(s => s.trim()).filter(Boolean) : []
    selectedTaboos.value = p.taboo ? p.taboo.split(',').map(s => s.trim()).filter(Boolean) : []
    form.value.warningProfile = p.warningProfile || ''

    alertRules.value = rulesRes.data.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function toggleTaste(taste) {
  const i = selectedTastes.value.indexOf(taste)
  if (i >= 0) selectedTastes.value.splice(i, 1)
  else selectedTastes.value.push(taste)
}

function toggleTaboo(taboo) {
  const i = selectedTaboos.value.indexOf(taboo)
  if (i >= 0) selectedTaboos.value.splice(i, 1)
  else selectedTaboos.value.push(taboo)
}

function toggleWarning(w) {
  if (w === '无') {
    form.value.warningProfile = ''
    return
  }
  const current = form.value.warningProfile ? form.value.warningProfile.split(',') : []
  const i = current.indexOf(w)
  if (i >= 0) current.splice(i, 1)
  else current.push(w)
  form.value.warningProfile = current.join(',')
}

async function saveProfile() {
  try {
    await api.updateProfile({
      goal: form.value.goal,
      age: form.value.age,
      heightCm: form.value.heightCm,
      weightKg: form.value.weightKg,
      tastePreference: selectedTastes.value.join(','),
      taboo: selectedTaboos.value.join(','),
      warningProfile: form.value.warningProfile,
    })
    alert('保存成功！')
  } catch (e) {
    alert('保存失败')
  }
}

async function toggleRule(rule) {
  await api.updateAlertRule(rule.id, { enabled: rule.enabled })
}

async function updateRule(rule) {
  await api.updateAlertRule(rule.id, { threshold: rule.threshold })
}

onMounted(fetchData)
</script>

<style scoped>
.user-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: linear-gradient(135deg, #E8F5E9, #C8E6C9);
  border: none;
}
.user-avatar {
  font-size: 48px;
  width: 64px;
  height: 64px;
  background: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.user-info h3 { font-size: 18px; margin-bottom: 4px; }
.user-info p { font-size: 13px; color: #666; }

.card-title { font-size: 15px; font-weight: 600; margin-bottom: 12px; }

.form-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.form-row label {
  width: 80px;
  font-size: 13px;
  color: #666;
  flex-shrink: 0;
}

.profile-select {
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  width: 100%;
}

.tag-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.tag-btn {
  padding: 6px 14px;
  border: 1px solid #ddd;
  border-radius: 16px;
  background: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.tag-btn.selected {
  background: #4CAF50;
  color: #fff;
  border-color: #4CAF50;
}
.tag-btn:active { transform: scale(0.95); }

.alert-rule {
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}
.rule-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.rule-name { font-size: 14px; }
.rule-input {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}
.rule-input input { width: 120px; }
.rule-unit { font-size: 12px; color: #999; }

/* Toggle switch */
.switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;
}
.switch input { opacity: 0; width: 0; height: 0; }
.slider {
  position: absolute;
  cursor: pointer;
  top: 0; left: 0; right: 0; bottom: 0;
  background: #ccc;
  transition: 0.3s;
  border-radius: 24px;
}
.slider:before {
  content: "";
  position: absolute;
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background: white;
  transition: 0.3s;
  border-radius: 50%;
}
input:checked + .slider { background: #4CAF50; }
input:checked + .slider:before { transform: translateX(20px); }

.save-btn {
  width: 100%;
  margin-top: 16px;
  padding: 12px;
  font-size: 16px;
}

.app-info {
  text-align: center;
  padding: 24px;
  color: #bbb;
  font-size: 12px;
  line-height: 1.6;
}
</style>

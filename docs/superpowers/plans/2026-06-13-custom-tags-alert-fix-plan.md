# 自定义标签 + 预警规则修复 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为"我的"页面口味偏好/忌口/病症三组标签添加自定义标签功能（增删改），修复新用户注册后预警规则为空的问题

**Architecture:** 前端在 ProfileView.vue 中新增自定义标签交互逻辑，标签数据仍以逗号分隔字符串存入现有字段；后端在 AuthService.register() 中注入 AlertRuleRepository，注册时创建 3 条默认预警规则

**Tech Stack:** Vue 3 Composition API, Java 17, Spring Boot 3.2.5, Spring Data JPA

---

### Task 1: 后端 — 注册时创建默认预警规则

**Files:**
- Modify: `backend/src/main/java/com/health/diet/service/AuthService.java`

- [ ] **Step 1: 注入 AlertRuleRepository 并添加默认规则创建逻辑**

在 `AuthService.java` 文件顶部，在现有 import 后添加：

```java
import com.health.diet.entity.AlertRule;
import com.health.diet.repository.AlertRuleRepository;
import java.math.BigDecimal;
```

修改构造函数，注入 `AlertRuleRepository`：

```java
// 修改前
private final UserProfileRepository userProfileRepository;

public AuthService(UserRepository userRepository,
                   UserProfileRepository userProfileRepository) {
    this.userRepository = userRepository;
    this.userProfileRepository = userProfileRepository;
}

// 修改后
private final UserProfileRepository userProfileRepository;
private final AlertRuleRepository alertRuleRepository;

public AuthService(UserRepository userRepository,
                   UserProfileRepository userProfileRepository,
                   AlertRuleRepository alertRuleRepository) {
    this.userRepository = userRepository;
    this.userProfileRepository = userProfileRepository;
    this.alertRuleRepository = alertRuleRepository;
}
```

在 `register()` 方法末尾，`log.info(...)` 之后、`return buildLoginResult(user, null)` 之前，插入：

```java
// 创建默认预警规则
AlertRule calorieRule = new AlertRule();
calorieRule.setUserId(user.getId());
calorieRule.setNutrientType("calorie");
calorieRule.setThreshold(new BigDecimal("2000"));
calorieRule.setEnabled(true);

AlertRule sugarRule = new AlertRule();
sugarRule.setUserId(user.getId());
sugarRule.setNutrientType("sugar");
sugarRule.setThreshold(new BigDecimal("50"));
sugarRule.setEnabled(true);

AlertRule sodiumRule = new AlertRule();
sodiumRule.setUserId(user.getId());
sodiumRule.setNutrientType("sodium");
sodiumRule.setThreshold(new BigDecimal("2400"));
sodiumRule.setEnabled(true);

alertRuleRepository.saveAll(List.of(calorieRule, sugarRule, sodiumRule));
```

- [ ] **Step 2: 编译验证**

```bash
cd backend && mvn compile -q
```

Expected: BUILD SUCCESS (无编译错误)

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/health/diet/service/AuthService.java
git commit -m "fix: 新用户注册时自动创建默认预警规则（热量2000/糖分50/钠2400）"
```

---

### Task 2: 前端 — 添加自定义标签的状态管理和辅助函数

**Files:**
- Modify: `frontend/src/views/ProfileView.vue`

- [ ] **Step 1: 导入 toast 模块**

在 `<script setup>` 中，`import auth from '../auth.js'` 之后添加：

```js
import toast from '../toast.js'
```

- [ ] **Step 2: 添加 computed 函数获取各组的 selected 数组（替换 warning 的字符串操作）**

在 `const selectedTaboos = ref([])` 之后添加：

```js
// 从 form.warningProfile 字符串派生的 selected 数组
const selectedWarnings = ref([])

// 更新 selectedWarnings（在 fetchData 中同步）
function syncWarningFromForm() {
  selectedWarnings.value = form.value.warningProfile
    ? form.value.warningProfile.split(',').map(s => s.trim()).filter(Boolean)
    : []
}
```

- [ ] **Step 3: 添加自定义标签判断函数和操作函数**

在 `toggleWarning` 函数之后，添加所有自定义标签相关逻辑：

```js
// ==================== 判断自定义标签 ====================
function isCustomTaste(tag) { return !tasteOptions.includes(tag) }
function isCustomTaboo(tag) { return !tabooOptions.includes(tag) }
function isCustomWarning(tag) { return !warningOptions.includes(tag) }

// ==================== 添加自定义标签状态 ====================
const addingTaste = ref(false)
const addingTaboo = ref(false)
const addingWarning = ref(false)
const tasteDraft = ref('')
const tabooDraft = ref('')
const warningDraft = ref('')

// ==================== 编辑自定义标签状态 ====================
const editingTag = ref({ group: '', oldName: '', draft: '' })

function startAddTaste()      { addingTaste.value = true; tasteDraft.value = '' }
function startAddTaboo()      { addingTaboo.value = true; tabooDraft.value = '' }
function startAddWarning()    { addingWarning.value = true; warningDraft.value = '' }

function confirmAddTaste() {
  const name = tasteDraft.value.trim()
  addingTaste.value = false
  if (!name) return
  if (selectedTastes.value.includes(name)) {
    toast.show('标签已存在')
    return
  }
  selectedTastes.value.push(name)
  tasteDraft.value = ''
}

function confirmAddTaboo() {
  const name = tabooDraft.value.trim()
  addingTaboo.value = false
  if (!name) return
  if (selectedTaboos.value.includes(name)) {
    toast.show('标签已存在')
    return
  }
  selectedTaboos.value.push(name)
  tabooDraft.value = ''
}

function confirmAddWarning() {
  const name = warningDraft.value.trim()
  addingWarning.value = false
  if (!name) return
  if (selectedWarnings.value.includes(name)) {
    toast.show('标签已存在')
    return
  }
  selectedWarnings.value.push(name)
  form.value.warningProfile = selectedWarnings.value.join(',')
  warningDraft.value = ''
}

// ==================== 编辑自定义标签 ====================
function startEditTaste(oldName) {
  editingTag.value = { group: 'taste', oldName, draft: oldName }
}
function startEditTaboo(oldName) {
  editingTag.value = { group: 'taboo', oldName, draft: oldName }
}
function startEditWarning(oldName) {
  editingTag.value = { group: 'warning', oldName, draft: oldName }
}

function confirmEditTag() {
  const { group, oldName, draft } = editingTag.value
  const newName = draft.trim()
  editingTag.value = { group: '', oldName: '', draft: '' }
  if (!newName || newName === oldName) return

  if (group === 'taste') {
    if (selectedTastes.value.includes(newName)) { toast.show('标签已存在'); return }
    const i = selectedTastes.value.indexOf(oldName)
    if (i >= 0) selectedTastes.value[i] = newName
  } else if (group === 'taboo') {
    if (selectedTaboos.value.includes(newName)) { toast.show('标签已存在'); return }
    const i = selectedTaboos.value.indexOf(oldName)
    if (i >= 0) selectedTaboos.value[i] = newName
  } else if (group === 'warning') {
    if (selectedWarnings.value.includes(newName)) { toast.show('标签已存在'); return }
    const i = selectedWarnings.value.indexOf(oldName)
    if (i >= 0) selectedWarnings.value[i] = newName
    form.value.warningProfile = selectedWarnings.value.join(',')
  }
}

function cancelEditTag() {
  editingTag.value = { group: '', oldName: '', draft: '' }
}

// ==================== 删除自定义标签 ====================
function deleteTaste(tag) {
  if (!confirm(`确定删除"${tag}"吗？`)) return
  const i = selectedTastes.value.indexOf(tag)
  if (i >= 0) selectedTastes.value.splice(i, 1)
}

function deleteTaboo(tag) {
  if (!confirm(`确定删除"${tag}"吗？`)) return
  const i = selectedTaboos.value.indexOf(tag)
  if (i >= 0) selectedTaboos.value.splice(i, 1)
}

function deleteWarning(tag) {
  if (!confirm(`确定删除"${tag}"吗？`)) return
  const i = selectedWarnings.value.indexOf(tag)
  if (i >= 0) selectedWarnings.value.splice(i, 1)
  form.value.warningProfile = selectedWarnings.value.join(',')
}
```

- [ ] **Step 4: 更新 fetchData() 同步 warning 数组**

修改 `fetchData()` 函数中读取 warning 的部分。找到：

```js
form.value.warningProfile = p.warningProfile || ''
```

在其后添加：

```js
syncWarningFromForm()
```

- [ ] **Step 5: 更新 toggleWarning 使用 selectedWarnings 数组**

修改 `toggleWarning()` 函数。替换原有实现为使用 `selectedWarnings`：

```js
// 修改前
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

// 修改后
function toggleWarning(w) {
  if (w === '无') {
    selectedWarnings.value = []
    form.value.warningProfile = ''
    return
  }
  const i = selectedWarnings.value.indexOf(w)
  if (i >= 0) selectedWarnings.value.splice(i, 1)
  else selectedWarnings.value.push(w)
  form.value.warningProfile = selectedWarnings.value.join(',')
}
```

- [ ] **Step 6: Commit**

```bash
git add frontend/src/views/ProfileView.vue
git commit -m "feat: 添加自定义标签的状态管理和辅助函数（增删改）"
```

---

### Task 3: 前端 — 重构三组标签模板

**Files:**
- Modify: `frontend/src/views/ProfileView.vue`

- [ ] **Step 1: 替换口味偏好标签组模板**

找到口味偏好的 `<div class="tag-selector">` 块（约第 71-78 行），替换为：

```html
<div class="tag-selector">
  <!-- 预设标签 -->
  <button v-for="taste in tasteOptions" :key="taste"
          class="tag-btn preset-tag"
          :class="{ selected: selectedTastes.includes(taste) }"
          @click="toggleTaste(taste)">
    {{ taste }}
  </button>
  <!-- 自定义口味标签（始终选中，不参与 toggle，仅通过 × 删除） -->
  <template v-for="(taste, idx) in selectedTastes.filter(t => isCustomTaste(t))" :key="'c-taste-'+idx">
    <span v-if="editingTag.group === 'taste' && editingTag.oldName === taste" class="tag-input-wrapper">
      <input v-model="editingTag.draft" class="tag-input"
             @blur="confirmEditTag" @keyup.enter="confirmEditTag" />
    </span>
    <span v-else class="tag-btn custom-tag selected">
      {{ taste }}
      <span class="tag-actions">
        <span class="tag-edit" @click.stop="startEditTaste(taste)">✎</span>
        <span class="tag-delete" @click.stop="deleteTaste(taste)">×</span>
      </span>
    </span>
  </template>
  <!-- + 自定义 或输入框 -->
  <span v-if="addingTaste" class="tag-input-wrapper">
    <input v-model="tasteDraft" class="tag-input" placeholder="输入自定义标签"
           @blur="confirmAddTaste" @keyup.enter="confirmAddTaste" />
  </span>
  <button v-else class="tag-btn tag-add-btn" @click="startAddTaste">
    + 自定义
  </button>
</div>
```

- [ ] **Step 2: 替换忌口标签组模板**

找到忌口的 `<div class="tag-selector">` 块（约第 84-91 行），替换为：

```html
<div class="tag-selector">
  <!-- 预设标签 -->
  <button v-for="t in tabooOptions" :key="t"
          class="tag-btn preset-tag"
          :class="{ selected: selectedTaboos.includes(t) }"
          @click="toggleTaboo(t)">
    {{ t }}
  </button>
  <!-- 自定义忌口标签（始终选中，不参与 toggle，仅通过 × 删除） -->
  <template v-for="(t, idx) in selectedTaboos.filter(t => isCustomTaboo(t))" :key="'c-taboo-'+idx">
    <span v-if="editingTag.group === 'taboo' && editingTag.oldName === t" class="tag-input-wrapper">
      <input v-model="editingTag.draft" class="tag-input"
             @blur="confirmEditTag" @keyup.enter="confirmEditTag" />
    </span>
    <span v-else class="tag-btn custom-tag selected">
      {{ t }}
      <span class="tag-actions">
        <span class="tag-edit" @click.stop="startEditTaboo(t)">✎</span>
        <span class="tag-delete" @click.stop="deleteTaboo(t)">×</span>
      </span>
    </span>
  </template>
  <!-- + 自定义 或输入框 -->
  <span v-if="addingTaboo" class="tag-input-wrapper">
    <input v-model="tabooDraft" class="tag-input" placeholder="输入自定义标签"
           @blur="confirmAddTaboo" @keyup.enter="confirmAddTaboo" />
  </span>
  <button v-else class="tag-btn tag-add-btn" @click="startAddTaboo">
    + 自定义
  </button>
</div>
```

- [ ] **Step 3: 替换慢性病/特殊饮食标签组模板**

找到慢性病的 `<div class="tag-selector">` 块（约第 97-104 行），替换为：

```html
<div class="tag-selector">
  <!-- 预设标签 -->
  <button v-for="w in warningOptions" :key="w"
          class="tag-btn preset-tag"
          :class="{ selected: selectedWarnings.includes(w) }"
          @click="toggleWarning(w)">
    {{ w }}
  </button>
  <!-- 自定义病症标签（始终选中，不参与 toggle，仅通过 × 删除） -->
  <template v-for="(w, idx) in selectedWarnings.filter(w => isCustomWarning(w))" :key="'c-warn-'+idx">
    <span v-if="editingTag.group === 'warning' && editingTag.oldName === w" class="tag-input-wrapper">
      <input v-model="editingTag.draft" class="tag-input"
             @blur="confirmEditTag" @keyup.enter="confirmEditTag" />
    </span>
    <span v-else class="tag-btn custom-tag selected">
      {{ w }}
      <span class="tag-actions">
        <span class="tag-edit" @click.stop="startEditWarning(w)">✎</span>
        <span class="tag-delete" @click.stop="deleteWarning(w)">×</span>
      </span>
    </span>
  </template>
  <!-- + 自定义 或输入框 -->
  <span v-if="addingWarning" class="tag-input-wrapper">
    <input v-model="warningDraft" class="tag-input" placeholder="输入自定义标签"
           @blur="confirmAddWarning" @keyup.enter="confirmAddWarning" />
  </span>
  <button v-else class="tag-btn tag-add-btn" @click="startAddWarning">
    + 自定义
  </button>
</div>
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/ProfileView.vue
git commit -m "feat: 重构三组标签模板，支持自定义标签的增删改交互"
```

---

### Task 4: 前端 — 添加自定义标签相关 CSS 样式

**Files:**
- Modify: `frontend/src/views/ProfileView.vue`

- [ ] **Step 1: 在 `<style scoped>` 中添加自定义标签样式**

在 `.tag-btn:active { transform: scale(0.95); }` 之后添加：

```css
/* 自定义标签 */
.tag-btn.custom-tag {
  border-style: dashed;
  border-color: #aaa;
  position: relative;
}
.tag-btn.custom-tag.selected {
  border-color: #4CAF50;
  padding-right: 48px; /* 为 ✎× 留空间 */
}

/* 操作图标（✎ 编辑 / × 删除） */
.tag-actions {
  display: inline-flex;
  gap: 4px;
  margin-left: 6px;
  vertical-align: middle;
}
.tag-edit, .tag-delete {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  font-size: 11px;
  line-height: 1;
  cursor: pointer;
  color: #fff;
  transition: opacity 0.15s;
}
.tag-edit { background: #4CAF50; }
.tag-delete { background: #f44336; }
.tag-edit:active, .tag-delete:active { opacity: 0.6; }

/* 内联输入框（新增/编辑自定义标签） */
.tag-input-wrapper {
  display: inline-block;
  vertical-align: middle;
}
.tag-input {
  padding: 6px 14px;
  border: 1px solid #4CAF50;
  border-radius: 16px;
  font-size: 13px;
  outline: none;
  width: 130px;
  background: #fafffe;
  transition: border-color 0.2s;
}
.tag-input:focus {
  border-color: #2E7D32;
}

/* + 自定义 添加按钮 */
.tag-add-btn {
  border-style: dashed;
  color: #4CAF50;
  border-color: #4CAF50;
  background: #f0faf0;
}
.tag-add-btn:active {
  background: #4CAF50;
  color: #fff;
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/ProfileView.vue
git commit -m "style: 添加自定义标签 CSS 样式（虚线边框、操作图标、内联输入框）"
```

---

### Task 5: 验证并完成

- [ ] **Step 1: 启动后端并验证编译**

```bash
cd backend && mvn spring-boot:run
```

Expected: 后端正常启动，无编译错误，无启动异常

- [ ] **Step 2: 启动前端并验证页面**

```bash
cd frontend && npm run dev
```

Expected: Vite 正常启动，访问 `http://localhost:5173/#/profile`

- [ ] **Step 3: 手动验证清单**

| 验证项 | 预期行为 |
|--------|---------|
| 未登录→注册新用户→查看"我的"页 | 预警阈值设置显示 3 条默认规则（热量/糖分/钠），带开关和数值 |
| 口味偏好末尾 | 显示 `+ 自定义` 虚线按钮 |
| 点击 `+ 自定义` | 变为内联输入框，输入"川味"→按 Enter→新气泡出现在末尾 |
| 选中自定义标签"川味" | 显示 ✎ × 操作图标，边框为虚线绿色 |
| 点击 ✎ | 变为输入框预填"川味"，改为"麻辣"→按 Enter→标签更新 |
| 输入重复标签 | toast 提示"标签已存在"，不添加 |
| 点击 × | confirm 弹出"确定删除"麻辣"吗？"，确认后标签移除 |
| 点击"保存设置"→刷新页面 | 自定义标签依然存在 |
| 对忌口、病症重复以上操作 | 行为一致 |

- [ ] **Step 4: 验证完成后 commit（如有其他调整）**

```bash
git add -A
git commit -m "chore: 验证完成，自定义标签+预警修复功能正常"
```

# 多项修复 — 设计文档

**日期**: 2026-06-13
**状态**: 已确认
**分支**: feature/MifuneShioriko-0613

---

## 一、概述

五项「我的」页面缺陷修复：

1. 新增性别字段
2. AI 智能分析前先保存 profile 数据
3. 自定义标签气泡宽度自适应文字
4. 错误登录导致白屏
5. 保存成功弹窗改为 toast

---

## 二、新增性别字段

### 2.1 数据库

`user_profile` 表新增列：
```sql
ALTER TABLE user_profile ADD COLUMN gender VARCHAR(8) NULL COMMENT '性别';
```

- 值：`'男'` / `'女'` / NULL
- NULL 表示未设置

`init.sql` 和 `data.sql` 同步更新。demo 用户（id=1）gender 填 `'男'`。

### 2.2 后端

**UserProfile.java** 新增字段：
```java
@Column(length = 8)
private String gender;

public String getGender() { return gender; }
public void setGender(String gender) { this.gender = gender; }
```

**UserProfileService.java** — update 方法在设置字段时加入 `profile.setGender(...)`（如 command 传入非 null）。

**AlertService.java** — `analyzeAndApply()` 的 prompt 构造中加入性别行：
```java
String gender = profile.getGender() != null ? profile.getGender() : "未知";
```
prompt 模板中加入 `- 性别：%s`。

### 2.3 前端

个人资料卡片，"体重"行下方添加：
```html
<div class="form-row">
  <label>性别</label>
  <select v-model="form.gender">
    <option value="">未设置</option>
    <option value="男">男</option>
    <option value="女">女</option>
  </select>
</div>
```

`form` ref 加 `gender: ''`，`fetchData()` 读 `p.gender`，`saveProfile()` 和 `analyzeThreshold()` 的 updateProfile 调用中传 `gender: form.value.gender`。

---

## 三、AI 分析前先保存

`analyzeThreshold()` 改为先调 `api.updateProfile(...)` 静默保存表单数据，再调 `api.analyzeAlertRules()`：

```js
async function analyzeThreshold() {
  analyzingThreshold.value = true
  try {
    await api.updateProfile({
      goal: form.value.goal,
      age: form.value.age,
      heightCm: form.value.heightCm,
      weightKg: form.value.weightKg,
      gender: form.value.gender,
      tastePreference: selectedTastes.value.join(','),
      taboo: selectedTaboos.value.join(','),
      warningProfile: form.value.warningProfile,
    })
    const res = await api.analyzeAlertRules()
    alertRules.value = res.data.data || []
    toast.show('AI 分析完成，可手动调整后保存')
  } catch (e) {
    toast.show('AI 分析失败，请稍后重试')
  } finally {
    analyzingThreshold.value = false
  }
}
```

同时将 `saveProfile()` 中的 `alert('保存成功！')` 改为 `toast.show('保存成功')`，统一交互体验。

---

## 四、自定义标签宽度自适应

自定义标签是 `<span class="tag-btn custom-tag selected">`，与预设标签的 `<button class="tag-btn">` 不同，`<span>` 默认 `display: inline` 导致内联 children（✎× 图标）布局异常。

CSS 修复 — 在 `.tag-btn.custom-tag.selected` 中添加：
```css
display: inline-flex;
align-items: center;
width: auto;
```

---

## 五、错误登录白屏修复

### 5.1 根因

登录失败时，`handleLogin` 的 catch 块尝试读取 `e.response.data.message`，但若错误对象结构与预期不符（如网络错误、JSON 解析失败等），可选的链式访问失败可能触发 Vue 的未捕获异常，导致整个应用崩溃白屏。

### 5.2 修复

**`auth.js`** — `login()` 和 `register()` 方法中，对 axios 错误做更安全的提取：

```js
async function login(username, password) {
  const res = await axios.post('/api/auth/login', { username, password })
  const data = res.data.data
  // ... existing success handling ...
}
```

无需改动 auth.js 正常路径。问题出在前端 catch 块不够安全。

**`ProfileView.vue` — `handleLogin`** — 错误提取改为更健壮的写法：
```js
} catch (e) {
  const msg = e?.response?.data?.message
  authError.value = msg || '登录失败，请重试'
}
```

**`ProfileView.vue` — `handleRegister`** — 同理加固。

**`api/index.js`** — 响应拦截器中 401 处理加 try-catch 防止 `auth.logout()` 抛异常导致白屏：
```js
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 401) {
      try {
        auth.logout()
        if (window.location.hash !== '#/profile') {
          toast.show('请先在"我的"页面登录')
        }
      } catch (ignored) {}
    }
    return Promise.reject(error)
  }
)
```

---

## 六、改动文件清单

| 文件 | 类型 | 改动 |
|------|------|------|
| `frontend/src/views/ProfileView.vue` | 修改 | 性别行、analyzeThreshold 先保存、CSS 自适应、alert→toast、错误处理加固 |
| `frontend/src/auth.js` | 修改 | 错误提取加固 |
| `frontend/src/api/index.js` | 修改 | 401 拦截器 try-catch |
| `backend/.../entity/UserProfile.java` | 修改 | 新增 gender 字段 |
| `backend/.../service/UserProfileService.java` | 修改 | update 支持 gender |
| `backend/.../service/AlertService.java` | 修改 | prompt 加性别行 |
| `backend/.../resources/data.sql` | 修改 | demo 数据加 gender |
| `backend/.../resources/init.sql` | 修改 | DDL 加 gender 列 |

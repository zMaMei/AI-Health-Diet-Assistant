# 多项修复（性别/先保存/CSS/白屏）— 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增性别字段，AI 分析前先保存 profile，自定义标签宽度自适应，修复错误登录白屏，alert 改 toast

**Architecture:** 后端 DB/Entity/DTO/Service 各层加 gender 字段并同步到 AI prompt；前端 analyzeThreshold 先 updateProfile 再 analyzeAlertRules；CSS 加 inline-flex；错误处理加固可选链和 try-catch

**Tech Stack:** Java 17, Spring Boot 3.2.5, Vue 3 Composition API, H2/MySQL

---

### Task 1: 后端 — UserProfile 层加 gender 字段

**Files:**
- Modify: `backend/src/main/java/com/health/diet/entity/UserProfile.java`
- Modify: `backend/src/main/java/com/health/diet/dto/command/UserProfileUpdateCommand.java`
- Modify: `backend/src/main/java/com/health/diet/dto/vo/UserProfileVO.java`
- Modify: `backend/src/main/java/com/health/diet/service/UserProfileService.java`

- [ ] **Step 1: UserProfile.java 加 gender**

在 `warningProfile` 字段下方插入：
```java
    @Column(length = 8)
    private String gender;
```

在 setter/getter 区（`setWarningProfile` 之后）插入：
```java
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
```

- [ ] **Step 2: UserProfileUpdateCommand.java 加 gender**

在 `warningProfile` 字段下方插入：
```java
    private String gender;
```

在 setter/getter 区（`setWarningProfile` 之后）插入：
```java
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
```

- [ ] **Step 3: UserProfileVO.java 加 gender**

在 `warningProfile` 字段下方插入：
```java
    private String gender;
```

在 setter/getter 区（`setWarningProfile` 之后）插入：
```java
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
```

- [ ] **Step 4: UserProfileService.java 加 gender 处理**

在 `getProfile()` 中，`setWarningProfile` 之后加：
```java
            vo.setGender(profile.getGender());
```

在 `updateProfile()` 中，`setWarningProfile` 之后加：
```java
        if (command.getGender() != null) profile.setGender(command.getGender());
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/health/diet/entity/UserProfile.java \
        backend/src/main/java/com/health/diet/dto/command/UserProfileUpdateCommand.java \
        backend/src/main/java/com/health/diet/dto/vo/UserProfileVO.java \
        backend/src/main/java/com/health/diet/service/UserProfileService.java
git commit -m "feat: UserProfile 新增 gender 性别字段（Entity/DTO/VO/Service）"
```

---

### Task 2: 后端 — AlertService prompt + DB init 加 gender

**Files:**
- Modify: `backend/src/main/java/com/health/diet/service/AlertService.java`
- Modify: `backend/src/main/resources/data.sql`
- Modify: `backend/src/main/resources/init.sql`

- [ ] **Step 1: AlertService.analyzeAndApply() prompt 加性别行**

在 `analyzeAndApply()` 方法中，`warning` 变量之后、`String prompt` 之前，插入：
```java
        String gender = profile.getGender() != null ? profile.getGender() : "未知";
```

修改 `String prompt = String.format(...)` 的参数，在 `warning` 之前插入 `gender`。

找到：
```java
            """, age, height, weight, bmiStr, goal, warning);
```

替换为：
```java
            """, age, gender, height, weight, bmiStr, goal, warning);
```

并在 prompt 模板中，"年龄"行之后插入 `- 性别：%s`。找到：
```java
            - 年龄：%s 岁
            - 身高：%s cm
```

替换为：
```java
            - 年龄：%s 岁
            - 性别：%s
            - 身高：%s cm
```

- [ ] **Step 2: data.sql 加 gender 列**

找到 `INSERT INTO user_profile ... VALUES` 行。在 `warning_profile` 和 `avatar_url` 之间插入 `gender`：

```sql
-- 修改前:
INSERT INTO user_profile (id, user_id, age, height_cm, weight_kg, goal, taboo, taste_preference, warning_profile, avatar_url)
VALUES (1, 1, 25, 170.00, 65.00, '减脂', '海鲜', '清淡,中式', '无', NULL);

-- 修改后:
INSERT INTO user_profile (id, user_id, age, height_cm, weight_kg, goal, taboo, taste_preference, warning_profile, gender, avatar_url)
VALUES (1, 1, 25, 170.00, 65.00, '减脂', '海鲜', '清淡,中式', '无', '男', NULL);
```

- [ ] **Step 3: init.sql 加 gender 列**

在 `user_profile` 表的 `warning_profile` 列定义之后、`avatar_url` 列定义之前，插入：
```sql
    `gender`           VARCHAR(8)   DEFAULT NULL            COMMENT '性别（男/女）',
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/health/diet/service/AlertService.java \
        backend/src/main/resources/data.sql \
        backend/src/main/resources/init.sql
git commit -m "feat: prompt加性别、DB init加gender列"
```

---

### Task 3: 前端 — ProfileView.vue 性别 + 先保存 + alert→toast + CSS

**Files:**
- Modify: `frontend/src/views/ProfileView.vue`

- [ ] **Step 1: form ref 加 gender**

找到 `const form = ref({...})`，在 `warningProfile: ''` 之后加：
```js
  gender: '',
```

- [ ] **Step 2: fetchData 读取 gender**

在 `fetchData()` 中 `form.value.warningProfile = p.warningProfile || ''` 之后加：
```js
    form.value.gender = p.gender || ''
```

- [ ] **Step 3: 模板 — 个人资料卡片加性别行**

在体重行之后、`</div>`（card 闭合）之前插入：
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

- [ ] **Step 4: analyzeThreshold 改为先保存再分析**

找到 `analyzeThreshold` 函数。替换为：
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

- [ ] **Step 5: saveProfile 中的 alert 改为 toast**

找到 `saveProfile` 函数中的：
```js
    alert('保存成功！')
```
替换为：
```js
    toast.show('保存成功')
```

- [ ] **Step 6: CSS — 自定义标签宽度自适应**

在 `.tag-btn.custom-tag.selected` 规则中添加 `display` 和 `align-items`。找到：
```css
.tag-btn.custom-tag.selected {
  border-color: #4CAF50;
  padding-right: 48px; /* 为 ✎× 留空间 */
}
```
替换为：
```css
.tag-btn.custom-tag.selected {
  display: inline-flex;
  align-items: center;
  width: auto;
  border-color: #4CAF50;
  padding-right: 48px; /* 为 ✎× 留空间 */
}
```

- [ ] **Step 7: Commit**

```bash
git add frontend/src/views/ProfileView.vue
git commit -m "fix: 性别字段、AI先保存、标签宽度自适应、alert改toast"
```

---

### Task 4: 前端 — 错误登录白屏修复

**Files:**
- Modify: `frontend/src/views/ProfileView.vue`
- Modify: `frontend/src/api/index.js`

- [ ] **Step 1: handleLogin 错误处理加固**

找到 `handleLogin` 的 catch 块：
```js
  } catch (e) {
    authError.value = e.response?.data?.message || '登录失败，请重试'
  }
```
替换为：
```js
  } catch (e) {
    const msg = e?.response?.data?.message
    authError.value = msg || '登录失败，请重试'
    console.error('登录失败', e)
  }
```

- [ ] **Step 2: handleRegister 错误处理加固**

找到 `handleRegister` 的 catch 块，同样替换为：
```js
  } catch (e) {
    const msg = e?.response?.data?.message
    authError.value = msg || '注册失败，请重试'
    console.error('注册失败', e)
  }
```

- [ ] **Step 3: api/index.js 401 拦截器加 try-catch**

找到响应拦截器中的 401 处理：
```js
    if (error.response && error.response.status === 401) {
      auth.logout()
      if (window.location.pathname !== '/profile') {
        toast.show('请先在"我的"页面登录')
      }
    }
```
替换为：
```js
    if (error.response && error.response.status === 401) {
      try {
        auth.logout()
        if (window.location.hash !== '#/profile') {
          toast.show('请先在"我的"页面登录')
        }
      } catch (ignored) {}
    }
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/ProfileView.vue frontend/src/api/index.js
git commit -m "fix: 错误登录白屏修复（catch加固+401 try-catch）"
```

---

### Task 5: 验证

- [ ] **Step 1: 编译后端**

```bash
cd backend && mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 2: 手动验证清单**

| 验证项 | 预期 |
|--------|------|
| 个人资料卡片 | 有性别下拉框（男/女/未设置） |
| 选择性别 + 保存 → 刷新 | 性别值保留 |
| 修改身高体重年龄性别 → AI 分析 | 结果反映新数据（先保存了） |
| 保存设置 | toast "保存成功" 而非 alert 弹窗 |
| 自定义标签 | 气泡宽度紧贴文字，✎× 图标正常显示 |
| 错误用户名密码登录 | 显示红色错误提示，页面不白屏 |
| 错误注册（用户名重复等） | 显示红色错误提示，页面正常 |

- [ ] **Step 3: 验证通过后 commit**

```bash
git add -A
git commit -m "chore: 验证完成，多项修复功能正常"
```

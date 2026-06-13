# 自定义标签 + 预警规则修复 — 设计文档

**日期**: 2026-06-13
**状态**: 已确认
**分支**: feature/MifuneShioriko-0613

---

## 一、概述

两项「我的」页面改进：

1. **自定义标签**：口味偏好、忌口、病症三组标签末尾新增 `+ 自定义` 气泡，点击后可内联输入并生成新标签
2. **预警规则修复**：修复新用户注册后预警阈值设置为空（无默认规则）的问题

---

## 二、自定义标签

### 2.1 数据模型（不改数据库）

标签仍以逗号分隔字符串存入 `user_profile` 表现有字段：

| 字段 | 说明 |
|------|------|
| `taste_preference` | 口味偏好（预设 + 自定义），逗号分隔 |
| `taboo` | 忌口（预设 + 自定义），逗号分隔 |
| `warning_profile` | 慢性病/特殊饮食（预设 + 自定义），逗号分隔 |

自定义标签与预设标签混合存储，下次加载时统一反序列化展示，无需区分来源。

**区分逻辑**：前端已知各分组预设标签数组（`tasteOptions`、`tabooOptions`、`warningOptions`），从后端加载的标签中，`preset.includes(tag)` 为预设，否则为自定义。无需后端额外字段。

### 2.2 前端交互

```
渲染顺序（每组独立）:
  [预设标签1] [预设标签2] ... [用户标签A(✎×)] [用户标签B(✎×)] [+ 自定义]

标签样式：
  - 预设标签: 实线边框，选中后绿色实心
  - 自定义标签: 虚线边框，选中后绿色实心 + 右侧显示 ✎ × 操作图标

点击 "+ 自定义":
  → 气泡变为内联 <input>，自动聚焦，placeholder="输入自定义标签"
  → Enter 或 blur 确认
  → 空白输入 → 放弃，恢复为 + 自定义 气泡
  → 重复标签（与已有标签完全匹配）→ toast 提示"标签已存在"，恢复为 + 自定义 气泡
  → 有效输入 → 插入到 selected 列表末尾，input 恢复为 + 自定义 气泡（仍在末尾）
```

**每个标签组的新增状态**：
- `editingCustomTaste` / `editingCustomTaboo` / `editingCustomWarning` — boolean, 是否处于输入模式
- `customTasteInput` / `customTabooInput` / `customWarningInput` — string, 输入框文本

**输入框样式**：与 `.tag-btn` 风格一致（`border-radius: 16px`, `padding: 6px 14px`, `border: 1px solid #4CAF50`），宽度自适应内容。

### 2.3 改动文件

| 文件 | 改动 |
|------|------|
| `frontend/src/views/ProfileView.vue` | 3 组标签区域各添加 `+ 自定义` 气泡 + 内联输入逻辑 |

---

## 三、预警规则修复

### 3.1 根因

`AuthService.register()` 注册新用户时只创建 `User` 和 `UserProfile`，**未创建默认预警规则**。`data.sql` 仅为 demo 用户（id=1）插入 3 条规则。新用户 `alert_rule` 表为空 → 前端渲染空卡片。

### 3.2 修复方案

在 `AuthService.register()` 中，创建用户和档案后，插入 3 条默认预警规则：

| 指标 | nutrient_type | threshold | enabled |
|------|-------------|-----------|---------|
| 每日热量上限 | `calorie` | 2000 | true |
| 每日糖分上限 | `sugar` | 50 | true |
| 每日钠上限 | `sodium` | 2400 | true |

```java
// AuthService.register() 末尾新增
alertRuleRepository.saveAll(List.of(
    createDefaultRule(user.getId(), "calorie", new BigDecimal("2000")),
    createDefaultRule(user.getId(), "sugar", new BigDecimal("50")),
    createDefaultRule(user.getId(), "sodium", new BigDecimal("2400"))
));
```

### 3.3 改动文件

| 文件 | 改动 |
|------|------|
| `backend/.../service/AuthService.java` | 注入 `AlertRuleRepository`，注册时创建默认规则 |

### 3.4 前端

无需改动。现有链路已完整：`fetchData()` → `api.getAlertRules()` → `v-for="rule in alertRules"` 渲染。新用户注册后刷新「我的」页面即可看到 3 条预警规则。

---

### 2.4 自定义标签的编辑与删除

自定义标签与预设标签的视觉区分：
- 预设标签：实线边框（`border: 1px solid #ddd`），选中后绿色实心
- 自定义标签：虚线边框（`border: 1px dashed #aaa`），选中后绿色实心 + 右侧显示 ✎ 和 × 图标

**编辑流程**：
```
点击自定义标签上的 ✎ 图标:
  → 标签气泡变为内联 <input>，预填当前标签名，自动聚焦
  → Enter 或 blur 确认
  → 空白输入 → 恢复原值（不修改）
  → 重复标签 → toast 提示"标签已存在"，恢复原值
  → 有效新值 → 更新列表中对应标签文本
```

**删除流程**：
```
点击自定义标签上的 × 图标:
  → 弹出确认：confirm("确定删除"<标签名>"吗？")
  → 确认 → 从 selected 列表中移除该标签
  → 取消 → 无操作
```

**操作区域**：✎ 和 × 图标仅在自定义标签被选中（`.selected`）时显示，避免视觉混乱。

```
自定义标签布局:
  [标签文本] [✎] [×]
   ✎ — 点击编辑
   × — 点击删除
```

---

## 四、边界情况

| 场景 | 处理 |
|------|------|
| 自定义标签输入空白 | 放弃输入，恢复 `+ 自定义` 气泡 |
| 自定义标签与已有标签重复 | toast 提示"标签已存在"，放弃输入 |
| 编辑标签后新名称与已有标签重复 | toast 提示"标签已存在"，恢复原值 |
| 删除标签 | confirm 二次确认，确认后移除 |
| 取消已选择的自定义标签 | 点击标签主体取消选择（与预设标签行为一致），从列表中移除 |
| 保存后自定义标签持久化 | `saveProfile()` 中 `selectedTastes.join(',')` 已将自定义标签包含在内 |
| 老用户（注册时无默认规则） | 不受影响，已手动创建的规则继续存在；若老用户也无规则则需手动创建（可通过前端兜底） |

---

## 五、不在范围

- 自定义标签的排序/拖拽
- 预警规则的前端创建按钮（本次仅修复后端默认创建）

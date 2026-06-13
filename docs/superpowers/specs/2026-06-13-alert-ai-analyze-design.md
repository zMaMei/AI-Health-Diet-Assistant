# 预警阈值 AI 分析 + 页面精简 — 设计文档

**日期**: 2026-06-13
**状态**: 已确认
**分支**: feature/MifuneShioriko-0613

---

## 一、概述

三项「我的」页面改动：

1. **移除"无"标签**：慢性病/特殊饮食的预设选项中删除 `'无'`，无选择即为无病症
2. **移除隐私说明**：删除页面底部的隐私说明卡片
3. **AI 智能分析预警阈值**：预警阈值卡片内新增「AI 智能分析」按钮，根据用户体型、健康目标、慢性病标签调用通义千问 API 综合分析，返回个性化阈值并自动填入

---

## 二、移除"无"标签

### 2.1 改动

**`warningOptions` 数组**：
```js
// 修改前
const warningOptions = ['糖尿病', '高血压', '高血脂', '痛风', '无']
// 修改后
const warningOptions = ['糖尿病', '高血压', '高血脂', '痛风']
```

**`toggleWarning` 函数**：删除处理 `'无'` 的分支（约 4 行）。

### 2.2 影响

用户不选任何慢性病标签即为无病症，逻辑上等价于原来选"无"。

---

## 三、移除隐私说明

### 3.1 改动

模板中删除 `<div class="card privacy-card">` 整块（含内部的隐私说明文字和列表），约 15 行。

`<style scoped>` 中 `.privacy-card` 和 `.privacy-content` 相关 CSS 一并删除。

---

## 四、AI 智能分析预警阈值

### 4.1 交互流程

```
预警阈值卡片内：
  [3 条规则（热量/糖分/钠，各带开关 + 数值）]
  [🤖 AI 智能分析] 按钮

点击按钮:
  → 按钮变为 loading 态（"分析中..."，disabled）
  → POST /api/alert-rules/analyze
  → 后端: 读取 user_profile → 构造 prompt → 通义千问 API → 解析 JSON → 更新 3 条 alert_rule
  → 返回更新后的规则列表 [{id, nutrientType, threshold, enabled}]
  → 前端 alertRules 替换，UI 自动刷新数值
  → toast "AI 分析完成，可手动调整后保存"
  → 失败 → toast "AI 分析失败，请稍后重试"
  → 按钮恢复正常
```

### 4.2 后端架构

```
POST /api/alert-rules/analyze
  ↓ AlertRuleController.analyze(request)
  ↓ AlertService.analyzeAndApply(userId)
      ↓ userProfileRepository.findByUserId(userId)
      ↓ 构造 prompt（年龄/身高/体重/BMI/goal/warningProfile）
      ↓ ThresholdAnalysisAdapter.analyze(prompt)
          ↓ 调用通义千问多模态生成 API
          → 返回 { calorie, sugar, sodium }
      ↓ 更新/创建 3 条 alert_rule（upsert 逻辑）
      → 返回 List<AlertRuleVO>
```

### 4.3 Prompt 模板

```
你是一位专业的营养师。请根据以下用户档案，综合分给出每日摄入上限建议。
- 年龄：{age} 岁
- 身高：{heightCm} cm
- 体重：{weightKg} kg
- BMI：{bmi}
- 健康目标：{goal}
- 慢性病/特殊饮食：{warningProfile}

请严格以 JSON 格式返回，不要包含其他文字：
{"calorie": 数字(kcal), "sugar": 数字(g), "sodium": 数字(mg)}
其中：
- calorie：每日热量上限
- sugar：每日糖分上限
- sodium：每日钠上限
```

替换规则：
- `{age}`：`userProfile.getAge()`，若 null 则填 "未知"
- `{heightCm}`：`userProfile.getHeightCm()`，若 null 则填 "未知"
- `{weightKg}`：`userProfile.getWeightKg()`，若 null 则填 "未知"
- `{bmi}`：`weightKg / (heightCm/100)^2`，保留一位小数，若缺少数据则填 "未知"
- `{goal}`：`userProfile.getGoal()`，默认 "均衡"
- `{warningProfile}`：`userProfile.getWarningProfile()`，若空则填 "无特殊疾病"

### 4.4 AI 响应解析

**期望格式**：纯 JSON `{"calorie": 1800, "sugar": 40, "sodium": 2000}`

**解析策略**：用正则 `/\\{[^}]+\\}/` 从响应文本中提取 JSON 对象，再用 Jackson 反序列化。如果解析失败或 JSON 中缺少三个字段中的任意一个 → 抛异常。

**Upsert 逻辑**：遍历现有 `alert_rule` 记录，按 `nutrientType` 匹配更新阈值；若某类型不存在（如老用户缺糖分规则），则新建。

### 4.5 ThresholdAnalysisAdapter

新建 `backend/src/main/java/com/health/diet/adapter/ThresholdAnalysisAdapter.java`：

- 注入 `DashScopeConfig`（复用现有 API 配置）
- 方法：`ThresholdResult analyze(String prompt)`
- 内部类 `ThresholdResult { BigDecimal calorie, sugar, sodium }`
- 参考 `ImageRecognitionAdapter` 的 HTTP 调用模式（RestTemplate / WebClient）
- 超时时间：15 秒

### 4.6 前端新增状态

```js
const analyzingThreshold = ref(false) // AI 分析按钮 loading 态
```

### 4.7 API 新增

`frontend/src/api/index.js`：
```js
analyzeAlertRules() {
  return api.post('/alert-rules/analyze', {}, { timeout: 20000 })
},
```

### 4.8 前端按钮

预警阈值卡片内，3 条规则列表下方、卡片闭合前：
```html
<button class="btn btn-ai-analyze" @click="analyzeThreshold" :disabled="analyzingThreshold">
  {{ analyzingThreshold ? '分析中...' : '🤖 AI 智能分析' }}
</button>
```

### 4.9 analyzeThreshold 函数

```js
async function analyzeThreshold() {
  analyzingThreshold.value = true
  try {
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

---

## 五、改动文件清单

| 文件 | 类型 | 改动 |
|------|------|------|
| `frontend/src/views/ProfileView.vue` | 修改 | 删除"无"标签、删除隐私说明、新增 AI 按钮和逻辑 |
| `frontend/src/api/index.js` | 修改 | 新增 `analyzeAlertRules()` 方法 |
| `backend/.../adapter/ThresholdAnalysisAdapter.java` | **新建** | AI 适配器，调用通义千问 API |
| `backend/.../service/AlertService.java` | 修改 | 新增 `analyzeAndApply(userId)` 方法 |
| `backend/.../controller/AlertRuleController.java` | 修改 | 新增 `POST /analyze` 端点 |

---

## 六、边界情况

| 场景 | 处理 |
|------|------|
| 用户档案数据不全（年龄/身高/体重为 null） | prompt 中填"未知"，AI 仍可给出通用建议 |
| AI 返回格式异常（非 JSON 或字段缺失） | 后端抛异常 → 前端 toast "AI 分析失败" |
| API 超时 | 后端 15s 超时 → 前端 20s 超时 → toast 提示失败 |
| AI 返回值为负数或 0 | 后端校验：阈值必须 > 0，否则抛异常 |
| 用户无现有预警规则 | upsert 逻辑自动创建 |
| AI 分析按钮重复点击 | `analyzingThreshold` 为 true 时按钮 disabled |
| 首次使用（无任何档案数据） | prompt 全部填"未知"，AI 返回通用建议 |

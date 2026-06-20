# 分析页重构 & AI 饮食分析 — 设计文档

**日期：** 2026-06-20
**状态：** 已确认

---

## 背景

- "评分"页面（ScoreView）的内容与"分析"页面（NutritionView）中的评分区域完全重复，定位重合。
- 当前"分析"页使用"前一天/后一天"按钮切换日期，操作不便。
- 缺少 AI 驱动的深度饮食分析能力。

## 目标

1. 删除"评分"页面，将其内容保留在"分析"页中（已合并）
2. 分析页增加当日饮食记录列表（可编辑）
3. 日期选择改为日历组件 + 快速切换按钮
4. 接入基于 DashScope 的 AI 对话式饮食分析

---

## 一、前端改动

### 1.1 路由 & 导航（router/index.js + App.vue）

- 删除 `/score` 路由和 `ScoreView` 导入
- App.vue 底部导航栏从 5 个 tab 减为 4 个：记录、分析、推荐、我的

### 1.2 删除 ScoreView.vue

评分内容已经合并到 NutritionView.vue 中，该文件不再需要。

### 1.3 NutritionView.vue（分析页）重构

页面从上到下 5 个区域：

#### 区域① — 日期选择器

```
[◀] [ 2026-06-20 ▼ ] [▶]  今天
```

- 使用原生 `<input type="date">`，移动端弹出系统原生日期选择器
- 左右箭头快速切换 ±1 天
- 不可选择未来日期
- "今天"快捷按钮
- 切换日期后重新加载所有数据（饮食记录 + 营养 + 评分 + AI 对话）

#### 区域② — 当日饮食记录（可编辑）

- 样式复用 RecordView 的餐次手风琴列表
- 按早餐/午餐/晚餐/夜宵/其他分组折叠
- 每条食物显示：名称、份量、来源标签、营养成分
- 支持编辑（弹出编辑弹窗）和删除（确认后删除）
- 操作后自动刷新数据和预警
- 如果当天无记录，显示空状态提示"去记录页添加"

#### 区域③ — 健康评分

- 评分环（0-100 分 SVG 环形图）
- 数据不足时的空状态提示
- 优点标签列表（绿色 tag）
- 风险项标签列表（橙色 tag）
- 改进建议列表
- 评分趋势历史条

#### 区域④ — 营养分析

- 4 个营养素进度环（热量/蛋白质/脂肪/碳水），点击查看食物来源
- 近一周热量趋势柱状图
- 饮食建议文字

#### 区域⑤ — AI 智能分析（对话式）

- 首次进入某天 → 自动发送系统消息触发 AI 生成当日分析报告
- 聊天气泡界面（AI 消息左对齐，用户消息右对齐）
- 底部输入框 + 发送按钮
- AI 回复时显示 loading 动画
- 对话历史按日期隔离（切换日期加载对应对话）
- AI 可访问当天及前后 7 天的饮食数据回答用户问题
- 支持 Markdown 格式渲染 AI 回复（加粗、列表等）

### 1.4 api/index.js 新增接口

```js
// AI 饮食分析对话
sendAiMessage(date, message)     // POST /api/ai/analyze-diet
getAiConversation(date)          // GET /api/ai/conversation?date=xxx
```

---

## 二、后端改动

### 2.1 新增文件

| 文件 | 说明 |
|------|------|
| `entity/AiConversation.java` | 对话实体：id, userId, date, createdAt |
| `entity/AiMessage.java` | 消息实体：id, conversationId, role(USER/AI), content, createdAt |
| `repository/AiConversationRepository.java` | 按 userId + date 查找对话 |
| `repository/AiMessageRepository.java` | 按 conversationId 查找消息列表 |
| `dto/command/AiChatCommand.java` | 请求体：date, message |
| `dto/vo/AiChatVO.java` | 响应体：conversationId, messages[], reply |
| `service/AiAnalysisService.java` | 核心 AI 分析逻辑 |
| `controller/AiAnalysisController.java` | REST 接口 |

### 2.2 AI 分析流程

```
用户发消息
  → 查找或创建该日期的对话
  → 查询用户 7 天饮食记录 + 营养汇总 + 健康评分 + 用户档案
  → 拼接 prompt：
     系统角色："你是一位专业的营养师..."
     数据上下文：最近 7 天的饮食记录、每日营养摄入、健康目标
     对话历史：该日期下的历史消息
     用户消息
  → 调用 DashScope qwen-turbo API
  → 保存用户消息和 AI 回复
  → 返回 AI 回复
```

### 2.3 API 接口

#### POST /api/ai/analyze-diet

请求：
```json
{
  "date": "2026-06-20",
  "message": "帮我分析今天的饮食"
}
```

响应：
```json
{
  "conversationId": 1,
  "messages": [
    { "role": "AI", "content": "...", "createdAt": "..." }
  ]
}
```

#### GET /api/ai/conversation?date=2026-06-20

响应：
```json
{
  "conversationId": 1,
  "messages": [
    { "role": "AI", "content": "...", "createdAt": "..." },
    { "role": "USER", "content": "...", "createdAt": "..." }
  ]
}
```

---

## 三、影响范围

| 层级 | 新增 | 修改 | 删除 |
|------|------|------|------|
| 前端 | 0 | 3（router/index.js, App.vue, NutritionView.vue, api/index.js） | 1（ScoreView.vue） |
| 后端 | 8（entity×2, repo×2, service, controller, dto×2） | 0 | 0 |

## 四、不涉及

- 后端现有 Controller/Service/Repository 不做修改
- "记录""推荐""我的"页面不做修改
- 不引入第三方 UI 库，日期选择使用原生 `<input type="date">`
- AI 对话使用现有 DashScope 配置，不引入新的 AI 服务

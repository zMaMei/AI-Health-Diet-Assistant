# 预警阈值 AI 分析 + 页面精简 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 移除"无"标签和隐私说明，新增预警阈值 AI 智能分析按钮，调用通义千问 API 根据用户档案生成个性化阈值

**Architecture:** 新建 `ThresholdAnalysisAdapter` 复用 `DashScopeConfig` 和 `RestClient` 模式调用通义千问 API；`AlertService` 新增 `analyzeAndApply()` 方法组装 prompt 并 upsert 规则；前端新增按钮触发 POST `/api/alert-rules/analyze`

**Tech Stack:** Java 17, Spring Boot 3.2.5, RestClient, Vue 3 Composition API, Axios，通义千问 qwen-omni-turbo

---

### Task 1: 前端 — 移除"无"标签

**Files:**
- Modify: `frontend/src/views/ProfileView.vue`

- [ ] **Step 1: 删除 warningOptions 中的 `'无'`**

Find the line:
```js
const warningOptions = ['糖尿病', '高血压', '高血脂', '痛风', '无']
```

Replace with:
```js
const warningOptions = ['糖尿病', '高血压', '高血脂', '痛风']
```

- [ ] **Step 2: 删除 toggleWarning 中的 `'无'` 处理分支**

Find the function `toggleWarning`, locate and delete these lines:
```js
  if (w === '无') {
    selectedWarnings.value = []
    form.value.warningProfile = ''
    return
  }
```

The resulting function should be:
```js
function toggleWarning(w) {
  const i = selectedWarnings.value.indexOf(w)
  if (i >= 0) selectedWarnings.value.splice(i, 1)
  else selectedWarnings.value.push(w)
  form.value.warningProfile = selectedWarnings.value.join(',')
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/ProfileView.vue
git commit -m "fix: 移除慢性病/特殊饮食中的\"无\"标签选项"
```

---

### Task 2: 前端 — 移除隐私说明

**Files:**
- Modify: `frontend/src/views/ProfileView.vue`

- [ ] **Step 1: 删除模板中的隐私说明卡片**

Find and delete the entire block (between 退出登录按钮的 `</button>` and App info):
```html
      <!-- Privacy notice -->
      <div class="card privacy-card">
        <h3 class="card-title">🔒 隐私说明</h3>
        <div class="privacy-content">
          <p>本系统为《AI智能个人健康饮食助手》课程实验作品。我们郑重承诺：</p>
          <ul>
            <li>仅采集完成课程实验必需的数据（年龄、身高、体重、饮食目标、口味偏好、忌口标签）</li>
            <li>不采集真实姓名、手机号、身份证、支付信息等高敏个人信息</li>
            <li>所有健康数据仅存储在本地实验数据库，不对外泄露或共享</li>
            <li>拍照和语音数据仅用于 AI 识别，处理完成后不长期留存原始文件</li>
            <li>饮食建议和健康评分仅供参考，不替代专业医生诊断</li>
          </ul>
        </div>
      </div>
```

- [ ] **Step 2: 删除对应 CSS**

Find and delete these style blocks:
```css
.privacy-card {
  background: #FAFAFA;
  border: 1px solid #e0e0e0;
}
.privacy-content {
  font-size: 13px;
  color: #666;
  line-height: 1.7;
}
.privacy-content ul {
  padding-left: 18px;
  margin-top: 6px;
}
.privacy-content ul li {
  margin-bottom: 4px;
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/ProfileView.vue
git commit -m "refactor: 移除隐私说明卡片"
```

---

### Task 3: 后端 — 新建 ThresholdAnalysisAdapter

**Files:**
- Create: `backend/src/main/java/com/health/diet/adapter/ThresholdAnalysisAdapter.java`

- [ ] **Step 1: 创建适配器文件**

Create the file with the following complete content:

```java
package com.health.diet.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.diet.config.DashScopeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 预警阈值分析适配器 — 调用通义千问 API 根据用户档案推荐个性化摄入上限。
 */
@Component
public class ThresholdAnalysisAdapter {

    private static final Logger log = LoggerFactory.getLogger(ThresholdAnalysisAdapter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final DashScopeConfig config;
    private final RestClient restClient;

    public ThresholdAnalysisAdapter(DashScopeConfig config) {
        this.config = config;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getTimeout());
        factory.setReadTimeout(config.getTimeout());
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /**
     * 根据用户档案 prompt 调用 AI 分析，返回推荐阈值。
     */
    public ThresholdResult analyze(String prompt) {
        log.info("调用千问 API 分析预警阈值");

        Map<String, Object> body = Map.of(
            "model", config.getModel(),
            "input", Map.of("messages", List.of(
                Map.of("role", "system", "content", List.of(Map.of("text",
                    "你是一位专业的营养师。请严格以 JSON 格式返回，不要包含其他文字。"))),
                Map.of("role", "user", "content", List.of(Map.of("text", prompt)))
            )),
            "parameters", Map.of("max_tokens", config.getMaxTokens())
        );

        try {
            String resp = restClient.post()
                    .uri(config.getMultimodalUrl())
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            log.debug("AI 原始响应: {}", resp);
            return parseResult(resp);
        } catch (Exception e) {
            log.error("AI 预警阈值分析失败", e);
            throw new RuntimeException("AI 分析服务暂时不可用，请稍后重试", e);
        }
    }

    @SuppressWarnings("unchecked")
    private ThresholdResult parseResult(String rawJson) throws Exception {
        // 先按多模态 API 格式提取 text
        String jsonText;
        try {
            Map<String, Object> root = objectMapper.readValue(rawJson, new TypeReference<>() {});
            jsonText = extractTextFromResponse(root);
        } catch (Exception e) {
            jsonText = rawJson;
        }

        // 去除 markdown 代码块
        jsonText = jsonText.trim();
        if (jsonText.startsWith("```")) {
            int start = jsonText.indexOf('\n') + 1;
            int end = jsonText.lastIndexOf("```");
            if (start > 0 && end > start) {
                jsonText = jsonText.substring(start, end).trim();
            }
        }

        // 用正则提取 JSON 对象
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{[^}]+\\}").matcher(jsonText);
        if (!m.find()) {
            throw new RuntimeException("AI 返回格式异常，未找到 JSON 数据");
        }
        String json = m.group();

        Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
        BigDecimal calorie = toBigDecimal(map, "calorie");
        BigDecimal sugar = toBigDecimal(map, "sugar");
        BigDecimal sodium = toBigDecimal(map, "sodium");

        if (calorie == null || sugar == null || sodium == null) {
            throw new RuntimeException("AI 返回数据不完整，缺少必要字段");
        }
        if (calorie.compareTo(BigDecimal.ZERO) <= 0
                || sugar.compareTo(BigDecimal.ZERO) <= 0
                || sodium.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("AI 返回阈值无效（必须大于 0）");
        }

        return new ThresholdResult(calorie, sugar, sodium);
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> root) {
        try {
            Map<String, Object> output = (Map<String, Object>) root.get("output");
            if (output != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) output.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                    List<Map<String, Object>> content = (List<Map<String, Object>>) msg.get("content");
                    if (content != null && !content.isEmpty()) {
                        return (String) content.get(0).get("text");
                    }
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private BigDecimal toBigDecimal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        if (v instanceof String s) {
            try { return new BigDecimal(s); } catch (Exception ignored) {}
        }
        return null;
    }

    public record ThresholdResult(BigDecimal calorie, BigDecimal sugar, BigDecimal sodium) {}
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/health/diet/adapter/ThresholdAnalysisAdapter.java
git commit -m "feat: 新建 ThresholdAnalysisAdapter，调用通义千问 API 分析预警阈值"
```

---

### Task 4: 后端 — AlertService 新增 analyzeAndApply 方法

**Files:**
- Modify: `backend/src/main/java/com/health/diet/service/AlertService.java`

- [ ] **Step 1: 注入 ThresholdAnalysisAdapter 和 UserProfileRepository**

在 `AlertService` 类的顶部，添加 import：
```java
import com.health.diet.adapter.ThresholdAnalysisAdapter;
import com.health.diet.entity.UserProfile;
import com.health.diet.repository.UserProfileRepository;
import java.math.RoundingMode;
```

修改构造函数注入新依赖。找到构造函数并修改：

```java
// 修改前
private final AlertRuleRepository alertRuleRepository;
private final DietRecordRepository dietRecordRepository;

public AlertService(AlertRuleRepository alertRuleRepository,
                    DietRecordRepository dietRecordRepository) {
    this.alertRuleRepository = alertRuleRepository;
    this.dietRecordRepository = dietRecordRepository;
}

// 修改后
private final AlertRuleRepository alertRuleRepository;
private final DietRecordRepository dietRecordRepository;
private final ThresholdAnalysisAdapter thresholdAnalysisAdapter;
private final UserProfileRepository userProfileRepository;

public AlertService(AlertRuleRepository alertRuleRepository,
                    DietRecordRepository dietRecordRepository,
                    ThresholdAnalysisAdapter thresholdAnalysisAdapter,
                    UserProfileRepository userProfileRepository) {
    this.alertRuleRepository = alertRuleRepository;
    this.dietRecordRepository = dietRecordRepository;
    this.thresholdAnalysisAdapter = thresholdAnalysisAdapter;
    this.userProfileRepository = userProfileRepository;
}
```

- [ ] **Step 2: 添加 analyzeAndApply 方法**

在 `AlertService` 类中，`nvl()` 方法之前，插入以下方法：

```java
/**
 * AI 分析预警阈值并更新/创建规则。
 * @param userId 用户 ID
 * @return 更新后的规则列表
 */
public List<AlertRuleVO> analyzeAndApply(Long userId) {
    UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("请先完善个人资料"));

    // 计算 BMI
    String bmiStr = "未知";
    if (profile.getHeightCm() != null && profile.getWeightKg() != null
            && profile.getHeightCm().compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal heightM = profile.getHeightCm().divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal bmi = profile.getWeightKg().divide(heightM.pow(2), 1, RoundingMode.HALF_UP);
        bmiStr = bmi.toString();
    }

    // 构造 prompt
    String age = profile.getAge() != null ? profile.getAge().toString() : "未知";
    String height = profile.getHeightCm() != null ? profile.getHeightCm().toString() : "未知";
    String weight = profile.getWeightKg() != null ? profile.getWeightKg().toString() : "未知";
    String goal = profile.getGoal() != null ? profile.getGoal() : "均衡";
    String warning = profile.getWarningProfile() != null && !profile.getWarningProfile().isEmpty()
            ? profile.getWarningProfile() : "无特殊疾病";

    String prompt = String.format("""
        你是一位专业的营养师。请根据以下用户档案，综合分给出每日摄入上限建议。
        - 年龄：%s 岁
        - 身高：%s cm
        - 体重：%s kg
        - BMI：%s
        - 健康目标：%s
        - 慢性病/特殊饮食：%s

        请严格以 JSON 格式返回，不要包含其他文字：
        {"calorie": 数字(kcal), "sugar": 数字(g), "sodium": 数字(mg)}
        其中：
        - calorie：每日热量上限
        - sugar：每日糖分上限
        - sodium：每日钠上限
        """, age, height, weight, bmiStr, goal, warning);

    ThresholdAnalysisAdapter.ThresholdResult result = thresholdAnalysisAdapter.analyze(prompt);

    // Upsert 规则
    List<AlertRule> existingRules = alertRuleRepository.findByUserId(userId);
    upsertRule(existingRules, userId, "calorie", result.calorie());
    upsertRule(existingRules, userId, "sugar", result.sugar());
    upsertRule(existingRules, userId, "sodium", result.sodium());

    log.info("AI 预警阈值分析完成: userId={}, calorie={}, sugar={}, sodium={}",
            userId, result.calorie(), result.sugar(), result.sodium());

    return listRules(userId);
}

private void upsertRule(List<AlertRule> existing, Long userId, String nutrientType, BigDecimal threshold) {
    AlertRule rule = existing.stream()
            .filter(r -> r.getNutrientType().equals(nutrientType))
            .findFirst()
            .orElseGet(() -> {
                AlertRule newRule = new AlertRule();
                newRule.setUserId(userId);
                newRule.setNutrientType(nutrientType);
                newRule.setEnabled(true);
                return newRule;
            });
    rule.setThreshold(threshold);
    alertRuleRepository.save(rule);
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/health/diet/service/AlertService.java
git commit -m "feat: AlertService 新增 analyzeAndApply 方法，AI 分析并 upsert 预警阈值"
```

---

### Task 5: 后端 — AlertRuleController 新增 POST /analyze 端点

**Files:**
- Modify: `backend/src/main/java/com/health/diet/controller/AlertRuleController.java`

- [ ] **Step 1: 添加 analyze 端点**

在 `AlertRuleController` 类中，`check()` 方法之后、类闭合 `}` 之前，插入：

```java
@PostMapping("/analyze")
public ApiResponse<List<AlertRuleVO>> analyze(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    return ApiResponse.success(alertService.analyzeAndApply(userId));
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/health/diet/controller/AlertRuleController.java
git commit -m "feat: AlertRuleController 新增 POST /api/alert-rules/analyze 端点"
```

---

### Task 6: 前端 — api/index.js 新增 analyzeAlertRules

**Files:**
- Modify: `frontend/src/api/index.js`

- [ ] **Step 1: 添加 `analyzeAlertRules` 方法**

在 `updateAlertRule` 方法之后、`checkAlerts` 方法之前，插入：

```js
analyzeAlertRules() {
  return api.post('/alert-rules/analyze', {}, { timeout: 20000 })
},
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/api/index.js
git commit -m "feat: api 新增 analyzeAlertRules 方法（POST /alert-rules/analyze）"
```

---

### Task 7: 前端 — ProfileView.vue 新增 AI 分析按钮

**Files:**
- Modify: `frontend/src/views/ProfileView.vue`

- [ ] **Step 1: 在预警阈值卡片模板中添加 AI 按钮**

找到预警阈值卡片的 alert-rule v-for 结束和卡片 `</div>` 闭合之间，在 `v-for` 的 `</div>` 后、卡片 `</div>` 前插入按钮：

```html
          <button class="btn btn-ai-analyze" @click="analyzeThreshold" :disabled="analyzingThreshold">
            {{ analyzingThreshold ? '分析中...' : '🤖 AI 智能分析' }}
          </button>
```

即在现有模板中：
```html
        <div class="card">
          <h3 class="card-title">🔔 预警阈值设置</h3>
          <div class="alert-rule" v-for="rule in alertRules" :key="rule.id">
            ...
          </div>
          <!-- 在此处插入按钮 -->
        </div>
```

- [ ] **Step 2: 添加 analyzeThreshold 函数和 analyzingThreshold 状态**

在 `<script setup>` 中添加状态。找到 `const editingTag = ref(...)` 附近，添加：

```js
const analyzingThreshold = ref(false)
```

在 `deleteWarning` 函数之后、`saveProfile` 函数之前，添加：

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

- [ ] **Step 3: 添加 AI 按钮 CSS**

在 `<style scoped>` 中，`.save-btn` 样式之前，添加：

```css
.btn-ai-analyze {
  width: 100%;
  margin-top: 12px;
  padding: 10px;
  font-size: 14px;
  color: #4CAF50;
  border: 1px dashed #4CAF50;
  border-radius: 8px;
  background: #f0faf0;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-ai-analyze:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-ai-analyze:not(:disabled):active {
  background: #4CAF50;
  color: #fff;
}
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/ProfileView.vue
git commit -m "feat: 预警阈值卡片新增 AI 智能分析按钮"
```

---

### Task 8: 验证

- [ ] **Step 1: 编译后端**

```bash
cd backend && mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 2: 验证前端无语法错误**

```bash
cd frontend && npx vite build --mode development 2>&1 | head -20
```
Expected: build successful

- [ ] **Step 3: 手动验证清单**

| 验证项 | 预期行为 |
|--------|---------|
| 慢性病/特殊饮食标签选项 | 无"无"选项，仅有"糖尿病/高血压/高血脂/痛风" |
| 不选任何慢性病 | 正常，无红色告警 |
| 页面底部 | 无隐私说明卡片，直接看到 App info |
| 登录后查看预警阈值卡片 | 底部有「🤖 AI 智能分析」虚线按钮 |
| 点击 AI 分析 | 按钮变为"分析中..."，约几秒后 toast "AI 分析完成"，阈值数值更新 |
| AI 分析失败 | toast "AI 分析失败，请稍后重试"，按钮恢复 |
| 分析后手动修改阈值 | 修改数值即自动保存（原有逻辑不变） |

- [ ] **Step 4: 验证通过后 commit**

```bash
git add -A
git commit -m "chore: 验证完成，AI预警分析+页面精简功能正常"
```

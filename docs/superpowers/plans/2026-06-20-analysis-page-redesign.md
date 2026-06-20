# 分析页重构 & AI 饮食分析 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 删除评分页，改造分析页（日期选择器 + 当日饮食记录 + AI对话分析），后端接入 DashScope 实现 AI 饮食分析。

**Architecture:** 后端新增 AiConversation/AiMessage 两个 JPA Entity + AiAnalysisService 调用 DashScope text API（OpenAI 兼容模式）；前端 NutritionView.vue 重构为 5 区域页面，复用 RecordView 的餐次列表样式和编辑弹窗。

**Tech Stack:** Java 17 + Spring Boot 3.2 + JPA + DashScope qwen-turbo / Vue 3 + Vite + Axios

## Global Constraints

- 不修改后端现有 Controller/Service/Repository
- 不修改"记录""推荐""我的"页面
- 不引入第三方 UI 库，日期选择使用原生 `<input type="date">`
- AI 对话使用现有 DashScope 配置 `DashScopeConfig`，text URL 为 `https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions`
- 对话历史按 userId + date 隔离持久化

---

### Task 1: AiConversation Entity

**Files:**
- Create: `backend/src/main/java/com/health/diet/entity/AiConversation.java`

**Interfaces:**
- Produces: `AiConversation` entity with fields `id:Long`, `userId:Long`, `recordDate:LocalDate`, `createdAt:LocalDateTime`

- [ ] **Step 1: Create AiConversation entity**

```java
package com.health.diet.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_conversation")
public class AiConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public AiConversation() {}

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDate getRecordDate() { return recordDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/health/diet/entity/AiConversation.java
git commit -m "feat: add AiConversation entity"
```

---

### Task 2: AiMessage Entity

**Files:**
- Create: `backend/src/main/java/com/health/diet/entity/AiMessage.java`

**Interfaces:**
- Consumes: `AiConversation.id` (foreign key)
- Produces: `AiMessage` entity with fields `id:Long`, `conversationId:Long`, `role:String` ("USER"|"AI"), `content:String`, `createdAt:LocalDateTime`

- [ ] **Step 1: Create AiMessage entity**

```java
package com.health.diet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_message")
public class AiMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(nullable = false, length = 16)
    private String role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public AiMessage() {}

    public Long getId() { return id; }
    public Long getConversationId() { return conversationId; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public void setRole(String role) { this.role = role; }
    public void setContent(String content) { this.content = content; }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/health/diet/entity/AiMessage.java
git commit -m "feat: add AiMessage entity"
```

---

### Task 3: Repositories

**Files:**
- Create: `backend/src/main/java/com/health/diet/repository/AiConversationRepository.java`
- Create: `backend/src/main/java/com/health/diet/repository/AiMessageRepository.java`

**Interfaces:**
- Produces: `AiConversationRepository.findByUserIdAndRecordDate(userId, date) → Optional<AiConversation>`, `AiMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId) → List<AiMessage>`

- [ ] **Step 1: Create AiConversationRepository**

```java
package com.health.diet.repository;

import com.health.diet.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {
    Optional<AiConversation> findByUserIdAndRecordDate(Long userId, LocalDate recordDate);
}
```

- [ ] **Step 2: Create AiMessageRepository**

```java
package com.health.diet.repository;

import com.health.diet.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {
    List<AiMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/health/diet/repository/AiConversationRepository.java backend/src/main/java/com/health/diet/repository/AiMessageRepository.java
git commit -m "feat: add AiConversation and AiMessage repositories"
```

---

### Task 4: DTOs

**Files:**
- Create: `backend/src/main/java/com/health/diet/dto/command/AiChatCommand.java`
- Create: `backend/src/main/java/com/health/diet/dto/vo/AiChatVO.java`

**Interfaces:**
- Produces: `AiChatCommand { date:LocalDate, message:String }`, `AiChatVO { conversationId:Long, messages:List<MessageVO> }`, `AiChatVO.MessageVO { role:String, content:String, createdAt:String }`

- [ ] **Step 1: Create AiChatCommand**

```java
package com.health.diet.dto.command;

import java.time.LocalDate;

public class AiChatCommand {
    private LocalDate date;
    private String message;

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
```

- [ ] **Step 2: Create AiChatVO**

```java
package com.health.diet.dto.vo;

import java.util.List;

public class AiChatVO {
    private Long conversationId;
    private List<MessageVO> messages;

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public List<MessageVO> getMessages() { return messages; }
    public void setMessages(List<MessageVO> messages) { this.messages = messages; }

    public static class MessageVO {
        private String role;
        private String content;
        private String createdAt;

        public MessageVO() {}
        public MessageVO(String role, String content, String createdAt) {
            this.role = role;
            this.content = content;
            this.createdAt = createdAt;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/health/diet/dto/command/AiChatCommand.java backend/src/main/java/com/health/diet/dto/vo/AiChatVO.java
git commit -m "feat: add AiChatCommand and AiChatVO DTOs"
```

---

### Task 5: AiAnalysisService

**Files:**
- Create: `backend/src/main/java/com/health/diet/service/AiAnalysisService.java`

**Interfaces:**
- Consumes: `AiConversationRepository`, `AiMessageRepository`, `DietRecordRepository`, `NutritionRecordRepository`, `UserProfileRepository`, `DashScopeConfig`
- Produces: `chat(userId, command) → AiChatVO`, `getConversation(userId, date) → AiChatVO`

- [ ] **Step 1: Create AiAnalysisService**

```java
package com.health.diet.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.diet.config.DashScopeConfig;
import com.health.diet.dto.command.AiChatCommand;
import com.health.diet.dto.vo.AiChatVO;
import com.health.diet.entity.*;
import com.health.diet.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AiConversationRepository conversationRepo;
    private final AiMessageRepository messageRepo;
    private final DietRecordRepository dietRecordRepo;
    private final NutritionRecordRepository nutritionRecordRepo;
    private final UserProfileRepository userProfileRepo;
    private final DashScopeConfig config;
    private final RestClient restClient;

    public AiAnalysisService(AiConversationRepository conversationRepo,
                             AiMessageRepository messageRepo,
                             DietRecordRepository dietRecordRepo,
                             NutritionRecordRepository nutritionRecordRepo,
                             UserProfileRepository userProfileRepo,
                             DashScopeConfig config) {
        this.conversationRepo = conversationRepo;
        this.messageRepo = messageRepo;
        this.dietRecordRepo = dietRecordRepo;
        this.nutritionRecordRepo = nutritionRecordRepo;
        this.userProfileRepo = userProfileRepo;
        this.config = config;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getTimeout());
        factory.setReadTimeout(config.getTimeout());
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public AiChatVO chat(Long userId, AiChatCommand command) {
        LocalDate date = command.getDate();

        // 1. 查找或创建对话
        AiConversation conv = conversationRepo.findByUserIdAndRecordDate(userId, date)
                .orElseGet(() -> {
                    AiConversation c = new AiConversation();
                    c.setUserId(userId);
                    c.setRecordDate(date);
                    return conversationRepo.save(c);
                });

        // 2. 保存用户消息
        AiMessage userMsg = new AiMessage();
        userMsg.setConversationId(conv.getId());
        userMsg.setRole("USER");
        userMsg.setContent(command.getMessage());
        messageRepo.save(userMsg);

        // 3. 收集上下文数据
        String contextData = buildContextData(userId, date);

        // 4. 获取历史消息
        List<AiMessage> history = messageRepo.findByConversationIdOrderByCreatedAtAsc(conv.getId());

        // 5. 构建 prompt 并调用 AI
        String aiReply = callAi(contextData, history);

        // 6. 保存 AI 回复
        AiMessage aiMsg = new AiMessage();
        aiMsg.setConversationId(conv.getId());
        aiMsg.setRole("AI");
        aiMsg.setContent(aiReply);
        messageRepo.save(aiMsg);

        // 7. 构建返回
        return buildVO(conv.getId(), List.of(aiMsg));
    }

    public AiChatVO getConversation(Long userId, LocalDate date) {
        Optional<AiConversation> convOpt = conversationRepo.findByUserIdAndRecordDate(userId, date);
        if (convOpt.isEmpty()) {
            AiChatVO vo = new AiChatVO();
            vo.setMessages(Collections.emptyList());
            return vo;
        }
        List<AiMessage> messages = messageRepo.findByConversationIdOrderByCreatedAtAsc(convOpt.get().getId());
        return buildVO(convOpt.get().getId(), messages);
    }

    private String buildContextData(Long userId, LocalDate date) {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM月dd日");

        // 用户档案
        userProfileRepo.findByUserId(userId).ifPresent(profile -> {
            sb.append("用户档案：");
            sb.append("目标=").append(profile.getGoal());
            if (profile.getAge() != null) sb.append(", 年龄=").append(profile.getAge());
            if (profile.getHeightCm() != null) sb.append(", 身高=").append(profile.getHeightCm()).append("cm");
            if (profile.getWeightKg() != null) sb.append(", 体重=").append(profile.getWeightKg()).append("kg");
            if (profile.getTaboo() != null && !profile.getTaboo().isEmpty())
                sb.append(", 忌口=").append(profile.getTaboo());
            sb.append("\n");
        });

        // 最近 7 天饮食记录
        LocalDate start = date.minusDays(6);
        sb.append("最近7天饮食记录（").append(start.format(fmt)).append(" 至 ").append(date.format(fmt)).append("）：\n");

        for (LocalDate d = start; !d.isAfter(date); d = d.plusDays(1)) {
            List<DietRecord> records = dietRecordRepo.findByUserAndDate(userId, d);
            if (records.isEmpty()) {
                sb.append(d.format(fmt)).append("：无记录\n");
                continue;
            }
            sb.append(d.format(fmt)).append("：\n");
            for (DietRecord r : records) {
                sb.append("  - ").append(r.getMealType()).append(" ")
                  .append(r.getFoodName()).append(" ").append(r.getAmount()).append("份");
                sb.append(" (热量").append(r.getCalorie().setScale(0, RoundingMode.HALF_UP))
                  .append("kcal, 蛋白质").append(r.getProtein().setScale(1, RoundingMode.HALF_UP))
                  .append("g, 脂肪").append(r.getFat().setScale(1, RoundingMode.HALF_UP))
                  .append("g, 碳水").append(r.getCarbohydrate().setScale(1, RoundingMode.HALF_UP)).append("g)\n");
            }
        }

        // 营养汇总
        List<NutritionRecord> nutritionRecords = nutritionRecordRepo
                .findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(userId, start, date);
        if (!nutritionRecords.isEmpty()) {
            sb.append("\n每日营养汇总：\n");
            for (NutritionRecord nr : nutritionRecords) {
                sb.append(nr.getRecordDate().format(fmt)).append("：")
                  .append("热量").append(nr.getCalorieTotal().setScale(0, RoundingMode.HALF_UP)).append("kcal, ")
                  .append("蛋白质").append(nr.getProteinTotal().setScale(1, RoundingMode.HALF_UP)).append("g, ")
                  .append("脂肪").append(nr.getFatTotal().setScale(1, RoundingMode.HALF_UP)).append("g, ")
                  .append("碳水").append(nr.getCarbohydrateTotal().setScale(1, RoundingMode.HALF_UP)).append("g\n");
            }
        }

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String callAi(String contextData, List<AiMessage> history) {
        log.info("调用 DashScope AI 饮食分析");

        List<Map<String, Object>> messages = new ArrayList<>();

        // System prompt
        messages.add(Map.of("role", "system", "content",
            "你是一位专业的营养师和健康饮食顾问。请根据用户的饮食记录数据，提供专业、具体、可操作的饮食分析和建议。" +
            "回答应包含：1) 对用户饮食的整体评价 2) 具体的营养摄入分析 3) 改进建议。" +
            "回答应简洁友好，使用中文，适当使用emoji。"));

        // Context
        messages.add(Map.of("role", "system", "content",
            "以下是用户的饮食数据，请基于这些数据回答用户的问题：\n\n" + contextData));

        // History (最近 10 轮)
        int startIdx = Math.max(0, history.size() - 20);
        for (int i = startIdx; i < history.size(); i++) {
            AiMessage msg = history.get(i);
            String role = "USER".equals(msg.getRole()) ? "user" : "assistant";
            messages.add(Map.of("role", role, "content", msg.getContent()));
        }

        Map<String, Object> body = Map.of(
            "model", config.getTextModel(),
            "messages", messages,
            "max_tokens", config.getMaxTokens()
        );

        try {
            String resp = restClient.post()
                    .uri(config.getTextUrl())
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            log.debug("AI 原始响应: {}", resp);
            Map<String, Object> root = objectMapper.readValue(resp, new TypeReference<>() {});
            List<Map<String, Object>> choices = (List<Map<String, Object>>) root.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                return (String) msg.get("content");
            }
            return "抱歉，AI 分析服务暂时不可用，请稍后重试。";
        } catch (Exception e) {
            log.error("AI 饮食分析失败", e);
            return "抱歉，AI 分析服务暂时不可用，请稍后重试。";
        }
    }

    private AiChatVO buildVO(Long conversationId, List<AiMessage> messages) {
        AiChatVO vo = new AiChatVO();
        vo.setConversationId(conversationId);
        List<AiChatVO.MessageVO> msgVOs = messages.stream()
                .map(m -> new AiChatVO.MessageVO(
                        m.getRole(),
                        m.getContent(),
                        m.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .toList();
        vo.setMessages(msgVOs);
        return vo;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/health/diet/service/AiAnalysisService.java
git commit -m "feat: add AiAnalysisService with DashScope integration"
```

---

### Task 6: AiAnalysisController

**Files:**
- Create: `backend/src/main/java/com/health/diet/controller/AiAnalysisController.java`

**Interfaces:**
- Consumes: `AiAnalysisService.chat(userId, command)`, `AiAnalysisService.getConversation(userId, date)`
- Produces: `POST /api/ai/analyze-diet → ApiResponse<AiChatVO>`, `GET /api/ai/conversation?date= → ApiResponse<AiChatVO>`

- [ ] **Step 1: Create AiAnalysisController**

```java
package com.health.diet.controller;

import com.health.diet.common.ApiResponse;
import com.health.diet.dto.command.AiChatCommand;
import com.health.diet.dto.vo.AiChatVO;
import com.health.diet.service.AiAnalysisService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/ai")
public class AiAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisController.class);

    private final AiAnalysisService aiAnalysisService;

    public AiAnalysisController(AiAnalysisService aiAnalysisService) {
        this.aiAnalysisService = aiAnalysisService;
    }

    @PostMapping("/analyze-diet")
    public ApiResponse<AiChatVO> analyzeDiet(@RequestBody AiChatCommand command,
                                              HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("POST /api/ai/analyze-diet — userId={}, date={}, message={}",
                userId, command.getDate(), command.getMessage());
        AiChatVO result = aiAnalysisService.chat(userId, command);
        return ApiResponse.success(result);
    }

    @GetMapping("/conversation")
    public ApiResponse<AiChatVO> getConversation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        log.debug("GET /api/ai/conversation — userId={}, date={}", userId, date);
        AiChatVO result = aiAnalysisService.getConversation(userId, date);
        return ApiResponse.success(result);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/health/diet/controller/AiAnalysisController.java
git commit -m "feat: add AiAnalysisController"
```

---

### Task 7: Database — Add AI tables to init.sql

**Files:**
- Modify: `backend/src/main/resources/init.sql`

**Interfaces:**
- Produces: `ai_conversation` and `ai_message` tables in database

- [ ] **Step 1: Add table definitions to init.sql**

Insert after the voice_record table (line ~197, before `SET FOREIGN_KEY_CHECKS = 1;`):

```sql
-- ============================================================
-- 11. ai_conversation 表（AI 对话）
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_conversation` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '对话主键',
    `user_id`     BIGINT   NOT NULL                COMMENT '所属用户',
    `record_date` DATE     NOT NULL                COMMENT '对话日期',
    `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_date` (`user_id`, `record_date`),
    CONSTRAINT `fk_ai_conv_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI饮食分析对话表';

-- ============================================================
-- 12. ai_message 表（AI 对话消息）
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_message` (
    `id`              BIGINT   NOT NULL AUTO_INCREMENT COMMENT '消息主键',
    `conversation_id` BIGINT   NOT NULL                COMMENT '所属对话',
    `role`            VARCHAR(16) NOT NULL             COMMENT 'USER 或 AI',
    `content`         TEXT     NOT NULL                COMMENT '消息内容',
    `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_conversation_id` (`conversation_id`),
    CONSTRAINT `fk_ai_msg_conv` FOREIGN KEY (`conversation_id`) REFERENCES `ai_conversation` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI饮食分析消息表';
```

- [ ] **Step 2: Update the table count comment (line ~197)**

Change `SET FOREIGN_KEY_CHECKS = 1;` location — insert new tables before this line.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/init.sql
git commit -m "feat: add ai_conversation and ai_message tables to init.sql"
```

---

### Task 8: Remove Score Page (Frontend Cleanup)

**Files:**
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/App.vue`
- Delete: `frontend/src/views/ScoreView.vue`

**Interfaces:**
- Consumes: none
- Produces: 4-tab navigation (记录/分析/推荐/我的), no /score route

- [ ] **Step 1: Remove /score route from router/index.js**

Delete the ScoreView import line and the `/score` route entry:

```js
import { createRouter, createWebHistory } from 'vue-router'
import RecordView from '../views/RecordView.vue'
import NutritionView from '../views/NutritionView.vue'
import RecommendView from '../views/RecommendView.vue'
import ProfileView from '../views/ProfileView.vue'

const routes = [
  { path: '/', redirect: '/record' },
  { path: '/record', name: 'Record', component: RecordView, meta: { title: '记录' } },
  { path: '/nutrition', name: 'Nutrition', component: NutritionView, meta: { title: '分析' } },
  { path: '/recommend', name: 'Recommend', component: RecommendView, meta: { title: '推荐' } },
  { path: '/profile', name: 'Profile', component: ProfileView, meta: { title: '我的' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  next()
})

export default router
```

- [ ] **Step 2: Remove "评分" nav tab from App.vue**

Remove the "评分" nav-item block (lines 23-26 in the nav section):

```html
<nav class="app-nav">
  <router-link to="/record" class="nav-item" active-class="active">
    <span class="nav-icon">📝</span>
    <span class="nav-label">记录</span>
  </router-link>
  <router-link to="/nutrition" class="nav-item" active-class="active">
    <span class="nav-icon">📊</span>
    <span class="nav-label">分析</span>
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
```

- [ ] **Step 3: Delete ScoreView.vue**

```bash
rm frontend/src/views/ScoreView.vue
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/router/index.js frontend/src/App.vue
git rm frontend/src/views/ScoreView.vue
git commit -m "feat: remove Score page, merge into Nutrition analysis page"
```

---

### Task 9: Add AI API Methods to Frontend

**Files:**
- Modify: `frontend/src/api/index.js`

**Interfaces:**
- Produces: `sendAiMessage(date, message) → Promise<AiChatVO>`, `getAiConversation(date) → Promise<AiChatVO>`

- [ ] **Step 1: Add AI API methods**

Add inside the `export default { }` object, before the closing `}`:

```js
  // AI 饮食分析对话
  sendAiMessage(date, message) {
    return api.post('/ai/analyze-diet', { date, message }, { timeout: 30000 })
  },
  getAiConversation(date) {
    return api.get('/ai/conversation', { params: { date } })
  },
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/api/index.js
git commit -m "feat: add AI chat API methods"
```

---

### Task 10: Rewrite NutritionView.vue — Part 1 (Date Picker + Diet Records + Score + Nutrition)

**Files:**
- Modify: `frontend/src/views/NutritionView.vue` (full rewrite)

**Interfaces:**
- Consumes: `api.getDietRecords(date)`, `api.getNutrition(date)`, `api.getHealthScore(date)`, `api.updateDietRecord(id, data)`, `api.deleteDietRecord(id)`, `toast`
- Produces: NutritionView with date picker, editable diet records, score section, nutrition section

- [ ] **Step 1: Write the complete NutritionView.vue (template + script + style)**

```vue
<template>
  <div class="nutrition-page">
    <!-- ===== 区域①: 日期选择器 ===== -->
    <div class="date-selector">
      <button @click="changeDate(-1)" class="btn btn-sm btn-outline">◀</button>
      <input type="date" v-model="currentDate" :max="todayStr"
             class="date-input" @change="onDateChange">
      <button @click="changeDate(1)" class="btn btn-sm btn-outline" :disabled="isToday">▶</button>
      <button v-if="!isToday" @click="goToday" class="btn btn-sm btn-outline today-btn">今天</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <template v-else>
      <!-- ===== 区域②: 当日饮食记录（可编辑） ===== -->
      <h2 class="page-section-title">🍽️ 当日饮食</h2>

      <template v-for="meal in mealTypes" :key="meal.key">
        <div class="meal-card card" v-if="groupedRecords[meal.key]?.length"
             :class="{ expanded: expandedMeals[meal.key] }">
          <div class="meal-header" @click="toggleMeal(meal.key)">
            <div class="meal-header-left">
              <span class="meal-icon">{{ meal.icon }}</span>
              <span class="meal-name">{{ meal.label }}</span>
              <span class="meal-count">{{ groupedRecords[meal.key].length }}种</span>
            </div>
            <div class="meal-header-right">
              <span class="meal-summary">🔥{{ mealTotals[meal.key]?.calorie || 0 }}</span>
              <span class="meal-summary-sub">🥩{{ mealTotals[meal.key]?.protein || 0 }}g</span>
              <span class="meal-arrow">{{ expandedMeals[meal.key] ? '▴' : '▾' }}</span>
            </div>
          </div>
          <div class="meal-body" v-show="expandedMeals[meal.key]">
            <div class="food-item" v-for="rec in groupedRecords[meal.key]" :key="rec.id">
              <div class="food-main">
                <div class="food-name-row">
                  <strong>{{ rec.foodName }}</strong>
                  <span class="food-amount">{{ rec.amount }}{{ rec.unit || '份' }}</span>
                  <span class="food-source">{{ sourceLabels[rec.source] || rec.source }}</span>
                </div>
                <div class="food-nutrition">
                  <span>🔥{{ rec.calorie || 0 }}kcal</span>
                  <span>🥩{{ rec.protein || 0 }}g</span>
                  <span>🧈{{ rec.fat || 0 }}g</span>
                  <span>🌾{{ rec.carbohydrate || 0 }}g</span>
                </div>
              </div>
              <div class="food-actions">
                <button class="btn btn-sm btn-outline" @click.stop="editRecord(rec)">编辑</button>
                <button class="btn btn-sm btn-danger" @click.stop="deleteRecord(rec.id)">删除</button>
              </div>
            </div>
          </div>
        </div>
      </template>

      <div v-if="!Object.values(groupedRecords).some(g => g.length)" class="empty-state" style="padding:20px">
        <p>当天还没有饮食记录</p>
        <router-link to="/record" class="btn btn-primary" style="display:inline-block;margin-top:8px">去记录</router-link>
      </div>

      <!-- ===== 区域③: 健康评分 ===== -->
      <h2 class="page-section-title">⭐ 健康评分</h2>

      <template v-if="scoreData">
        <div class="card score-card">
          <div v-if="scoreData.score !== null" class="score-ring">
            <svg viewBox="0 0 120 120" class="score-svg">
              <circle cx="60" cy="60" r="54" fill="none" stroke="#f0f0f0" stroke-width="8"/>
              <circle cx="60" cy="60" r="54" fill="none" stroke="#4CAF50" stroke-width="8"
                      :stroke-dasharray="circumference"
                      :stroke-dashoffset="dashOffset"
                      stroke-linecap="round" transform="rotate(-90 60 60)"/>
            </svg>
            <div class="score-text">
              <span class="score-number">{{ scoreData.score }}</span>
              <span class="score-unit">分</span>
            </div>
          </div>
          <div v-else class="score-empty">
            <p>⚠️ 数据不足</p>
            <p style="font-size:13px;color:#999;margin-top:4px">
              {{ scoreData.suggestions?.[0] || '今日饮食记录不足2餐，无法生成评分' }}
            </p>
          </div>
        </div>

        <div class="card" v-if="scoreData.strengths?.length">
          <h3 class="card-title">✅ 优点</h3>
          <div class="tag-list">
            <span class="tag tag-green" v-for="s in scoreData.strengths" :key="s">{{ s }}</span>
          </div>
        </div>
        <div class="card" v-if="scoreData.risks?.length">
          <h3 class="card-title">⚠️ 风险项</h3>
          <div class="tag-list">
            <span class="tag tag-orange" v-for="r in scoreData.risks" :key="r">{{ r }}</span>
          </div>
        </div>
        <div class="card" v-if="scoreData.suggestions?.length">
          <h3 class="card-title">💡 改进建议</h3>
          <ul class="suggestion-list">
            <li v-for="(s, i) in scoreData.suggestions" :key="i">{{ s }}</li>
          </ul>
        </div>
        <div class="card" v-if="scoreData.history?.length">
          <h3 class="card-title">📈 评分趋势</h3>
          <div class="history-list">
            <div class="history-item" v-for="h in scoreData.history" :key="h.date">
              <span class="history-date">{{ formatScoreDate(h.date) }}</span>
              <div class="history-bar-bg">
                <div class="history-bar" :style="{ width: h.score + '%' }"
                     :class="{ good: h.score >= 70, mid: h.score >= 40 && h.score < 70, bad: h.score < 40 }"></div>
              </div>
              <span class="history-score">{{ h.score }}</span>
            </div>
          </div>
        </div>
      </template>
      <div v-else-if="scoreLoading" class="loading">评分计算中...</div>

      <!-- ===== 区域④: 营养分析 ===== -->
      <h2 class="page-section-title">📊 营养分析</h2>

      <template v-if="nutrition">
        <div class="card">
          <div class="nutrient-grid">
            <div class="nutrient-item" v-for="n in nutrients" :key="n.key"
                 @click="showSourceDetail(n)">
              <div class="progress-ring" :style="{ borderColor: n.color }">
                <span class="progress-value">{{ getPercent(n.key) }}%</span>
              </div>
              <span class="nutrient-name">{{ n.label }}</span>
              <span class="nutrient-value">{{ getValue(n.key) }} / {{ getGoal(n.key) }}</span>
              <span class="click-hint">点击查看来源</span>
            </div>
          </div>
        </div>
        <div class="card">
          <h3 class="card-title">📈 近一周热量趋势</h3>
          <div class="trend-chart" v-if="nutrition.trend?.length">
            <div class="bar-container" v-for="(point, i) in nutrition.trend" :key="i">
              <div class="bar-wrapper">
                <div class="bar" :style="{ height: barHeight(point.calorie) + '%' }"
                     :class="{ today: isTodayDate(point.date) }"></div>
              </div>
              <span class="bar-label">{{ formatDateShort(point.date) }}</span>
            </div>
          </div>
          <div v-else class="empty-state" style="padding:20px"><p>暂无趋势数据</p></div>
        </div>
        <div class="card" v-if="nutrition.suggestion">
          <h3 class="card-title">💡 饮食建议</h3>
          <p class="suggestion-text">{{ nutrition.suggestion }}</p>
        </div>
      </template>
      <div v-else class="empty-state"><div class="empty-icon">📊</div><p>暂无营养数据</p></div>
    </template>

    <!-- ===== 区域⑤: AI 智能分析（对话式） ===== -->
    <h2 class="page-section-title">🤖 AI 智能分析</h2>
    <div class="card ai-chat-card">
      <!-- Chat messages -->
      <div class="chat-messages" ref="chatMsgsRef">
        <div v-if="aiLoading" class="loading" style="padding:12px">AI 思考中...</div>
        <div v-if="!aiMessages.length && !aiLoading" class="chat-empty">
          <p>点击下方按钮，让 AI 帮你分析今日饮食</p>
          <button class="btn btn-primary" @click="startAiAnalysis" style="margin-top:8px">
            🤖 开始分析
          </button>
        </div>
        <div v-for="(msg, i) in aiMessages" :key="i"
             :class="['chat-bubble', msg.role === 'AI' ? 'ai' : 'user']">
          <div class="bubble-avatar">{{ msg.role === 'AI' ? '🤖' : '👤' }}</div>
          <div class="bubble-content">
            <div class="bubble-text" v-html="renderMarkdown(msg.content)"></div>
            <div class="bubble-time">{{ formatMsgTime(msg.createdAt) }}</div>
          </div>
        </div>
      </div>

      <!-- Input area -->
      <div class="chat-input-area">
        <input v-model="aiInput" type="text" class="chat-input"
               placeholder="输入你的问题，如：昨天的蛋白质够吗？"
               @keyup.enter="sendAiMessage"
               :disabled="aiLoading">
        <button class="btn btn-primary chat-send-btn"
                @click="sendAiMessage" :disabled="aiLoading || !aiInput.trim()">
          发送
        </button>
      </div>
    </div>

    <!-- Edit record modal -->
    <div class="modal-overlay" v-if="showEditModal" @click.self="showEditModal=false">
      <div class="modal-content">
        <h3>✏️ 编辑饮食记录</h3>
        <div class="form-group">
          <label>食物名称</label>
          <input v-model="editForm.foodName">
        </div>
        <div class="form-group">
          <label>餐次</label>
          <select v-model="editForm.mealType">
            <option value="早餐">早餐</option>
            <option value="午餐">午餐</option>
            <option value="晚餐">晚餐</option>
            <option value="夜宵">夜宵</option>
            <option value="其他">其他</option>
          </select>
        </div>
        <div class="form-group">
          <label>份量</label>
          <input type="number" v-model.number="editForm.amount" min="0.1" step="0.5">
        </div>
        <button class="btn btn-primary" @click="saveEdit">保存修改</button>
        <button class="btn btn-outline" @click="showEditModal=false" style="margin-top:8px">取消</button>
      </div>
    </div>

    <!-- Nutrient source detail modal -->
    <div class="modal-overlay" v-if="showSourceModal" @click.self="showSourceModal=false">
      <div class="modal-content">
        <h3>{{ sourceDetailTitle }}</h3>
        <div v-if="sourceDetailItems?.length">
          <div class="source-item" v-for="(item, i) in sourceDetailItems" :key="i">
            <div class="source-info">
              <strong>{{ item.foodName }}</strong>
              <span style="color:#666;font-size:12px">{{ item.mealType }} · {{ item.amount }}{{ item.unit || '份' }}</span>
            </div>
            <span class="source-value">{{ item.value }}{{ sourceDetailUnit }}</span>
          </div>
        </div>
        <div v-else class="empty-state" style="padding:20px"><p>暂无该营养素的食物来源数据</p></div>
        <button class="btn btn-outline" @click="showSourceModal=false" style="margin-top:12px;width:100%">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, nextTick, watch } from 'vue'
import api from '../api/index.js'
import toast from '../toast.js'

const todayStr = new Date().toISOString().split('T')[0]
const currentDate = ref(todayStr)
const nutrition = ref(null)
const scoreData = ref(null)
const loading = ref(false)
const scoreLoading = ref(false)
const circumference = 2 * Math.PI * 54

// Diet records state
const records = ref([])
const mealTypes = [
  { key: '早餐', label: '早餐', icon: '🍳' },
  { key: '午餐', label: '午餐', icon: '🍚' },
  { key: '晚餐', label: '晚餐', icon: '🍜' },
  { key: '夜宵', label: '夜宵', icon: '🌙' },
  { key: '其他', label: '其他', icon: '🍽️' },
]
const sourceLabels = { photo: '拍照', voice: '语音', manual: '手动' }
const expandedMeals = reactive({ '早餐': true, '午餐': true, '晚餐': false, '夜宵': false, '其他': false })

const groupedRecords = computed(() => {
  const groups = {}
  mealTypes.forEach(m => { groups[m.key] = [] })
  records.value.forEach(r => {
    if (groups[r.mealType] !== undefined) groups[r.mealType].push(r)
    else groups['其他'].push(r)
  })
  return groups
})

const mealTotals = computed(() => {
  const totals = {}
  mealTypes.forEach(m => {
    const items = groupedRecords.value[m.key]
    if (items?.length) {
      totals[m.key] = {
        calorie: items.reduce((s, r) => s + (Number(r.calorie) || 0), 0).toFixed(0),
        protein: items.reduce((s, r) => s + (Number(r.protein) || 0), 0).toFixed(1),
      }
    }
  })
  return totals
})

// Edit modal
const showEditModal = ref(false)
const editForm = ref({ id: null, foodName: '', mealType: '午餐', amount: 1 })

// AI chat state
const aiMessages = ref([])
const aiInput = ref('')
const aiLoading = ref(false)
const chatMsgsRef = ref(null)

// Source detail modal
const showSourceModal = ref(false)
const sourceDetailTitle = ref('')
const sourceDetailUnit = ref('')
const sourceDetailItems = ref([])

// Computed
const isToday = computed(() => currentDate.value === todayStr)
const dashOffset = computed(() => {
  if (!scoreData.value?.score) return circumference
  return circumference - (scoreData.value.score / 100) * circumference
})

// Nutrient config
const nutrientKeyMap = {
  calorie: { label: '热量', unit: 'kcal' },
  protein: { label: '蛋白质', unit: 'g' },
  fat: { label: '脂肪', unit: 'g' },
  carbohydrate: { label: '碳水', unit: 'g' },
}
const nutrients = [
  { key: 'calorie', label: '热量', color: '#FF5722' },
  { key: 'protein', label: '蛋白质', color: '#2196F3' },
  { key: 'fat', label: '脂肪', color: '#FF9800' },
  { key: 'carbohydrate', label: '碳水', color: '#4CAF50' },
]

// Date handling
function changeDate(delta) {
  const d = new Date(currentDate.value + 'T00:00:00')
  d.setDate(d.getDate() + delta)
  if (d > new Date()) return
  currentDate.value = d.toISOString().split('T')[0]
  onDateChange()
}

function goToday() {
  currentDate.value = todayStr
  onDateChange()
}

function onDateChange() {
  fetchAll()
}

// Data fetching
async function fetchAll() {
  loading.value = true
  scoreLoading.value = true
  try {
    const [recRes, nutRes, scoreRes, convRes] = await Promise.all([
      api.getDietRecords(currentDate.value),
      api.getNutrition(currentDate.value),
      api.getHealthScore(currentDate.value),
      api.getAiConversation(currentDate.value),
    ])
    records.value = (recRes.data?.data) || []
    nutrition.value = (nutRes.data?.data) || null
    scoreData.value = (scoreRes.data?.data) || null

    // Auto-expand meals with records
    mealTypes.forEach(m => {
      if (groupedRecords.value[m.key]?.length) expandedMeals[m.key] = true
    })

    // Load conversation history
    const convData = (convRes.data?.data)
    if (convData?.messages?.length) {
      aiMessages.value = convData.messages
    } else {
      aiMessages.value = []
    }
  } catch (e) {
    console.error('获取数据失败', e)
  } finally {
    loading.value = false
    scoreLoading.value = false
  }
}

// Diet record actions
function toggleMeal(key) { expandedMeals[key] = !expandedMeals[key] }

function editRecord(rec) {
  editForm.value = { id: rec.id, foodName: rec.foodName, mealType: rec.mealType, amount: rec.amount }
  showEditModal.value = true
}

async function saveEdit() {
  try {
    await api.updateDietRecord(editForm.value.id, {
      foodName: editForm.value.foodName,
      mealType: editForm.value.mealType,
      amount: editForm.value.amount,
    })
    showEditModal.value = false
    await fetchAll()
  } catch (e) {
    console.error('编辑失败', e)
    toast.show('保存失败：' + (e?.response?.data?.message || e.message || '未知错误'))
  }
}

async function deleteRecord(id) {
  if (!confirm('确定删除这条记录？')) return
  try {
    await api.deleteDietRecord(id)
    await fetchAll()
  } catch (e) {
    console.error('删除失败', e)
    toast.show('删除失败')
  }
}

// AI chat actions
async function startAiAnalysis() {
  aiLoading.value = true
  try {
    const res = await api.sendAiMessage(currentDate.value, '请帮我分析今天的饮食情况，包括营养摄入是否均衡、哪些方面做得好、哪些需要改进。')
    const data = res.data?.data
    if (data?.messages?.length) {
      aiMessages.value = data.messages
    }
  } catch (e) {
    console.error('AI 分析失败', e)
    toast.show('AI 分析失败，请稍后重试')
  } finally {
    aiLoading.value = false
    scrollChatBottom()
  }
}

async function sendAiMessage() {
  const msg = aiInput.value.trim()
  if (!msg || aiLoading.value) return
  aiInput.value = ''
  aiLoading.value = true
  // Optimistic user message
  aiMessages.value.push({ role: 'USER', content: msg, createdAt: new Date().toISOString() })
  scrollChatBottom()
  try {
    const res = await api.sendAiMessage(currentDate.value, msg)
    const data = res.data?.data
    if (data?.messages?.length) {
      // Replace optimistic message with server data (includes AI reply)
      aiMessages.value = data.messages
    }
  } catch (e) {
    console.error('AI 回复失败', e)
    aiMessages.value.push({ role: 'AI', content: '抱歉，AI 服务暂时不可用，请稍后重试。', createdAt: new Date().toISOString() })
  } finally {
    aiLoading.value = false
    scrollChatBottom()
  }
}

function scrollChatBottom() {
  nextTick(() => {
    const el = chatMsgsRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

// Markdown render (simple: bold, newlines)
function renderMarkdown(text) {
  if (!text) return ''
  return text
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}

function formatMsgTime(timeStr) {
  if (!timeStr) return ''
  const d = new Date(timeStr + (timeStr.endsWith('Z') ? '' : 'Z'))
  if (isNaN(d.getTime())) return ''
  return `${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}

// Nutrition helpers
function getValue(key) {
  if (!nutrition.value) return 0
  const v = nutrition.value[key + 'Total']
  return v ? Number(v).toFixed(0) : 0
}
function getGoal(key) {
  if (!nutrition.value) return 0
  const g = nutrition.value[key + 'Goal']
  return g ? Number(g).toFixed(0) : 0
}
function getPercent(key) {
  const v = parseFloat(getValue(key))
  const g = parseFloat(getGoal(key))
  if (!g) return 0
  return Math.min(100, Math.round(v / g * 100))
}
function barHeight(cal) {
  if (!cal) return 0
  const max = Math.max(...nutrition.value.trend.map(p => Number(p.calorie)), 2000)
  return Math.min(100, Number(cal) / max * 100)
}
function isTodayDate(dateStr) { return dateStr === todayStr }
function formatDateShort(dateStr) {
  const d = new Date(dateStr + 'T00:00:00')
  const days = ['日','一','二','三','四','五','六']
  return '周' + days[d.getDay()]
}
function formatScoreDate(dateStr) {
  const d = new Date(dateStr + 'T00:00:00')
  return `${d.getMonth()+1}/${d.getDate()}`
}

async function showSourceDetail(nutrient) {
  const info = nutrientKeyMap[nutrient.key]
  sourceDetailTitle.value = `🔍 ${info.label}食物来源`
  sourceDetailUnit.value = info.unit
  try {
    const res = await api.getDietRecords(currentDate.value)
    const allRecords = (res.data?.data) || []
    const keyTotal = nutrient.key + 'Total'
    sourceDetailItems.value = allRecords
      .filter(r => r[keyTotal] !== undefined && r[keyTotal] !== null)
      .map(r => ({
        foodName: r.foodName,
        mealType: r.mealType,
        amount: r.amount,
        unit: r.unit,
        value: Number(r[keyTotal] || r[nutrient.key] || 0).toFixed(1),
      }))
    if (!sourceDetailItems.value.length) {
      sourceDetailItems.value = allRecords.map(r => ({
        foodName: r.foodName, mealType: r.mealType, amount: r.amount, unit: r.unit, value: '—',
      }))
    }
  } catch (e) { console.error(e); sourceDetailItems.value = [] }
  showSourceModal.value = true
}

onMounted(fetchAll)
</script>

<style scoped>
/* ---- Date selector ---- */
.date-selector {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 16px;
}
.date-input {
  width: auto;
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #333;
  text-align: center;
}
.today-btn { padding: 6px 14px; }

.page-section-title {
  font-size: 17px;
  font-weight: 600;
  margin: 18px 0 10px;
  color: #333;
}

/* ---- Meal accordion ---- */
.meal-card { padding: 0 !important; overflow: hidden; margin-bottom: 10px; border-left: 3px solid #4CAF50; }
.meal-card.expanded { border-left-color: #388E3C; }
.meal-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px; cursor: pointer; user-select: none;
}
.meal-header:active { background: #F5F5F5; }
.meal-header-left { display: flex; align-items: center; gap: 8px; }
.meal-icon { font-size: 18px; }
.meal-name { font-size: 14px; font-weight: 600; }
.meal-count { font-size: 11px; color: #999; background: #f0f0f0; padding: 2px 8px; border-radius: 10px; }
.meal-header-right { display: flex; align-items: center; gap: 8px; }
.meal-summary { font-size: 15px; font-weight: 700; color: #E65100; }
.meal-summary-sub { font-size: 11px; color: #999; }
.meal-arrow { font-size: 14px; color: #bbb; }
.meal-body { padding: 0 14px 10px 14px; }

/* ---- Food item ---- */
.food-item {
  display: flex; justify-content: space-between; align-items: flex-start;
  padding: 10px 12px; background: #FAFAFA; border-radius: 10px; margin-bottom: 8px;
}
.food-main { flex: 1; min-width: 0; }
.food-name-row { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; margin-bottom: 4px; }
.food-name-row strong { font-size: 14px; }
.food-amount { font-size: 12px; color: #666; background: #E8F5E9; padding: 1px 6px; border-radius: 4px; }
.food-source { font-size: 11px; color: #999; background: #f0f0f0; padding: 1px 6px; border-radius: 4px; }
.food-nutrition { display: flex; flex-wrap: wrap; gap: 6px; font-size: 11px; color: #888; }
.food-actions { display: flex; gap: 6px; flex-shrink: 0; margin-left: 8px; }

/* ---- Score ---- */
.score-card { text-align: center; padding: 24px; }
.score-ring { position: relative; width: 120px; height: 120px; margin: 0 auto; }
.score-svg { width: 100%; height: 100%; }
.score-text { position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%); text-align: center; }
.score-number { font-size: 32px; font-weight: 700; color: #4CAF50; display: block; }
.score-unit { font-size: 12px; color: #999; }
.score-empty { color: #999; }

.card-title { font-size: 15px; font-weight: 600; margin-bottom: 8px; }
.tag-list { display: flex; flex-wrap: wrap; gap: 6px; }
.suggestion-list { list-style: none; }
.suggestion-list li { padding: 6px 0; font-size: 14px; color: #555; border-bottom: 1px solid #f0f0f0; }
.suggestion-list li:last-child { border-bottom: none; }
.history-list { display: flex; flex-direction: column; gap: 8px; }
.history-item { display: flex; align-items: center; gap: 8px; }
.history-date { font-size: 12px; color: #999; width: 40px; }
.history-bar-bg { flex: 1; height: 8px; background: #f0f0f0; border-radius: 4px; overflow: hidden; }
.history-bar { height: 100%; border-radius: 4px; transition: width 0.3s; }
.history-bar.good { background: #4CAF50; }
.history-bar.mid { background: #FF9800; }
.history-bar.bad { background: #f44336; }
.history-score { font-size: 12px; color: #666; width: 30px; text-align: right; }

/* ---- Nutrition ---- */
.nutrient-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.nutrient-item {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  cursor: pointer; padding: 4px; border-radius: 10px; transition: background 0.2s;
}
.nutrient-item:hover { background: #f9f9f9; }
.click-hint { font-size: 10px; color: #bbb; }
.progress-ring {
  width: 64px; height: 64px; border-radius: 50%; border: 4px solid;
  display: flex; align-items: center; justify-content: center; background: #fafafa;
}
.progress-value { font-size: 14px; font-weight: 700; }
.nutrient-name { font-size: 13px; color: #666; }
.nutrient-value { font-size: 11px; color: #999; }
.trend-chart { display: flex; align-items: flex-end; gap: 4px; height: 120px; }
.bar-container { flex: 1; display: flex; flex-direction: column; align-items: center; height: 100%; }
.bar-wrapper { flex: 1; width: 100%; display: flex; align-items: flex-end; justify-content: center; }
.bar {
  width: 70%; background: linear-gradient(to top, #81C784, #4CAF50);
  border-radius: 4px 4px 0 0; min-height: 4px; transition: height 0.3s;
}
.bar.today { background: linear-gradient(to top, #FFB74D, #FF9800); }
.bar-label { font-size: 10px; color: #999; margin-top: 4px; }
.suggestion-text { font-size: 14px; line-height: 1.6; color: #555; }

/* ---- AI Chat ---- */
.ai-chat-card { padding: 12px !important; }
.chat-messages {
  max-height: 350px; overflow-y: auto; padding: 8px 4px;
  display: flex; flex-direction: column; gap: 10px;
}
.chat-empty { text-align: center; padding: 24px; color: #999; }
.chat-bubble { display: flex; gap: 8px; }
.chat-bubble.ai { align-self: flex-start; }
.chat-bubble.user { align-self: flex-end; flex-direction: row-reverse; }
.bubble-avatar { font-size: 22px; flex-shrink: 0; margin-top: 2px; }
.bubble-content { max-width: 85%; }
.bubble-text {
  padding: 10px 14px; border-radius: 14px; font-size: 14px; line-height: 1.6;
  word-break: break-word;
}
.chat-bubble.ai .bubble-text { background: #F1F8E9; color: #333; }
.chat-bubble.user .bubble-text { background: #4CAF50; color: #fff; }
.bubble-time { font-size: 10px; color: #bbb; margin-top: 2px; padding: 0 4px; }
.chat-bubble.user .bubble-time { text-align: right; }
.chat-input-area { display: flex; gap: 8px; margin-top: 10px; padding-top: 10px; border-top: 1px solid #f0f0f0; }
.chat-input { flex: 1; padding: 10px 12px; border: 1px solid #ddd; border-radius: 20px; font-size: 14px; }
.chat-send-btn { border-radius: 20px; padding: 10px 20px; }

/* ---- Modals ---- */
.modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5); z-index: 200;
  display: flex; align-items: center; justify-content: center;
}
.modal-content {
  background: #fff; border-radius: 16px; padding: 24px;
  width: 90%; max-width: 400px; max-height: 80vh; overflow-y: auto;
}
.modal-content h3 { margin-bottom: 16px; }
.form-group { margin-bottom: 12px; }
.form-group label { display: block; font-size: 13px; color: #666; margin-bottom: 4px; }
.source-item { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid #f0f0f0; }
.source-info strong { display: block; margin-bottom: 2px; }
.source-value { font-weight: 600; color: #4CAF50; font-size: 14px; }
</style>
```

- [ ] **Step 2: Verify the API response data paths match existing controllers**

The existing controllers return `ApiResponse<T>` where `data` holds the payload. The frontend `api/index.js` interceptor returns `response`, so callers access `.data.data` for the payload. The existing NutritionView already uses `res.data.data` pattern for nutrition and score, and RecordView directly uses `res` (because the API methods unwrap). Check:
- `api.getDietRecords`: returns `api.get(...)` (axios response), caller uses `recRes.data?.data`
- `api.getNutrition`: returns `api.get(...)`, caller uses `nutRes.data?.data`
- `api.getHealthScore`: returns `api.get(...)`, caller uses `scoreRes.data?.data`
- These are consistent with existing callers.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/NutritionView.vue
git commit -m "feat: rewrite NutritionView with date picker, diet records, and AI chat"
```

---

### Task 11: End-to-End Verification

**Files:**
- Verify: `backend/src/main/java/com/health/diet/` (all new files compile)
- Verify: `frontend/src/` (all changes work together)

- [ ] **Step 1: Verify backend compiles**

```bash
cd backend && mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 2: Verify frontend builds**

```bash
cd frontend && npm run build 2>&1 | tail -5
```
Expected: Build completes without errors (vite build)

- [ ] **Step 3: Start backend and test AI endpoint**

```bash
cd backend && mvn spring-boot:run &
# Wait for startup, then:
curl -X POST http://localhost:8080/api/ai/analyze-diet \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"date":"2026-06-20","message":"帮我分析今天的饮食"}'
```
Expected: Returns `{"code":200,"data":{"conversationId":1,"messages":[...]}}`

- [ ] **Step 4: Final commit (if any fixes needed)**

```bash
git add -A && git commit -m "chore: final adjustments after e2e verification"
```

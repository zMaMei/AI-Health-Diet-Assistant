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

        // 7. 加载全部消息并返回
        List<AiMessage> allMessages = messageRepo.findByConversationIdOrderByCreatedAtAsc(conv.getId());
        return buildVO(conv.getId(), allMessages);
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

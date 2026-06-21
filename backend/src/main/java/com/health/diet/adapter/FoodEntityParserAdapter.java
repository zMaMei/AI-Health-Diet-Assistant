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
 * 食物实体解析适配器 — 调用通义千问文本模型从自然语言中提取食物信息。
 * 对应设计文档 9 节 FoodEntityParserAdapter 类。
 */
/* 食物实体解析适配器 */
@Component
public class FoodEntityParserAdapter {

    private static final Logger log = LoggerFactory.getLogger(FoodEntityParserAdapter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final DashScopeConfig config;
    private final RestClient restClient;

    public FoodEntityParserAdapter(DashScopeConfig config) {
        this.config = config;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getTimeout());
        factory.setReadTimeout(config.getTimeout());
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /**
     * 从自然语言文本中提取食物实体。
     *
     * @param text 用户输入的文本（例如语音转写结果）
     * @return 提取到的食物实体列表
     */
    public List<FoodEntity> extractFoodEntities(String text) {
        log.info("调用千问文本 API 提取食物实体: {}", text);

        String systemPrompt = """
            你是一个专业的食物实体提取与营养分析助手。从用户的饮食描述中提取食物信息，
            并估算每种食物的营养成分。

            对每种食物给出：
            - foodName: 食物中文名称
            - amount: 份量(数字)
            - unit: 单位（碗/个/份/杯/g/ml）
            - mealType: 餐次（早餐/午餐/晚餐/夜宵/其他）
            - calorie: 估算热量(kcal)
            - protein: 估算蛋白质(g)
            - fat: 估算脂肪(g)
            - carbohydrate: 估算碳水化合物(g)
            - sugar: 估算糖(g)
            - sodium: 估算钠(mg)

            **重要规则：复合菜品（如"韭菜炒鸡蛋""番茄炒蛋""青椒肉丝"）必须作为一个完整食物，
            不要拆分为单独的食材。** 一道菜的营养成分按整道菜估算。
            餐次从上下文推断，默认为"午餐"。单位从上下文推断，默认为"份"。

            严格按以下JSON格式返回，不要包含其他文字：
            {"entities":[{"foodName":"米饭","amount":1,"unit":"碗","mealType":"午餐","calorie":116,"protein":2.6,"fat":0.3,"carbohydrate":25.9,"sugar":0.1,"sodium":2.0}]}
            """;

        /* 构建食物实体解析请求 */
        // 使用多模态端点（纯文本内容格式），避免兼容模式 403
        Map<String, Object> body = Map.of(
            "model", config.getModel(),
            "input", Map.of("messages", List.of(
                Map.of("role", "system", "content", List.of(Map.of("text", systemPrompt))),
                Map.of("role", "user", "content", List.of(Map.of("text",
                        "请从以下文本中提取食物信息：" + text)))
            )),
            "parameters", Map.of("max_tokens", config.getMaxTokens())
        );

        try {
            /* 调用AI提取食物实体 */
            String resp = restClient.post()
                    .uri(config.getMultimodalUrl())
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            log.debug("食物实体提取响应: {}", resp);
            /* 解析食物实体JSON */
            return parseEntities(resp);

        } catch (Exception e) {
            /* 降级处理 */
            log.error("千问实体提取失败", e);
            throw new RuntimeException("AI 食物解析服务暂时不可用，请重试", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<FoodEntity> parseEntities(String rawJson) throws Exception {
        Map<String, Object> root = objectMapper.readValue(rawJson, new TypeReference<>() {});

        /* 兼容多种响应格式 */
        // 解析响应文本：先试多模态格式，再试 OpenAI 兼容格式
        String jsonText;
        try {
            // 多模态格式: output.choices[0].message.content[0].text
            Map<String, Object> output = (Map<String, Object>) root.get("output");
            if (output != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) output.get("choices");
                Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                List<Map<String, Object>> content = (List<Map<String, Object>>) msg.get("content");
                jsonText = (String) content.get(0).get("text");
            } else {
                // OpenAI 兼容格式: choices[0].message.content
                List<Map<String, Object>> choices = (List<Map<String, Object>>) root.get("choices");
                Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                Object content = msg.get("content");
                if (content instanceof String s) jsonText = s;
                else if (content instanceof List<?> l && !l.isEmpty()) {
                    jsonText = (String) ((Map<String, Object>) l.get(0)).get("text");
                } else {
                    jsonText = "";
                }
            }
        } catch (Exception e) {
            jsonText = rawJson;
        }

        // 清理 markdown
        jsonText = jsonText.trim();
        if (jsonText.startsWith("```")) {
            int s = jsonText.indexOf('\n') + 1;
            int e = jsonText.lastIndexOf("```");
            if (s > 0 && e > s) jsonText = jsonText.substring(s, e).trim();
        }

        Map<String, Object> entitiesRoot = objectMapper.readValue(jsonText, new TypeReference<>() {});
        List<Map<String, Object>> entities = (List<Map<String, Object>>) entitiesRoot.get("entities");
        if (entities == null || entities.isEmpty()) {
            log.warn("API 未返回食物实体");
            return List.of();
        }

        return entities.stream().map(e -> new FoodEntity(
            (String) e.get("foodName"),
            toDouble(e, "amount", 1.0),
            (String) e.getOrDefault("unit", "份"),
            (String) e.getOrDefault("mealType", "午餐"),
            bd(toDouble(e, "calorie", 0)),
            bd(toDouble(e, "protein", 0)),
            bd(toDouble(e, "fat", 0)),
            bd(toDouble(e, "carbohydrate", 0)),
            bd(toDouble(e, "sugar", 0)),
            bd(toDouble(e, "sodium", 0))
        )).toList();
    }

    private double toDouble(Map<String, Object> map, String key, double def) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) { try { return Double.parseDouble(s); } catch (Exception ignored) {} }
        return def;
    }

    private BigDecimal bd(double v) {
        return BigDecimal.valueOf(Math.round(v * 100.0) / 100.0);
    }

    // ==================== FoodEntity 记录 ====================

    public record FoodEntity(
            String foodName,
            double amount,
            String unit,
            String mealType,
            BigDecimal calorie,
            BigDecimal protein,
            BigDecimal fat,
            BigDecimal carbohydrate,
            BigDecimal sugar,
            BigDecimal sodium
    ) {}
}

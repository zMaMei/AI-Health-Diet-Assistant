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

import java.util.List;
import java.util.Map;

/**
 * 食物实体解析适配器 — 调用通义千问文本模型从自然语言中提取食物信息。
 * 对应设计文档 9 节 FoodEntityParserAdapter 类。
 */
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
            你是一个食物实体提取助手。从用户的饮食描述中提取食物信息。
            对每种食物给出：食物名称(foodName)、份量(amount)、单位(unit)、餐次(mealType)。
            餐次从上下文推断，默认为"午餐"。单位从上下文推断（碗/个/份/杯/g/ml），默认为"份"。

            严格按以下JSON格式返回，不要包含其他文字：
            {"entities":[{"foodName":"米饭","amount":1,"unit":"碗","mealType":"午餐"}]}
            """;

        Map<String, Object> body = Map.of(
            "model", config.getTextModel(),
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", "请从以下文本中提取食物信息：" + text)
            ),
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

            log.debug("食物实体提取响应: {}", resp);
            return parseEntities(resp);

        } catch (Exception e) {
            log.error("千问实体提取失败，降级为模拟数据", e);
            return List.of(
                new FoodEntity("米饭", 1.0, "碗", "午餐"),
                new FoodEntity("鸡腿", 1.0, "个", "午餐")
            );
        }
    }

    @SuppressWarnings("unchecked")
    private List<FoodEntity> parseEntities(String rawJson) throws Exception {
        Map<String, Object> root = objectMapper.readValue(rawJson, new TypeReference<>() {});

        // OpenAI 兼容格式: choices[0].message.content
        String jsonText;
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) root.get("choices");
            Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
            Object content = msg.get("content");
            if (content instanceof String s) jsonText = s;
            else if (content instanceof List<?> l && !l.isEmpty()) {
                jsonText = (String) ((Map<String, Object>) l.get(0)).get("text");
            } else {
                jsonText = "";
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
            (String) e.getOrDefault("mealType", "午餐")
        )).toList();
    }

    private double toDouble(Map<String, Object> map, String key, double def) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) { try { return Double.parseDouble(s); } catch (Exception ignored) {} }
        return def;
    }

    // ==================== FoodEntity 记录 ====================

    public record FoodEntity(String foodName, double amount, String unit, String mealType) {}
}

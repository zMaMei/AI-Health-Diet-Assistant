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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 推荐适配器 — 调用 DashScope 多模态模型根据用户营养缺口 + 菜谱库生成个性化推荐。
 * 使用 multimodalUrl（与 ThresholdAnalysisAdapter 一致，已验证可用）。
 */
@Component
public class RecommendationAdapter {

    private static final Logger log = LoggerFactory.getLogger(RecommendationAdapter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final DashScopeConfig config;
    private final RestClient restClient;

    public RecommendationAdapter(DashScopeConfig config) {
        this.config = config;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getTimeout());
        factory.setReadTimeout(Math.max(config.getTimeout(), 30000));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /**
     * 调用 AI 分析并返回推荐结果。
     * @param prompt 包含用户画像 + 营养缺口 + 菜谱库摘要的完整 prompt
     * @return AI 推荐的菜谱列表
     */
    public RecommendationResult analyze(String prompt) {
        log.info("调用 DashScope 多模态模型进行智能推荐");

        // 使用 multimodal 端点格式（与 ThresholdAnalysisAdapter 一致）
        Map<String, Object> body = Map.of(
            "model", config.getModel(),
            "input", Map.of("messages", List.of(
                Map.of("role", "system", "content", List.of(Map.of("text",
                    "你是一位专业的营养师。请严格以 JSON 格式返回，不要包含其他文字。"))),
                Map.of("role", "user", "content", List.of(Map.of("text", prompt)))
            )),
            "parameters", Map.of("max_tokens", 2000)
        );

        try {
            String resp = restClient.post()
                    .uri(config.getMultimodalUrl())
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            log.debug("AI 推荐原始响应: {}", resp);
            return parseResult(resp);
        } catch (Exception e) {
            log.error("AI 推荐分析失败", e);
            throw new RuntimeException("AI 推荐服务暂时不可用，请稍后重试", e);
        }
    }

    @SuppressWarnings("unchecked")
    private RecommendationResult parseResult(String rawJson) throws Exception {
        // 多模态 API 响应格式：output.choices[0].message.content[0].text
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

        // 提取 JSON 对象
        Matcher m = Pattern.compile("\\{[\\s\\S]*\\}").matcher(jsonText);
        if (m.find()) {
            jsonText = m.group();
        }

        Map<String, Object> resultMap = objectMapper.readValue(jsonText, new TypeReference<>() {});

        List<Map<String, Object>> recipeList = (List<Map<String, Object>>) resultMap.get("recipes");
        if (recipeList == null || recipeList.isEmpty()) {
            throw new RuntimeException("AI 未返回推荐菜谱");
        }

        List<RecommendedRecipe> recipes = recipeList.stream()
                .map(r -> {
                    Long recipeId = toLong(r.get("recipeId"));
                    String reason = (String) r.get("reason");
                    BigDecimal score = toBigDecimal(r.get("score"));
                    return new RecommendedRecipe(recipeId, reason, score);
                })
                .filter(r -> r.recipeId() != null)
                .limit(5)
                .toList();

        if (recipes.isEmpty()) {
            throw new RuntimeException("AI 返回的推荐列表为空");
        }

        return new RecommendationResult(recipes);
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
        // 兼容 OpenAI 格式：choices[0].message.content
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) root.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                String text = (String) msg.get("content");
                if (text != null) return text;
            }
        } catch (Exception ignored) {}
        return "";
    }

    private Long toLong(Object v) {
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) {
            try { return Long.parseLong(s); } catch (Exception ignored) {}
        }
        return null;
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        if (v instanceof String s) {
            try { return new BigDecimal(s); } catch (Exception ignored) {}
        }
        return BigDecimal.valueOf(50);
    }

    public record RecommendedRecipe(Long recipeId, String reason, BigDecimal score) {}

    public record RecommendationResult(List<RecommendedRecipe> recipes) {}

    // ── AI 直接创造菜谱 ──────────────────────────────────────────

    /**
     * 调用 AI 直接创造新菜谱（换一批用）。
     */
    public FreshResult analyzeFresh(String prompt) {
        log.info("调用 AI 创造新菜谱");

        Map<String, Object> body = Map.of(
            "model", config.getModel(),
            "input", Map.of("messages", List.of(
                Map.of("role", "system", "content", List.of(Map.of("text",
                    "你是一位创意营养厨师。请严格以 JSON 格式返回，不要包含其他文字。"))),
                Map.of("role", "user", "content", List.of(Map.of("text", prompt)))
            )),
            "parameters", Map.of("max_tokens", 2000)
        );

        try {
            String resp = restClient.post()
                    .uri(config.getMultimodalUrl())
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            log.debug("AI 创造菜谱原始响应: {}", resp);
            return parseFreshResult(resp);
        } catch (Exception e) {
            log.error("AI 创造菜谱失败", e);
            throw new RuntimeException("AI 推荐服务暂时不可用，请稍后重试", e);
        }
    }

    @SuppressWarnings("unchecked")
    private FreshResult parseFreshResult(String rawJson) throws Exception {
        String jsonText;
        try {
            Map<String, Object> root = objectMapper.readValue(rawJson, new TypeReference<>() {});
            jsonText = extractTextFromResponse(root);
        } catch (Exception e) {
            jsonText = rawJson;
        }

        jsonText = jsonText.trim();
        if (jsonText.startsWith("```")) {
            int start = jsonText.indexOf('\n') + 1;
            int end = jsonText.lastIndexOf("```");
            if (start > 0 && end > start) {
                jsonText = jsonText.substring(start, end).trim();
            }
        }

        Matcher m = Pattern.compile("\\{[\\s\\S]*\\}").matcher(jsonText);
        if (m.find()) {
            jsonText = m.group();
        }

        Map<String, Object> resultMap = objectMapper.readValue(jsonText, new TypeReference<>() {});

        List<Map<String, Object>> recipeList = (List<Map<String, Object>>) resultMap.get("recipes");
        if (recipeList == null || recipeList.isEmpty()) {
            throw new RuntimeException("AI 未返回菜谱");
        }

        List<FreshRecipeData> recipes = recipeList.stream()
                .map(r -> new FreshRecipeData(
                    (String) r.get("name"),
                    (String) r.get("ingredients"),
                    (String) r.get("steps"),
                    (String) r.get("tags"),
                    toBigDecimal(r.get("calorie")),
                    toBigDecimal(r.get("protein")),
                    toBigDecimal(r.get("fat")),
                    toBigDecimal(r.get("carbohydrate")),
                    toBigDecimal(r.get("sugar")),
                    toBigDecimal(r.get("sodium")),
                    (String) r.get("reason"),
                    toBigDecimal(r.get("score"))
                ))
                .filter(r -> r.name() != null && !r.name().isBlank())
                .limit(5)
                .toList();

        if (recipes.isEmpty()) {
            throw new RuntimeException("AI 返回的菜谱列表为空");
        }

        return new FreshResult(recipes);
    }

    public record FreshRecipeData(
        String name, String ingredients, String steps, String tags,
        BigDecimal calorie, BigDecimal protein, BigDecimal fat, BigDecimal carbohydrate,
        BigDecimal sugar, BigDecimal sodium,
        String reason, BigDecimal score
    ) {}

    public record FreshResult(List<FreshRecipeData> recipes) {}
}

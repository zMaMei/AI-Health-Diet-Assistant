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
 * AI 推荐适配器 — 调用 DashScope 文本模型根据用户营养缺口 + 菜谱库生成个性化推荐。
 * 使用 OpenAI 兼容 textUrl（比 multimodalUrl 更便宜）。
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
        factory.setReadTimeout(Math.max(config.getTimeout(), 30000)); // 推荐可能需要更长时间
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /**
     * 调用 AI 分析并返回推荐结果。
     * @param prompt 包含用户画像 + 营养缺口 + 菜谱库摘要的完整 prompt
     * @return AI 推荐的菜谱列表
     */
    public RecommendationResult analyze(String prompt) {
        log.info("调用 DashScope 文本模型进行智能推荐");

        Map<String, Object> body = Map.of(
            "model", config.getTextModel(),
            "messages", List.of(
                Map.of("role", "system", "content",
                    "你是一位专业的营养师。请严格以 JSON 格式返回，不要包含其他文字。"),
                Map.of("role", "user", "content", prompt)
            ),
            "max_tokens", 1500
        );

        try {
            String resp = restClient.post()
                    .uri(config.getTextUrl())
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
        // OpenAI 兼容格式响应：choices[0].message.content
        String jsonText;
        try {
            Map<String, Object> root = objectMapper.readValue(rawJson, new TypeReference<>() {});
            List<Map<String, Object>> choices = (List<Map<String, Object>>) root.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                jsonText = (String) msg.get("content");
            } else {
                jsonText = rawJson;
            }
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

        // 提取 JSON 对象/数组
        Matcher m = Pattern.compile("\\{[\\s\\S]*\\}").matcher(jsonText);  // 跨行匹配
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
        return BigDecimal.valueOf(50); // 默认分数
    }

    public record RecommendedRecipe(Long recipeId, String reason, BigDecimal score) {}

    public record RecommendationResult(List<RecommendedRecipe> recipes) {}
}

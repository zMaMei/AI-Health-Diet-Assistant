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
/* 阈值分析适配器 */
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
    /* 构建用户档案上下文 */
    public ThresholdResult analyze(String prompt) {
        log.info("调用千问 API 分析预警阈值");

        /* 计算BMI */
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
            /* 调用AI生成个性化阈值 */
            String resp = restClient.post()
                    .uri(config.getMultimodalUrl())
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            log.debug("AI 原始响应: {}", resp);
            /* 解析阈值结果 */
            return parseResult(resp);
        } catch (Exception e) {
            /* 降级处理 */
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
        // protein/fat/carb are optional — null if AI doesn't return them
        BigDecimal protein = toBigDecimal(map, "protein");
        BigDecimal fat = toBigDecimal(map, "fat");
        BigDecimal carb = toBigDecimal(map, "carb");

        if (calorie == null || sugar == null || sodium == null) {
            throw new RuntimeException("AI 返回数据不完整，缺少必要字段");
        }
        if (calorie.compareTo(BigDecimal.ZERO) <= 0
                || sugar.compareTo(BigDecimal.ZERO) <= 0
                || sodium.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("AI 返回阈值无效（必须大于 0）");
        }

        return new ThresholdResult(calorie, sugar, sodium, protein, fat, carb);
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

    public record ThresholdResult(BigDecimal calorie, BigDecimal sugar, BigDecimal sodium,
                                   BigDecimal protein, BigDecimal fat, BigDecimal carb) {}
}

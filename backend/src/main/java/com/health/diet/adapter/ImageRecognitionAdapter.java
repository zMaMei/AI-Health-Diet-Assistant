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
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 图像识别适配器 — 调用通义千问 qwen-omni-turbo 多模态模型识别食物图片。
 * 对应设计文档 9 节 ImageRecognitionAdapter 类。
 */
/* 图片识别适配器 */
@Component
public class ImageRecognitionAdapter {

    private static final Logger log = LoggerFactory.getLogger(ImageRecognitionAdapter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final DashScopeConfig config;
    private final RestClient restClient;

    public ImageRecognitionAdapter(DashScopeConfig config) {
        this.config = config;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getTimeout());
        factory.setReadTimeout(config.getTimeout());
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /**
     * 识别图片中的食物并估算营养成分。
     *
     * @param imageBytes  图片字节
     * @param contentType MIME 类型 (image/jpeg, image/png …)
     * @return 识别到的食物标签列表（含营养估算）
     */
    public List<FoodLabel> detectFood(byte[] imageBytes, String contentType) {
        /* 图片Base64编码 */
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        String mime = resolveMime(contentType);

        log.info("调用千问多模态 API 识别食物图片, size={} bytes, mime={}", imageBytes.length, mime);

        /* 构建营养估算提示词 */
        /* 复合菜品不拆分（Prompt约束） */
        String systemPrompt = """
            你是一个专业的营养分析AI助手。请仔细观察图片中的食物，对每一种识别到的食物：
            1. 给出食物中文名称
            2. 给出置信度（0.0-1.0）
            3. 估算每份的营养成分（热量/kcal, 蛋白质/g, 脂肪/g, 碳水化合物/g, 糖/g, 钠/mg）

            **重要规则：复合菜品（如"韭菜炒鸡蛋""番茄炒蛋""青椒肉丝"）必须作为一个完整食物识别，
            不要拆分为单独的食材。** 图片中每一道完整的菜是一个食物条目，营养按整道菜估算。

            严格按以下JSON格式返回，不要包含其他文字：
            {"foods":[{"name":"食物名","confidence":0.95,"calorie":120,"protein":2.5,"fat":0.3,"carbohydrate":26.0,"sugar":0.1,"sodium":2.0}]}
            """;

        /* 构建图片识别请求 */
        Map<String, Object> body = buildMultimodalBody(systemPrompt,
                "请识别图片中所有的食物（每道菜作为一个整体）并估算营养成分。",
                "image", "data:" + mime + ";base64," + base64);

        try {
            /* 调用DashScope多模态API */
            String resp = callApi(body);
            /* 解析AI返回的食物列表 */
            return parseFoodLabels(resp);
        } catch (Exception e) {
            /* 降级处理 */
            log.error("千问图片识别失败", e);
            throw new RuntimeException("AI 图片识别服务暂时不可用，请重试", e);
        }
    }

    /**
     * 根据食物名称分析营养成分（用于手动添加的"智能分析"）。
     */
    /* 食物名称文本分析 */
    public FoodLabel analyzeFoodByName(String foodName) {
        log.info("调用千问文本 API 分析食物营养: {}", foodName);

        /* 构建文本分析提示词 */
        String systemPrompt = """
            你是一个专业的营养分析AI助手。请根据食物名称估算营养成分。
            严格按以下JSON格式返回，不要包含其他文字：
            {"foods":[{"name":"食物名","confidence":0.90,"calorie":120,"protein":2.5,"fat":0.3,"carbohydrate":26.0,"sugar":0.1,"sodium":2.0}]}
            """;

        // 使用多模态端点（纯文本），避免兼容模式 403
        Map<String, Object> body = buildTextMultimodalBody(systemPrompt,
                "请分析以下食物的营养成分：" + foodName);

        try {
            String resp = callApi(body);
            List<FoodLabel> labels = parseFoodLabels(resp);
            return labels.isEmpty() ? null : labels.get(0);
        } catch (Exception e) {
            log.error("千问文本分析失败", e);
            throw new RuntimeException("AI 文本分析服务暂时不可用，请重试", e);
        }
    }

    // ==================== 请求构建 ====================

    /** 构建多模态 API 请求体（支持 image / audio） */
    private Map<String, Object> buildMultimodalBody(String system, String prompt,
                                                     String mediaType, String dataUrl) {
        // 多模态 API 使用本地文件引用方式：{ "image": "data:..." }
        return Map.of(
            "model", config.getModel(),
            "input", Map.of("messages", List.of(
                Map.of("role", "system", "content", List.of(Map.of("text", system))),
                Map.of("role", "user", "content", List.of(
                    Map.of("text", prompt),
                    Map.of(mediaType, dataUrl)
                ))
            )),
            "parameters", Map.of("max_tokens", config.getMaxTokens())
        );
    }

    /** 构建纯文本多模态请求体（无图片，用多模态端点避免 403） */
    private Map<String, Object> buildTextMultimodalBody(String system, String prompt) {
        return Map.of(
            "model", config.getModel(),
            "input", Map.of("messages", List.of(
                Map.of("role", "system", "content", List.of(Map.of("text", system))),
                Map.of("role", "user", "content", List.of(Map.of("text", prompt)))
            )),
            "parameters", Map.of("max_tokens", config.getMaxTokens())
        );
    }

    /** 构建文本 API 请求体（OpenAI 兼容格式，备用） */
    private Map<String, Object> buildTextBody(String system, String prompt) {
        return Map.of(
            "model", config.getTextModel(),
            "messages", List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", prompt)
            ),
            "max_tokens", config.getMaxTokens()
        );
    }

    // ==================== API 调用 ====================

    private String callApi(Map<String, Object> body) {
        return restClient.post()
                .uri(config.getMultimodalUrl())
                .header("Authorization", "Bearer " + config.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
    }

    private String callTextApi(Map<String, Object> body) {
        return restClient.post()
                .uri(config.getTextUrl())
                .header("Authorization", "Bearer " + config.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
    }

    // ==================== 响应解析 ====================

    @SuppressWarnings("unchecked")
    private List<FoodLabel> parseFoodLabels(String rawJson) throws Exception {
        log.debug("API 原始响应: {}", rawJson);

        // 先尝试多模态 API 响应格式: output.choices[0].message.content[0].text
        // 再尝试 OpenAI 兼容格式: choices[0].message.content
        String jsonText;
        try {
            Map<String, Object> root = objectMapper.readValue(rawJson, new TypeReference<>() {});
            jsonText = extractTextFromResponse(root);
        } catch (Exception e) {
            // 如果顶层解析失败，尝试直接当作 JSON 解析
            jsonText = rawJson;
        }

        // 去除 markdown 代码块
        jsonText = cleanJsonText(jsonText);

        // AI 可能返回 {"foods":[...]} 对象，也可能直接返回 [...] 数组
        List<Map<String, Object>> foods;
        if (jsonText.trim().startsWith("[")) {
            foods = objectMapper.readValue(jsonText, new TypeReference<List<Map<String, Object>>>() {});
        } else {
            Map<String, Object> foodsRoot = objectMapper.readValue(jsonText, new TypeReference<>() {});
            foods = (List<Map<String, Object>>) foodsRoot.get("foods");
        }
        if (foods == null || foods.isEmpty()) {
            log.warn("API 未返回食物数据");
            return List.of();
        }

        return foods.stream().map(f -> new FoodLabel(
                (String) f.get("name"),
                toDouble(f, "confidence", 0.8),
                bd(toDouble(f, "calorie", 0)),
                bd(toDouble(f, "protein", 0)),
                bd(toDouble(f, "fat", 0)),
                bd(toDouble(f, "carbohydrate", 0)),
                bd(toDouble(f, "sugar", 0)),
                bd(toDouble(f, "sodium", 0))
        )).toList();
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> root) {
        try {
            // 多模态格式: output.choices[0].message.content[0].text
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

        try {
            // OpenAI 兼容格式: choices[0].message.content
            List<Map<String, Object>> choices = (List<Map<String, Object>>) root.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                Object content = msg.get("content");
                if (content instanceof String s) return s;
                if (content instanceof List<?> l && !l.isEmpty()) {
                    Object first = l.get(0);
                    if (first instanceof Map<?,?> m) return (String) m.get("text");
                }
            }
        } catch (Exception ignored) {}

        return "";
    }

    private String cleanJsonText(String text) {
        text = text.trim();
        if (text.startsWith("```")) {
            int start = text.indexOf('\n') + 1;
            int end = text.lastIndexOf("```");
            if (start > 0 && end > start) {
                text = text.substring(start, end).trim();
            }
        }
        return text;
    }

    // ==================== 工具方法 ====================

    private String resolveMime(String ct) {
        if (ct == null) return "image/jpeg";
        return switch (ct.toLowerCase()) {
            case "image/png" -> "image/png";
            case "image/gif" -> "image/gif";
            case "image/webp" -> "image/webp";
            default -> "image/jpeg";
        };
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

    // ==================== FoodLabel 记录 ====================

    public record FoodLabel(
            String label,
            double confidence,
            BigDecimal calorie,
            BigDecimal protein,
            BigDecimal fat,
            BigDecimal carbohydrate,
            BigDecimal sugar,
            BigDecimal sodium
    ) {}
}

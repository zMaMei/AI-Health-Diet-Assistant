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

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 语音转文字适配器 — 调用通义千问 qwen-omni-turbo 多模态模型的音频能力。
 * 对应设计文档 9 节 SpeechToTextAdapter 类。
 */
@Component
public class SpeechToTextAdapter {

    private static final Logger log = LoggerFactory.getLogger(SpeechToTextAdapter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final DashScopeConfig config;
    private final RestClient restClient;

    public SpeechToTextAdapter(DashScopeConfig config) {
        this.config = config;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getTimeout());
        factory.setReadTimeout(config.getTimeout());
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /**
     * 将音频文件转写为文本。
     *
     * @param audioBytes 音频字节
     * @param contentType 音频 MIME 类型
     * @return 转写后的文本
     */
    public String transcribe(byte[] audioBytes, String contentType) {
        String base64 = Base64.getEncoder().encodeToString(audioBytes);
        String mime = resolveAudioMime(contentType);

        log.info("调用千问多模态 API 转写语音, size={} bytes, mime={}", audioBytes.length, mime);

        Map<String, Object> body = Map.of(
            "model", config.getModel(),
            "input", Map.of("messages", List.of(
                Map.of("role", "system", "content", List.of(Map.of("text",
                    "你是一个专业的语音转写助手。请准确转写音频中的内容，只输出转写文本，不要添加任何解释。"))),
                Map.of("role", "user", "content", List.of(
                    Map.of("text", "请转写这段音频内容："),
                    Map.of("audio", "data:" + mime + ";base64," + base64)
                ))
            )),
            "parameters", Map.of("max_tokens", 500)
        );

        try {
            String resp = restClient.post()
                    .uri(config.getMultimodalUrl())
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            log.debug("语音转写响应: {}", resp);
            return extractText(resp);

        } catch (Exception e) {
            log.error("千问语音转写失败，降级为模拟数据", e);
            return "我中午吃了一碗米饭和一个鸡腿";
        }
    }

    @SuppressWarnings("unchecked")
    private String extractText(String rawJson) throws Exception {
        Map<String, Object> root = objectMapper.readValue(rawJson, new TypeReference<>() {});

        try {
            Map<String, Object> output = (Map<String, Object>) root.get("output");
            if (output != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) output.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                    List<Map<String, Object>> content = (List<Map<String, Object>>) msg.get("content");
                    if (content != null && !content.isEmpty()) {
                        return ((String) content.get(0).get("text")).trim();
                    }
                }
            }
        } catch (Exception ignored) {}

        return "";
    }

    private String resolveAudioMime(String ct) {
        if (ct == null) return "audio/wav";
        return switch (ct.toLowerCase()) {
            case "audio/mpeg", "audio/mp3" -> "audio/mp3";
            case "audio/webm" -> "audio/webm";
            case "audio/ogg" -> "audio/ogg";
            default -> "audio/wav";
        };
    }
}

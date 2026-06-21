package com.health.diet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai.dashscope")
/* 阿里云百炼AI配置 */
public class DashScopeConfig {

    /* AI API密钥 */
    private String apiKey;

    /* 多模态模型名称 */
    private String model = "qwen-omni-turbo";

    /* 纯文本模型名称 */
    private String textModel = "qwen-turbo-latest";

    /* 多模态API地址 */
    private String multimodalUrl = "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";

    /* 文本API地址 */
    private String textUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    /* 最大Token数 */
    private int maxTokens = 1000;
    /* 请求超时时间 */
    private int timeout = 20000;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getTextModel() { return textModel; }
    public void setTextModel(String textModel) { this.textModel = textModel; }
    public String getMultimodalUrl() { return multimodalUrl; }
    public void setMultimodalUrl(String multimodalUrl) { this.multimodalUrl = multimodalUrl; }
    public String getTextUrl() { return textUrl; }
    public void setTextUrl(String textUrl) { this.textUrl = textUrl; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }
}

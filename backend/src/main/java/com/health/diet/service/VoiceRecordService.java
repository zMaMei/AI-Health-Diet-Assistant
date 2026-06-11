package com.health.diet.service;

import com.health.diet.adapter.SpeechToTextAdapter;
import com.health.diet.adapter.FoodEntityParserAdapter;
import com.health.diet.dto.vo.VoiceParseResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 语音记录服务 — 调用语音转写适配器和食物实体解析适配器，
 * 完成"音频→文本→食物实体"的完整流程。
 * 对应设计文档 9 节 VoiceRecordService 类。
 */
@Service
public class VoiceRecordService {

    private static final Logger log = LoggerFactory.getLogger(VoiceRecordService.class);

    private final SpeechToTextAdapter speechToTextAdapter;
    private final FoodEntityParserAdapter foodEntityParserAdapter;

    public VoiceRecordService(SpeechToTextAdapter speechToTextAdapter,
                               FoodEntityParserAdapter foodEntityParserAdapter) {
        this.speechToTextAdapter = speechToTextAdapter;
        this.foodEntityParserAdapter = foodEntityParserAdapter;
    }

    /**
     * 解析语音文件：先转写为文字，再从文字中提取食物实体。
     *
     * @param audioBytes  音频文件字节
     * @param contentType 音频 MIME 类型
     * @return 转写文本 + 食物实体列表
     */
    public VoiceParseResultVO parseVoice(byte[] audioBytes, String contentType) {
        // 步骤1：语音→文字
        String transcribed = speechToTextAdapter.transcribe(audioBytes, contentType);
        log.info("语音转写完成: {}", transcribed);

        // 步骤2：文字→食物实体
        List<FoodEntityParserAdapter.FoodEntity> entities =
                foodEntityParserAdapter.extractFoodEntities(transcribed);
        log.info("食物实体提取完成: {} 个", entities.size());

        VoiceParseResultVO result = new VoiceParseResultVO();
        result.setTranscribedText(transcribed);
        result.setFoodEntities(entities.stream()
                .map(e -> new VoiceParseResultVO.FoodEntity(
                        e.foodName(), e.amount(), e.unit(), e.mealType()))
                .toList());

        return result;
    }
}

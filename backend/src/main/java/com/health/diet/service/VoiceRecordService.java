package com.health.diet.service;

import com.health.diet.adapter.SpeechToTextAdapter;
import com.health.diet.adapter.FoodEntityParserAdapter;
import com.health.diet.dto.vo.VoiceParseResultVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VoiceRecordService {

    private final SpeechToTextAdapter speechToTextAdapter;
    private final FoodEntityParserAdapter foodEntityParserAdapter;

    public VoiceRecordService(SpeechToTextAdapter speechToTextAdapter,
                               FoodEntityParserAdapter foodEntityParserAdapter) {
        this.speechToTextAdapter = speechToTextAdapter;
        this.foodEntityParserAdapter = foodEntityParserAdapter;
    }

    public VoiceParseResultVO parseVoice(String audioUrl) {
        String transcribed = speechToTextAdapter.transcribe(audioUrl);
        List<FoodEntityParserAdapter.FoodEntity> entities = foodEntityParserAdapter.extractFoodEntities(transcribed);

        VoiceParseResultVO result = new VoiceParseResultVO();
        result.setTranscribedText(transcribed);
        result.setFoodEntities(entities.stream()
                .map(e -> new VoiceParseResultVO.FoodEntity(
                        e.foodName(), e.amount(), e.unit(), e.mealType()))
                .toList());

        return result;
    }
}

package com.health.diet.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SpeechToTextAdapter {

    private static final Logger log = LoggerFactory.getLogger(SpeechToTextAdapter.class);

    public String transcribe(String audioUrl) {
        log.info("Simulating speech-to-text for: {}", audioUrl);

        // Mock transcription for demo
        return "我中午吃了一碗米饭和一个鸡腿";
    }
}

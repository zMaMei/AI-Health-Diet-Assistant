package com.health.diet.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ImageRecognitionAdapter {

    private static final Logger log = LoggerFactory.getLogger(ImageRecognitionAdapter.class);

    public List<FoodLabel> detectFood(String imageUrl) {
        log.info("Simulating image recognition for: {}", imageUrl);

        // Mock recognition results for demo
        return List.of(
                new FoodLabel("米饭", 0.95),
                new FoodLabel("红烧肉", 0.82),
                new FoodLabel("炒青菜", 0.78)
        );
    }

    public record FoodLabel(String label, double confidence) {}
}

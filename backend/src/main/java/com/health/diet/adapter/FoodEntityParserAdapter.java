package com.health.diet.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FoodEntityParserAdapter {

    private static final Logger log = LoggerFactory.getLogger(FoodEntityParserAdapter.class);

    public List<FoodEntity> extractFoodEntities(String text) {
        log.info("Simulating food entity extraction from: {}", text);

        // Mock entity extraction for demo
        return List.of(
                new FoodEntity("米饭", 1.0, "碗", "午餐"),
                new FoodEntity("鸡腿", 1.0, "个", "午餐")
        );
    }

    public record FoodEntity(String foodName, double amount, String unit, String mealType) {}
}

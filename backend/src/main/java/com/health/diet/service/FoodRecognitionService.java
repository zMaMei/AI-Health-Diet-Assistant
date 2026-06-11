package com.health.diet.service;

import com.health.diet.adapter.ImageRecognitionAdapter;
import com.health.diet.adapter.ImageRecognitionAdapter.FoodLabel;
import com.health.diet.dto.vo.FoodRecognizeResultVO;
import com.health.diet.dto.vo.FoodRecognizeResultVO.FoodCandidate;
import com.health.diet.dto.vo.FoodRecognizeResultVO.NutritionPreview;
import com.health.diet.entity.FoodItem;
import com.health.diet.repository.FoodItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class FoodRecognitionService {

    private static final Logger log = LoggerFactory.getLogger(FoodRecognitionService.class);

    private final ImageRecognitionAdapter imageRecognitionAdapter;
    private final FoodItemRepository foodItemRepository;

    public FoodRecognitionService(ImageRecognitionAdapter imageRecognitionAdapter,
                                   FoodItemRepository foodItemRepository) {
        this.imageRecognitionAdapter = imageRecognitionAdapter;
        this.foodItemRepository = foodItemRepository;
    }

    /**
     * Recognize food from an image using AI vision analysis.
     * Matches AI results against the local food database for accurate nutrition.
     *
     * @param imageBytes  raw image bytes
     * @param contentType MIME type of the image
     * @return recognition result with candidates and nutrition preview
     */
    public FoodRecognizeResultVO recognizeImage(byte[] imageBytes, String contentType) {
        // Call AI adapter to analyze the image
        List<FoodLabel> labels = imageRecognitionAdapter.detectFood(imageBytes, contentType);

        FoodRecognizeResultVO result = new FoodRecognizeResultVO();
        result.setImageUrl("analyzed-image");
        result.setCandidates(new ArrayList<>());

        for (FoodLabel label : labels) {
            FoodCandidate candidate = new FoodCandidate();
            candidate.setFoodName(label.label());
            candidate.setConfidence(label.confidence());

            // Try to match against the local food database for precise nutrition
            FoodItem food = foodItemRepository.findByName(label.label()).orElse(null);

            if (food != null) {
                // Use database nutrition data (more accurate)
                candidate.setUnit(food.getUnit());
                candidate.setDefaultAmount(1.0);
                candidate.setCategory(food.getCategory());
                candidate.setNutritionPreview(new NutritionPreview(
                        food.getCalorie(),
                        food.getProtein(),
                        food.getFat(),
                        food.getCarbohydrate(),
                        nvl(food.getSugar()),
                        nvl(food.getSodium())
                ));
            } else {
                // Use AI-estimated nutrition as fallback
                candidate.setUnit("份");
                candidate.setDefaultAmount(1.0);
                candidate.setCategory("未分类");
                candidate.setNutritionPreview(new NutritionPreview(
                        label.calorie(),
                        label.protein(),
                        label.fat(),
                        label.carbohydrate(),
                        nvl(label.sugar()),
                        nvl(label.sodium())
                ));
            }

            result.getCandidates().add(candidate);
        }

        log.info("Food recognition complete: {} candidates found", result.getCandidates().size());
        return result;
    }

    /**
     * Analyze a food name via AI to get estimated nutrition data.
     * Used by the manual add flow's "智能分析" feature.
     *
     * @param foodName the food name to analyze
     * @return nutrition preview, or null if analysis fails
     */
    public NutritionPreview analyzeFoodName(String foodName) {
        // First try database lookup
        FoodItem food = foodItemRepository.findByName(foodName).orElse(null);
        if (food != null) {
            log.info("Found {} in database, returning precise nutrition", foodName);
            return new NutritionPreview(
                    food.getCalorie(),
                    food.getProtein(),
                    food.getFat(),
                    food.getCarbohydrate(),
                    nvl(food.getSugar()),
                    nvl(food.getSodium())
            );
        }

        // Fall back to AI estimation
        log.info("{} not in database, requesting AI estimation", foodName);
        FoodLabel label = imageRecognitionAdapter.analyzeFoodByName(foodName);
        if (label == null) {
            return null;
        }

        return new NutritionPreview(
                label.calorie(),
                label.protein(),
                label.fat(),
                label.carbohydrate(),
                nvl(label.sugar()),
                nvl(label.sodium())
        );
    }

    private BigDecimal nvl(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}

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

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class FoodRecognitionService {

    private static final Logger log = LoggerFactory.getLogger(FoodRecognitionService.class);

    private final ImageRecognitionAdapter imageRecognitionAdapter;
    private final FoodItemRepository foodItemRepository;
    private final MealPhotoService mealPhotoService;

    public FoodRecognitionService(ImageRecognitionAdapter imageRecognitionAdapter,
                                   FoodItemRepository foodItemRepository,
                                   MealPhotoService mealPhotoService) {
        this.imageRecognitionAdapter = imageRecognitionAdapter;
        this.foodItemRepository = foodItemRepository;
        this.mealPhotoService = mealPhotoService;
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
        /* AI识别食物 */
        // Call AI adapter to analyze the image
        List<FoodLabel> labels;
        try {
            labels = imageRecognitionAdapter.detectFood(imageBytes, contentType);
        } catch (Exception e) {
            log.error("AI image recognition failed", e);
            FoodRecognizeResultVO result = new FoodRecognizeResultVO();
            result.setImageUrl("analyzed-image");
            result.setCandidates(List.of());
            return result;
        }

        FoodRecognizeResultVO result = new FoodRecognizeResultVO();
        result.setImageUrl("analyzed-image");
        result.setCandidates(new ArrayList<>());

        /* 解析AI返回结果 */
        for (FoodLabel label : labels) {
            FoodCandidate candidate = new FoodCandidate();
            candidate.setFoodName(label.label());
            candidate.setConfidence(label.confidence());

            // ① 营养值 100% 使用 AI 根据图片实际内容分析的结果
            candidate.setNutritionPreview(new NutritionPreview(
                    label.calorie(),
                    label.protein(),
                    label.fat(),
                    label.carbohydrate(),
                    nvl(label.sugar()),
                    nvl(label.sodium())
            ));

            /* 匹配本地食物库补全信息 */
            // ② food_item 库仅用于补全 unit 和 category，不覆盖营养值
            FoodItem food = foodItemRepository.findByName(label.label()).orElse(null);
            if (food != null) {
                candidate.setUnit(food.getUnit());
                candidate.setCategory(food.getCategory());
            } else {
                candidate.setUnit("份");
                candidate.setCategory("未分类");
            }
            candidate.setDefaultAmount(1.0);

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
    /* 文本分析食物营养 */
    public NutritionPreview analyzeFoodName(String foodName) {
        /* 调用AI接口 */
        // AI 营养分析优先
        log.info("Requesting AI nutrition analysis for: {}", foodName);
        try {
            FoodLabel label = imageRecognitionAdapter.analyzeFoodByName(foodName);
            if (label != null) {
                return new NutritionPreview(
                        label.calorie(),
                        label.protein(),
                        label.fat(),
                        label.carbohydrate(),
                        nvl(label.sugar()),
                        nvl(label.sodium())
                );
            }
        } catch (Exception e) {
            log.warn("AI analysis failed for {}, falling back to database", foodName, e);
        }

        // AI 失败时兜底查 food_item 库
        FoodItem food = foodItemRepository.findByName(foodName).orElse(null);
        if (food != null) {
            log.info("Using database nutrition for: {}", foodName);
            return new NutritionPreview(
                    food.getCalorie(),
                    food.getProtein(),
                    food.getFat(),
                    food.getCarbohydrate(),
                    nvl(food.getSugar()),
                    nvl(food.getSodium())
            );
        }

        return null;
    }

    /* 图片保存 */
    /**
     * 保存上传的图片到磁盘，返回相对路径。
     */
    public String saveImage(byte[] imageBytes, Long userId) throws IOException {
        return mealPhotoService.saveImageToDisk(imageBytes, userId);
    }

    private BigDecimal nvl(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}

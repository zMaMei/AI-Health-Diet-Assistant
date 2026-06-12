package com.health.diet.controller;

import com.health.diet.common.ApiResponse;
import com.health.diet.dto.vo.FoodRecognizeResultVO;
import com.health.diet.dto.vo.FoodRecognizeResultVO.NutritionPreview;
import com.health.diet.service.FoodRecognitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/food")
public class FoodRecognitionController {

    private static final Logger log = LoggerFactory.getLogger(FoodRecognitionController.class);

    private final FoodRecognitionService foodRecognitionService;

    public FoodRecognitionController(FoodRecognitionService foodRecognitionService) {
        this.foodRecognitionService = foodRecognitionService;
    }

    /**
     * POST /api/food/recognize
     * Upload an image, save it to disk, and get AI-recognized food candidates.
     * Returns imageUrl so the frontend can later associate the photo with the meal.
     */
    @PostMapping("/recognize")
    public ApiResponse<FoodRecognizeResultVO> recognize(@RequestParam("image") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.error(400, "图片文件不能为空");
        }

        try {
            log.info("Received image for recognition: name={}, size={}, type={}",
                    file.getOriginalFilename(), file.getSize(), file.getContentType());

            byte[] imageBytes = file.getBytes();

            // ① 先保存照片到磁盘
            String imageUrl = foodRecognitionService.saveImage(imageBytes);

            // ② AI 识别
            FoodRecognizeResultVO result = foodRecognitionService.recognizeImage(
                    imageBytes, file.getContentType());

            // ③ 把 imageUrl 带回前端
            result.setImageUrl(imageUrl);

            return ApiResponse.success(result);

        } catch (IOException e) {
            log.error("Failed to read uploaded image", e);
            return ApiResponse.error(500, "图片读取失败，请重试");
        }
    }

    /**
     * POST /api/food/analyze-text
     * Analyze a food name to get estimated nutrition data.
     * Used by the manual add "智能分析" feature.
     */
    @PostMapping("/analyze-text")
    public ApiResponse<NutritionPreview> analyzeText(@RequestBody Map<String, String> request) {
        String foodName = request.get("foodName");
        if (foodName == null || foodName.isBlank()) {
            return ApiResponse.error(400, "食物名称不能为空");
        }

        log.info("Analyzing food text: {}", foodName);
        NutritionPreview result = foodRecognitionService.analyzeFoodName(foodName.trim());

        if (result == null) {
            return ApiResponse.error(404, "未找到该食物的营养信息");
        }

        return ApiResponse.success(result);
    }
}

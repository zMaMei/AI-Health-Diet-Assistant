package com.health.diet.controller;

import com.health.diet.common.ApiResponse;
import com.health.diet.dto.vo.FoodRecognizeResultVO;
import com.health.diet.dto.vo.FoodRecognizeResultVO.NutritionPreview;
import com.health.diet.service.FoodRecognitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
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

    /* 拍照识别食物 */
    @PostMapping("/recognize")
    public ApiResponse<FoodRecognizeResultVO> recognize(@RequestParam("image") MultipartFile file,
                                                         HttpServletRequest request) {
        /* 参数校验 */
        if (file.isEmpty()) {
            return ApiResponse.error(400, "图片文件不能为空");
        }

        /* 异常处理 */
        try {
            log.info("Received image for recognition: name={}, size={}, type={}",
                    file.getOriginalFilename(), file.getSize(), file.getContentType());

            /* 从拦截器注入的用户ID */
            Long userId = (Long) request.getAttribute("userId");
            /* 读取图片字节数据 */
            byte[] imageBytes = file.getBytes();

            /* 存储文件 -- 先保存照片到磁盘 */
            String imageUrl = foodRecognitionService.saveImage(imageBytes, userId);

            /* 调用AI识别服务 -- AI 识别食物 */
            FoodRecognizeResultVO result = foodRecognitionService.recognizeImage(
                    imageBytes, file.getContentType());

            /* 把 imageUrl 带回前端 */
            result.setImageUrl(imageUrl);

            return ApiResponse.success(result);

        } catch (IOException e) {
            log.error("Failed to read uploaded image", e);
            return ApiResponse.error(500, "图片读取失败，请重试");
        } catch (RuntimeException e) {
            log.error("AI recognition failed", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    /* 文本分析食物 */
    @PostMapping("/analyze-text")
    public ApiResponse<NutritionPreview> analyzeText(@RequestBody Map<String, String> request) {
        String foodName = request.get("foodName");
        /* 参数校验 */
        if (foodName == null || foodName.isBlank()) {
            return ApiResponse.error(400, "食物名称不能为空");
        }

        /* 异常处理 */
        try {
            log.info("Analyzing food text: {}", foodName);
            /* 调用AI文本分析服务 */
            NutritionPreview result = foodRecognitionService.analyzeFoodName(foodName.trim());

            if (result == null) {
                return ApiResponse.error(404, "未找到该食物的营养信息");
            }

            return ApiResponse.success(result);
        } catch (RuntimeException e) {
            log.error("AI text analysis failed", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }
}

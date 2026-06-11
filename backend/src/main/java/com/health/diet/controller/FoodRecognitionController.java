package com.health.diet.controller;

import com.health.diet.common.ApiResponse;
import com.health.diet.dto.vo.FoodRecognizeResultVO;
import com.health.diet.service.FoodRecognitionService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/food")
public class FoodRecognitionController {

    private final FoodRecognitionService foodRecognitionService;

    public FoodRecognitionController(FoodRecognitionService foodRecognitionService) {
        this.foodRecognitionService = foodRecognitionService;
    }

    @PostMapping("/recognize")
    public ApiResponse<FoodRecognizeResultVO> recognize(@RequestParam("image") MultipartFile file) {
        // In a real app, we would upload to cloud storage
        // For demo, we simulate with a placeholder URL
        String imageUrl = "simulated://" + file.getOriginalFilename();
        return ApiResponse.success(foodRecognitionService.recognizeImage(imageUrl));
    }
}

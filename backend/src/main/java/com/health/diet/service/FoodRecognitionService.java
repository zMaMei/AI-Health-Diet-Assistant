package com.health.diet.service;

import com.health.diet.adapter.ImageRecognitionAdapter;
import com.health.diet.dto.vo.FoodRecognizeResultVO;
import com.health.diet.entity.FoodItem;
import com.health.diet.repository.FoodItemRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FoodRecognitionService {

    private final ImageRecognitionAdapter imageRecognitionAdapter;
    private final FoodItemRepository foodItemRepository;

    public FoodRecognitionService(ImageRecognitionAdapter imageRecognitionAdapter,
                                   FoodItemRepository foodItemRepository) {
        this.imageRecognitionAdapter = imageRecognitionAdapter;
        this.foodItemRepository = foodItemRepository;
    }

    public FoodRecognizeResultVO recognizeImage(String imageUrl) {
        // Call adapter to get food labels
        List<ImageRecognitionAdapter.FoodLabel> labels = imageRecognitionAdapter.detectFood(imageUrl);

        // Build candidates by matching against food database
        FoodRecognizeResultVO result = new FoodRecognizeResultVO();
        result.setImageUrl(imageUrl);
        result.setCandidates(new ArrayList<>());

        for (ImageRecognitionAdapter.FoodLabel label : labels) {
            FoodItem food = foodItemRepository.findByName(label.label()).orElse(null);
            if (food != null) {
                result.getCandidates().add(new FoodRecognizeResultVO.FoodCandidate(
                        food.getName(),
                        label.confidence(),
                        food.getUnit(),
                        1.0
                ));
            } else {
                // If not in database, still return the label
                result.getCandidates().add(new FoodRecognizeResultVO.FoodCandidate(
                        label.label(),
                        label.confidence(),
                        "份",
                        1.0
                ));
            }
        }

        return result;
    }
}

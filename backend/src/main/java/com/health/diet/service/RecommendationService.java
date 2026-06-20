package com.health.diet.service;

import com.health.diet.dto.vo.RecommendationVO;
import com.health.diet.entity.Recipe;
import com.health.diet.entity.Recommendation;
import com.health.diet.entity.UserProfile;
import com.health.diet.repository.RecipeRepository;
import com.health.diet.repository.RecommendationRepository;
import com.health.diet.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final RecipeRepository recipeRepository;
    private final UserProfileRepository userProfileRepository;
    private final RecommendationRepository recommendationRepository;

    public RecommendationService(RecipeRepository recipeRepository,
                                  UserProfileRepository userProfileRepository,
                                  RecommendationRepository recommendationRepository) {
        this.recipeRepository = recipeRepository;
        this.userProfileRepository = userProfileRepository;
        this.recommendationRepository = recommendationRepository;
    }

    public List<RecommendationVO> recommendToday(Long userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

        // Get all recipes
        List<Recipe> allRecipes = recipeRepository.findAll();

        // Score and filter recipes
        List<ScoredRecipe> scored = new ArrayList<>();
        for (Recipe recipe : allRecipes) {
            BigDecimal score = scoreRecipe(recipe, profile);
            String reason = generateReason(recipe, profile);

            scored.add(new ScoredRecipe(recipe, score, reason));
        }

        // Sort by score descending and take top 5
        scored.sort((a, b) -> b.score.compareTo(a.score));
        List<ScoredRecipe> top = scored.stream()
                .limit(5)
                .collect(Collectors.toList());

        // Save recommendations and return
        List<RecommendationVO> result = new ArrayList<>();
        for (ScoredRecipe sr : top) {
            Recommendation rec = new Recommendation();
            rec.setUserId(userId);
            rec.setRecipeId(sr.recipe.getId());
            rec.setReason(sr.reason);
            rec.setScore(sr.score);
            recommendationRepository.save(rec);

            result.add(toVO(rec, sr.recipe));
        }

        return result;
    }

    private BigDecimal scoreRecipe(Recipe recipe, UserProfile profile) {
        BigDecimal score = new BigDecimal("50");

        if (profile == null) return score;

        // Goal matching
        if ("减脂".equals(profile.getGoal())) {
            if (recipe.getCalorie().compareTo(new BigDecimal("300")) < 0) {
                score = score.add(new BigDecimal("20"));
            }
            if (recipe.getTags() != null && recipe.getTags().contains("低卡")) {
                score = score.add(new BigDecimal("15"));
            }
        } else if ("增肌".equals(profile.getGoal())) {
            if (recipe.getProtein().compareTo(new BigDecimal("20")) > 0) {
                score = score.add(new BigDecimal("20"));
            }
            if (recipe.getTags() != null && recipe.getTags().contains("高蛋白")) {
                score = score.add(new BigDecimal("15"));
            }
        } else if ("控糖".equals(profile.getGoal())) {
            if (recipe.getCarbohydrate().compareTo(new BigDecimal("30")) < 0) {
                score = score.add(new BigDecimal("20"));
            }
            if (recipe.getTags() != null && recipe.getTags().contains("低糖")) {
                score = score.add(new BigDecimal("15"));
            }
        }

        // Preference matching
        if (profile.getTastePreference() != null && recipe.getTags() != null) {
            String[] prefs = profile.getTastePreference().split(",");
            for (String pref : prefs) {
                if (recipe.getTags().contains(pref.trim())) {
                    score = score.add(new BigDecimal("5"));
                }
            }
        }

        // Taboo filter
        if (profile.getTaboo() != null && recipe.getTags() != null) {
            String[] taboos = profile.getTaboo().split(",");
            for (String taboo : taboos) {
                if (recipe.getTags().contains(taboo.trim())) {
                    score = score.subtract(new BigDecimal("50"));
                }
            }
        }

        return score.max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }

    private String generateReason(Recipe recipe, UserProfile profile) {
        List<String> reasons = new ArrayList<>();

        if (profile != null) {
            if ("减脂".equals(profile.getGoal()) && recipe.getCalorie().compareTo(new BigDecimal("300")) < 0) {
                reasons.add("低热量");
            }
            if ("增肌".equals(profile.getGoal()) && recipe.getProtein().compareTo(new BigDecimal("20")) > 0) {
                reasons.add("高蛋白");
            }
            if ("控糖".equals(profile.getGoal()) && recipe.getCarbohydrate().compareTo(new BigDecimal("30")) < 0) {
                reasons.add("低碳水");
            }
        }

        if (recipe.getTags() != null) {
            String[] tags = recipe.getTags().split(",");
            for (String tag : tags) {
                String t = tag.trim();
                if (!t.isEmpty() && reasons.size() < 3) {
                    reasons.add(t);
                }
            }
        }

        if (reasons.isEmpty()) {
            reasons.add("营养均衡");
        }

        String target = profile != null ? profile.getGoal() : "健康";
        return "推荐理由：这道菜" + String.join("、", reasons) + "，符合您的"
                + target + "目标";
    }

    private RecommendationVO toVO(Recommendation rec, Recipe recipe) {
        RecommendationVO vo = new RecommendationVO();
        vo.setId(rec.getId());
        vo.setRecipeId(recipe.getId());
        vo.setRecipeName(recipe.getName());
        vo.setIngredients(recipe.getIngredients());
        vo.setSteps(recipe.getSteps());
        vo.setTags(recipe.getTags());
        vo.setCalorie(recipe.getCalorie());
        vo.setProtein(recipe.getProtein());
        vo.setFat(recipe.getFat());
        vo.setCarbohydrate(recipe.getCarbohydrate());
        vo.setSugar(recipe.getSugar());
        vo.setSodium(recipe.getSodium());
        vo.setReason(rec.getReason());
        vo.setMatchScore(rec.getScore());
        return vo;
    }

    private record ScoredRecipe(Recipe recipe, BigDecimal score, String reason) {}
}

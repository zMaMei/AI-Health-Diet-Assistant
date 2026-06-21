package com.health.diet.repository;

import com.health.diet.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/* 食谱库数据访问层 */
public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    /* findByNameContaining 按名称模糊搜索菜谱 */
    List<Recipe> findByNameContaining(String keyword);
}

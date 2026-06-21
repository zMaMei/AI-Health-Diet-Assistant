package com.health.diet.repository;

import com.health.diet.entity.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/* 食物营养成分数据访问层 */
public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    /* findByName 按名称精确查找食物 */
    Optional<FoodItem> findByName(String name);

    /* findByNameContaining 按名称模糊搜索食物 */
    List<FoodItem> findByNameContaining(String keyword);
}

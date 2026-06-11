package com.health.diet.repository;

import com.health.diet.entity.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {
    Optional<FoodItem> findByName(String name);
    List<FoodItem> findByNameContaining(String keyword);
}

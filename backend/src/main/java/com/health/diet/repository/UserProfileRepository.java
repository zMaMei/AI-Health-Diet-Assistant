package com.health.diet.repository;

import com.health.diet.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/* 用户健康档案数据访问层 */
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    /* findByUserId 按用户ID查询档案 */
    Optional<UserProfile> findByUserId(Long userId);
}

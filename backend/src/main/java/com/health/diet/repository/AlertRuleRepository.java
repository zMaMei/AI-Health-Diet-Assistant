package com.health.diet.repository;

import com.health.diet.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/* 预警规则数据访问层 */
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    /* findByUserId 按用户查询预警规则列表 */
    List<AlertRule> findByUserId(Long userId);
}

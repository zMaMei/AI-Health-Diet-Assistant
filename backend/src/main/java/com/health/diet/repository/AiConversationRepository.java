package com.health.diet.repository;

import com.health.diet.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

/* AI对话会话数据访问层 */
public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

    /* findByUserIdAndRecordDate 按用户和日期查询AI对话会话 */
    Optional<AiConversation> findByUserIdAndRecordDate(Long userId, LocalDate recordDate);
}

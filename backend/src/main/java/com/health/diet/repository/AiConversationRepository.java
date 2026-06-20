package com.health.diet.repository;

import com.health.diet.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {
    Optional<AiConversation> findByUserIdAndRecordDate(Long userId, LocalDate recordDate);
}

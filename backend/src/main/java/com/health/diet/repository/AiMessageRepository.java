package com.health.diet.repository;

import com.health.diet.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/* AI对话消息数据访问层 */
public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {

    /* findByConversationIdOrderByCreatedAtAsc 按会话ID查询消息列表（正序） */
    List<AiMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}

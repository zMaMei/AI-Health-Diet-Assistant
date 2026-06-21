package com.health.diet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/* AI对话消息表 */
@Entity /* JPA实体 */
@Table(name = "ai_message") /* 数据库表名 */
public class AiMessage {

    /* 消息主键ID */
    @Id /* 主键 */
    @GeneratedValue(strategy = GenerationType.IDENTITY) /* 自增主键 */
    private Long id;

    /* 会话ID */
    @Column(name = "conversation_id", nullable = false) /* 数据库列 */
    private Long conversationId;

    /* 角色（用户/AI） */
    @Column(nullable = false, length = 16)
    private String role;

    /* 消息内容 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /* 创建时间 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public AiMessage() {}

    public Long getId() { return id; }
    public Long getConversationId() { return conversationId; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public void setRole(String role) { this.role = role; }
    public void setContent(String content) { this.content = content; }
}

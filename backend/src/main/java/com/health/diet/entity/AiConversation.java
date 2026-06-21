package com.health.diet.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/* AI对话会话表 */
@Entity /* JPA实体 */
@Table(name = "ai_conversation") /* 数据库表名 */
public class AiConversation {

    /* 会话主键ID */
    @Id /* 主键 */
    @GeneratedValue(strategy = GenerationType.IDENTITY) /* 自增主键 */
    private Long id;

    /* 关联用户ID */
    @Column(name = "user_id", nullable = false) /* 数据库列 */
    private Long userId;

    /* 记录日期 */
    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    /* 创建时间 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public AiConversation() {}

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDate getRecordDate() { return recordDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
}

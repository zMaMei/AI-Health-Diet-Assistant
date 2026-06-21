package com.health.diet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/* 用户基础信息表 */
@Entity /* JPA实体 */
@Table(name = "users") /* 数据库表名 */
public class User {

    /* 用户ID */
    @Id /* 主键 */
    @GeneratedValue(strategy = GenerationType.IDENTITY) /* 自增主键 */
    private Long id;

    /* 用户昵称 */
    @Column(nullable = false, length = 32) /* 数据库列 */
    private String nickname;

    /* 用户名（唯一） */
    @Column(nullable = false, length = 32, unique = true)
    private String username;

    /* BCrypt密码哈希 */
    @Column(name = "password_hash", nullable = false, length = 128)
    private String passwordHash;

    /* 创建时间 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /* 更新时间 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public User() {}

    public User(String username, String passwordHash, String nickname) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
    }

    public void rename(String newNickname) {
        this.nickname = newNickname;
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getNickname() { return nickname; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

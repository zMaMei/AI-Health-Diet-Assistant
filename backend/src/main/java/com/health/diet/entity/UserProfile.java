package com.health.diet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/* 用户健康档案表 */
@Entity /* JPA实体 */
@Table(name = "user_profile") /* 数据库表名 */
public class UserProfile {

    /* 档案主键ID */
    @Id /* 主键 */
    @GeneratedValue(strategy = GenerationType.IDENTITY) /* 自增主键 */
    private Long id;

    /* 关联用户ID */
    @Column(name = "user_id", nullable = false, unique = true) /* 数据库列 */
    private Long userId;

    /* 年龄 */
    private Integer age;

    /* 身高 */
    @Column(name = "height_cm", precision = 5, scale = 2)
    private BigDecimal heightCm;

    /* 体重 */
    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    /* 健康目标 */
    @Column(nullable = false, length = 32)
    private String goal;

    /* 忌口 */
    @Column(length = 255)
    private String taboo;

    /* 口味偏好 */
    @Column(name = "taste_preference", length = 255)
    private String tastePreference;

    /* 预警档案 */
    @Column(name = "warning_profile", length = 255)
    private String warningProfile;

    /* 性别 */
    @Column(length = 8)
    private String gender;

    /* 头像URL */
    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    public UserProfile() {}

    public UserProfile(Long userId, String goal) {
        this.userId = userId;
        this.goal = goal;
    }

    public void updateGoal(String goal) {
        this.goal = goal;
    }

    public void updatePreference(String taboo, String tastePreference) {
        this.taboo = taboo;
        this.tastePreference = tastePreference;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Integer getAge() { return age; }
    public BigDecimal getHeightCm() { return heightCm; }
    public BigDecimal getWeightKg() { return weightKg; }
    public String getGoal() { return goal; }
    public String getTaboo() { return taboo; }
    public String getTastePreference() { return tastePreference; }
    public String getWarningProfile() { return warningProfile; }
    public String getAvatarUrl() { return avatarUrl; }

    public void setAge(Integer age) { this.age = age; }
    public void setHeightCm(BigDecimal heightCm) { this.heightCm = heightCm; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public void setGoal(String goal) { this.goal = goal; }
    public void setTaboo(String taboo) { this.taboo = taboo; }
    public void setTastePreference(String tastePreference) { this.tastePreference = tastePreference; }
    public void setWarningProfile(String warningProfile) { this.warningProfile = warningProfile; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}

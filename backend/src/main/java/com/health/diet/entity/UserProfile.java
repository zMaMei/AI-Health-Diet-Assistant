package com.health.diet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    private Integer age;

    @Column(name = "height_cm", precision = 5, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(nullable = false, length = 32)
    private String goal;

    @Column(length = 255)
    private String taboo;

    @Column(name = "taste_preference", length = 255)
    private String tastePreference;

    @Column(name = "warning_profile", length = 255)
    private String warningProfile;

    @Column(length = 8)
    private String gender;

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

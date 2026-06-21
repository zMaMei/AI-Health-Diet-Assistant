package com.health.diet.dto.vo;

import java.math.BigDecimal;

/* 用户档案返回 */
public class UserProfileVO {

    /* 档案ID */
    private Long id;
    /* 用户ID */
    private Long userId;
    /* 用户名 */
    private String username;
    /* 头像URL */
    private String avatarUrl;
    /* 昵称 */
    private String nickname;
    /* 年龄 */
    private Integer age;
    /* 身高(cm) */
    private BigDecimal heightCm;
    /* 体重(kg) */
    private BigDecimal weightKg;
    /* 健康目标 */
    private String goal;
    /* 饮食禁忌 */
    private String taboo;
    /* 口味偏好 */
    private String tastePreference;
    /* 预警配置 */
    private String warningProfile;
    /* 性别 */
    private String gender;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public BigDecimal getHeightCm() { return heightCm; }
    public void setHeightCm(BigDecimal heightCm) { this.heightCm = heightCm; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
    public String getTaboo() { return taboo; }
    public void setTaboo(String taboo) { this.taboo = taboo; }
    public String getTastePreference() { return tastePreference; }
    public void setTastePreference(String tastePreference) { this.tastePreference = tastePreference; }
    public String getWarningProfile() { return warningProfile; }
    public void setWarningProfile(String warningProfile) { this.warningProfile = warningProfile; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
}

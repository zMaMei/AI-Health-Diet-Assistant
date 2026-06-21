package com.health.diet.dto.command;

import java.math.BigDecimal;

/* 更新用户档案请求 */
public class UserProfileUpdateCommand {

    /* 年龄 */
    private Integer age;
    /* 身高（厘米） */
    private BigDecimal heightCm;
    /* 体重（千克） */
    private BigDecimal weightKg;
    /* 健康目标 */
    private String goal;
    /* 饮食禁忌 */
    private String taboo;
    /* 口味偏好 */
    private String tastePreference;
    /* 预警档案 */
    private String warningProfile;
    /* 性别 */
    private String gender;

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

package com.health.diet.dto.command;

import java.math.BigDecimal;

public class UserProfileUpdateCommand {

    private Integer age;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private String goal;
    private String taboo;
    private String tastePreference;
    private String warningProfile;

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
}

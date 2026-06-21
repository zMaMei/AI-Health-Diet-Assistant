package com.health.diet.dto.command;

import jakarta.validation.constraints.NotBlank;

/* 登录请求 */
public class LoginCommand {

    /* 用户名 */
    @NotBlank
    private String username;

    /* 密码 */
    @NotBlank
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

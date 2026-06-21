package com.health.diet.dto.vo;

/* 登录/注册返回结果 */
public class LoginResultVO {

    /* 用户ID */
    private Long userId;
    /* 用户名 */
    private String username;
    /* 昵称 */
    private String nickname;
    /* 头像URL */
    private String avatarUrl;
    /* Token */
    private String token;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}

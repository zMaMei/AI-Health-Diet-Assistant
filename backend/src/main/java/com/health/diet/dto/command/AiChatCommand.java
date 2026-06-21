package com.health.diet.dto.command;

import java.time.LocalDate;

/* AI对话请求 */
public class AiChatCommand {
    /* 日期 */
    private LocalDate date;
    /* 用户消息 */
    private String message;

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

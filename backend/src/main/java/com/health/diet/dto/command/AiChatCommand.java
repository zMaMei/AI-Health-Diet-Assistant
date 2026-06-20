package com.health.diet.dto.command;

import java.time.LocalDate;

public class AiChatCommand {
    private LocalDate date;
    private String message;

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

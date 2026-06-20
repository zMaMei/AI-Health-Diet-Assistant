package com.health.diet.dto.vo;

import java.util.List;

public class AiChatVO {
    private Long conversationId;
    private List<MessageVO> messages;

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public List<MessageVO> getMessages() { return messages; }
    public void setMessages(List<MessageVO> messages) { this.messages = messages; }

    public static class MessageVO {
        private String role;
        private String content;
        private String createdAt;

        public MessageVO() {}
        public MessageVO(String role, String content, String createdAt) {
            this.role = role;
            this.content = content;
            this.createdAt = createdAt;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
}

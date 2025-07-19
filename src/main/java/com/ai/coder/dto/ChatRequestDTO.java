package com.ai.coder.dto;

import lombok.Data;

/**
 * 聊天请求数据传输对象
 */
@Data
public class ChatRequestDTO {
    private String message;
    private String sessionId; // 可选：用于会话管理

    public ChatRequestDTO() {
    }

    public ChatRequestDTO(String message) {
        this.message = message;
    }

    public ChatRequestDTO(String message, String sessionId) {
        this.message = message;
        this.sessionId = sessionId;
    }

    @Override
    public String toString() {
        return "ChatRequestDto{" +
                "message='" + message + '\'' +
                ", sessionId='" + sessionId + '\'' +
                '}';
    }
}

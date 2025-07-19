package com.ai.coder.dto;

import lombok.Data;

@Data
public class ChatResponseDTO {
    private String taskId;
    private String message;
    private boolean success;
    private boolean asyncTask;
    private boolean streamResponse; // 新增：标识是否为流式响应
    private int totalTurns;
    private boolean reachedMaxTurns;
    private String stopReason;
    private long totalDurationMs;

}
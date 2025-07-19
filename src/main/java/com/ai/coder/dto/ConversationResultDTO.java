package com.ai.coder.dto;

import lombok.Data;

// 对话结果DTO类
@Data
public class ConversationResultDTO {
    private String taskId;
    private String fullResponse;
    private java.util.List<String> turnResponses;
    private int totalTurns;
    private boolean reachedMaxTurns;
    private String stopReason;
    private long totalDurationMs;

}
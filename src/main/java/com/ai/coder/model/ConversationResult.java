package com.ai.coder.model;

import lombok.Data;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 连续对话结果
 */
@Data
public class ConversationResult {
    private final String fullResponse;
    private final List<String> turnResponses;
    private final List<Message> finalHistory;
    private final int totalTurns;
    private final boolean reachedMaxTurns;
    private final String stopReason;
    private final long totalDurationMs;

    public ConversationResult(String fullResponse, List<String> turnResponses,
                              List<Message> finalHistory, int totalTurns, boolean reachedMaxTurns,
                              String stopReason, long totalDurationMs) {
        this.fullResponse = fullResponse;
        this.turnResponses = turnResponses;
        this.finalHistory = finalHistory;
        this.totalTurns = totalTurns;
        this.reachedMaxTurns = reachedMaxTurns;
        this.stopReason = stopReason;
        this.totalDurationMs = totalDurationMs;
    }

}
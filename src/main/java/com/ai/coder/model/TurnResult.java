package com.ai.coder.model;

import lombok.Data;

/**
 * 单轮对话结果
 */
@Data
public class TurnResult {
    private final boolean success;
    private final String response;
    private final String errorMessage;

    public TurnResult(boolean success, String response, String errorMessage) {
        this.success = success;
        this.response = response;
        this.errorMessage = errorMessage;
    }

}
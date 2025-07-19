package com.ai.coder.dto;

import lombok.Data;

/**
 * 错误响应类
 */
@Data
public class ErrorResponseDTO {
    private String errorCode;
    private String message;
    private String suggestion;
    private long timestamp;

    public ErrorResponseDTO(String errorCode, String message, String suggestion) {
        this.errorCode = errorCode;
        this.message = message;
        this.suggestion = suggestion;
        this.timestamp = System.currentTimeMillis();
    }

}
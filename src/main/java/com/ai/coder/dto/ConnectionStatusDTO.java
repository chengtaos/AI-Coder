package com.ai.coder.dto;

import lombok.Data;

/**
 * 连接状态DTO
 */
@Data
public class ConnectionStatusDTO {
    private int activeConnections;
    private String status;

}
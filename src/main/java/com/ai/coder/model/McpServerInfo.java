package com.ai.coder.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * MCP服务器信息
 */
@Data
public class McpServerInfo {
    // Getters and Setters
    private String name;
    private McpServerConfig config;
    private String status; // RUNNING, STOPPED, ERROR
    private LocalDateTime startTime;
    private String errorMessage;
    private int toolCount;

    public McpServerInfo() {
    }

    public McpServerInfo(String name, McpServerConfig config) {
        this.name = name;
        this.config = config;
        this.status = "STOPPED";
    }

    @Override
    public String toString() {
        return "McpServerInfo{" +
                "name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", startTime=" + startTime +
                ", toolCount=" + toolCount +
                '}';
    }
}

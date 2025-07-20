package com.ai.coder.model;

import com.ai.coder.model.McpServerConfig;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MCP服务器进程信息
 */
@Data
public class McpServerProcess {
    // Getters
    private final String name;
    private final Process process;
    private final McpServerConfig config;
    private final LocalDateTime startTime;

    public McpServerProcess(String name, Process process, McpServerConfig config) {
        this.name = name;
        this.process = process;
        this.config = config;
        this.startTime = LocalDateTime.now();
    }

}
package com.ai.coder.model;

import lombok.Data;

/**
 * MCP服务器添加请求
 */
@Data
public class McpServerRequest {
    private String name;
    private McpServerConfig config;

    public McpServerRequest() {
    }

    public McpServerRequest(String name, McpServerConfig config) {
        this.name = name;
        this.config = config;
    }

    @Override
    public String toString() {
        return "McpServerRequest{" +
                "name='" + name + '\'' +
                ", config=" + config +
                '}';
    }
}

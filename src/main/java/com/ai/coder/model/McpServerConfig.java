package com.ai.coder.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * MCP服务器配置
 */
@Data
public class McpServerConfig {
    private String command;
    private List<String> args;
    private Map<String, String> env;
    private String workingDirectory;

    public McpServerConfig() {
    }

    public McpServerConfig(String command, List<String> args) {
        this.command = command;
        this.args = args;
    }

    @Override
    public String toString() {
        return "McpServerConfig{" +
                "command='" + command + '\'' +
                ", args=" + args +
                ", env=" + env +
                ", workingDirectory='" + workingDirectory + '\'' +
                '}';
    }
}

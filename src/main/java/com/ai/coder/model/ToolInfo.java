package com.ai.coder.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工具信息统一模型
 */
@Data
public class ToolInfo {
    private String name;
    private String displayName;
    private String description;
    private String type; // SYSTEM, MCP
    private String source; // 来源类名或MCP服务器名
    private boolean enabled;
    private boolean builtIn; // 是否为内置工具
    private List<ToolParameter> parameters;
    private Map<String, Object> metadata;

    public ToolInfo() {
    }

    public ToolInfo(String name, String displayName, String description) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
    }

    @Override
    public String toString() {
        return "ToolInfo{" +
                "name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", type='" + type + '\'' +
                ", source='" + source + '\'' +
                ", enabled=" + enabled +
                ", builtIn=" + builtIn +
                '}';
    }
}

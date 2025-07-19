package com.ai.coder.model;

import lombok.Data;

/**
 * 工具参数信息
 */
@Data
public class ToolParameter {
    private String name;
    private String type;
    private String description;
    private boolean required;
    private Object defaultValue;

    public ToolParameter() {
    }

    public ToolParameter(String name, String type, String description, boolean required) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.required = required;
    }

    @Override
    public String toString() {
        return "ToolParameter{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", description='" + description + '\'' +
                ", required=" + required +
                '}';
    }
}

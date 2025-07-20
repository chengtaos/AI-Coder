package com.ai.coder.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 工具执行结果
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ToolResult {
    private final boolean success;
    private final String llmContent;
    private final Object returnDisplay;
    private final String errorMessage;

    private ToolResult(boolean success, String llmContent, Object returnDisplay, String errorMessage) {
        this.success = success;
        this.llmContent = llmContent;
        this.returnDisplay = returnDisplay;
        this.errorMessage = errorMessage;
    }

    // 静态工厂方法
    public static ToolResult success(String llmContent) {
        return new ToolResult(true, llmContent, llmContent, null);
    }

    public static ToolResult success(String llmContent, Object returnDisplay) {
        return new ToolResult(true, llmContent, returnDisplay, null);
    }

    public static ToolResult error(String errorMessage) {
        return new ToolResult(false, "Error: " + errorMessage, "Error: " + errorMessage, errorMessage);
    }

    @Override
    public String toString() {
        if (success) {
            return "ToolResult{success=true, content='" + llmContent + "'}";
        } else {
            return "ToolResult{success=false, error='" + errorMessage + "'}";
        }
    }
}




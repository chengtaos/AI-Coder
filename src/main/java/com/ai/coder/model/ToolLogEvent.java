package com.ai.coder.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 工具日志事件类
 * 继承自LogEvent，添加工具相关的字段
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ToolLogEvent extends LogEvent {

    // Getters and Setters
    private String toolName;
    private String filePath;
    private String icon;
    private String status; // RUNNING, SUCCESS, ERROR
    private Long executionTime; // 执行时间(毫秒)
    private String summary; // 操作摘要

    // Constructors
    public ToolLogEvent() {
        super();
    }

    public ToolLogEvent(String type, String taskId, String toolName, String filePath,
                        String message, String timestamp, String icon, String status) {
        super(type, taskId, message, timestamp);
        this.toolName = toolName;
        this.filePath = filePath;
        this.icon = icon;
        this.status = status;
    }

    @Override
    public String toString() {
        return "ToolLogEvent{" +
                "toolName='" + toolName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", icon='" + icon + '\'' +
                ", status='" + status + '\'' +
                ", executionTime=" + executionTime +
                ", summary='" + summary + '\'' +
                "} " + super.toString();
    }
}

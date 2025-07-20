package com.ai.coder.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 日志事件基类
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class LogEvent {

    private String type;
    private String taskId;
    private String message;
    private String timestamp;

    // Constructors
    public LogEvent() {
    }

    public LogEvent(String type, String taskId, String message, String timestamp) {
        this.type = type;
        this.taskId = taskId;
        this.message = message;
        this.timestamp = timestamp;
    }

    // Static factory methods
    public static LogEvent createConnectionEvent(String taskId) {
        LogEvent event = new LogEvent();
        event.setType("CONNECTION_ESTABLISHED");
        event.setTaskId(taskId);
        event.setMessage("SSE连接已建立");
        event.setTimestamp(java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return event;
    }

    @Override
    public String toString() {
        return "LogEvent{" +
                "type='" + type + '\'' +
                ", taskId='" + taskId + '\'' +
                ", message='" + message + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}

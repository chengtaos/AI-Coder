package com.ai.coder.controller;

import com.ai.coder.dto.ConnectionStatusDTO;
import com.ai.coder.service.LogStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE日志流控制器
 * 提供SSE连接端点
 */
@RestController
@RequestMapping("/api/logs")
public class LogStreamController {

    private static final Logger logger = LoggerFactory.getLogger(LogStreamController.class);

    @Autowired
    private LogStreamService logStreamService;

    /**
     * 建立SSE连接
     * 前端通过此端点建立实时日志推送连接
     */
    @GetMapping(value = "/stream/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLogs(@PathVariable("taskId") String taskId) {
        logger.info("🔗 收到SSE连接请求: taskId={}", taskId);

        try {
            SseEmitter emitter = logStreamService.createConnection(taskId);
            logger.info("✅ SSE连接建立成功: taskId={}", taskId);
            return emitter;
        } catch (Exception e) {
            logger.error("❌ SSE连接建立失败: taskId={}, error={}", taskId, e.getMessage());
            throw new RuntimeException("创建SSE连接失败: " + e.getMessage());
        }
    }

    /**
     * 关闭SSE连接
     */
    @PostMapping("/close/{taskId}")
    public void closeConnection(@PathVariable("taskId") String taskId) {
        logger.info("🔚 收到关闭SSE连接请求: taskId={}", taskId);
        logStreamService.closeConnection(taskId);
    }

    /**
     * 获取连接状态
     */
    @GetMapping("/status")
    public ConnectionStatusDTO getConnectionStatus() {
        int activeConnections = logStreamService.getActiveConnectionCount();
        logger.debug("📊 当前活跃SSE连接数: {}", activeConnections);

        ConnectionStatusDTO status = new ConnectionStatusDTO();
        status.setActiveConnections(activeConnections);
        status.setStatus("OK");
        return status;
    }


}

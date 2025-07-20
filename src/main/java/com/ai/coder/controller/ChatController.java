package com.ai.coder.controller;

import com.ai.coder.dto.ChatRequestDTO;
import com.ai.coder.dto.ChatResponseDTO;
import com.ai.coder.dto.MessageDTO;
import com.ai.coder.service.ContinuousConversationService;
import com.ai.coder.service.ToolExecutionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 聊天控制器
 * 处理与AI的对话和工具调用
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatClient chatClient;
    private final ContinuousConversationService continuousConversationService;
    private final ToolExecutionLogger executionLogger;

    // 简单的会话存储（生产环境应该使用数据库或Redis）
    private final List<Message> conversationHistory = new ArrayList<>();

    public ChatController(ChatClient chatClient, ContinuousConversationService continuousConversationService, ToolExecutionLogger executionLogger) {
        this.chatClient = chatClient;
        this.continuousConversationService = continuousConversationService;
        this.executionLogger = executionLogger;
    }

    /**
     * 发送消息给AI - 支持连续工具调用
     */
    @PostMapping("/message")
    public Mono<ChatResponseDTO> sendMessage(@RequestBody ChatRequestDTO request) {
        return Mono.fromCallable(() -> {
            try {
                logger.info("💬 ========== 新的聊天请求 ==========");
                logger.info("📝 用户消息: {}", request.getMessage());
                logger.info("🕐 请求时间: {}", java.time.LocalDateTime.now());

                // 智能判断是否需要工具调用
                boolean needsToolExecution = continuousConversationService.isLikelyToNeedTools(request.getMessage());
                logger.info("🔍 工具需求分析: {}", needsToolExecution ? "需要工具" : "不需要工具的简单对话");

                if (needsToolExecution) {
                    // 需要工具调用的复杂任务 - 使用异步模式
                    String taskId = continuousConversationService.startTask(request.getMessage());
                    logger.info("🆔 任务ID: {}", taskId);

                    // 获取工具执行统计
                    executionLogger.logToolStatistics();

                    // 异步执行连续对话
                    CompletableFuture.runAsync(() -> {
                        try {
                            logger.info("🚀 开始异步执行连续对话任务: {}", taskId);
                            continuousConversationService.executeContinuousConversation(
                                    taskId, request.getMessage(), conversationHistory
                            );
                            logger.info("✅ 连续对话任务完成: {}", taskId);
                        } catch (Exception e) {
                            logger.error("❌ 异步对话执行错误: {}", e.getMessage(), e);
                        }
                    });

                    // 返回异步任务响应
                    ChatResponseDTO responseDto = new ChatResponseDTO();
                    responseDto.setTaskId(taskId);
                    responseDto.setMessage("任务已启动，正在处理中...");
                    responseDto.setSuccess(true);
                    responseDto.setAsyncTask(true);

                    logger.info("📤 返回响应: taskId={}, 异步任务已启动", taskId);
                    return responseDto;
                } else {
                    // 简单对话 - 使用流式模式
                    logger.info("🔄 执行流式对话处理");

                    // 返回流式响应标识，让前端建立流式连接
                    ChatResponseDTO responseDto = new ChatResponseDTO();
                    responseDto.setMessage("开始流式对话...");
                    responseDto.setSuccess(true);
                    responseDto.setAsyncTask(false); // 关键：设置为false，表示不是工具任务
                    responseDto.setStreamResponse(true); // 新增：标识为流式响应
                    responseDto.setTotalTurns(1);

                    logger.info("📤 返回流式响应标识");
                    return responseDto;
                }

            } catch (Exception e) {
                logger.error("Error processing chat message", e);
                ChatResponseDTO errorResponse = new ChatResponseDTO();
                errorResponse.setMessage("Error: " + e.getMessage());
                errorResponse.setSuccess(false);
                return errorResponse;
            }
        });
    }


    /**
     * 流式聊天 - 真正的流式实现
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamMessage(@RequestBody ChatRequestDTO request) {
        logger.info("🌊 开始流式对话: {}", request.getMessage());

        return Flux.create(sink -> {
            try {
                UserMessage userMessage = new UserMessage(request.getMessage());
                conversationHistory.add(userMessage);

                // 使用Spring AI的流式API
                Flux<String> contentStream = chatClient.prompt()
                        .messages(conversationHistory)
                        .stream()
                        .content();

                // 订阅流式内容并转发给前端
                contentStream
                        .doOnNext(content -> {
                            logger.debug("📨 流式内容片段: {}", content);
                            // 发送SSE格式的数据
                            sink.next("data: " + content + "\n\n");
                        })
                        .doOnComplete(() -> {
                            logger.info("✅ 流式对话完成");
                            sink.next("data: [DONE]\n\n");
                            sink.complete();
                        })
                        .doOnError(error -> {
                            logger.error("❌ 流式对话错误: {}", error.getMessage());
                            sink.error(error);
                        })
                        .subscribe();

            } catch (Exception e) {
                logger.error("❌ 流式对话启动失败: {}", e.getMessage());
                sink.error(e);
            }
        });
    }

    /**
     * 清除对话历史
     */
    @PostMapping("/clear")
    public Mono<Map<String, String>> clearHistory() {
        conversationHistory.clear();
        logger.info("Conversation history cleared");
        return Mono.just(Map.of("status", "success", "message", "Conversation history cleared"));
    }

    /**
     * 获取对话历史
     */
    @GetMapping("/history")
    public Mono<List<MessageDTO>> getHistory() {
        List<MessageDTO> history = conversationHistory.stream()
                .map(message -> {
                    MessageDTO dto = new MessageDTO();
                    dto.setContent(message.getText());
                    dto.setRole(message instanceof UserMessage ? "user" : "assistant");
                    return dto;
                })
                .toList();

        return Mono.just(history);
    }


}

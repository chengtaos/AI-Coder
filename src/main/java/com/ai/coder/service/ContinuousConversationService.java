package com.ai.coder.service;

import com.ai.coder.config.TaskContextHolder;
import com.ai.coder.model.ConversationResult;
import com.ai.coder.model.NextSpeakerResponse;
import com.ai.coder.model.TaskStatus;
import com.ai.coder.model.TurnResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 连续对话服务
 */
@Service
public class ContinuousConversationService {

    private static final Logger logger = LoggerFactory.getLogger(ContinuousConversationService.class);

    private final ChatClient chatClient;
    private final NextSpeakerService nextSpeakerService;

    @Autowired
    private LogStreamService logStreamService;

    // 最大轮数限制，防止无限循环
    private static final int MAX_TURNS = 20;

    // 单轮对话超时时间（毫秒）
    private static final long TURN_TIMEOUT_MS = 60_000; // 60秒

    // 总对话超时时间（毫秒）
    private static final long TOTAL_TIMEOUT_MS = 10 * 60_000; // 10分钟

    // 继续对话的提示语
    private static final String[] CONTINUE_PROMPTS = {
            "请继续完成任务的后续步骤。",
            "请继续处理剩余工作。",
            "下一步是什么？请继续。",
            "请继续执行任务。",
            "请继续进行实现。"
    };


    private final TaskSummaryService taskSummaryService;
    private final Map<String, TaskStatus> taskStatusMap = new ConcurrentHashMap<>();
    private final Map<String, ConversationResult> conversationResults = new ConcurrentHashMap<>();

    // 修改构造函数
    public ContinuousConversationService(ChatClient chatClient,
                                         NextSpeakerService nextSpeakerService,
                                         TaskSummaryService taskSummaryService) {
        this.chatClient = chatClient;
        this.nextSpeakerService = nextSpeakerService;
        this.taskSummaryService = taskSummaryService;
    }

    // 添加任务状态管理方法
    public TaskStatus getTaskStatus(String taskId) {
        return taskStatusMap.get(taskId);
    }

    // 获取对话结果
    public ConversationResult getConversationResult(String taskId) {
        return conversationResults.get(taskId);
    }

    // 存储对话结果
    private void storeConversationResult(String taskId, ConversationResult result) {
        conversationResults.put(taskId, result);
    }

    public String startTask(String initialMessage) {
        String taskId = UUID.randomUUID().toString();
        TaskStatus status = new TaskStatus(taskId);

        // 估算任务复杂度
        int estimatedTurns = taskSummaryService.estimateTaskComplexity(initialMessage);
        status.setTotalEstimatedTurns(estimatedTurns);
        status.setCurrentAction("开始分析任务...");

        taskStatusMap.put(taskId, status);
        return taskId;
    }

    /**
     * 智能判断用户消息是否可能需要工具调用
     * 用于决定是否使用异步模式和显示工具执行状态
     */
    public boolean isLikelyToNeedTools(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        String lowerMessage = message.toLowerCase().trim();

        // 明确的简单对话模式 - 不需要工具
        String[] simplePatterns = {
                "你好", "hello", "hi", "嗨", "哈喽",
                "谢谢", "thank you", "thanks", "感谢",
                "再见", "goodbye", "bye", "拜拜",
                "好的", "ok", "okay", "行", "可以",
                "不用了", "算了", "没事", "不需要",
                "怎么样", "如何", "什么意思", "是什么",
                "介绍一下", "解释一下", "说明一下"
        };

        // 检查是否是简单问候或确认
        for (String pattern : simplePatterns) {
            if (lowerMessage.equals(pattern) ||
                    (lowerMessage.length() <= 10 && lowerMessage.contains(pattern))) {
                return false;
            }
        }

        // 明确需要工具的关键词
        String[] toolRequiredPatterns = {
                "创建", "create", "新建", "生成", "建立",
                "编辑", "edit", "修改", "更新", "改变",
                "删除", "delete", "移除", "清除",
                "文件", "file", "目录", "folder", "项目", "project",
                "代码", "code", "程序", "script", "函数", "function",
                "分析", "analyze", "检查", "查看", "读取", "read",
                "写入", "write", "保存", "save",
                "搜索", "search", "查找", "find",
                "下载", "download", "获取", "fetch",
                "安装", "install", "配置", "config",
                "运行", "run", "执行", "execute",
                "测试", "test", "调试", "debug"
        };

        // 检查是否包含工具相关关键词
        for (String pattern : toolRequiredPatterns) {
            if (lowerMessage.contains(pattern)) {
                return true;
            }
        }

        // 基于消息长度和复杂度的启发式判断
        // 长消息更可能需要工具处理
        if (message.length() > 50) {
            return true;
        }

        // 包含路径、URL、代码片段等的消息
        if (lowerMessage.contains("/") || lowerMessage.contains("\\") ||
                lowerMessage.contains("http") || lowerMessage.contains("www") ||
                lowerMessage.contains("{") || lowerMessage.contains("}") ||
                lowerMessage.contains("<") || lowerMessage.contains(">")) {
            return true;
        }

        // 默认情况下，对于不确定的消息，倾向于不使用工具
        // 这样可以避免不必要的工具准备状态显示
        return false;
    }

    public void executeContinuousConversation(String taskId, String initialMessage, List<Message> conversationHistory) {
        TaskStatus taskStatus = taskStatusMap.get(taskId);
        if (taskStatus == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        // 设置任务上下文，供AOP切面使用
        TaskContextHolder.setCurrentTaskId(taskId);

        long conversationStartTime = System.currentTimeMillis();
        logger.info("开始连续对话，输入为: {}", initialMessage);

        // 更新任务状态
        taskStatus.setCurrentAction("开始处理对话...");
        taskStatus.setCurrentTurn(0);

        // 创建工作副本
        List<Message> workingHistory = new ArrayList<>(conversationHistory);
        StringBuilder fullResponse = new StringBuilder();
        List<String> turnResponses = new ArrayList<>();

        // 添加初始用户消息
        UserMessage userMessage = new UserMessage(initialMessage);
        workingHistory.add(userMessage);

        int turnCount = 0;
        boolean shouldContinue = true;
        String stopReason = null;

        try {
            while (shouldContinue && turnCount < MAX_TURNS) {
                turnCount++;
                logger.info("执行对话轮次: {}", turnCount);

                // 更新任务状态
                taskStatus.setCurrentTurn(turnCount);
                taskStatus.setCurrentAction(String.format("执行第 %d 轮对话...", turnCount));

                // 检查总超时
                long elapsedTime = System.currentTimeMillis() - conversationStartTime;
                if (elapsedTime > TOTAL_TIMEOUT_MS) {
                    logger.warn("经过 {}ms 对话超时", elapsedTime);
                    stopReason = "对话总时间超时";
                    break;
                }

                try {
                    // 执行单轮对话
                    TurnResult turnResult = executeSingleTurn(workingHistory, turnCount);

                    if (!turnResult.isSuccess()) {
                        logger.error("第 {} 轮执行失败: {}", turnCount, turnResult.getErrorMessage());
                        stopReason = "第"+turnCount+"轮对话执行失败: " + turnResult.getErrorMessage();
                        break;
                    }

                    // 添加响应到历史
                    String responseText = turnResult.getResponse();
                    if (responseText != null && !responseText.trim().isEmpty()) {
                        AssistantMessage assistantMessage = new AssistantMessage(responseText);
                        workingHistory.add(assistantMessage);

                        // 累积响应
                        if (!fullResponse.isEmpty()) {
                            fullResponse.append("\n\n");
                        }
                        fullResponse.append(responseText);
                        turnResponses.add(responseText);

                        // 更新任务状态 - 显示当前响应的简短摘要
                        String responseSummary = responseText.length() > 100 ?
                                responseText.substring(0, 100) + "..." : responseText;
                        taskStatus.setCurrentAction(String.format("第 %d 轮完成: %s", turnCount, responseSummary));
                    }

                    // 判断是否应该继续
                    taskStatus.setCurrentAction(String.format("分析第 %d 轮结果，判断是否继续...", turnCount));
                    shouldContinue = shouldContinueConversation(workingHistory, turnCount, responseText);

                    if (shouldContinue && turnCount < MAX_TURNS) {
                        // 添加继续提示
                        String continuePrompt = getContinuePrompt(turnCount);
                        UserMessage continueMessage = new UserMessage(continuePrompt);
                        workingHistory.add(continueMessage);
                        logger.info("为第 {} 轮添加连续提示词: {}", turnCount + 1, continuePrompt);
                        taskStatus.setCurrentAction(String.format("准备第 %d 轮对话...", turnCount + 1));
                    } else {
                        taskStatus.setCurrentAction("对话即将结束...");
                    }

                } catch (Exception e) {
                    logger.error("Error in conversation turn {}: {}", turnCount, e.getMessage(), e);
                    stopReason = "Exception in turn " + turnCount + ": " + e.getMessage();

                    // 添加错误信息到响应中
                    String errorMessage = String.format("❌ Error in turn %d: %s", turnCount, e.getMessage());
                    if (!fullResponse.isEmpty()) {
                        fullResponse.append("\n\n");
                    }
                    fullResponse.append(errorMessage);
                    turnResponses.add(errorMessage);

                    // 更新任务状态为错误
                    taskStatus.setStatus("FAILED");
                    taskStatus.setErrorMessage(e.getMessage());
                    taskStatus.setCurrentAction("执行出错: " + e.getMessage());
                    break;
                }
            }

            long totalDuration = System.currentTimeMillis() - conversationStartTime;
            logger.info("连续对话经过 {} 轮 结束，耗时 {}ms. 结束原因: {}",
                    turnCount, totalDuration, stopReason);

            // 创建结果对象
            ConversationResult result = new ConversationResult(
                    fullResponse.toString(),
                    turnResponses,
                    workingHistory,
                    turnCount,
                    turnCount >= MAX_TURNS,
                    stopReason,
                    totalDuration
            );

            // 更新任务状态为完成
            taskStatus.setStatus("COMPLETED");
            taskStatus.setCurrentAction("对话完成");
            String summary = String.format("对话完成，共 %d 轮，耗时 %.1f 秒",
                    turnCount, totalDuration / 1000.0);
            if (stopReason != null) {
                summary += "，停止原因: " + stopReason;
            }
            taskStatus.setSummary(summary);

            // 存储结果到任务状态中
            storeConversationResult(taskId, result);

            // 推送任务完成事件
            logStreamService.pushTaskComplete(taskId);

        } catch (Exception e) {
            // 处理整个对话过程中的异常
            logger.error("Fatal error in continuous conversation: {}", e.getMessage(), e);
            taskStatus.setStatus("FAILED");
            taskStatus.setErrorMessage("Fatal error: " + e.getMessage());
            taskStatus.setCurrentAction("执行失败");
            throw e;
        } finally {
            // 清理任务上下文
            TaskContextHolder.clearCurrentTaskId();
        }
    }

    /**
     * 执行单轮对话
     */
    private TurnResult executeSingleTurn(List<Message> conversationHistory, int turnNumber) {
        long turnStartTime = System.currentTimeMillis();
        try {
            logger.info("执行第 {} 轮对话，历史消息数量: {}", turnNumber, conversationHistory.size());

            // 调用AI
            ChatResponse response = chatClient.prompt()
                    .messages(conversationHistory)
                    .call()
                    .chatResponse();

            // 处理响应
            String responseText = Optional.ofNullable(response)
                    .map(ChatResponse::getResult)
                    .map(Generation::getOutput)
                    .map(AssistantMessage::getText)
                    .orElse(null);

            long turnDuration = System.currentTimeMillis() - turnStartTime;
            logger.debug("Turn {} completed in {}ms, response length: {} characters",
                    turnNumber, turnDuration, responseText != null ? responseText.length() : 0);

            return new TurnResult(true, responseText, null);

        } catch (Exception e) {
            long turnDuration = System.currentTimeMillis() - turnStartTime;
            logger.error("Failed to execute turn {} after {}ms: {}", turnNumber, turnDuration, e.getMessage(), e);
            return new TurnResult(false, null, e.getMessage());
        }
    }

    /**
     * 判断是否应该继续对话，添加LLM智能判断
     */
    private boolean shouldContinueConversation(List<Message> conversationHistory, int turnCount, String lastResponse) {
        long startTime = System.currentTimeMillis();

        // 达到最大轮数
        if (turnCount >= MAX_TURNS) {
            logger.debug("Reached maximum turns ({}), stopping conversation", MAX_TURNS);
            return false;
        }

        // 响应为空
        if (lastResponse == null || lastResponse.trim().isEmpty()) {
            logger.debug("Empty response, stopping conversation");
            return false;
        }

        // 优化：首先使用增强的内容分析判断
        boolean contentSuggestsContinue = nextSpeakerService.shouldContinueBasedOnContent(lastResponse);
        logger.debug("Content analysis result: {}", contentSuggestsContinue);

        // 如果内容分析明确建议停止，直接停止（避免LLM调用）
        if (!contentSuggestsContinue) {
            logger.debug("Content analysis suggests stopping, skipping LLM check");
            return false;
        }

        // 优化：对于文件编辑等明确的工具调用场景，可以基于简单规则继续
        if (isObviousToolCallScenario(lastResponse, turnCount)) {
            logger.debug("Obvious tool call scenario detected, continuing without LLM check");
            return true;
        }

        // 只有在不确定的情况下才使用智能判断服务（包含LLM调用）
        try {
            NextSpeakerResponse nextSpeaker =
                    nextSpeakerService.checkNextSpeaker(conversationHistory);

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Next speaker check completed in {}ms, result: {}", duration, nextSpeaker);

            return nextSpeaker.isModelNext();

        } catch (Exception e) {
            logger.warn("Failed to check next speaker, defaulting to stop: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查是否是明显的工具调用场景
     */
    private boolean isObviousToolCallScenario(String lastResponse, int turnCount) {
        if (lastResponse == null) return false;

        String lowerResponse = lastResponse.toLowerCase();

        // 如果是前几轮且包含工具调用成功的标志，很可能需要继续
        if (turnCount <= 10) {
            String[] toolCallIndicators = {
                    "successfully created",
                    "successfully updated",
                    "file created",
                    "file updated",
                    "✅",
                    "created file",
                    "updated file",
                    "next, i'll",
                    "now i'll",
                    "let me create",
                    "let me edit"
            };

            for (String indicator : toolCallIndicators) {
                if (lowerResponse.contains(indicator)) {
                    // 但如果同时包含明确的完成信号，则不继续
                    String[] completionSignals = {
                            "all files created", "project complete", "setup complete",
                            "everything is ready", "task completed", "all done"
                    };

                    boolean hasCompletionSignal = false;
                    for (String signal : completionSignals) {
                        if (lowerResponse.contains(signal)) {
                            hasCompletionSignal = true;
                            break;
                        }
                    }

                    if (!hasCompletionSignal) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 获取继续对话的提示语
     */
    private String getContinuePrompt(int turnNumber) {
        int index = (turnNumber - 1) % CONTINUE_PROMPTS.length;
        return CONTINUE_PROMPTS[index];
    }

}

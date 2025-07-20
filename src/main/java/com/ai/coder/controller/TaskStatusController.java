package com.ai.coder.controller;

import com.ai.coder.dto.ConversationResultDTO;
import com.ai.coder.dto.TaskStatusDTO;
import com.ai.coder.model.ConversationResult;
import com.ai.coder.model.TaskStatus;
import com.ai.coder.service.ContinuousConversationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/task")
public class TaskStatusController {

    private final ContinuousConversationService conversationService;

    public TaskStatusController(ContinuousConversationService conversationService) {
        this.conversationService = conversationService;
    }

    /**
     * 获取任务状态
     */
    @GetMapping("/status/{taskId}")
    public Mono<TaskStatusDTO> getTaskStatus(@PathVariable("taskId") String taskId) {
        return Mono.fromCallable(() -> {
            TaskStatus status = conversationService.getTaskStatus(taskId);
            if (status == null) {
                throw new RuntimeException("Task not found: " + taskId);
            }

            TaskStatusDTO dto = new TaskStatusDTO();
            dto.setTaskId(status.getTaskId());
            dto.setStatus(status.getStatus());
            dto.setCurrentAction(status.getCurrentAction());
            dto.setSummary(status.getSummary());
            dto.setCurrentTurn(status.getCurrentTurn());
            dto.setTotalEstimatedTurns(status.getTotalEstimatedTurns());
            dto.setProgressPercentage(status.getProgressPercentage());
            dto.setElapsedTime(status.getElapsedTime());
            dto.setErrorMessage(status.getErrorMessage());

            return dto;
        });
    }

    /**
     * 获取对话结果
     */
    @GetMapping("/result/{taskId}")
    public Mono<ConversationResultDTO> getConversationResult(@PathVariable("taskId") String taskId) {
        return Mono.fromCallable(() -> {
            ConversationResult result = conversationService.getConversationResult(taskId);
            if (result == null) {
                throw new RuntimeException("Conversation result not found: " + taskId);
            }

            ConversationResultDTO dto = new ConversationResultDTO();
            dto.setTaskId(taskId);
            dto.setFullResponse(result.getFullResponse());
            dto.setTurnResponses(result.getTurnResponses());
            dto.setTotalTurns(result.getTotalTurns());
            dto.setReachedMaxTurns(result.isReachedMaxTurns());
            dto.setStopReason(result.getStopReason());
            dto.setTotalDurationMs(result.getTotalDurationMs());

            return dto;
        });
    }

}

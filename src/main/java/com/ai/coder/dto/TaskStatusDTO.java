package com.ai.coder.dto;

import lombok.Data;

@Data
public class TaskStatusDTO {
    private String taskId;
    private String status;
    private String currentAction;
    private String summary;
    private int currentTurn;
    private int totalEstimatedTurns;
    private double progressPercentage;
    private long elapsedTime;
    private String errorMessage;

}